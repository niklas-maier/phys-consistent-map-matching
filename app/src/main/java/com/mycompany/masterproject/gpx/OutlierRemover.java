package com.mycompany.masterproject.gpx;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;

import com.mycompany.masterproject.data.GPXData;
import com.mycompany.masterproject.graph.TimedGeoPosition;
import com.mycompany.masterproject.ui.DrawingLogic;

public class OutlierRemover {
    private final double vMin = 0.0;    // Minimum velocity (always 0)
    private final double vMax;          // Maximum allowable velocity

    // Constructor that sets velocity constraints
    public OutlierRemover(double maxVelocity) {
        this.vMax = maxVelocity;
    }

    // Static method to remove outliers based on speed limit and file names for input and output
    public static void nk(DrawingLogic drawingLogic, double speedLimit, String inputFileName, String outputFileName) {
        try {
            
            // Load the GPX file
            GPXData gpxData = drawingLogic.getTrack(inputFileName);
            System.out.println("Loaded GPX data from file: " + inputFileName);
            long startTime = System.nanoTime(); // Start timing
            if (gpxData == null) {
                System.out.println("No GPX data loaded for file: " + inputFileName);
                return;
            }
            // Create an OutlierRemover with the speed limit
            OutlierRemover remover = new OutlierRemover(speedLimit);

            // Remove outliers
            GPXData cleanedData = remover.removeOutliers(gpxData);
            long endTime = System.nanoTime(); // End timing
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds

            System.out.println("Outlier removal took " + duration + " ms");
            System.out.println("Cleaned Trackpoints" + (gpxData.getTrackPoints().size() - cleanedData.getTrackPoints().size()));
            // Write the cleaned data to the output file
            writeCleanedGPXToFile(cleanedData, outputFileName);

        } catch (Exception e) {
            System.err.println("An error occurred while processing the GPX file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // Helper class to store a GPS point, its predecessor, and the length of the subsequence
    private static class LinkedNode {
        TimedGeoPosition point;
        int subsequenceLength;
        LinkedNode predecessor;

        LinkedNode(TimedGeoPosition point, int subsequenceLength, LinkedNode predecessor) {
            this.point = point;
            this.subsequenceLength = subsequenceLength;
            this.predecessor = predecessor;
        }
    }

    // Method to remove outliers using the linked list approach to maintain the longest subsequence
    private GPXData removeOutliers(GPXData gpxData) {
        List<TimedGeoPosition> trackPoints = gpxData.getTrackPoints();
        List<LinkedNode> linkedList = new ArrayList<>();

        if (trackPoints.size() < 2) {
            return gpxData; // No outliers if there's less than 2 points
        }

        // Process each point in the GPX data
        for (TimedGeoPosition currentPoint : trackPoints) {
            LinkedNode bestPredecessor = null;
            int longestSubsequenceLength = 0;

            // Find the first consistent point (the best predecessor) in the linked list
            for (LinkedNode node : linkedList) {
                if (isConsistent(node.point, currentPoint)) {
                    if (node.subsequenceLength > longestSubsequenceLength) {
                        bestPredecessor = node;
                        longestSubsequenceLength = node.subsequenceLength;
                    }
                }
            }

            // Add the current point to the linked list with its calculated subsequence length
            LinkedNode newNode = new LinkedNode(currentPoint, longestSubsequenceLength + 1, bestPredecessor);
            linkedList.add(newNode);
        }

        // Find the node with the longest subsequence in the linked list
        LinkedNode longestSubsequenceNode = null;
        int maxSubsequenceLength = 0;

        for (LinkedNode node : linkedList) {
            if (node.subsequenceLength > maxSubsequenceLength) {
                maxSubsequenceLength = node.subsequenceLength;
                longestSubsequenceNode = node;
            }
        }

        // Reconstruct the longest consistent subsequence by following the predecessor pointers
        List<TimedGeoPosition> cleanedTrackPoints = new ArrayList<>();
        while (longestSubsequenceNode != null) {
            cleanedTrackPoints.add(0, longestSubsequenceNode.point); // Add in reverse order
            longestSubsequenceNode = longestSubsequenceNode.predecessor;
        }

        // Return the cleaned GPX data with outliers removed
        return new GPXData(gpxData.getName(), gpxData.getWaypoints(), cleanedTrackPoints);
    }

    // Method to check if two points are consistent based on velocity
    private boolean isConsistent(TimedGeoPosition p1, TimedGeoPosition p2) {
        double timeDiff = (p2.getTimestamp() - p1.getTimestamp()) / 1000.0; // Time difference in seconds
        if (timeDiff <= 0) {
            return false; // Points must be in chronological order
        }

        double distance = calculateDistance(p1.getPosition(), p2.getPosition());
        double velocity = distance / timeDiff * 3.6; // Convert m/s to km/h

        // Velocity check
        return velocity >= vMin && velocity <= vMax;
    }

    // Method to calculate the distance between two GeoPositions (Haversine formula)
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

    // Method to write cleaned GPX data to a file
    private static void writeCleanedGPXToFile(GPXData cleanedData, String outputFileName) {
        try (FileWriter writer = new FileWriter(outputFileName)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<gpx version=\"1.1\" creator=\"OutlierRemover\">\n");
            writer.write("  <trk>\n");
            writer.write("    <trkseg>\n");

            for (TimedGeoPosition timedGeoPos : cleanedData.getTrackPoints()) {
                GeoPosition pos = timedGeoPos.getPosition();
                writer.write(String.format("      <trkpt lat=\"%f\" lon=\"%f\">\n", pos.getLatitude(), pos.getLongitude()));
                writer.write(String.format("        <time>%s</time>\n", 
                    java.time.Instant.ofEpochMilli(timedGeoPos.getTimestamp()).toString()));
                writer.write("      </trkpt>\n");
            }

            writer.write("    </trkseg>\n");
            writer.write("  </trk>\n");
            writer.write("</gpx>\n");

            System.out.println("Cleaned GPX track written to " + outputFileName);
        } catch (IOException e) {
            System.err.println("Error writing cleaned GPX to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

