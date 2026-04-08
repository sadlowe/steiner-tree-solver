# Steiner Tree Solver

Interactive web application to visualize the Euclidean Steiner Tree problem.

This project is composed of a Java/Spring Boot backend and an Angular frontend.

## Deployment

### Prerequisites

- Docker
- Docker Compose
- `make` utility

### Build

To build the Docker images for both the backend and frontend, run the following command:

```sh
make build
```

### Run

To start the application stack, run:

```sh
make up
```

The application will be available at `http://localhost`.

### Stop

To stop the application stack, run:

```sh
make down
```

### Logs

To view the logs of the running application, use:

```sh
make logs
```

### Clean

To stop the application and remove the Docker images, run:

```sh
make clean
```

## Development

### Backend (Java/Spring Boot)

To run the backend in development mode:

```sh
cd backend
./mvnw spring-boot:run
```

The backend will be available at `http://localhost:8080`.

### Frontend (Angular)

To run the frontend in development mode:

```sh
cd frontend
npm install
npm start
```

The frontend will be available at `http://localhost:4200`.
