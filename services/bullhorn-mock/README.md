# Bullhorn Mock Service

A production-ready mock REST API service simulating Bullhorn CRM functionality with realistic test data. Built with Java 21, Spring Boot 3.5.7, PostgreSQL 18, and following enterprise best practices.

## Features

- **Complete Bullhorn CRM Simulation**: Mock endpoints for Candidates, Consultants, Job Orders, and Submissions
- **Realistic Test Data**: Automatically generates mock data using JavaFaker (configurable from 1 to 500,000+ records)
- **Enterprise Architecture**: Layered architecture with proper separation of concerns
- **RESTful API**: Well-designed REST endpoints with pagination, filtering, and search
- **API Documentation**: Interactive Swagger UI for API exploration and testing
- **Production-Ready**: Includes exception handling, validation, logging, and monitoring
- **Database Integration**: Full PostgreSQL integration with JPA/Hibernate
- **Best Practices**: DTOs, MapStruct mapping, service layer pattern, and more

## Tech Stack

- **Java**: 21 LTS (Latest Long-Term Support version)
- **Spring Boot**: 3.5.7 (Latest stable version as of Nov 2025)
- **PostgreSQL**: 18 (Latest version)
- **Build Tool**: Maven
- **ORM**: Spring Data JPA / Hibernate
- **API Docs**: SpringDoc OpenAPI 3 (Swagger UI)
- **Mock Data**: JavaFaker 1.0.2
- **Mapping**: MapStruct 1.6.3
- **Validation**: Jakarta Bean Validation
- **Logging**: SLF4J + Logback

## Project Structure

```
src/main/java/com/bullhorn/mockservice/
├── config/              # Configuration classes (OpenAPI, CORS, Data Initialization)
├── controller/          # REST controllers
├── dto/
│   ├── request/        # Request DTOs
│   └── response/       # Response DTOs
├── entity/             # JPA entities
├── repository/         # Spring Data JPA repositories
├── service/            # Business logic services
│   └── impl/          # Service implementations
├── mapper/             # MapStruct mappers
├── exception/          # Custom exceptions and global handler
└── util/               # Constants and utilities
```

## Prerequisites

- Java 21 or higher
- Maven 3.9+
- PostgreSQL 18 (or use Docker Compose)
- Git

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd bullhorn-mock-service
```

### 2. Start PostgreSQL (Using Docker Compose)

```bash
docker-compose up -d
```

Or use the provided docker-compose.yml to start PostgreSQL:

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: bullhorn_mock_dev
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### 3. Build the Application

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/bullhorn-mock-service-1.0.0.jar
```

The application will start on `http://localhost:8080`

### 5. Access Swagger UI

Open your browser and navigate to:
```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

### Candidates

- `POST /api/v1/candidates` - Create a new candidate
- `GET /api/v1/candidates` - Get all candidates (paginated)
- `GET /api/v1/candidates/{id}` - Get candidate by ID
- `GET /api/v1/candidates/bullhorn/{bullhornId}` - Get candidate by Bullhorn ID
- `GET /api/v1/candidates/search?query={query}` - Search candidates
- `GET /api/v1/candidates/status/{status}` - Get candidates by status
- `GET /api/v1/candidates/modified-since?modifiedSince={date}` - Get recently modified candidates
- `PUT /api/v1/candidates/{id}` - Update a candidate
- `DELETE /api/v1/candidates/{id}` - Soft delete a candidate
- `DELETE /api/v1/candidates/{id}/hard` - Hard delete a candidate

### Consultants

- `POST /api/v1/consultants` - Create a new consultant
- `GET /api/v1/consultants` - Get all consultants (paginated)
- `GET /api/v1/consultants/{id}` - Get consultant by ID
- `GET /api/v1/consultants/bullhorn/{bullhornId}` - Get consultant by Bullhorn ID
- `GET /api/v1/consultants/active` - Get active consultants
- `GET /api/v1/consultants/search?query={query}` - Search consultants
- `PUT /api/v1/consultants/{id}` - Update a consultant
- `DELETE /api/v1/consultants/{id}` - Delete a consultant

### Job Orders

- `POST /api/v1/job-orders` - Create a new job order
- `GET /api/v1/job-orders` - Get all job orders (paginated)
- `GET /api/v1/job-orders/{id}` - Get job order by ID
- `GET /api/v1/job-orders/bullhorn/{bullhornId}` - Get job order by Bullhorn ID
- `GET /api/v1/job-orders/open` - Get open job orders
- `GET /api/v1/job-orders/status/{status}` - Get job orders by status
- `GET /api/v1/job-orders/search?query={query}` - Search job orders
- `PUT /api/v1/job-orders/{id}` - Update a job order
- `DELETE /api/v1/job-orders/{id}` - Soft delete a job order
- `DELETE /api/v1/job-orders/{id}/hard` - Hard delete a job order

### Job Submissions

- `POST /api/v1/submissions` - Create a new job submission (referral)
- `GET /api/v1/submissions/{id}` - Get job submission by ID

### Health & Monitoring

- `GET /actuator/health` - Application health check
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Application metrics

## Configuration

### Application Profiles

The application supports multiple profiles:

- **dev** (default): Development configuration with auto-DDL and verbose logging
- **prod**: Production configuration with optimized settings

Switch profiles using:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Or:
```bash
java -jar target/bullhorn-mock-service-1.0.0.jar --spring.profiles.active=prod
```

### Database Configuration

Edit `src/main/resources/application-dev.yml` or `application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bullhorn_mock_dev
    username: postgres
    password: postgres
```

### Mock Data Configuration

Configure mock data generation in `application.yml`:

```yaml
app:
  mock-data:
    default-count: 1000  # Number of records to generate
    enable-auto-generation: true  # Auto-generate on startup
```

## Dependencies

All required dependencies are managed in the `pom.xml`:

- **Spring Boot Starter Web** - REST API framework
- **Spring Boot Starter Data JPA** - Database access
- **Spring Boot Starter Validation** - Bean validation
- **Spring Boot Starter Actuator** - Health checks & monitoring
- **PostgreSQL Driver** - Database driver
- **Lombok** - Reduce boilerplate code
- **MapStruct** - DTO mapping
- **JavaFaker** - Mock data generation
- **SpringDoc OpenAPI** - API documentation (Swagger)
- **Apache Commons Lang3** - Utility functions
- **Spring Boot DevTools** - Development productivity

## Building for Production

### Create Production JAR

```bash
mvn clean package -DskipTests
```

The JAR will be created in `target/bullhorn-mock-service-1.0.0.jar`

### Run in Production

```bash
java -jar target/bullhorn-mock-service-1.0.0.jar --spring.profiles.active=prod
```

### Environment Variables

Configure using environment variables:

```bash
export DATABASE_URL=jdbc:postgresql://prod-db:5432/bullhorn_mock
export DATABASE_USERNAME=admin
export DATABASE_PASSWORD=secure_password
export SERVER_PORT=8080

java -jar bullhorn-mock-service-1.0.0.jar
```

## Testing the API

### Using cURL

Create a candidate:
```bash
curl -X POST http://localhost:8080/api/v1/candidates \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "currentTitle": "Software Engineer"
  }'
```

Get all candidates:
```bash
curl http://localhost:8080/api/v1/candidates?page=0&size=10
```

### Using Swagger UI

1. Navigate to `http://localhost:8080/swagger-ui.html`
2. Explore all available endpoints
3. Try out API calls directly from the browser

## Architecture & Best Practices

This project follows enterprise Java development best practices:

1. **Layered Architecture**: Clear separation between controllers, services, and repositories
2. **DTO Pattern**: Request/Response objects separate from entities
3. **Service Layer**: Business logic encapsulated in service classes
4. **Repository Pattern**: Data access through Spring Data JPA
5. **Exception Handling**: Global exception handler with custom exceptions
6. **Validation**: Bean Validation for request DTOs
7. **Mapping**: MapStruct for efficient object mapping
8. **API Documentation**: OpenAPI 3.0 with Swagger UI
9. **Logging**: Structured logging with SLF4J
10. **Configuration Management**: Profile-based configuration
11. **Database Indexing**: Proper indexes on frequently queried fields
12. **Pagination**: All list endpoints support pagination
13. **CORS**: Configurable CORS support for frontend integration

## Performance Considerations

- **Connection Pooling**: HikariCP for optimal database performance
- **Lazy Loading**: JPA lazy loading for related entities
- **Batch Processing**: Mock data generation in batches
- **Pagination**: All list operations use pagination
- **Indexing**: Database indexes on key fields

## Troubleshooting

### Port Already in Use

Change the port in `application.yml`:
```yaml
server:
  port: 8081
```

### Database Connection Issues

Verify PostgreSQL is running:
```bash
docker ps  # If using Docker
# or
psql -U postgres -h localhost  # If using local PostgreSQL
```

### Build Issues

Clean and rebuild:
```bash
mvn clean install -U
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please open an issue in the GitHub repository.

## Acknowledgments

- Built with Spring Boot
- Mock data generation powered by JavaFaker
- API documentation with SpringDoc OpenAPI
