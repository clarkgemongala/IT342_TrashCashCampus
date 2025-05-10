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
   - `FIREBASE_CONFIG`: Copy the entire Firebase service account JSON content as a minified single line string (see below for details)
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `PORT`: `8080` (Render will override this automatically)

5. Set advanced settings:
   - Set Auto-Deploy to true for CI/CD
   - Configure health check path: `/api/health`

### Firebase Configuration Using Environment Variables

For security reasons, we store Firebase credentials as an environment variable rather than a file:

1. Obtain your Firebase Service Account JSON file from the Firebase Console
2. Minify the JSON content into a single line (remove all line breaks)
   - You can use a JSON minifier tool or copy it into one line manually
   - Ensure that the `\n` escapes in the private key are properly double-escaped as `\\n`
3. Add this minified JSON string as the `FIREBASE_CONFIG` environment variable in Render

Example of properly formatted environment variable value:
```
{"type":"service_account","project_id":"your-project-id","private_key_id":"somekeyid","private_key":"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BA...\\n-----END PRIVATE KEY-----\\n","client_email":"firebase-adminsdk-abc@your-project-id.iam.gserviceaccount.com",...}
```

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
3. Set up Firebase credentials using one of these methods:
   - **Option 1 (Recommended for production)**: Set the `FIREBASE_CONFIG` environment variable with the minified JSON content
   - **Option 2 (Development only)**: Place your Firebase credentials JSON file in:
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