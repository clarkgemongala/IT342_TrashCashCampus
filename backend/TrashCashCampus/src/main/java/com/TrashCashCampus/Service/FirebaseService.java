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

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {

    private Firestore firestore;
    private FirebaseAuth firebaseAuth;

    @PostConstruct
    public void initialize() {
        try {
            // Use the service account credentials file from resources
            InputStream serviceAccount = getClass().getClassLoader()
                .getResourceAsStream("trashcashcampusmobile-firebase-adminsdk-fbsvc-0a3b17cdcd.json");

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
            
            System.out.println("Firebase initialized successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Authentication methods
    
    public String createUser(String email, String password) throws FirebaseAuthException {
        CreateRequest request = new CreateRequest()
            .setEmail(email)
            .setPassword(password)
            .setEmailVerified(false);
            
        UserRecord userRecord = firebaseAuth.createUser(request);
        return userRecord.getUid();
    }
    
    public String signIn(String email, String password) {
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
        return firebaseAuth.getUser(uid);
    }
    
    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        return firebaseAuth.getUserByEmail(email);
    }
    
    // Firestore methods
    
    public Map<String, Object> getDocument(String collection, String documentId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(collection).document(documentId);
        DocumentSnapshot document = docRef.get().get();
        
        if (document.exists()) {
            return document.getData();
        } else {
            return null;
        }
    }
    
    public String createDocument(String collection, Map<String, Object> data) throws ExecutionException, InterruptedException {
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
        DocumentReference docRef = firestore.collection(collection).document(documentId);
        WriteResult result = docRef.update(data).get();
        return result.getUpdateTime().toString();
    }
    
    public List<Map<String, Object>> getAllDocuments(String collection) throws ExecutionException, InterruptedException {
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
        DocumentReference docRef = firestore.collection(collection).document(documentId);
        docRef.delete().get();
    }
} 