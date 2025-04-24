# TrashCash Campus Admin Setup

This document explains how to set up the admin access for TrashCash Campus frontend web application.

## About This Implementation

The TrashCash Campus frontend web has been modified to be accessible only to administrators. When non-admin users try to log in, they will receive a notification that only administrators can access this application and will be signed out immediately.

The admin check happens during the login process rather than at the route level. This means:
1. Only users with the 'admin' role can successfully log in
2. Non-admin users will be prevented from accessing the application at the login step
3. The application maintains its original route structure, with user management still being admin-only

## Making Users Administrators

### Option 1: Using the Node.js Script

1. Navigate to the frontend_web/trashcash-campus directory
2. Make sure you have Node.js installed
3. Install the required dependencies if not already installed:
   ```
   npm install firebase
   ```
4. Run the script:
   ```
   node makeUserAdmin.js
   ```
5. The script will update the user with email "drewadrein.odilao@cit.edu" to have the admin role

### Option 2: Using the Web Interface

1. Deploy the application
2. Navigate to `/makeAdmin.html` in your browser
3. Click the "Make User Admin" button
4. The page will show the status of the operation

### Option 3: Manual Update via Firebase Console

1. Go to the Firebase Console: https://console.firebase.google.com/
2. Select your project
3. Navigate to Firestore Database
4. Find the "users" collection
5. Locate the document for the user with email "drewadrein.odilao@cit.edu"
6. Edit the document and set the "role" field to "admin"
7. Save the changes

## Verification

After making a user an admin, you can verify it worked by:

1. Logging into the application with the admin credentials
2. If successful, you should be able to access the dashboard and other pages
3. Non-admin users will receive a notification and be signed out during login

## Troubleshooting

If you encounter issues:

1. Check the browser console for any errors
2. Verify the user document exists in Firestore
3. Ensure the "role" field is set to exactly "admin" (case-sensitive)
4. Confirm you're using the correct Firebase project configuration 