import sqlite3
import json

def build_movie_database(jsonl_file="final_movie_graph.jsonl", db_file="movie_connections.db"):
    print(f"Creating database {db_file}...")
    

    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS connections (
            movie_a INTEGER,
            movie_b INTEGER,
            rating_sum INTEGER,
            count INTEGER
        )
    ''')
    

    cursor.execute('DELETE FROM connections') 
    

    batch_size = 500_000 
    current_batch = []
    lines_processed = 0
    
    print(f"Reading {jsonl_file} and inserting into database...")
    print("-" * 50)
    
    with open(jsonl_file, 'r', encoding='utf-8') as f:
        for line in f:
            data = json.loads(line)
            
            for edge_id_str, stats in data.items():
                edge_id = int(edge_id_str)
                rating_sum = stats[0]
                count = stats[1]
                

                movie_a = edge_id >> 32
                movie_b = edge_id & 0xFFFFFFFF
                
                current_batch.append((movie_a, movie_b, rating_sum, count))
                lines_processed += 1
                

            if len(current_batch) >= batch_size:
                cursor.executemany('''
                    INSERT INTO connections (movie_a, movie_b, rating_sum, count)
                    VALUES (?, ?, ?, ?)
                ''', current_batch)
                conn.commit()
                print(f"  ... inserted {lines_processed:,} edges ...")
                current_batch = []
                

        if current_batch:
            cursor.executemany('''
                INSERT INTO connections (movie_a, movie_b, rating_sum, count)
                VALUES (?, ?, ?, ?)
            ''', current_batch)
            conn.commit()

    print("-" * 50)
    print("Data insertion complete. Now building the database indexes")
    

    cursor.execute('CREATE INDEX idx_movie_a ON connections(movie_a)')
    cursor.execute('CREATE INDEX idx_movie_b ON connections(movie_b)')
    conn.commit()
    

    conn.close()
    print("Database built successfully!")

if __name__ == "__main__":
    build_movie_database()