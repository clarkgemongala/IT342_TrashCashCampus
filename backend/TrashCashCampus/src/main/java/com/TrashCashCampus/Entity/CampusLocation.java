package com.TrashCashCampus.Entity;

/**
 * CampusLocation entity for storing campus bin locations
 */
public class CampusLocation {

    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private String description;
    private String binType;

    public CampusLocation() {
    }

    public CampusLocation(String name, double latitude, double longitude, String description, String binType) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.binType = binType;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBinType() {
        return binType;
    }

    public void setBinType(String binType) {
        this.binType = binType;
    }
} 