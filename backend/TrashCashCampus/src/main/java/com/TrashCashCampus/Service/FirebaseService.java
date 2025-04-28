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

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class FirebaseService {

    private Firestore firestore;
    private FirebaseAuth firebaseAuth;
    private final ReentrantLock initLock = new ReentrantLock();
    private boolean initialized = false;
    private boolean inDegradedMode = false;
    
    @Value("${firebase.credentials.path:trashcashcampusmobile-firebase-adminsdk-fbsvc-0a3b17cdcd.json}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void initialize() {
        initLock.lock();
        try {
            if (initialized) {
                System.out.println("Firebase is already initialized, skipping initialization");
                return;
            }
            
            if (inDegradedMode) {
                System.out.println("Firebase is in degraded mode, skipping initialization");
                return;
            }
            
            try {
                // Use Spring's ClassPathResource to load the credentials file
                Resource resource = new ClassPathResource(firebaseCredentialsPath);
                
                // Check if the resource exists before trying to open it
                if (!resource.exists()) {
                    System.err.println("Firebase credentials file not found: " + firebaseCredentialsPath);
                    enterDegradedMode("Credentials file not found");
                    return;
                }
                
                InputStream serviceAccount = resource.getInputStream();
                
                System.out.println("Loading Firebase credentials from: " + firebaseCredentialsPath);
    
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
    
                // Initialize the Firebase app
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    System.out.println("Firebase app initialized");
                } else {
                    System.out.println("Firebase app already initialized");
                }
    
                // Get Firestore and Auth instances
                this.firestore = FirestoreClient.getFirestore();
                this.firebaseAuth = FirebaseAuth.getInstance();
                
                if (this.firestore == null) {
                    enterDegradedMode("Firestore failed to initialize");
                    return;
                }
                
                if (this.firebaseAuth == null) {
                    enterDegradedMode("FirebaseAuth failed to initialize");
                    return;
                }
                
                // Test a simple Firestore operation to verify connection
                try {
                    this.firestore.collection("test").document("test").get().get();
                    System.out.println("Firestore connection test successful");
                } catch (Exception e) {
                    System.err.println("Firestore connection test failed: " + e.getMessage());
                    // Continue anyway as this might be because the collection doesn't exist
                }
                
                initialized = true;
                System.out.println("Firebase initialized successfully");
            } catch (IOException e) {
                System.err.println("Failed to initialize Firebase: " + e.getMessage());
                e.printStackTrace();
                enterDegradedMode("IOException during initialization: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error initializing Firebase: " + e.getMessage());
                e.printStackTrace();
                enterDegradedMode("Unexpected error: " + e.getMessage());
            }
        } finally {
            initLock.unlock();
        }
    }
    
    private void enterDegradedMode(String reason) {
        System.err.println("Firebase entering DEGRADED MODE: " + reason);
        System.err.println("Application will continue without Firebase functionality");
        this.initialized = false;
        this.inDegradedMode = true;
    }
    
    // Ensure Firebase is initialized
    private boolean ensureInitialized() {
        if (inDegradedMode) {
            System.out.println("Firebase is in degraded mode, skipping operation");
            return false;
        }
        
        if (!initialized || firestore == null) {
            System.out.println("Firebase not initialized, attempting to initialize...");
            initialize();
        }
        
        return initialized;
    }
    
    // Get the Firestore instance with retry logic
    public Firestore getFirestore() {
        if (inDegradedMode) {
            System.out.println("Firebase is in degraded mode, Firestore is not available");
            return null;
        }
        
        if (firestore == null) {
            initLock.lock();
            try {
                System.out.println("Firestore is null, reinitializing...");
                if (FirebaseApp.getApps().isEmpty()) {
                    // Need to reinitialize the app
                    initialize();
                } else {
                    // Just need to get Firestore
                    try {
                        this.firestore = FirestoreClient.getFirestore();
                    } catch (Exception e) {
                        System.err.println("Failed to get Firestore: " + e.getMessage());
                        enterDegradedMode("Failed to get Firestore: " + e.getMessage());
                    }
                }
                
                if (this.firestore == null && !inDegradedMode) {
                    enterDegradedMode("Failed to get Firestore instance after reinitialization");
                }
            } catch (Exception e) {
                System.err.println("Error getting Firestore: " + e.getMessage());
                e.printStackTrace();
                enterDegradedMode("Error getting Firestore: " + e.getMessage());
            } finally {
                initLock.unlock();
            }
        }
        return firestore;
    }

    // Authentication methods
    
    public String createUser(String email, String password) throws FirebaseAuthException {
        if (!ensureInitialized()) {
            throw new RuntimeException("Firebase is not available");
        }
        CreateRequest request = new CreateRequest()
            .setEmail(email)
            .setPassword(password)
            .setEmailVerified(false);
            
        UserRecord userRecord = firebaseAuth.createUser(request);
        return userRecord.getUid();
    }
    
    public String signIn(String email, String password) {
        if (!ensureInitialized()) {
            throw new RuntimeException("Firebase is not available");
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
                CollectionReference usersRef = getFirestore().collection("users");
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
        if (!ensureInitialized()) {
            throw new RuntimeException("Firebase is not available");
        }
        return firebaseAuth.getUser(uid);
    }
    
    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        if (!ensureInitialized()) {
            throw new RuntimeException("Firebase is not available");
        }
        return firebaseAuth.getUserByEmail(email);
    }
    
    // Firestore methods
    
    public Map<String, Object> getDocument(String collection, String documentId) throws ExecutionException, InterruptedException {
        if (!ensureInitialized()) {
            return new HashMap<>(); // Return empty map in degraded mode
        }
        
        Firestore db = getFirestore();
        if (db == null) {
            return new HashMap<>(); // Return empty map if Firestore is not available
        }
        
        DocumentReference docRef = db.collection(collection).document(documentId);
        DocumentSnapshot document = docRef.get().get();
        
        if (document.exists()) {
            return document.getData();
        } else {
            return null;
        }
    }
    
    public String createDocument(String collection, Map<String, Object> data) throws ExecutionException, InterruptedException {
        if (!ensureInitialized()) {
            return "firebase-unavailable"; // Return placeholder ID in degraded mode
        }
        
        Firestore db = getFirestore();
        if (db == null) {
            return "firebase-unavailable"; // Return placeholder ID
        }
        
        DocumentReference docRef = db.collection(collection).document();
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
        if (!ensureInitialized()) {
            return documentId; // Return the same ID in degraded mode
        }
        
        Firestore db = getFirestore();
        if (db == null) {
            return documentId; // Return the same ID
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
        DocumentReference docRef = db.collection(collection).document(documentId);
        WriteResult result = docRef.set(data).get();
        System.out.println("Document created successfully at: " + result.getUpdateTime());
        return documentId;
    }
    
    public String updateDocument(String collection, String documentId, Map<String, Object> data) throws ExecutionException, InterruptedException {
        if (!ensureInitialized()) {
            return "firebase-unavailable"; // Return placeholder update time in degraded mode
        }
        
        Firestore db = getFirestore();
        if (db == null) {
            return "firebase-unavailable"; // Return placeholder update time
        }
        
        DocumentReference docRef = db.collection(collection).document(documentId);
        WriteResult result = docRef.update(data).get();
        return result.getUpdateTime().toString();
    }
    
    public List<Map<String, Object>> getAllDocuments(String collection) throws ExecutionException, InterruptedException {
        if (!ensureInitialized()) {
            return new ArrayList<>(); // Return empty list in degraded mode
        }
        
        Firestore db = getFirestore();
        if (db == null) {
            return new ArrayList<>(); // Return empty list
        }
        
        CollectionReference colRef = db.collection(collection);
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
        if (!ensureInitialized()) {
            return; // Do nothing in degraded mode
        }
        
        Firestore db = getFirestore();
        if (db == null) {
            return; // Do nothing
        }
        
        DocumentReference docRef = db.collection(collection).document(documentId);
        docRef.delete().get();
    }
    
    // Check if Firebase is available
    public boolean isAvailable() {
        return initialized && !inDegradedMode;
    }
} 