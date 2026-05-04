from flask import Flask, request, jsonify
import sqlite3
import os
import logging
import re
from urllib.parse import unquote

app = Flask(__name__)

# 1. Define BOTH database paths, allowing environment variable overrides
DB_PATH = os.environ.get("DATABASE_PATH", "../movie_connections.db")
CATALOG_DB_PATH = os.environ.get("CATALOG_DB_PATH", "../movies_catalog.db")
EDGES_DB_PATH = os.environ.get("EDGES_DB_PATH", "../movie_edges.db")


def get_db_connection():
  conn = sqlite3.connect(f"file:{DB_PATH}?mode=ro", uri=True)
  conn.execute(f"ATTACH DATABASE 'file:{EDGES_DB_PATH}?mode=ro' AS edges")
  return conn


@app.route("/calculate-overlap", methods=["POST"])
def calculate_overlap():
    data = request.get_json()
    # Now expects a dictionary: { "movie_id": rating }
    watched_ratings = data.get("watchedRatings", data.get("watched_ratings", {}))
    print(watched_ratings)
    target_movie_id = data.get("target_movie_id")
    app.logger.info(f"Search results for {target_movie_id}")

    if not watched_ratings or not target_movie_id:
        return jsonify({"score": 0.0})

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Build the dynamic VALUES clause for the CTE
        values_placeholders = ", ".join(["(?, ?)"] * len(watched_ratings))
        cte_params = []
        for mid, rating in watched_ratings.items():
            cte_params.extend([str(mid), float(rating)])

        query = f"""
            WITH user_ratings(movie_id, rating) AS (
                VALUES {values_placeholders}
            )
            SELECT SUM(c.rating_sum * (u.rating / 5.0)), SUM(c.count * (u.rating / 5.0))
            FROM connections c
            JOIN user_ratings u ON u.movie_id = c.movie_a OR u.movie_id = c.movie_b
            WHERE (c.movie_a = ? AND c.movie_b = u.movie_id)
               OR (c.movie_b = ? AND c.movie_a = u.movie_id)
        """
        
        params = cte_params + [target_movie_id, target_movie_id]
        cursor.execute(query, params)
        row = cursor.fetchone()
        
        adjusted_rating_sum = row[0] if row[0] else 0
        adjusted_total_count = row[1] if row[1] else 0

        if adjusted_total_count == 0:
            return jsonify({"target_id": target_movie_id, "overlap_score": 0.0})

        avg_combined = adjusted_rating_sum / adjusted_total_count

        MIN_POSSIBLE = 20.0
        MAX_POSSIBLE = 100.0

        raw_score = (avg_combined - MIN_POSSIBLE) / (MAX_POSSIBLE - MIN_POSSIBLE)
        raw_score = max(0.0, min(1.0, raw_score))

        VIEWS_THRESHOLD = 50.0
        confidence_factor = min(1.0, adjusted_total_count / VIEWS_THRESHOLD)

        final_score = raw_score * confidence_factor
        
        conn.close()
        return jsonify(
            {"target_id": target_movie_id, "overlap_score": round(final_score, 4)}
        )

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# 2. Expose this function via a GET route and return JSON
@app.route("/movie/<int:movie_id>", methods=["GET"])
def get_movie_by_id(movie_id):
  conn = sqlite3.connect(CATALOG_DB_PATH)
  conn.row_factory = sqlite3.Row
  cursor = conn.cursor()

  cursor.execute("SELECT id, title, year, genres FROM movies WHERE id = ?", (movie_id,))
  row = cursor.fetchone()
  conn.close()

  if row:
    return jsonify(dict(row))
  return jsonify({"error": "Movie not found"}), 404


# 3. Expose this function via a GET route, read the search query from the URL, and return JSON
@app.route("/movie/search", methods=["GET"])
def search_movies_by_name():
  search_term = request.args.get("q", "")

  if not search_term:
    return jsonify([])

  # 1. Decode any lingering URL encoding (fixes the literal '%20' issue)
  decoded_term = unquote(search_term)

  # 2. Strip out FTS5 reserved characters (keep only letters, numbers, and spaces)
  clean_term = re.sub(r"[^\w\s]", " ", decoded_term)

  # 3. Split into words and ignore empty strings
  words = clean_term.split()

  if not words:
    return jsonify([])

  # 4. Append wildcard to each word for a robust partial match (e.g., "Airpl* II*")
  query_term = " ".join([f"{word}*" for word in words])
  app.logger.info(f"Moviename searched {query_term}")

  conn = sqlite3.connect(CATALOG_DB_PATH)
  conn.row_factory = sqlite3.Row
  cursor = conn.cursor()

  query = """
            SELECT m.id, m.title, m.year, m.genres
            FROM movies m
            JOIN movies_fts fts ON m.id = fts.rowid
            WHERE movies_fts MATCH ?
            ORDER BY fts.rank
            LIMIT 10
            """

  try:
    cursor.execute(query, (query_term,))
    results = [dict(row) for row in cursor.fetchall()]
  except Exception as e:
    app.logger.error(f"FTS Search Error on query '{query_term}': {e}")
    results = []
  finally:
    conn.close()

  return jsonify(results)


@app.route("/movie/search/bulk", methods=["POST"])
def search_movies_bulk():
  data = request.get_json()
  if not data or "movies" not in data:
    return jsonify({"error": "Missing 'movies' array"}), 400

  search_requests = data.get("movies", [])
  results_dict = {}

  conn = sqlite3.connect(CATALOG_DB_PATH)
  conn.row_factory = sqlite3.Row
  cursor = conn.cursor()

  for item in search_requests:
    name = item.get("name")
    year = item.get("year")
    
    if not name:
        continue

    # Prepare search term
    decoded_term = unquote(name)
    clean_term = re.sub(r"[^\w\s]", " ", decoded_term)
    words = clean_term.split()
    if not words:
      results_dict[name] = []
      continue
    query_term = " ".join([f"{word}*" for word in words])

    # Base query components
    base_sql = "SELECT m.id, m.title, m.year, m.genres FROM movies m JOIN movies_fts fts ON m.id = fts.rowid WHERE movies_fts MATCH ?"
    order_limit = " ORDER BY fts.rank LIMIT 10"

    results = []

    # --- LAYER 1: Strict Match (Name + Exact Year) ---
    if year:
        try:
            cursor.execute(base_sql + " AND m.year = ?" + order_limit, [query_term, year])
            results = [dict(row) for row in cursor.fetchall()]
        except Exception as e:
            app.logger.error(f"Strict search error for {name}: {e}")

    # --- LAYER 2: Fuzzy Year Match (Name + Year +/- 1) ---
    # Try this if strict match failed but we have a year
    if not results and year:
        try:
            cursor.execute(base_sql + " AND m.year BETWEEN ? AND ?" + order_limit, [query_term, year - 1, year + 1])
            results = [dict(row) for row in cursor.fetchall()]
        except Exception as e:
            app.logger.error(f"Fuzzy year search error for {name}: {e}")

    # --- LAYER 3: Name Only (Final Fallback) ---
    # Try this if we still have no results or if no year was provided
    if not results:
        try:
            cursor.execute(base_sql + order_limit, [query_term])
            results = [dict(row) for row in cursor.fetchall()]
        except Exception as e:
            app.logger.error(f"Loose search error for {name}: {e}")

    results_dict[name] = results

  conn.close()
  return jsonify(results_dict)




@app.route("/recommend", methods=["POST"])
def recommend_movies():
    print("getting group recommendations")
    data = request.get_json()
    if not data:
        return jsonify({"error": "Invalid JSON payload"}), 400

    watched_ratings = data.get("watchedRatings", data.get("watched_ratings", {}))
    limit = int(data.get("limit", 10))
    offset = int(data.get("offset", 0))

    # --- NEW CONFIGURABLE VARIABLES ---
    # The maximum debuff applied to heavily connected movies (0.5 means 50% max penalty)
    max_debuff = float(data.get("max_popularity_debuff", 0.1))
    # The threshold of edges at which the max debuff is applied
    edge_threshold = float(data.get("popularity_edge_threshold", 1408.0))

    if not watched_ratings:
        return jsonify([])

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        values_placeholders = ", ".join(["(?, ?)"] * len(watched_ratings))
        cte_params = []
        for mid, rating in watched_ratings.items():
            cte_params.extend([str(mid), float(rating)])

        # --- 1. CTE Definitions ---
        # `user_ratings` holds EVERYTHING to ensure they are excluded from final recommendations.
        # `weighted_user_ratings` excludes <= 3.0 stars and counts their edges dynamically.
        cte_sql = f"""
            WITH user_ratings(movie_id, rating) AS (
                VALUES {values_placeholders}
            ),
            weighted_user_ratings AS (
                SELECT 
                    u.movie_id, 
                    u.rating,
                    COALESCE(e.edge_count, 0) AS edge_count
                FROM user_ratings u
                LEFT JOIN edges.movie_edge_counts e ON u.movie_id = e.movie_id
                WHERE u.rating > 3.0
            ),
            candidate_edges AS (
                SELECT 
                    c.movie_b AS candidate_id,
                    c.rating_sum,
                    c.count,
                    u.rating,
                    u.edge_count
                FROM connections c
                JOIN weighted_user_ratings u ON c.movie_a = u.movie_id
                
                UNION ALL
                
                SELECT 
                    c.movie_a AS candidate_id,
                    c.rating_sum,
                    c.count,
                    u.rating,
                    u.edge_count
                FROM connections c
                JOIN weighted_user_ratings u ON c.movie_b = u.movie_id
            )
        """

        # --- 2. Calculate Recommendations ---
        query = f"""
            {cte_sql}
            SELECT
                candidate_id,
                (
                   MAX(0.0, MIN(1.0, ((SUM(adjusted_rating_sum) * 1.0 / SUM(adjusted_count)) - 20.0) / 80.0))
                   * MIN(1.0, SUM(adjusted_count) * 1.0 / 50.0)
                ) as overlap_score
            FROM (
                SELECT
                    candidate_id,
                    rating_sum * (rating / 5.0) * (1.0 - (MIN(edge_count, {edge_threshold}) / {edge_threshold}) * {max_debuff}) AS adjusted_rating_sum,
                    count * (rating / 5.0) * (1.0 - (MIN(edge_count, {edge_threshold}) / {edge_threshold}) * {max_debuff}) AS adjusted_count
                FROM candidate_edges
            )
            GROUP BY candidate_id
            HAVING candidate_id NOT IN (SELECT movie_id FROM user_ratings)
            AND SUM(adjusted_count) >= 500
            ORDER BY overlap_score DESC
            LIMIT ? OFFSET ?
        """

        params = cte_params + [limit, offset] 
        cursor.execute(query, params)
        results = cursor.fetchall()
        conn.close()

        if not results:
            return jsonify([])

        # --- 3. Fetch Titles from the Catalog DB ---
        rec_ids = [row[0] for row in results]
        catalog_conn = sqlite3.connect(f"file:{CATALOG_DB_PATH}?mode=ro", uri=True)
        catalog_cursor = catalog_conn.cursor()

        id_placeholders = ",".join(["?"] * len(rec_ids))
        catalog_cursor.execute(f"SELECT id, title FROM movies WHERE id IN ({id_placeholders})", rec_ids)

        titles_map = {row[0]: row[1] for row in catalog_cursor.fetchall()}
        catalog_conn.close()

        # --- 4. Build Final Output ---
        recommendations = [
            {
                "movie_id": str(row[0]),
                "title": titles_map.get(row[0], "Unknown Title"),
                "overlap_score": round(row[1], 4),
            }
            for row in results
        ]
        return jsonify(recommendations)

    except Exception as e:
        app.logger.error(f"Error calculating recommendations: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/user-overlap", methods=["POST"])
def calculate_user_overlap():
    data = request.get_json()
    user1 = data.get("user1_ratings", {})  # Dict of {movie_id: rating}
    user2 = data.get("user2_ratings", {})

    if not user1 or not user2:
        return jsonify({"overlap_score": 0})

    user1_ids = set(user1.keys())
    user2_ids = set(user2.keys())
    print(user1)
    shared_ids = user1_ids.intersection(user2_ids)
    union_ids = user1_ids.union(user2_ids)

    # Dynamically find the max rating used (e.g., handles both 5-star and 10-star scales)
    max_rating = float(max(max(user1.values(), default=1), max(user2.values(), default=1), 5.0))
    total_score = 0.0

    # --- 1. DIRECT OVERLAP (Shared Movies) ---
    for mid in shared_ids:
        r1 = float(user1[mid])
        r2 = float(user2[mid])
        # Penalty for rating difference
        rating_diff = abs(r1 - r2)
        match_score = max(0.0, 1.0 - (rating_diff / max_rating))
        total_score += match_score

    # --- 2. INDIRECT OVERLAP (Graph Connections) ---
    u1_unique = list(user1_ids - shared_ids)
    u2_unique = list(user2_ids - shared_ids)

    if u1_unique and u2_unique:
        conn = get_db_connection()
        cursor = conn.cursor()

        p1 = ",".join(["?"] * len(u1_unique))
        p2 = ",".join(["?"] * len(u2_unique))

        # Fetch only edges bridging the users' unique libraries
        query = f"""
            SELECT movie_a, movie_b, rating_sum, count
            FROM connections
            WHERE (movie_a IN ({p1}) AND movie_b IN ({p2}))
               OR (movie_b IN ({p1}) AND movie_a IN ({p2}))
        """
        params = u1_unique + u2_unique + u1_unique + u2_unique
        cursor.execute(query, params)
        edges = cursor.fetchall()
        conn.close()

        # To prevent score inflation, we only map the *single best* connection for each movie
        best_indirect_matches = {}

        for row in edges:
            ma, mb, r_sum, count = str(row[0]), str(row[1]), row[2], row[3]

            # Figure out which movie belongs to which user
            if ma in user1 and mb in user2:
                m1, m2 = ma, mb
            elif mb in user1 and ma in user2:
                m1, m2 = mb, ma
            else:
                continue

            # Ignore weak/noisy data
            if count < 5:
                continue

            # Calculate Edge Strength (from previous logic: bounded 0.0 to 1.0)
            avg = r_sum / count
            edge_strength = max(0.0, min(1.0, (avg - 20.0) / 80.0))

            # Factor in how highly they rated these connected movies
            r1_factor = float(user1[m1]) / max_rating
            r2_factor = float(user2[m2]) / max_rating

            indirect_match_score = edge_strength * r1_factor * r2_factor

            # Keep the highest indirect connection score for Movie 1
            if m1 not in best_indirect_matches or indirect_match_score > best_indirect_matches[m1]:
                best_indirect_matches[m1] = indirect_match_score

        # Add best indirect matches to total score
        # Weighted at 0.8 so an indirect connection is never mathematically stronger than a perfect direct match
        for score in best_indirect_matches.values():
            total_score += (score * 0.8)

    # --- 3. FINAL PERCENTAGE ---
    union_size = len(union_ids)
    if union_size == 0:
        return jsonify({"overlap_score": 0})

    # Divide our accumulated score by the total number of unique movies considered
    final_percentage = int(round((total_score / union_size) * 100))
    final_percentage = min(100, max(0, final_percentage))

    return jsonify({"overlap_score": final_percentage})

if __name__ == "__main__":
  logging.basicConfig(level=logging.DEBUG)
  app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 8081)))
