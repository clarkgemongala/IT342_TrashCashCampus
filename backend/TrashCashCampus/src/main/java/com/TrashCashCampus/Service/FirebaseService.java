package com.TrashCashCampus.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.CollectionReference;
import com.google.firebase.auth.ActionCodeSettings;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {

    private Firestore firestore;
    private FirebaseAuth firebaseAuth;
    private boolean firebaseInitialized = false;
    
    @Value("${firebase.credentials.path:trashcashcampusmobile-firebase-adminsdk-fbsvc-0a3b17cdcd.json}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void initialize() {
        try {
            // First try to get credentials from environment variable
            String firebaseConfig = System.getenv("FIREBASE_CONFIG");
            InputStream serviceAccount;
            
            if (firebaseConfig != null && !firebaseConfig.isEmpty()) {
                // Use credentials from environment variable
                System.out.println("Loading Firebase credentials from environment variable FIREBASE_CONFIG");
                serviceAccount = new ByteArrayInputStream(firebaseConfig.getBytes(StandardCharsets.UTF_8));
            } else {
                // Fall back to loading from file
                System.out.println("Loading Firebase credentials from file: " + firebaseCredentialsPath);
                Resource resource = new ClassPathResource(firebaseCredentialsPath);
                serviceAccount = resource.getInputStream();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            // Initialize the Firebase app
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            // Get Firestore and Auth instances
            this.firestore = FirestoreClient.getFirestore();
            this.firebaseAuth = FirebaseAuth.getInstance();
            
            this.firebaseInitialized = true;
            System.out.println("Firebase initialized successfully");
        } catch (IOException e) {
            this.firebaseInitialized = false;
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Application will continue without Firebase functionality");
        }
    }

    // Add a method to check if Firebase is initialized
    public boolean isFirebaseInitialized() {
        return firebaseInitialized;
    }

    // Add a method to safely get Firestore
    public Firestore getFirestore() {
        if (!firebaseInitialized) {
            System.out.println("Firebase is not initialized, returning null Firestore");
            return null;
        }
        return this.firestore;
    }

    // Add a method to safely get FirebaseAuth
    public FirebaseAuth getFirebaseAuth() {
        if (!firebaseInitialized) {
            System.out.println("Firebase is not initialized, returning null FirebaseAuth");
            return null;
        }
        return this.firebaseAuth;
    }

    // Authentication methods
    
    public String createUser(String email, String password) throws FirebaseAuthException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        CreateRequest request = new CreateRequest()
            .setEmail(email)
            .setPassword(password)
            .setEmailVerified(false);
            
        UserRecord userRecord = firebaseAuth.createUser(request);
        return userRecord.getUid();
    }
    
    public String signIn(String email, String password) {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        // Firebase doesn't support server-side email/password authentication directly
        // This is a security issue - we need to properly validate credentials
        try {
            UserRecord userRecord = firebaseAuth.getUserByEmail(email);
            
            // In a real production app, we would need to use Firebase Admin SDK with
            // a custom authentication system that verifies passwords.
            // For now, let's add a simple check against Firestore
            
            try {
                System.out.println("Attempting to authenticate: " + email);
                
                // Look up the user document in Firestore to check credentials
                CollectionReference usersRef = firestore.collection("users");
                QuerySnapshot querySnapshot = usersRef.whereEqualTo("email", email).get().get();
                
                if (querySnapshot.isEmpty()) {
                    System.out.println("User not found in database: " + email);
                    throw new RuntimeException("User not found in database");
                }
                
                // Get the user document from Firestore
                DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                
                // Check if the password is stored in Firestore
                // Note: In a production app, passwords should be hashed, never stored in plain text
                if (userDoc.contains("password")) {
                    String storedPassword = userDoc.getString("password");
                    if (password.equals(storedPassword)) {
                        System.out.println("User authenticated successfully: " + email);
                        return userRecord.getUid();
                    }
                } else {
                    // For test account without migration to proper password storage yet
                    if (email.equals("test@cit.edu") && password.equals("Test123!")) {
                        System.out.println("Test account authenticated successfully");
                        return userRecord.getUid();
                    }
                }
                
                // Authentication failed - password doesn't match
                System.out.println("Authentication failed for: " + email + " - Invalid password");
                throw new RuntimeException("Invalid credentials");
            } catch (Exception e) {
                System.out.println("Authentication error: " + e.getMessage());
                throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
            }
        } catch (FirebaseAuthException e) {
            System.out.println("Firebase Auth Exception: " + e.getMessage());
            throw new RuntimeException("Authentication failed: User not found", e);
        }
    }
    
    public UserRecord getUserById(String uid) throws FirebaseAuthException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        return firebaseAuth.getUser(uid);
    }
    
    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        return firebaseAuth.getUserByEmail(email);
    }
    
    // Firestore methods
    
    public Map<String, Object> getDocument(String collection, String documentId) throws ExecutionException, InterruptedException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        DocumentReference docRef = firestore.collection(collection).document(documentId);
        DocumentSnapshot document = docRef.get().get();
        
        if (document.exists()) {
            return document.getData();
        } else {
            return null;
        }
    }
    
    public String createDocument(String collection, Map<String, Object> data) throws ExecutionException, InterruptedException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        DocumentReference docRef = firestore.collection(collection).document();
        WriteResult result = docRef.set(data).get();
        return docRef.getId();
    }
    
    /**
     * Creates a document with a specified ID
     * 
     * @param collection The collection to add the document to
     * @param documentId The document ID to use
     * @param data The data to store in the document
     * @return The document ID
     */
    public String createDocumentWithId(String collection, String documentId, Map<String, Object> data) throws ExecutionException, InterruptedException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        // Log document details for debugging
        System.out.println("Creating document in collection: " + collection + " with ID: " + documentId);
        System.out.println("Document has " + data.size() + " fields");
        
        // Check if imageBase64 field exists and log its size
        if (data.containsKey("imageBase64")) {
            String imageData = (String) data.get("imageBase64");
            System.out.println("Document contains imageBase64 field with length: " + 
                (imageData != null ? imageData.length() : 0));
        } else {
            System.out.println("Document does NOT contain imageBase64 field");
        }
        
        // Check for other photo-related fields
        if (data.containsKey("photoRef")) {
            System.out.println("Document contains photoRef: " + data.get("photoRef"));
        }
        if (data.containsKey("photoPreview")) {
            System.out.println("Document contains photoPreview with length: " + 
                ((String)data.get("photoPreview")).length());
        }
        
        // Create the document in Firestore
        DocumentReference docRef = firestore.collection(collection).document(documentId);
        WriteResult result = docRef.set(data).get();
        System.out.println("Document created successfully at: " + result.getUpdateTime());
        return documentId;
    }
    
    public String updateDocument(String collection, String documentId, Map<String, Object> data) throws ExecutionException, InterruptedException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        DocumentReference docRef = firestore.collection(collection).document(documentId);
        WriteResult result = docRef.update(data).get();
        return result.getUpdateTime().toString();
    }
    
    public List<Map<String, Object>> getAllDocuments(String collection) throws ExecutionException, InterruptedException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return new ArrayList<>();
        }
        
        CollectionReference colRef = firestore.collection(collection);
        QuerySnapshot querySnapshot = colRef.get().get();
        
        List<Map<String, Object>> documents = new ArrayList<>();
        querySnapshot.getDocuments().forEach(doc -> {
            Map<String, Object> data = doc.getData();
            data.put("id", doc.getId());
            documents.add(data);
        });
        
        return documents;
    }
    
    public void deleteDocument(String collection, String documentId) throws ExecutionException, InterruptedException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return;
        }
        
        DocumentReference docRef = firestore.collection(collection).document(documentId);
        docRef.delete().get();
    }

    /**
     * Finds a user document by email in Firestore
     * 
     * @param email The email to search for
     * @return A map containing the user document data and docId field with the document ID
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Map<String, Object> findUserByEmail(String email) throws ExecutionException, InterruptedException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        try {
            // Query users collection for the email
            CollectionReference usersRef = firestore.collection("users");
            QuerySnapshot querySnapshot = usersRef.whereEqualTo("email", email).get().get();
            
            if (querySnapshot.isEmpty()) {
                System.out.println("No user found with email: " + email);
                return null;
            }
            
            // Get the first matching document
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            
            if (document.exists()) {
                Map<String, Object> userData = document.getData();
                // Add the document ID to the data map
                userData.put("docId", document.getId());
                return userData;
            } else {
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error finding user by email: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Generates an email verification link for the specified email
     * 
     * @param email The email to generate a verification link for
     * @return The verification link
     * @throws FirebaseAuthException If there is an error generating the link
     */
    public String generateEmailVerificationLink(String email) throws FirebaseAuthException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        // Configure action code settings
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.builder()
            // URL you want to redirect back to. The domain (www.example.com) for this
            // URL must be whitelisted in the Firebase Console.
            .setUrl("https://trashcash-campus.netlify.app/verify-email-completed")
            // This must be true for email link sign-in
            .setHandleCodeInApp(false)
            .setDynamicLinkDomain(null)
            .build();
        
        try {
            String link = firebaseAuth.generateEmailVerificationLink(email, actionCodeSettings);
            System.out.println("Email verification link generated for: " + email);
            return link;
        } catch (FirebaseAuthException e) {
            System.out.println("Error generating email verification link: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Generates a password reset link for the specified email
     * 
     * @param email The email to generate a password reset link for
     * @return The password reset link
     * @throws FirebaseAuthException If there is an error generating the link
     */
    public String generatePasswordResetLink(String email) throws FirebaseAuthException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        // Configure action code settings
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.builder()
            .setUrl("https://trashcash-campus.netlify.app/reset-password")
            .setHandleCodeInApp(false)
            .setDynamicLinkDomain(null)
            .build();
        
        try {
            String link = firebaseAuth.generatePasswordResetLink(email, actionCodeSettings);
            System.out.println("Password reset link generated for: " + email);
            return link;
        } catch (FirebaseAuthException e) {
            System.out.println("Error generating password reset link: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Sends an email using Firebase Auth action links
     * 
     * @param email The email to send to
     * @param subject The email subject
     * @param body The email body
     */
    public void sendEmail(String email, String subject, String body) {
        // Firebase Authentication automatically sends emails when you generate
        // verification or password reset links, so we don't need to implement
        // custom email sending logic. This method is a placeholder that logs
        // what would happen if we did need to send a custom email.
        System.out.println("Email would be sent to: " + email);
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
        
        // In a real implementation, you might want to use JavaMail, SendGrid, Mailgun, etc.
        // to send additional custom emails beyond what Firebase provides.
    }

    /**
     * Creates a Firebase Auth verification check endpoint
     * This method should be called from a controller endpoint after email verification
     * 
     * @param oobCode The out-of-band code from the verification email
     * @return The email if verification is successful, null otherwise
     */
    public String checkActionCode(String oobCode) {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        try {
            // Check the action code to get the email
            com.google.firebase.auth.ActionCodeDetails response = 
                firebaseAuth.checkActionCode(oobCode);
            
            if (response != null && response.getEmail() != null) {
                String email = response.getEmail();
                
                if (email != null && !email.isEmpty()) {
                    // If this is a verification action, apply it
                    firebaseAuth.applyActionCode(oobCode);
                    System.out.println("Successfully applied action code for email: " + email);
                    
                    // NOW is when we should actually set isEmailVerified to true in Firestore
                    try {
                        // Get user by email to get the UID
                        UserRecord user = firebaseAuth.getUserByEmail(email);
                        String uid = user.getUid();
                        
                        // Update Firestore document with verification status
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("isEmailVerified", true);
                        
                        try {
                            // First try to update by UID
                            updateDocument("users", uid, updates);
                            System.out.println("Updated isEmailVerified to true for user: " + email);
                        } catch (Exception e) {
                            System.out.println("Error updating by UID, trying by email: " + e.getMessage());
                            
                            // Find by email as fallback
                            Map<String, Object> userDoc = findUserByEmail(email);
                            if (userDoc != null && userDoc.containsKey("docId")) {
                                String docId = (String) userDoc.get("docId");
                                updateDocument("users", docId, updates);
                                System.out.println("Updated isEmailVerified to true for user found by email: " + email);
                            } else {
                                System.out.println("Could not find user document to update verification status");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error updating verification status: " + e.getMessage());
                    }
                    
                    return email;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.out.println("Error checking action code: " + e.getMessage());
            return null;
        }
    }
} 