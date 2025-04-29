package com.mycompany.masterproject.grid;

import java.util.Objects;

public class Endpoint {
    public final long nodeId;  // Original node ID
    public final double lat;   // Latitude
    public final double lon;   // Longitude

    // Constructor
    public Endpoint(long nodeId, double lat, double lon) {
        this.nodeId = nodeId;
        this.lat = lat;
        this.lon = lon;
    }

    // Getters
    public long getNodeId() {
        return nodeId;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Endpoint)) return false;
        Endpoint endpoint = (Endpoint) o;
        return nodeId == endpoint.nodeId &&
               Double.compare(endpoint.lat, lat) == 0 &&
               Double.compare(endpoint.lon, lon) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, lat, lon);
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "nodeId=" + nodeId +
                ", lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
