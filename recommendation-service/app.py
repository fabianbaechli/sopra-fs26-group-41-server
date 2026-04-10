from flask import Flask, request, jsonify
import sqlite3
import os
import logging
import re
from urllib.parse import unquote

app = Flask(__name__)

# 1. Define BOTH database paths, allowing environment variable overrides
DB_PATH = os.environ.get('DATABASE_PATH', '../movie_connections.db')
CATALOG_DB_PATH = os.environ.get('CATALOG_DB_PATH', '../movies_catalog.db')

def get_db_connection():
    conn = sqlite3.connect(f"file:{DB_PATH}?mode=ro", uri=True)
    return conn

@app.route('/calculate-overlap', methods=['POST'])
def calculate_overlap():
    data = request.get_json()
    watched_ids = data.get('watched_ids', [])
    target_movie_id = data.get('target_movie_id')
    app.logger.info(f"Search results for {target_movie_id}")

    if not watched_ids or not target_movie_id:
        return jsonify({"score": 0.0})

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        app.logger.info(f"Watched: {watched_ids}  Target: {target_movie_id}")
        placeholders = ','.join(['?'] * len(watched_ids))
        query = f"""
            SELECT SUM(rating_sum), SUM(count)
            FROM connections
            WHERE (movie_a = ? AND movie_b IN ({placeholders}))
               OR (movie_b = ? AND movie_a IN ({placeholders}))
        """
        
        params = [target_movie_id] + watched_ids + [target_movie_id] + watched_ids
        cursor.execute(query, params)
        row = cursor.fetchone()
        rating_sum = row[0] if row[0] else 0
        total_count = row[1] if row[1] else 0

        if total_count == 0:
            return jsonify({"target_id": target_movie_id, "overlap_score": 0.0})

        avg_combined = rating_sum / total_count

        MIN_POSSIBLE = 20.0 
        MAX_POSSIBLE = 100.0
        
        raw_score = (avg_combined - MIN_POSSIBLE) / (MAX_POSSIBLE - MIN_POSSIBLE)
        raw_score = max(0.0, min(1.0, raw_score)) 

        VIEWS_THRESHOLD = 50.0 
        confidence_factor = min(1.0, total_count / VIEWS_THRESHOLD)

        final_score = raw_score * confidence_factor
        app.logger.info(f"Search results for {target_movie_id}: {final_score}")

        conn.close()
        return jsonify({
            "target_id": target_movie_id, 
            "overlap_score": round(final_score, 4)
        })
        

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# 2. Expose this function via a GET route and return JSON
@app.route('/movie/<int:movie_id>', methods=['GET'])
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
@app.route('/movie/search', methods=['GET'])
def search_movies_by_name():
    search_term = request.args.get('q', '')
    
    if not search_term:
        return jsonify([])

    # 1. Decode any lingering URL encoding (fixes the literal '%20' issue)
    decoded_term = unquote(search_term)

    # 2. Strip out FTS5 reserved characters (keep only letters, numbers, and spaces)
    clean_term = re.sub(r'[^\w\s]', ' ', decoded_term)

    # 3. Split into words and ignore empty strings
    words = clean_term.split()
    
    if not words:
        return jsonify([])

    # 4. Append wildcard to each word for a robust partial match (e.g., "Airpl* II*")
    query_term = ' '.join([f"{word}*" for word in words])
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

# 4. Bulk search movies by a list of names
@app.route('/movie/search/bulk', methods=['POST'])
def search_movies_bulk():
    data = request.get_json()
    
    # Safely check if data exists and contains our list
    if not data or 'names' not in data:
        return jsonify({"error": "Please provide a JSON body with a 'names' array."}), 400
        
    search_terms = data.get('names', [])
    if not search_terms:
        return jsonify({})

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
            
    results_dict = {}

    for search_term in search_terms:
        # Re-use your existing parsing logic for each term
        decoded_term = unquote(search_term)
        clean_term = re.sub(r'[^\w\s]', ' ', decoded_term)
        words = clean_term.split()
        
        if not words:
            results_dict[search_term] = []
            continue

        query_term = ' '.join([f"{word}*" for word in words])
        
        try:
            cursor.execute(query, (query_term,))
            results_dict[search_term] = [dict(row) for row in cursor.fetchall()]
        except Exception as e:
            app.logger.error(f"FTS Search Error on query '{query_term}': {e}")
            results_dict[search_term] = []

    conn.close()
    
    # Returns a dictionary where keys are the requested movie names 
    # and values are the list of search results
    return jsonify(results_dict)


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 8081)))