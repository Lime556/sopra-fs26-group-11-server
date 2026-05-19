
# Settlers of Catan — Game Server

This repository contains the backend services for the course project implementation of Settlers of Catan. It provides the REST API, game state management, matchmaking/lobby controllers, and persistence.

The motivation for the project was to bring players together through a seamless and engaging  online experience, making strategy and competition accessible anytime, anywhere.

## Introduction

The server runs a Spring Boot application that manages game logic, player sessions, and synchronization between clients. The default victory points target is configurable (default: 10 VP).

## Technologies

- Java 17 (system.properties)
- Spring Boot (REST controllers)
- Gradle (wrapper provided)
- Docker (Dockerfile included)

## High-level components

- **Application**: Spring Boot main class and CORS config [src/main/java/ch/uzh/ifi/hase/soprafs26/Application.java](sopra-fs26-group-11-server/src/main/java/ch/uzh/ifi/hase/soprafs26/Application.java#L1).
- **Controllers**: REST endpoints for game, lobby, and user flows e.g. [src/main/java/ch/uzh/ifi/hase/soprafs26/controller/GameController.java](sopra-fs26-group-11-server/src/main/java/ch/uzh/ifi/hase/soprafs26/controller/GameController.java#L1), [src/main/java/ch/uzh/ifi/hase/soprafs26/controller/LobbyController.java](sopra-fs26-group-11-server/src/main/java/ch/uzh/ifi/hase/soprafs26/controller/LobbyController.java#L1), [src/main/java/ch/uzh/ifi/hase/soprafs26/controller/UserController.java](sopra-fs26-group-11-server/src/main/java/ch/uzh/ifi/hase/soprafs26/controller/UserController.java#L1).
- **Services & Domain**: Business logic and domain model under `src/main/java/ch/uzh/ifi/hase/soprafs26/service` and `domain` (search the `src/main/java` tree for specific classes).
- **Persistence / Repositories**: Data access layer and in-memory or persistent stores used by the services.

## Launch & Deployment

Prerequisites: Java 17 (or higher), Docker (optional).

Run locally using the Gradle wrapper (from this folder):

```bash
./gradlew bootRun    # starts the server on default port 8080
./gradlew build      # builds the project
./gradlew test       # runs server-side tests
```

Build an executable JAR for release:

```bash
./gradlew bootJar
# then run with:
java -jar build/libs/*.jar
```

Docker image / deployment:

- Build the image with `docker build -t catan-server .` and run it with `docker run -p 8080:8080 catan-server`.
- Cloud and CI deployment helpers (Cloud Run / Cloud Build) are included in `cloudbuild.yaml` and `deploy-cloud-run.sh`.


## Illustrations — API & game flow

1. Client authenticates and creates/joins a lobby via the LobbyController endpoints.
2. When enough players are ready, the server initializes a game instance and exposes game endpoints for moves, trades, and state polling.
3. The server enforces game rules and synchronizes state for all connected clients.

## Roadmap

- Introduce a ranked mode with internal MMR for compeditive play.
- For better scalability implement websocket for the polling.
- Improve API documentation (OpenAPI / Swagger docs).
- To continue working on the project host it on your own server and insert a huggingface token for the bot to work

## Authors & Acknowledgments

Developed by the `sopra-fs26-group-11` team for the course project. 
## License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
