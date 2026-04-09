from flask import Flask, request, jsonify
import sqlite3
import os

app = Flask(__name__)

DB_PATH = os.environ.get('DATABASE_PATH', '../movie_connections.db')

def get_db_connection():
    conn = sqlite3.connect(f"file:{DB_PATH}?mode=ro", uri=True)
    return conn

@app.route('/calculate-overlap', methods=['POST'])
def calculate_overlap():
    data = request.get_json()
    watched_ids = data.get('watched_ids', [])
    target_movie_id = data.get('target_movie_id')

    if not watched_ids or not target_movie_id:
        return jsonify({"score": 0.0})

    try:
        conn = get_db_connection()
        cursor = conn.cursor()


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

        conn.close()
        return jsonify({
            "target_id": target_movie_id, 
            "overlap_score": round(final_score, 4)
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 8080)))