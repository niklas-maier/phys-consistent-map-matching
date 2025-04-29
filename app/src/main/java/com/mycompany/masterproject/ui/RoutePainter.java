package com.mycompany.masterproject.ui;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;

import com.mycompany.masterproject.gpx.TrackWithColors;
import com.mycompany.masterproject.graph.TimedGeoPosition;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

// Custom painter class to draw lines between waypoints
public class RoutePainter implements Painter<JXMapViewer> {
    private final List<TrackWithColors> trackData;  // List of tracks with their corresponding colors
    private final Map<String, Color> trackColors;      // Map to store unique colors for each track
    private final Set<Color> usedColors;               // Set to keep track of colors already in use
    private boolean useIndividualTrackColors = true; // Flag to toggle color mode

    public RoutePainter() {
        this.trackData = new ArrayList<>();
        this.trackColors = new HashMap<>();
        this.usedColors = new HashSet<>();
    }

    public void setUseIndividualTrackColors(boolean useIndividual) {
        this.useIndividualTrackColors = useIndividual;
    }

    // Add a new track with its corresponding colors
    public void addTrack(String trackName, List<TimedGeoPosition> trackPoints, List<Color> segmentColors, Color color) {
        if (trackPoints.size() - 1 == segmentColors.size()) {  // Ensure the number of colors matches the segments
            this.trackData.add(new TrackWithColors(trackName, trackPoints, segmentColors));
            
            // Assign a unique color for the whole track and store it in the map
            Color trackColor = color;
            trackColors.put(trackName, trackColor);
            usedColors.add(trackColor); // Mark the color as used
        } else {
            throw new IllegalArgumentException("The number of colors must match the number of track segments.");
        }
    }

    // Remove a track by its identifier (trackName)
    public void removeTrack(String trackName) {
        trackData.removeIf(track -> track.getTrackName().equals(trackName));  // Remove track by name
    }

    // Method to retrieve a track’s color by track name
    public Color getTrackColor(String trackName) {
        return trackColors.get(trackName);
    }

    // Clear all tracks
    public void clearTracks() {
        this.trackData.clear();
        this.trackColors.clear();
        this.usedColors.clear();
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        g = (Graphics2D) g.create();
        Rectangle rect = map.getViewportBounds();
        g.translate(-rect.x, -rect.y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(10));

        // Loop through all TrackWithColors objects
        for (TrackWithColors trackWithColors : trackData) {
            List<TimedGeoPosition> trackPoints = trackWithColors.getTrackPoints();
            List<Color> segmentColors = trackWithColors.getColors();
            Color trackColor = trackColors.get(trackWithColors.getTrackName()); // Fetch color from map

            // Ensure track points and colors are valid
            if (trackPoints.size() > 1 && segmentColors.size() == trackPoints.size() - 1) {
                // Loop through each TimedGeoPosition in the track
                for (int j = 1; j < trackPoints.size(); j++) {
                    TimedGeoPosition p1 = trackPoints.get(j - 1);
                    TimedGeoPosition p2 = trackPoints.get(j);

                    // Convert GeoPosition to pixel coordinates on the map
                    Point2D pt1 = map.getTileFactory().geoToPixel(p1.getPosition(), map.getZoom());
                    Point2D pt2 = map.getTileFactory().geoToPixel(p2.getPosition(), map.getZoom());

                    // Set color based on the mode
                    if (useIndividualTrackColors) {
                        g.setColor(trackColor);  // Single color for the whole track
                    } else {
                        g.setColor(segmentColors.get(j - 1));  // Per-segment color
                    }

                    // Draw line from pt1 to pt2
                    g.drawLine((int) pt1.getX(), (int) pt1.getY(), (int) pt2.getX(), (int) pt2.getY());
                }
            }
        }

        g.dispose();
    }

}