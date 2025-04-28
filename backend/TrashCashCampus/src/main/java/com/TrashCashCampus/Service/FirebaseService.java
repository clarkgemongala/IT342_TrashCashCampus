package com.TrashCashCampus.Service;

import com.TrashCashCampus.Config.FirebaseConfig;
import com.TrashCashCampus.DTO.*;
import com.TrashCashCampus.Domain.FirebaseDocument;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class FirebaseService {
    private final FirebaseConfig config;
    private final DegradedModeService degradedModeService;
    
    private Firestore firestore;
    private FirebaseAuth firebaseAuth;
    private boolean serviceAvailable = false;
    
    @Value("${firebase.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${firebase.retry.initial-delay-ms:1000}")
    private int initialRetryDelayMs;
    
    @Value("${firebase.operation.timeout-ms:10000}")
    private int operationTimeoutMs;

    @Autowired
    public FirebaseService(FirebaseConfig config, DegradedModeService degradedModeService) {
        this.config = config;
        this.degradedModeService = degradedModeService;
    }

    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing Firebase...");
            
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getClass().getResourceAsStream(config.getServiceAccountPath());
                if (serviceAccount == null) {
                    String errorMsg = "Firebase credentials file not found: " + config.getServiceAccountPath();
                    log.error(errorMsg);
                    degradedModeService.enableDegradedMode("Firebase initialization failed: Missing credentials");
                    return;
                }

                try {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setDatabaseUrl(config.getDatabaseUrl())
                            .build();
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase App has been initialized successfully");
                } catch (IOException e) {
                    String errorMsg = "Error initializing Firebase: " + e.getMessage();
                    log.error(errorMsg, e);
                    degradedModeService.enableDegradedMode("Firebase initialization failed: " + e.getMessage());
                    return;
                }
            } else {
                log.info("Existing Firebase App found, reusing it");
            }

            // Initialize Firestore
            try {
                this.firestore = FirestoreOptions.getDefaultInstance().getService();
                if (this.firestore == null) {
                    log.error("Failed to initialize Firestore");
                    degradedModeService.enableDegradedMode("Firebase initialization failed: Firestore unavailable");
                    return;
                }
                log.info("Firestore initialized successfully");
            } catch (Exception e) {
                log.error("Error initializing Firestore: {}", e.getMessage(), e);
                degradedModeService.enableDegradedMode("Firebase initialization failed: Firestore error - " + e.getMessage());
                return;
            }

            // Initialize Firebase Auth
            try {
                this.firebaseAuth = FirebaseAuth.getInstance();
                if (this.firebaseAuth == null) {
                    log.error("Failed to initialize Firebase Auth");
                    degradedModeService.enableDegradedMode("Firebase initialization failed: Auth service unavailable");
                    return;
                }
                log.info("Firebase Auth initialized successfully");
            } catch (Exception e) {
                log.error("Error initializing Firebase Auth: {}", e.getMessage(), e);
                degradedModeService.enableDegradedMode("Firebase initialization failed: Auth error - " + e.getMessage());
                return;
            }

            this.serviceAvailable = true;
            // If we reach here, we can disable degraded mode for Firebase
            degradedModeService.disableDegradedMode("Firebase");
            log.info("Firebase fully initialized and ready");
        } catch (Exception e) {
            log.error("Unexpected error during Firebase initialization: {}", e.getMessage(), e);
            degradedModeService.enableDegradedMode("Firebase initialization failed: " + e.getMessage());
        }
    }

    /**
     * Retry initialization if it failed previously
     */
    public boolean retryInitialization() {
        if (!serviceAvailable) {
            log.info("Retrying Firebase initialization...");
            initialize();
        }
        return serviceAvailable;
    }

    /**
     * Check if Firebase is properly initialized
     */
    public boolean isFirebaseInitialized() {
        return serviceAvailable;
    }

    public boolean isServiceAvailable() {
        return serviceAvailable;
    }

    /**
     * Creates a new Firebase user
     */
    @Retryable(
        value = {FirebaseAuthException.class, InterruptedException.class, ExecutionException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public UserRecord createUser(CreateUserDTO userDTO) throws FirebaseException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot create user");
            degradedModeService.recordFailedOperation("createUser");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Creating new user: {}", userDTO.getEmail());
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(userDTO.getEmail())
                .setPassword(userDTO.getPassword())
                .setDisplayName(userDTO.getDisplayName())
                .setDisabled(false);
            
            UserRecord userRecord = firebaseAuth.createUser(request);
            log.info("User created successfully: {}", userRecord.getUid());
            return userRecord;
        } catch (FirebaseAuthException e) {
            log.error("Failed to create user: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("createUser");
            throw e;
        }
    }

    /**
     * Retrieves a user by ID
     */
    @Retryable(
        value = {FirebaseAuthException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public UserRecord getUserById(String uid) throws FirebaseAuthException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot get user by ID");
            degradedModeService.recordFailedOperation("getUserById");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Getting user by ID: {}", uid);
        try {
            UserRecord userRecord = firebaseAuth.getUser(uid);
            log.debug("User retrieved successfully: {}", userRecord.getUid());
            return userRecord;
        } catch (FirebaseAuthException e) {
            log.error("Failed to get user by ID: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("getUserById");
            throw e;
        }
    }

    /**
     * Retrieves a user by email
     */
    @Retryable(
        value = {FirebaseAuthException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot get user by email");
            degradedModeService.recordFailedOperation("getUserByEmail");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Getting user by email: {}", email);
        try {
            UserRecord userRecord = firebaseAuth.getUserByEmail(email);
            log.debug("User retrieved successfully: {}", userRecord.getUid());
            return userRecord;
        } catch (FirebaseAuthException e) {
            log.error("Failed to get user by email: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("getUserByEmail");
            throw e;
        }
    }

    /**
     * Updates user information
     */
    @Retryable(
        value = {FirebaseAuthException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public UserRecord updateUser(String uid, UpdateUserDTO userDTO) throws FirebaseAuthException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot update user");
            degradedModeService.recordFailedOperation("updateUser");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Updating user: {}", uid);
        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid);
            
            if (userDTO.getEmail() != null) {
                request.setEmail(userDTO.getEmail());
            }
            
            if (userDTO.getPassword() != null) {
                request.setPassword(userDTO.getPassword());
            }
            
            if (userDTO.getDisplayName() != null) {
                request.setDisplayName(userDTO.getDisplayName());
            }
            
            if (userDTO.getDisabled() != null) {
                request.setDisabled(userDTO.getDisabled());
            }
            
            UserRecord userRecord = firebaseAuth.updateUser(request);
            log.info("User updated successfully: {}", userRecord.getUid());
            return userRecord;
        } catch (FirebaseAuthException e) {
            log.error("Failed to update user: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("updateUser");
            throw e;
        }
    }

    /**
     * Deletes a user by ID
     */
    @Retryable(
        value = {FirebaseAuthException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public void deleteUser(String uid) throws FirebaseAuthException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot delete user");
            degradedModeService.recordFailedOperation("deleteUser");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Deleting user: {}", uid);
        try {
            firebaseAuth.deleteUser(uid);
            log.info("User deleted successfully: {}", uid);
        } catch (FirebaseAuthException e) {
            log.error("Failed to delete user: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("deleteUser");
            throw e;
        }
    }

    /**
     * Creates a document in Firestore
     */
    @Retryable(
        value = {InterruptedException.class, ExecutionException.class, FirestoreException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public DocumentReference createDocument(String collection, Map<String, Object> data) throws FirestoreException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot create document");
            degradedModeService.recordFailedOperation("createDocument");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Creating document in collection: {}", collection);
        try {
            DocumentReference docRef = firestore.collection(collection).document();
            ApiFuture<WriteResult> result = docRef.set(data);
            
            try {
                result.get(operationTimeoutMs, TimeUnit.MILLISECONDS);
                log.info("Document created successfully with ID: {}", docRef.getId());
                return docRef;
            } catch (TimeoutException e) {
                log.error("Timeout creating document: {}", e.getMessage(), e);
                degradedModeService.recordFailedOperation("createDocument");
                throw new FirestoreException("Timeout while creating document", e);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to create document: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("createDocument");
            throw new FirestoreException("Error creating document", e);
        }
    }

    /**
     * Creates a document with a specific ID in Firestore
     */
    @Retryable(
        value = {InterruptedException.class, ExecutionException.class, FirestoreException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public DocumentReference createDocumentWithId(String collection, String documentId, Map<String, Object> data) throws FirestoreException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot create document with ID");
            degradedModeService.recordFailedOperation("createDocumentWithId");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Creating document with ID: {} in collection: {}", documentId, collection);
        try {
            DocumentReference docRef = firestore.collection(collection).document(documentId);
            ApiFuture<WriteResult> result = docRef.set(data);
            
            try {
                result.get(operationTimeoutMs, TimeUnit.MILLISECONDS);
                log.info("Document created successfully with ID: {}", docRef.getId());
                return docRef;
            } catch (TimeoutException e) {
                log.error("Timeout creating document with ID: {}", e.getMessage(), e);
                degradedModeService.recordFailedOperation("createDocumentWithId");
                throw new FirestoreException("Timeout while creating document", e);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to create document with ID: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("createDocumentWithId");
            throw new FirestoreException("Error creating document with ID", e);
        }
    }

    /**
     * Updates a document in Firestore
     */
    @Retryable(
        value = {InterruptedException.class, ExecutionException.class, FirestoreException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public DocumentSnapshot updateDocument(String collection, String documentId, Map<String, Object> data) throws FirestoreException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot update document");
            degradedModeService.recordFailedOperation("updateDocument");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Updating document: {} in collection: {}", documentId, collection);
        try {
            DocumentReference docRef = firestore.collection(collection).document(documentId);
            ApiFuture<WriteResult> result = docRef.update(data);
            
            try {
                result.get(operationTimeoutMs, TimeUnit.MILLISECONDS);
                log.info("Document updated successfully: {}", documentId);
                
                // Get and return the updated document
                ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
                return documentSnapshot.get(operationTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.error("Timeout updating document: {}", e.getMessage(), e);
                degradedModeService.recordFailedOperation("updateDocument");
                throw new FirestoreException("Timeout while updating document", e);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to update document: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("updateDocument");
            throw new FirestoreException("Error updating document", e);
        }
    }

    /**
     * Gets a document from Firestore
     */
    @Retryable(
        value = {InterruptedException.class, ExecutionException.class, FirestoreException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public DocumentSnapshot getDocument(String collection, String documentId) throws FirestoreException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot get document");
            degradedModeService.recordFailedOperation("getDocument");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Getting document: {} from collection: {}", documentId, collection);
        try {
            DocumentReference docRef = firestore.collection(collection).document(documentId);
            ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
            
            try {
                DocumentSnapshot snapshot = documentSnapshot.get(operationTimeoutMs, TimeUnit.MILLISECONDS);
                if (snapshot.exists()) {
                    log.info("Document retrieved successfully: {}", documentId);
                } else {
                    log.info("Document not found: {}", documentId);
                }
                return snapshot;
            } catch (TimeoutException e) {
                log.error("Timeout getting document: {}", e.getMessage(), e);
                degradedModeService.recordFailedOperation("getDocument");
                throw new FirestoreException("Timeout while retrieving document", e);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to get document: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("getDocument");
            throw new FirestoreException("Error retrieving document", e);
        }
    }

    /**
     * Gets all documents from a collection in Firestore
     */
    @Retryable(
        value = {InterruptedException.class, ExecutionException.class, FirestoreException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public List<DocumentSnapshot> getAllDocuments(String collection) throws FirestoreException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot get all documents");
            degradedModeService.recordFailedOperation("getAllDocuments");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Getting all documents from collection: {}", collection);
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(collection).get();
            
            try {
                QuerySnapshot querySnapshot = future.get(operationTimeoutMs, TimeUnit.MILLISECONDS);
                List<DocumentSnapshot> documents = querySnapshot.getDocuments();
                log.info("Retrieved {} documents from collection: {}", documents.size(), collection);
                return documents;
            } catch (TimeoutException e) {
                log.error("Timeout getting all documents: {}", e.getMessage(), e);
                degradedModeService.recordFailedOperation("getAllDocuments");
                throw new FirestoreException("Timeout while retrieving all documents", e);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to get all documents: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("getAllDocuments");
            throw new FirestoreException("Error retrieving all documents", e);
        }
    }

    /**
     * Deletes a document from Firestore
     */
    @Retryable(
        value = {InterruptedException.class, ExecutionException.class, FirestoreException.class},
        maxAttempts = "${firebase.retry.max-attempts:3}",
        backoff = @Backoff(delay = "${firebase.retry.initial-delay-ms:1000}"))
    public void deleteDocument(String collection, String documentId) throws FirestoreException {
        if (!serviceAvailable && !retryInitialization()) {
            log.warn("Firebase service unavailable, cannot delete document");
            degradedModeService.recordFailedOperation("deleteDocument");
            throw new ServiceUnavailableException("Firebase service is unavailable");
        }

        log.info("Deleting document: {} from collection: {}", documentId, collection);
        try {
            DocumentReference docRef = firestore.collection(collection).document(documentId);
            ApiFuture<WriteResult> result = docRef.delete();
            
            try {
                result.get(operationTimeoutMs, TimeUnit.MILLISECONDS);
                log.info("Document deleted successfully: {}", documentId);
            } catch (TimeoutException e) {
                log.error("Timeout deleting document: {}", e.getMessage(), e);
                degradedModeService.recordFailedOperation("deleteDocument");
                throw new FirestoreException("Timeout while deleting document", e);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to delete document: {}", e.getMessage(), e);
            degradedModeService.recordFailedOperation("deleteDocument");
            throw new FirestoreException("Error deleting document", e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = maxRetryAttempts, 
               backoff = @Backoff(delay = initialRetryDelayMs))
    public List<Map<String, Object>> getCollection(String collection) throws ExecutionException, InterruptedException, TimeoutException {
        if (!serviceAvailable) {
            if (!retryInitialization()) {
                throw new IllegalStateException("Firebase is not initialized");
            }
        }

        try {
            log.info("Retrieving documents from collection: {}", collection);
            List<Map<String, Object>> documents = new ArrayList<>();
            ApiFuture<QuerySnapshot> future = firestore.collection(collection).get();
            QuerySnapshot querySnapshot = future.get(operationTimeoutMs, TimeUnit.MILLISECONDS);
            
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                Map<String, Object> data = document.getData();
                data.put("id", document.getId());
                documents.add(data);
            }
            
            log.info("Retrieved {} documents from collection {}", documents.size(), collection);
            return documents;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error retrieving documents from collection: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = maxRetryAttempts, 
               backoff = @Backoff(delay = initialRetryDelayMs))
    public List<FirebaseDocument> getCollectionAsDocuments(String collection) throws ExecutionException, InterruptedException, TimeoutException {
        if (!serviceAvailable) {
            if (!retryInitialization()) {
                throw new IllegalStateException("Firebase is not initialized");
            }
        }

        try {
            log.info("Retrieving documents from collection: {}", collection);
            List<FirebaseDocument> documents = new ArrayList<>();
            ApiFuture<QuerySnapshot> future = firestore.collection(collection).get();
            QuerySnapshot querySnapshot = future.get(operationTimeoutMs, TimeUnit.MILLISECONDS);
            
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                Map<String, Object> data = document.getData();
                documents.add(new FirebaseDocument(document.getId(), data));
            }
            
            log.info("Retrieved {} documents from collection {}", documents.size(), collection);
            return documents;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error retrieving documents from collection: {}", e.getMessage(), e);
            throw e;
        }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }

    /**
     * Custom exception for Firestore operations
     */
    public static class FirestoreException extends Exception {
        public FirestoreException(String message) {
            super(message);
        }
        
        public FirestoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 