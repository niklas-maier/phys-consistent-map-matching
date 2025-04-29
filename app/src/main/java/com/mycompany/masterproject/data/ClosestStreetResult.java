package com.mycompany.masterproject.data;

import com.mycompany.masterproject.graph.Segment;
import com.mycompany.masterproject.graph.TimedGeoPosition;

public class ClosestStreetResult {
    private TimedGeoPosition position;// the position of the snapped point
    private Segment segment;

    public ClosestStreetResult(TimedGeoPosition position, Segment segment) {
        this.position = position;
        this.segment = segment;
    }

    public TimedGeoPosition getPosition() {
        return position;
    }

    public Segment getSegment() {
        return segment;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true; // Same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null or different class
    
        ClosestStreetResult other = (ClosestStreetResult) obj;
    
        // Compare the position and segment for equality
        return position.equals(other.position) && segment.equals(other.segment);
    }

    @Override
    public String toString() {
        return "ClosestStreetResult{" +
                "position=" + position +
                ", segment=" + segment +
                '}';
    }
}
