package com.TrashCashCampus.Controller;

import com.TrashCashCampus.Service.FirebaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@RestController
@RequestMapping("/recycling")
@CrossOrigin(origins = {"http://localhost:5173", "http://10.0.2.2:8080", "http://10.0.2.2", "https://trashcashcampus-testenvironment--trashcash-campus.netlify.app", "https://trashcash-campus.netlify.app"})
public class RecyclingController {

    private static final Logger logger = Logger.getLogger(RecyclingController.class.getName());

    @Autowired
    private FirebaseService firebaseService;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitRecyclingForApproval(@RequestBody Map<String, Object> request) {
        try {
            logger.info("Received recycling submission: " + request);

            // Validate required fields
            if (!request.containsKey("userId") || !request.containsKey("binId") || 
                !request.containsKey("wasteType") || !request.containsKey("itemSize")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Missing required fields");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String userId = (String) request.get("userId");
            String binId = (String) request.get("binId");
            String binName = (String) request.getOrDefault("binName", "Recycling Bin");
            
            // Generate unique ID for this recycling activity
            String activityId = "pending_" + System.currentTimeMillis() + "_" + userId.substring(0, Math.min(5, userId.length()));
            
            // Set status as pending for admin approval
            request.put("status", "pending");
            request.put("approved", false);
            request.put("processed", false);
            request.put("submittedAt", System.currentTimeMillis());
            
            // Handle photo data if provided
            if (request.containsKey("photoData")) {
                String photoData = (String) request.get("photoData");
                logger.info("Received photoData with length: " + (photoData != null ? photoData.length() : 0));
                
                // Store a reference to the photo instead of the full data
                String photoRef = "photo_" + System.currentTimeMillis();
                request.put("photoRef", photoRef);
                logger.info("Generated photoRef: " + photoRef);
                
                // Create a thumbnail/preview (first 50 chars)
                if (photoData != null && photoData.length() > 0) {
                    String preview = photoData.substring(0, Math.min(50, photoData.length())) + "...";
                    request.put("photoPreview", preview);
                    logger.info("Created photoPreview with length: " + preview.length());
                    
                    // Store the base64 data for direct rendering
                    // Check if the data is too large (> 500KB) for a single Firestore document
                    if (photoData.length() > 500000) {
                        logger.info("Photo data exceeds 500KB, splitting into chunks");
                        
                        // Calculate how many chunks we need
                        int chunkSize = 450000; // 450KB per chunk to stay safely under limits
                        int totalChunks = (int) Math.ceil(photoData.length() / (double) chunkSize);
                        logger.info("Splitting into " + totalChunks + " chunks");
                        
                        // Store the first chunk
                        String firstChunk = photoData.substring(0, Math.min(chunkSize, photoData.length()));
                        request.put("imageBase64", firstChunk);
                        logger.info("Added imageBase64 (chunk 1) with length: " + firstChunk.length());
                        
                        // Store remaining chunks if needed
                        for (int i = 1; i < totalChunks; i++) {
                            int start = i * chunkSize;
                            int end = Math.min((i + 1) * chunkSize, photoData.length());
                            String chunk = photoData.substring(start, end);
                            request.put("imageBase64_part" + (i + 1), chunk);
                            logger.info("Added imageBase64_part" + (i + 1) + " with length: " + chunk.length());
                        }
                        
                        // Store the total number of chunks for later reassembly
                        request.put("imageBase64_chunks", totalChunks);
                    } else {
                        // Photo is small enough for a single document
                        request.put("imageBase64", photoData);
                        logger.info("Added imageBase64 with length: " + photoData.length());
                    }
                } else {
                    logger.warning("photoData is null or empty");
                }
                
                // Remove original photoData field to avoid duplication
                request.remove("photoData");
                logger.info("Removed original photoData field from request");
            } else {
                logger.warning("No photoData field in the request");
            }
            
            // Save to pendingRecycling collection
            firebaseService.createDocument("pendingRecycling", request);
            
            // Also save to bin logs collection with bin ID as a field rather than in the path
            Map<String, Object> binLogEntry = new HashMap<>(request);
            binLogEntry.put("binIdentifier", binId); // Add the bin ID as a field
            
            // Explicitly ensure imageBase64 is included in binLogEntry
            if (request.containsKey("imageBase64")) {
                binLogEntry.put("imageBase64", request.get("imageBase64"));
                logger.info("Added imageBase64 to binLogEntry, length: " + 
                    (request.get("imageBase64") != null ? ((String)request.get("imageBase64")).length() : 0));
            }
            
            // Copy chunked image data if present
            if (request.containsKey("imageBase64_chunks")) {
                binLogEntry.put("imageBase64_chunks", request.get("imageBase64_chunks"));
                int chunks = ((Number)request.get("imageBase64_chunks")).intValue();
                logger.info("Copying " + chunks + " image chunks to binLogEntry");
                
                // Copy all chunk parts
                for (int i = 2; i <= chunks; i++) {
                    String chunkKey = "imageBase64_part" + i;
                    if (request.containsKey(chunkKey)) {
                        binLogEntry.put(chunkKey, request.get(chunkKey));
                        logger.info("Copied " + chunkKey + " to binLogEntry, length: " + 
                            (request.get(chunkKey) != null ? ((String)request.get(chunkKey)).length() : 0));
                    }
                }
            }
            
            // Ensure all photo fields are copied
            if (request.containsKey("photoRef")) {
                binLogEntry.put("photoRef", request.get("photoRef"));
            }
            if (request.containsKey("photoPreview")) {
                binLogEntry.put("photoPreview", request.get("photoPreview"));
            }
            
            // Generate a unique ID for the bin log document
            String binLogId = "log_" + System.currentTimeMillis() + "_" + userId.substring(0, Math.min(5, userId.length()));
            
            // Create document with explicit ID instead of letting Firestore auto-generate one
            firebaseService.createDocumentWithId("binLogs", binLogId, binLogEntry);
            
            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Your recycling has been submitted for admin approval!");
            response.put("activityId", activityId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.severe("Error in submitRecyclingForApproval: " + e.getMessage());
            
            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to submit recycling: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingRecycling() {
        try {
            // Get all pending recycling submissions
            var pendingSubmissions = firebaseService.getAllDocuments("pendingRecycling");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", pendingSubmissions.size());
            response.put("items", pendingSubmissions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.severe("Error in getPendingRecycling: " + e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get pending recycling: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/approve/{activityId}")
    public ResponseEntity<Map<String, Object>> approveRecycling(
            @PathVariable String activityId,
            @RequestBody Map<String, Object> request) {
        try {
            // Get the recycling submission
            Map<String, Object> submission = firebaseService.getDocument("pendingRecycling", activityId);
            
            if (submission == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Recycling submission not found");
                return ResponseEntity.notFound().build();
            }
            
            // Update the submission status
            submission.put("status", "approved");
            submission.put("approved", true);
            submission.put("processedAt", System.currentTimeMillis());
            submission.put("adminNotes", request.getOrDefault("adminNotes", ""));
            
            // Save the updated submission
            firebaseService.updateDocument("pendingRecycling", activityId, submission);
            
            // Get user ID and points from the submission
            String userId = (String) submission.get("userId");
            int points = ((Long) submission.getOrDefault("potentialPoints", 0L)).intValue();
            
            // Update user points
            Map<String, Object> userData = firebaseService.getDocument("users", userId);
            
            if (userData != null) {
                // Calculate new points
                int currentPoints = ((Long) userData.getOrDefault("totalPoints", 0L)).intValue();
                int newTotalPoints = currentPoints + points;
                
                // Get today's date (for daily points tracking)
                long currentTime = System.currentTimeMillis();
                long lastPointsUpdate = ((Long) userData.getOrDefault("lastPointsUpdate", 0L)).longValue();
                
                // Calculate daily points (recentPoints)
                int currentDailyPoints = ((Long) userData.getOrDefault("recentPoints", 0L)).intValue();
                
                // Check if last update was on a different day
                boolean isNewDay = !isSameDay(lastPointsUpdate, currentTime);
                int newDailyPoints = isNewDay ? points : currentDailyPoints + points;
                
                // Update user data
                Map<String, Object> updates = new HashMap<>();
                updates.put("totalPoints", newTotalPoints);
                updates.put("recentPoints", newDailyPoints);
                updates.put("lastPointsUpdate", currentTime);
                
                firebaseService.updateDocument("users", userId, updates);
            }
            
            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recycling submission approved successfully");
            response.put("pointsAwarded", points);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.severe("Error in approveRecycling: " + e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to approve recycling: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Helper method to check if two timestamps are on the same day
     */
    private boolean isSameDay(long timestamp1, long timestamp2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        cal2.setTimeInMillis(timestamp2);
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
} 