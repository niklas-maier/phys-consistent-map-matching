package com.mycompany.masterproject.data;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

import com.mycompany.masterproject.graph.TimedGeoPosition;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.awt.Color;

/**
 * Represents a GPX data object with waypoints and track points. Additionally, calculates speeds and colors
 */

public class GPXData {
    private final String name;
    private final Set<Waypoint> waypoints;
    private final List<TimedGeoPosition> trackPoints;
    private final List<Double> speeds; //list to store speed between consecutive track points
    private  List<Color> colors;   // Colors corresponding to each speed
    private  Color trackColor; // Unique color assigned to this track

    
    public GPXData(String name, Set<Waypoint> waypoints, List<TimedGeoPosition> trackPoints) {
        this.name = name;
        this.waypoints = waypoints;
        this.trackPoints = trackPoints;
        this.speeds = calculateSpeeds(trackPoints);
        this.colors = calculateColors(speeds);
        this.trackColor = null; // Assign the provided color
    }

    public String getName() {
        return name;
    }

    public Set<Waypoint> getWaypoints() {
        return waypoints;
    }

    public List<TimedGeoPosition> getTrackPoints() {
        return trackPoints;
    }

    public List<Double> getSpeeds() {
        return speeds;
    }

    // Returns the list of colors corresponding to the per edge speed
    public List<Color> getColors() {
        return colors;
    }

    // Returns the single unique color assigned to this track
    public Color getTrackColor() {
        return trackColor;
    }

    public void setTrackColor(Color trackColor) {
        this.trackColor = trackColor;
    }

    private List<Double> calculateSpeeds(List<TimedGeoPosition> trackPoints) {
        List<Double> speeds = new ArrayList<>();
        for (int i = 1; i < trackPoints.size(); i++) {
            TimedGeoPosition p1 = trackPoints.get(i - 1);
            TimedGeoPosition p2 = trackPoints.get(i);
    
            double distance = calculateDistance(p1.getPosition(), p2.getPosition());
            double timeDiff = (p2.getTimestamp() - p1.getTimestamp()) / 1000.0; // Time diff in seconds
    
            if (timeDiff > 0) {
                double speed = (distance / timeDiff) * 3.6; // Convert m/s to km/h
                speeds.add(speed);
            } else {
                // Handle zero or negative time difference
                //System.err.println("Warning: Zero or negative time difference between points: "+ p1 + " and " + p2);
                speeds.add(0.0); // Default to zero speed
            }
        }
        return speeds;
    }
    

    private double calculateDistance(GeoPosition pos1, GeoPosition pos2) {
        double R = 6371e3; // Earth's radius in meters
        double lat1 = Math.toRadians(pos1.getLatitude());
        double lat2 = Math.toRadians(pos2.getLatitude());
        double deltaLat = Math.toRadians(pos2.getLatitude() - pos1.getLatitude());
        double deltaLon = Math.toRadians(pos2.getLongitude() - pos1.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                 + Math.cos(lat1) * Math.cos(lat2)
                 * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Return distance in meters
    }

    // Convert each speed to a corresponding color using the 20-color palette
    private List<Color> calculateColors(List<Double> speeds) {
        List<Color> colors = new ArrayList<>();
        for (double speed : speeds) {
            colors.add(mapSpeedToColor(speed));  // Use the global minSpeed and maxSpeed
        }
        return colors;
    }

    // Example of mapping speed to color
    public Color mapSpeedToColor(double speed) {
        // Hardcoded speed-to-color mapping for specific speed ranges
        if (speed <= 10) {
            return new Color(68, 1, 84);   // Dark purple for 0-10 km/h
        } else if (speed <= 20) {
            return new Color(72, 35, 116); // Dark blue-purple for 10-20 km/h
        } else if (speed <= 30) {
            return new Color(64, 67, 135); // Dark blue for 20-30 km/h
        } else if (speed <= 40) {
            return new Color(52, 94, 141); // Blue for 30-40 km/h
        } else if (speed <= 50) {
            return new Color(41, 120, 142); // Blue-green for 40-50 km/h
        } else if (speed <= 60) {
            return new Color(32, 144, 140); // Greenish blue for 50-60 km/h
        } else if (speed <= 70) {
            return new Color(34, 167, 132); // Green for 60-70 km/h
        } else if (speed <= 80) {
            return new Color(68, 190, 112); // Light green for 70-80 km/h
        } else if (speed <= 90) {
            return new Color(121, 209, 81); // Yellow-green for 80-90 km/h
        } else if (speed <= 100) {
            return new Color(189, 222, 38); // Bright yellow-green for 90-100 km/h
        } else if (speed <= 110) {
            return new Color(253, 231, 37); // Bright yellow for 100-110 km/h
        } else if (speed <= 120) {
            return new Color(255, 215, 0);  // Golden yellow for 110-120 km/h
        } else if (speed <= 130) {
            return new Color(255, 165, 0);  // Orange for 120-130 km/h
        } else if (speed <= 140) {
            return new Color(255, 140, 0);  // Dark orange for 130-140 km/h
        } else if (speed <= 150) {
            return new Color(255, 99, 71);  // Tomato for 140-150 km/h
        } else if (speed <= 160) {
            return new Color(255, 127, 80); // Coral for 150-160 km/h
        } else if (speed <= 170) {
            return new Color(255, 160, 122); // Light salmon for 160-170 km/h
        } else if (speed <= 180) {
            return new Color(255, 204, 92); // Light yellow-orange for 170-180 km/h
        } else if (speed <= 190) {
            return new Color(255, 239, 153); // Pale yellow for 180-190 km/h
        } else {  // Speed between above 190 km/h or more
            return new Color(255, 255, 204); // Very pale yellow for 190-200 km/h and above
        }
    }
    
    
}

