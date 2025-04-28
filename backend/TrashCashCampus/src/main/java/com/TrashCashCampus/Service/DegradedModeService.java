package com.TrashCashCampus.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for handling degraded mode operations when critical services are unavailable.
 * This allows the application to continue functioning with limited capabilities.
 */
@Service
@Slf4j
public class DegradedModeService {
    
    private final AtomicBoolean degradedMode = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, String> serviceFailures = new ConcurrentHashMap<>();
    
    /**
     * Enables degraded mode with a reason
     * 
     * @param reason The reason for enabling degraded mode
     */
    public void enableDegradedMode(String reason) {
        degradedMode.set(true);
        log.warn("System entering degraded mode: {}", reason);
        serviceFailures.put(generateKey(), reason);
    }
    
    /**
     * Disables degraded mode for a specific service
     * 
     * @param serviceName The name of the service that is now available
     */
    public void disableDegradedMode(String serviceName) {
        serviceFailures.remove(serviceName);
        if (serviceFailures.isEmpty()) {
            degradedMode.set(false);
            log.info("System exiting degraded mode - all services restored");
        }
    }
    
    /**
     * Checks if the system is currently in degraded mode
     * 
     * @return true if in degraded mode, false otherwise
     */
    public boolean isInDegradedMode() {
        return degradedMode.get();
    }
    
    /**
     * Gets all active failure reasons
     * 
     * @return List of failure reasons
     */
    public List<String> getFailureReasons() {
        return new ArrayList<>(serviceFailures.values());
    }
    
    /**
     * Records a failed operation to track service degradation patterns
     * 
     * @param operationName Name of the operation that failed
     */
    public void recordFailedOperation(String operationName) {
        log.warn("Failed operation recorded: {}", operationName);
        // Could be enhanced to count failures and trigger degraded mode after threshold
    }
    
    private String generateKey() {
        return "service-" + System.currentTimeMillis();
    }
} 