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
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

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
        
        // Use Firebase's built-in email verification
        try {
            ActionCodeSettings actionCodeSettings = ActionCodeSettings.builder()
                .setUrl("https://trashcash-campus.netlify.app/emailVerified")
                .setHandleCodeInApp(false)
                .build();
            
            String link = firebaseAuth.generateEmailVerificationLink(email, actionCodeSettings);
            System.out.println("Email verification link generated: " + link);
            
            // Actually send the verification email
            sendVerificationEmail(email, link);
            
            System.out.println("Verification email sent to: " + email);
        } catch (FirebaseAuthException e) {
            System.out.println("Failed to generate verification email: " + e.getMessage());
            // Continue with user creation even if email link generation fails
        }
        
        return userRecord.getUid();
    }
    
    /**
     * Sends a verification email to the user
     * 
     * @param email The recipient's email address
     * @param verificationLink The verification link to include in the email
     */
    public void sendVerificationEmail(String email, String verificationLink) {
        // Create email content
        String subject = "TrashCash Campus - Verify Your Email";
        String htmlContent = 
            "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
            "<h2 style='color: #4CAF50;'>TrashCash Campus - Email Verification</h2>" +
            "<p>Thank you for registering with TrashCash Campus! Please verify your email address by clicking the button below:</p>" +
            "<div style='margin: 25px 0;'>" +
            "<a href='" + verificationLink + "' style='background-color: #4CAF50; color: white; padding: 12px 20px; text-decoration: none; border-radius: 4px; display: inline-block;'>Verify Email</a>" +
            "</div>" +
            "<p>If the button doesn't work, copy and paste this link into your browser:</p>" +
            "<p style='word-break: break-all;'><a href='" + verificationLink + "'>" + verificationLink + "</a></p>" +
            "<p>This link will expire in 24 hours.</p>" +
            "<p>If you didn't register for TrashCash Campus, you can ignore this email.</p>" +
            "</div>";
        
        // Send the email
        sendEmail(email, subject, htmlContent);
    }
    
    /**
     * General method to send emails via SMTP
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlContent HTML content of the email
     */
    public void sendEmail(String to, String subject, String htmlContent) {
        // Get credentials from environment variables
        final String username = System.getenv("EMAIL_USERNAME");
        final String password = System.getenv("EMAIL_PASSWORD");
        
        if (username == null || password == null) {
            System.err.println("Email credentials not configured. Set EMAIL_USERNAME and EMAIL_PASSWORD env variables.");
            return;
        }
        
        // Set properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.office365.com"); // For Outlook/Office365
        props.put("mail.smtp.port", "587");
        
        // Create session
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        
        try {
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            
            // Set HTML content
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            // Send message
            Transport.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
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
    
    /**
     * Updates a user's email verification status in Firebase Auth
     * 
     * @param uid The user's ID
     * @param isEmailVerified The email verification status
     * @return The updated UserRecord
     * @throws FirebaseAuthException If there's an error updating the user
     */
    public UserRecord updateUserEmailVerified(String uid, boolean isEmailVerified) throws FirebaseAuthException {
        if (!firebaseInitialized) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return null;
        }
        
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
            .setEmailVerified(isEmailVerified);
            
        return firebaseAuth.updateUser(request);
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
} 