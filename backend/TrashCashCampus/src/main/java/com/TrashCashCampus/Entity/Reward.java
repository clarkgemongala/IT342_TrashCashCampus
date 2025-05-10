package com.TrashCashCampus.Entity;

/**
 * Reward entity for Firestore
 */
public class Reward {

    private String id;
    private String name;
    private int pointsCost;

    public Reward() {}

    public Reward(String id, String name, int pointsCost) {
        this.id = id;
        this.name = name;
        this.pointsCost = pointsCost;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPointsCost() { return pointsCost; }
    public void setPointsCost(int pointsCost) { this.pointsCost = pointsCost; }
}
