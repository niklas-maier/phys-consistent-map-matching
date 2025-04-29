package com.mycompany.masterproject.analysis;

import java.util.List;
import java.util.Map;

import com.mycompany.masterproject.graph.Edge;

public class PathAnalyzer {
    /**
     * Analyzes a path by computing its total length and number of street type changes.
     *
     * @param result a map expected to contain a key "edges" with a List of Edge objects
     * @return a double array where [0] is total length (meters), [1] is street type change count
     */

    public static double[] analyzePath(Map<String, Object> result) {
        double totalLength = 0.0;
        int streetTypeChanges = 0;

        // Retrieve edges from the result map
        @SuppressWarnings("unchecked")
        List<Edge> edges = (List<Edge>) result.get("edges");

        if (edges == null || edges.isEmpty()) {
            return new double[]{totalLength, streetTypeChanges}; // Return 0 values if no edges
        }

        String previousStreetType = null;

        for (Edge edge : edges) {
            // Add to the total length
            totalLength += edge.distance;

            // Check for street type changes
            if (previousStreetType != null && !previousStreetType.equals(edge.streetType)) {
                streetTypeChanges++;
            }

            // Update previousStreetType for the next iteration
            previousStreetType = edge.streetType;
        }

        return new double[]{totalLength, streetTypeChanges};
    }
}

