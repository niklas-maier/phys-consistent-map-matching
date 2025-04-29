package com.mycompany.masterproject.graph;


//Used in the street graph for dijkstra and segment construction.
public class Edge {
    public double distance;
    public String streetType;
    public String maxSpeed;
    public int wayId;

    public Edge(double distance, String streetType, String maxSpeed, int wayId) {
        this.distance = distance;
        this.streetType = streetType;
        this.maxSpeed = maxSpeed;
        this.wayId = wayId;
    }

    @Override
    public String toString() {
        return String.format("Edge [distance=%.2f, streetType='%s', maxSpeed='%s', wayId=%d]", 
                            distance, streetType, maxSpeed, wayId);
    }
}
