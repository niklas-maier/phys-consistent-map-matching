package com.mycompany.masterproject.gpx;

import java.awt.Color;

import java.util.List;

import com.mycompany.masterproject.graph.TimedGeoPosition;


// For drawing the tracks with colors
public class TrackWithColors {
    private final String trackName;              // Identifier for the track
    private final List<TimedGeoPosition> trackPoints;
    private final List<Color> colors;

    public TrackWithColors(String trackName, List<TimedGeoPosition> trackPoints, List<Color> colors) {
        this.trackName = trackName;
        this.trackPoints = trackPoints;
        this.colors = colors;
    }

    public String getTrackName() {
        return trackName;
    }

    public List<TimedGeoPosition> getTrackPoints() {
        return trackPoints;
    }

    public List<Color> getColors() {
        return colors;
    }
} 
