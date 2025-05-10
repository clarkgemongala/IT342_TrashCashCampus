package com.TrashCashCampus.dto;

import java.util.List;
import java.util.Map;

public class PickupLocationResponse {
    private List<Map<String, Object>> locations;

    public PickupLocationResponse() {
    }

    public List<Map<String, Object>> getLocations() {
        return locations;
    }

    public void setLocations(List<Map<String, Object>> locations) {
        this.locations = locations;
    }
} 