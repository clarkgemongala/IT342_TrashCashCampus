# TrashCash Campus

A sustainable campus application that incentivizes recycling through QR code bin scanning, waste validation, and a rewards system.

## System Features

- **Single Sign-On Integration**: Users can log in with university credentials
- **Waste Validation and Tracking**: Real-time image recognition for waste type verification
- **Points and Rewards System**: Browse and redeem available rewards
- **Analytics and Reporting**: Visual representation of personal recycling statistics
- **Bin Management System**: Uses phone camera to scan QR codes on recycling bins 
- **Community Engagement**: Environmental impact visualization

## Project Structure

- `frontend_web`: React web application for the admin portal and user dashboard
- `frontend_mobile`: Kotlin-based Android application for mobile recycling activities
- `backend`: Server components for the application

## Getting Started

### Web Application

1. Navigate to the web frontend directory:
   ```
   cd frontend_web/trashcash-campus
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the development server:
   ```
   npm run dev
   ```

4. Open your browser to [http://localhost:5173](http://localhost:5173)

### Mobile Application

1. Open the mobile project in Android Studio
   ```
   cd frontend_mobile/TrashCashCampus-mobile
   ```

2. Connect your Android device or start an emulator

3. Build and run the app from Android Studio

## Firebase Configuration

The project uses Firebase for authentication, database, and storage. The configuration is already set up in both web and mobile applications.

- Web: `frontend_web/trashcash-campus/src/firebase.js`
- Mobile: `frontend_mobile/TrashCashCampus-mobile/app/google-services.json`

## Testing the Application

### Web Login

- Use any email with `@cit.edu` domain
- For testing, create an account using the registration flow

### Mobile Login

- The mobile app requires a valid account created through registration
- Verify your email to access the app features

## QR Code Testing

- Sample QR codes for bins can be generated from any QR code generator
- Use simple bin IDs that match the bin information in the database

## Technologies Used

- **Frontend Web**: React, Firebase SDK
- **Frontend Mobile**: Kotlin, Firebase Android SDK
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Authentication**: Firebase Authentication with Google OAuth
- **Database**: Firebase Firestore

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request 