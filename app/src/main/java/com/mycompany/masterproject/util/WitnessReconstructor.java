package com.mycompany.masterproject.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jxmapviewer.viewer.GeoPosition;

import com.mycompany.masterproject.data.CandidateInterval;
import com.mycompany.masterproject.data.ClosestStreetResult;
import com.mycompany.masterproject.data.GPXData;
import com.mycompany.masterproject.graph.Graph;
import com.mycompany.masterproject.graph.TimedGeoPosition;

public class WitnessReconstructor {

    // Reconstruct a path from the final CandidateInterval
    public List<ClosestStreetResult> reconstructPath(CandidateInterval finalInterval) {
        List<ClosestStreetResult> path = new ArrayList<>();
    
        CandidateInterval current = finalInterval;
        while (current != null) {
            // Add ClosestStreetResult to the start of the path
            path.add(0, current.getCandidate());
            current = current.getPredecessor(); // Move to the predecessor
        }
    
        return path;
    }
    
    public void reconstructAndExport(CandidateInterval finalInterval, String filePath, Graph graph) {
        List<ClosestStreetResult> closestStreetResults = null;
        List<Long> fullPath = new ArrayList<>();
        double totalScore = 0.0; // Initialize the cumulative score
    
        // Reconstruct the path and calculate the score
        try {
            closestStreetResults = reconstructPath(finalInterval);
            CandidateInterval current = finalInterval;
            while (current != null) {
                totalScore += current.getScore(); // Accumulate scores
                current = current.getPredecessor();
            }
        } catch (Exception e) {
            System.err.println("Error reconstructing path: " + e.getMessage());
        }
    
        // Process each pair of successive snapped points
        for (int i = 0; i < closestStreetResults.size() - 1; i++) {
            ClosestStreetResult start = closestStreetResults.get(i);
            ClosestStreetResult end = closestStreetResults.get(i + 1);
    
            try {
                // Run Dijkstra between the two closest streets
                Map<String, Object> result = graph.dijkstraBetweenClosestStreetResults(start, end);
    
                // Extract the path and add it to the fullPath
                @SuppressWarnings("unchecked")
                List<Long> path = (List<Long>) result.get("path");
                if (path != null) {
                    fullPath.addAll(path);
                }
    
            } catch (Exception e) {
                System.err.println("Failed to calculate path between points " + i + " and " + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        // Append the total score to the file name
        String newFilePath = filePath.substring(0, filePath.lastIndexOf('/') + 1) + String.format("score_%.2f_", totalScore) + filePath.substring(filePath.lastIndexOf('/') + 1);


    
        // Convert the full path to GPX data and write it to a file
        GPXData pathData = graph.convertPathToGPXData(fullPath, "UnraveledPath");
        writeCleanedGPXToFile(pathData, newFilePath);
    }

    public void reconstructAndExport2(CandidateInterval finalInterval, String filePath, Graph graph) {
        List<ClosestStreetResult> closestStreetResults = null;
    
        // Reconstruct the path and calculate the score
        try {
            closestStreetResults = reconstructPath(finalInterval);
            CandidateInterval current = finalInterval;
            while (current != null) {
                current = current.getPredecessor();
            }
        } catch (Exception e) {
            System.err.println("Error reconstructing path: " + e.getMessage());
        }
    
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<gpx version=\"1.1\" creator=\"ClosestStreetResultToGPX\">\n");
            writer.write("  <trk>\n");
            writer.write("    <trkseg>\n");
    
            for (ClosestStreetResult result : closestStreetResults) {
                TimedGeoPosition timedGeoPos = result.getPosition();
                GeoPosition pos = timedGeoPos.getPosition();
                writer.write(String.format("      <trkpt lat=\"%f\" lon=\"%f\">\n", pos.getLatitude(), pos.getLongitude()));
                writer.write(String.format("        <time>%s</time>\n",
                    java.time.Instant.ofEpochMilli(timedGeoPos.getTimestamp())
                        .atZone(java.time.ZoneId.of("UTC"))
                        .format(java.time.format.DateTimeFormatter.ISO_INSTANT)));
                writer.write("      </trkpt>\n");
            }
    
            writer.write("    </trkseg>\n");
            writer.write("  </trk>\n");
            writer.write("</gpx>\n");
    
            System.out.println("GPX file successfully written to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing GPX file: " + e.getMessage());
            e.printStackTrace();
        }
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
