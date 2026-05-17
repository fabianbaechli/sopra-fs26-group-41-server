import csv
import sqlite3
import re

CSV_FILE = 'movies.csv' # Replace with your csv path
DB_FILE = 'movies_catalog.db'

def clean_title(raw_title):
    # 1. Extract the year
    year_match = re.search(r'\((\d{4})\)\s*$', raw_title)
    year = int(year_match.group(1)) if year_match else None
    

    clean_name = re.sub(r'\s*\(\d{4}\)\s*$', '', raw_title).strip()
    

    articles = [', The', ', A', ', An', ', Les', ', Le', ', La']
    for article in articles:
        if clean_name.endswith(article):

            clean_name = article[2:] + ' ' + clean_name[:-len(article)]
            break
            
    return clean_name, year

def build_database():
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()


    cursor.execute('''
        CREATE TABLE IF NOT EXISTS movies (
            id INTEGER PRIMARY KEY,
            title TEXT,
            year INTEGER,
            genres TEXT
        )
    ''')


    cursor.execute('''
        CREATE VIRTUAL TABLE IF NOT EXISTS movies_fts 
        USING fts5(title, content='movies', content_rowid='id')
    ''')

    print("Reading CSV and cleaning data...")
    movies_data = []
    
    with open(CSV_FILE, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        next(reader)
        
        for row in reader:
            if len(row) != 3: continue
            movie_id = int(row[0])
            raw_title = row[1]
            genres = row[2]
            
            title, year = clean_title(raw_title)
            movies_data.append((movie_id, title, year, genres))

    print(f"Inserting {len(movies_data)} records into the database...")
    cursor.executemany('INSERT OR IGNORE INTO movies (id, title, year, genres) VALUES (?, ?, ?, ?)', movies_data)
    
    # Populate the FTS table
    cursor.execute("INSERT INTO movies_fts (rowid, title) SELECT id, title FROM movies")

    conn.commit()
    conn.close()
    print("Database built successfully!")

if __name__ == '__main__':
    build_database()