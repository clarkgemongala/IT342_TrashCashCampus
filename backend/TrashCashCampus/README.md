# TrashCash Campus Backend

This is the Spring Boot backend for the TrashCash Campus application.

## Deployment on Render.com

The application is currently deployed at: [https://it342-trashcashcampus.onrender.com](https://it342-trashcashcampus.onrender.com)

### Deployment Instructions

To deploy this application on Render:

1. Create a new **Web Service** on Render.com
2. Link your GitHub repository
3. Configure the following settings:
   - **Name**: it342-trashcashcampus
   - **Environment**: Java
   - **Build Command**: `cd IT342_TrashCashCampus/backend/TrashCashCampus && ./mvnw clean package -DskipTests`
   - **Start Command**: `cd IT342_TrashCashCampus/backend/TrashCashCampus && java -jar -Dspring.profiles.active=prod target/*.jar`
   - **Repo Root**: `/`
   - **Branch**: `main` (or your deployment branch)

4. Configure the following environment variables:
   - `FIREBASE_CREDENTIALS`: Copy the entire JSON content from the Firebase credentials file
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `PORT`: `8080` (Render will override this automatically)

5. Set advanced settings:
   - Set Auto-Deploy to true for CI/CD
   - Configure health check path: `/api/health`

### Project Structure

```
IT342_TrashCashCampus/backend/TrashCashCampus/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/TrashCashCampus/
│   │   │       ├── Controller/   # REST API endpoints
│   │   │       ├── Entity/       # Data models
│   │   │       ├── Repository/   # Data access
│   │   │       ├── Service/      # Business logic
│   │   │       └── DTO/          # Data transfer objects
│   │   └── resources/
│   │       ├── application.properties       # Default configuration
│   │       └── application-prod.properties  # Production configuration
│   └── test/                               # Unit and integration tests
├── pom.xml               # Maven dependencies
├── mvnw                  # Maven wrapper script (Unix)
├── mvnw.cmd              # Maven wrapper script (Windows)
└── README.md             # This file
```

## Development Setup

### Prerequisites

- Java 17 or later
- Maven
- Firebase project with Firestore and Authentication enabled

### Running Locally

1. Clone the repository
2. Navigate to the project directory:
   ```
   cd IT342_TrashCashCampus/backend/TrashCashCampus
   ```
3. Place your Firebase credentials JSON file in:
   ```
   src/main/resources/trashcashcampusmobile-firebase-adminsdk-fbsvc-0a3b17cdcd.json
   ```
4. Run the application:
   ```
   ./mvnw spring-boot:run
   ```
5. The application will be available at:
   ```
   http://localhost:8080
   ```

## API Endpoints

### Authentication

- `POST /api/auth/login`: User login
- `POST /api/auth/register`: User registration
- `POST /api/auth/request-password-reset`: Request password reset
- `POST /api/auth/verify`: Verify authentication token
- `POST /api/auth/update-password/{userId}`: Update user password

### Campus Locations

- `GET /api/campus-locations`: Get all campus locations
- `POST /api/campus-locations`: Create a new campus location

### Additional Endpoints

For a full list of endpoints, refer to the controller classes in the codebase.

## Error Handling

The backend implements a fault-tolerant design with a degraded mode for Firebase operations.
If Firebase initialization fails, the application will still start and operate in a limited mode.

## Outbound IP Addresses

For network security configurations, the application's outbound traffic from Render.com will come from one of these IP addresses:
- 35.160.120.126
- 44.233.151.27
- 34.211.200.85 