import sqlite3
import os

# Define paths (adjust if yours are different)
CONNECTIONS_DB = "movie_connections.db"
EDGES_DB = "movie_edges.db"

def build_edges():
    print(f"Connecting to {CONNECTIONS_DB}...")
    conn = sqlite3.connect(CONNECTIONS_DB)
    

    if os.path.exists(EDGES_DB):
        os.remove(EDGES_DB)
        
    print(f"Attaching {EDGES_DB}...")

    conn.execute(f"ATTACH DATABASE '{EDGES_DB}' AS edges_db")
    
    print("Aggregating edge counts.")

    sql = """
        CREATE TABLE edges_db.movie_edge_counts AS
        SELECT movie_id, SUM(cnt) as edge_count
        FROM (
            SELECT movie_a AS movie_id, COUNT(movie_b) as cnt 
            FROM connections 
            GROUP BY movie_a
            
            UNION ALL
            
            SELECT movie_b AS movie_id, COUNT(movie_a) as cnt 
            FROM connections 
            GROUP BY movie_b
        )
        GROUP BY movie_id;
    """
    conn.execute(sql)
    
    print("Creating an index")
    conn.execute("CREATE UNIQUE INDEX edges_db.idx_movie_edge_counts_id ON movie_edge_counts(movie_id);")
    
    conn.commit()
    conn.close()
    print(f"Success! '{EDGES_DB}' has been created.")

if __name__ == "__main__":
    build_edges()