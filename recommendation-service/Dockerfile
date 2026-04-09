FROM python:3.11-slim

WORKDIR /app

# Install system dependencies for SQLite
RUN apt-get update && apt-get install -y sqlite3 libsqlite3-dev

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

CMD ["python", "app.py"]