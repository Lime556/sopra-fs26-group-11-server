# Settlers of Catan - Game Server

## Introduction

This repository contains the backend for our SoPra implementation of Settlers of Catan. The server provides the REST API used by the web client and manages users, lobbies, friendships, game state, turns, trades, bots, and persistence.

The goal of the project was to make a playable online version of Catan where players can create lobbies, start matches, and play the game through a browser client. The server contains most of the actual game rules, so the client can stay focused on displaying the game and sending player actions.

## Technologies

- Java 17
- Spring Boot
- Gradle with the included wrapper
- Spring Data JPA with an H2 database for local development
- Docker for container builds and deployment

## High-level components

- **Application setup**: Starts the Spring Boot application and contains the main configuration entry point. Main file: [src/main/java/ch/uzh/ifi/hase/soprafs26/Application.java](src/main/java/ch/uzh/ifi/hase/soprafs26/Application.java).
- **Controllers**: Define the REST endpoints used by the client. Important examples are [GameController](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/GameController.java), [LobbyController](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/LobbyController.java), [UserController](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/UserController.java), and [FriendController](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/FriendController.java).
- **Services**: Contain the main business logic for users, lobbies, friends, bots, ambience, and the game itself. Examples: [GameService](src/main/java/ch/uzh/ifi/hase/soprafs26/service/GameService.java), [LobbyService](src/main/java/ch/uzh/ifi/hase/soprafs26/service/LobbyService.java), and [UserService](src/main/java/ch/uzh/ifi/hase/soprafs26/service/UserService.java).
- **Entities and repositories**: Store and load the application state. The game model is under [src/main/java/ch/uzh/ifi/hase/soprafs26/entity](src/main/java/ch/uzh/ifi/hase/soprafs26/entity), while database access is handled in [src/main/java/ch/uzh/ifi/hase/soprafs26/repository](src/main/java/ch/uzh/ifi/hase/soprafs26/repository).
- **DTOs and mapper**: Convert between internal entities and the data sent to the client. The DTO classes are in [src/main/java/ch/uzh/ifi/hase/soprafs26/rest/dto](src/main/java/ch/uzh/ifi/hase/soprafs26/rest/dto), with mapping logic in [DTOMapper](src/main/java/ch/uzh/ifi/hase/soprafs26/rest/mapper/DTOMapper.java).

In short, the client sends actions to a controller, the controller calls the matching service, the service updates entities through repositories, and DTOs are returned to the client.

## Launch & Deployment

Prerequisites:

- Java 17
- The included Gradle wrapper (`./gradlew`)
- Docker, only if you want to build or run the container image

Run the server locally from this folder:

```bash
./gradlew bootRun
```

By default, the server starts on [http://localhost:8080](http://localhost:8080). The local H2 console is available at [http://localhost:8080/h2-console/](http://localhost:8080/h2-console/) while the server is running. The default database settings are in [src/main/resources/application.properties](src/main/resources/application.properties).

Useful commands:

```bash
./gradlew build          # build the project and run checks
./gradlew test           # run server-side tests
./gradlew jacocoTestReport
./gradlew bootJar        # create an executable jar
```

Run the built JAR:

```bash
java -jar build/libs/*.jar
```

Optional bot configuration:

- `BOT_AI_ENABLED`: enables the Hugging Face based bot when set to `true`
- `HF_TOKEN`: Hugging Face API token
- `BOT_AI_MODEL`: model name, if a different model should be used
- `BOT_AI_TIMEOUT_MS`: timeout for bot requests

If the AI bot is not enabled or cannot answer, the deterministic fallback bot can still be used.

Docker:

```bash
docker build -t catan-server .
docker run -p 8080:8080 catan-server
```

Deployment files for Google Cloud are included in [cloudbuild.yaml](cloudbuild.yaml), [app.yaml](app.yaml), and [deploy-cloud-run.sh](deploy-cloud-run.sh). For a release, build and test the project first, then deploy the container or JAR to the target environment.

## API & Game Flow

1. A user registers or logs in through the user endpoints.
2. The client creates or joins a lobby through the lobby endpoints.
3. Once the lobby is ready, the server creates a game and initializes the board, players, resources, and turn order.
4. During the match, the client sends actions such as rolling dice, building, trading, or playing development cards.
5. The server validates the action, updates the game state, and returns the new state to the clients.

## Roadmap

- Replace polling with WebSockets for smoother synchronization between clients.
- Add OpenAPI/Swagger documentation for the REST API.
- Add a ranked mode with an internal MMR system.

## Authors & Acknowledgments

Developed by the `sopra-fs26-group-11` team for the SoPra course project in 2026.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
