FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y sqlite3 libsqlite3-dev

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Swap Flask dev server for Gunicorn
CMD ["gunicorn", "--bind", "0.0.0.0:8081", "--workers", "1", "--threads", "8", "--timeout", "0", "app:app"]