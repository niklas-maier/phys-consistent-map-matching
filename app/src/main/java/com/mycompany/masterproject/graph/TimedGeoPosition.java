package com.mycompany.masterproject.graph;

import java.util.Objects;

import org.jxmapviewer.viewer.GeoPosition;

//Combines the GeoPosition with a timestamp

public class TimedGeoPosition {
    private GeoPosition position;
    private long timestamp;  // Timestamp in milliseconds or another suitable time unit

    public TimedGeoPosition(GeoPosition position, long timestamp) {
        this.position = position;
        this.timestamp = timestamp;
    }

    public GeoPosition getPosition() {
        return position;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null or different class

        TimedGeoPosition other = (TimedGeoPosition) obj;

        // Compare position and timestamp for equality
        return Objects.equals(this.position, other.position) &&
               this.timestamp == other.timestamp;
    }

    @Override
    public String toString() {
        return "TimedGeoPosition [position=" + position + ", timestamp=" + timestamp + "]";
    }
}

