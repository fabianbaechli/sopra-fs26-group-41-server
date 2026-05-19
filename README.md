<div align="center">
  <img src="public/logo.png" alt="Movieblendr logo" width="140" />
  <h1>Movieblendr.</h1>
  <p><strong>Find the movie your whole group actually wants to watch.</strong></p>
  <p>
    <a href="https://github.com/fabianbaechli/sopra-fs26-group-41-client">Client</a> ·
    <a href="https://github.com/fabianbaechli/sopra-fs26-group-41-server">Server</a>
  </p>
  <p>
    <a href="https://github.com/fly-die"><img src="https://wsrv.nl/?url=https://github.com/fly-die.png&mask=circle&w=60&h=60" width="60" alt="fly-die" /></a>
    <a href="https://github.com/fabianbaechli"><img src="https://wsrv.nl/?url=https://github.com/fabianbaechli.png&mask=circle&w=60&h=60" width="60" alt="fabianbaechli" /></a>
    <a href="https://github.com/BB8006"><img src="https://wsrv.nl/?url=https://github.com/BB8006.png&mask=circle&w=60&h=60" width="60" alt="BB8006" /></a>
    <a href="https://github.com/EmritoHeltar"><img src="https://wsrv.nl/?url=https://github.com/EmritoHeltar.png&mask=circle&w=60&h=60" width="60" alt="EmritoHeltar" /></a>
  </p>
</div>

---

## What is this?

Picking a movie with a group is painful. Everyone has different taste, nobody commits, and you end up rewatching something you've all seen. Movieblendr fixes that.

Each member connects their [Letterboxd](https://letterboxd.com) history to build a personal taste profile. The app runs a recommendation algorithm across the whole group, surfaces movies everyone is likely to enjoy, and lets the group vote on a shortlist together in real time. No more endless discussions!

---

# Movieblendr. Backend

Backend service for a movie-night planning application. The server handles user accounts, authentication, Letterboxd data import, movie search, taste-overlap calculations, group management, group recommendations, polls, and collaborative drawing sessions. It exposes a Spring Boot REST API and WebSocket endpoint used by the client application, and it delegates recommendation-calculations to a small Python recommendation service.

## Technologies used

- Java 17
- Spring Boot with Spring Web MVC, Spring Data JPA, Spring Security, and WebSocket support
- Gradle
- H2 in-memory database
- JUnit/SonarCloud for testing and quality checks
- Python, Flask, and SQLite for the recommendation service
- Docker, GitHub Actions, Google App Engine Flex, and Google Cloud Run for deployment

## High-level components
### 1. Authentication and users

[`RegistrationController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/RegistrationController.java), [`AuthenticationController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/AuthenticationController.java), [`UsersController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/UsersController.java), [`UserService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/UserService.java), and [`PasswordService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/PasswordService.java) implement registration, login, logout, token validation, password hashing, and user-profile retrieval. Authenticated requests use a bearer token in the `Authorization` header. Passwords are hashed using `BCrypt`.

### 2. Movie search, Letterboxd import, and taste profiles

[`MovieSearchController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/MovieSearchController.java), [`MovieSearchService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/MovieSearchService.java), [`LetterboxdImportController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/LetterboxdImportController.java), and [`LetterboxdImportService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/LetterboxdImportService.java) connect the app to movie data using OMDb API. The backend can search movies, fetch movie details, import a user's Letterboxd export zip, map imported letterboxd titles to OMDb titles, store rated movies, and compute taste-overlap values.

### 3. Groups, recommendations, and polls

[`GroupController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/GroupController.java), [`GroupService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/GroupService.java), [`PollService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/PollService.java), and [`PollBroadcastService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/PollBroadcastService.java) manage movie groups. Users can create or join groups through join tokens, leave groups, request group recommendations, start a poll from the recommended movies, vote, and retrieve poll results. Poll events are also broadcast to users of a group over WebSockets.

### 4. Realtime WebSocket and collaborative drawing

[`WebSocketConfig`](src/main/java/ch/uzh/ifi/hase/soprafs26/config/WebSocketConfig.java), [`AppWebSocketHandler`](src/main/java/ch/uzh/ifi/hase/soprafs26/websocket/AppWebSocketHandler.java), [`AuthHandshakeInterceptor`](src/main/java/ch/uzh/ifi/hase/soprafs26/websocket/AuthHandshakeInterceptor.java), and [`DrawingService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/DrawingService.java) provide the realtime layer. The `/ws` endpoint authenticates users during the handshake, tracks sessions per username, routes drawing messages, broadcasts strokes and presence updates, and allows a group's drawing to be saved as the group profile picture.

### 5. Python Recommendation Microservice
[`recommendation-service/app.py`](recommendation-service/app.py) is a python microservice which provides the main recommendation functionality of the backend. The Python script is responsible for computing movie recommendations. The recommendation service interacts with a local SQLite database that contains a graph built from the MovieLens dataset. This graph is based on more than 30 million movie reviews and around 200,000 users. Instead of storing only plain movie and rating tables, the database represents relationships between movies, and ratings in a graph structure that can be queried by the recommendation script.The SQLite database file is larger than 70 GB. Because of its size, it is not included in this repository. Developers who want to run the recommendation microservice locally need to obtain or generate the database separately and configure the script to point to the local database file.

## Getting started

### Prerequisites

Install the following tools:

- Java 17
- Git
- Python 3.11, only if you run the recommendation service locally

### Clone the repository

```bash
git clone git@github.com:fabianbaechli/sopra-fs26-group-41-server.git
cd sopra-fs26-group-41-server
```

### Configure environment variables

For local development, create a `local.properties` file or export the variables in your shell:

```bash
export OMDB_API_KEY=<your-omdb-api-key>
export REMOTE_SERVICE_URL=http://localhost:8081
```
`OMDB_API_KEY` is required for OMDb-backed movie details. A free key can be requested here [here](https://www.omdbapi.com/apikey.aspx) by providing your email address. `REMOTE_SERVICE_URL` points the Spring backend to the python recommendation service. If you do not run the Python service locally, set this variable to a deployed recommendation-service URL: `https://recommendation-service-522822317054.europe-west6.run.app`

The Spring backend uses an H2 in-memory database by default. You can inspect it while the server is running at:

```text
http://localhost:8080/h2-console
```

Use the following local credentials:

```text
JDBC URL: jdbc:h2:mem:testdb
User: sa
Password: <empty>
```

## Running locally

### Start the Spring backend

```bash
./gradlew bootRun
```

The backend starts on:

```text
http://localhost:8080
```

### Optional: Build local database
See appendix

### Start the recommendation service locally

The recommendation service expects SQLite database files:
- `movies_catalog.db` is included in the backend repository.
- `movie_connections.db` and `movie_edges.db` are not provided in the repository. Provide them locally by running the previous section or point `REMOTE_SERVICE_URL` to the deployed service.

```bash
cd recommendation-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

export CATALOG_DB_PATH=../movies_catalog.db
export DATABASE_PATH=../movie_connections.db
export EDGES_DB_PATH=../movie_edges.db

gunicorn --bind 0.0.0.0:8081 --workers 1 --threads 8 --timeout 0 app:app
```

The recommendation service then runs on:

```text
http://localhost:8081
```

## Building

Build the backend with Gradle:

```bash
./gradlew build
```

Create the executable Spring Boot jar:

```bash
./gradlew bootJar
```

The jar is written to:

```text
./build/libs/soprafs26.jar
```

Run it via: `java -jar ./build/libs/soprafs26.jar`

## Running tests

Run all automated tests:

```bash
./gradlew test
```

Run tests and generate the Jacoco report:

```bash
./gradlew test jacocoTestReport
```

## Deployment

### GitHub Actions

This repository contains two deployment-related workflows:

- [`.github/workflows/main.yml`](.github/workflows/main.yml): runs tests/SonarCloud analysis and deploys the Spring backend to Google App Engine on pushes to `main`.
- [`.github/workflows/dockerize.yml`](.github/workflows/dockerize.yml): builds and pushes a Docker image to Docker Hub.

The workflows expect these repository secrets:

```text
SONAR_TOKEN
SONAR_PROJECT_KEY
SONAR_ORGANIZATION
OMDB_API_KEY
GCP_SERVICE_CREDENTIALS
dockerhub_username
dockerhub_password
dockerhub_repo_name
```
Add them in github under Repository Settings -> Secrets and Variables -> Actions

### Google App Engine

[`app.yaml`](app.yaml) deploys the backend to the Java 17 flexible environment and configures:

```yaml
REMOTE_SERVICE_URL: "https://recommendation-service-522822317054.europe-west6.run.app"
OMDB_API_KEY: "OMDB_API_KEY_PLACEHOLDER"
```

During deployment, the workflow replaces `OMDB_API_KEY_PLACEHOLDER` with the `OMDB_API_KEY` GitHub secret and then deploys using `google-github-actions/deploy-appengine`.

## Google Cloud Run

The recommendation-service folder is run in a google cloud run enviroment. The cloud run has access to a bucket that contains all the .db files

## Roadmap

- Make recommendation algorithm more sophisticated. Biggest issue at the moment is that the service doesn't take individual popularity preference of users into consideration. This results in users who typically like popular movies getting recommendations for niche movies and vice versa.
- Mark movies as seen. After a user has uploaded their letterboxd data, the application may recommend movies that they have seen in the meantime. Since it's not easily possible to directly communicate with the letterboxd api, the service should provide the functionality to mark movies as seen so that they aren't recommended anymore. Note that this problem can be dealt with by just exporting the data from letterboxd a second time and updating it in the profile.
- Provide progress updates on long running actions like getting recommendations for a group. For groups that have tracked many movies, getting the recommendations takes a few minutes. Providing them with a progress update would make the wait time less cumbersome.

## Authors and acknowledgment

- Fabian Bächli
- Emre Timur Halter
- Flynn Diener
- Benjamin Boksberger

This project was developed for the SoPra FS26 course at the University of Zurich. Thanks to the teaching team for the Spring Boot server template and project guidance.

## Appendix

If you wish to build the `movies_connections.db` `movie_edges.db` and `movies_catalog.db` you need to run the python programs in the builder folder in the following order:
1. `Json_builder.py`
2. `Movie_connections_builder.py`
3. `Movie_edges_builder.py`
4. `Movie_catalog_builder.py`

> [!IMPORTANT] 
> If you do not have a lot of RAM and a fast processor we do not recommend trying to build these on your own. It will take a long time and totally occupy all of your computers Memory.

## License

This project is licensed under the Apache License 2.0. See [`LICENSE`](LICENSE) for details.
