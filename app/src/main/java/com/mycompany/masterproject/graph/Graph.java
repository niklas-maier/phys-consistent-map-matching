package com.mycompany.masterproject.graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.DefaultWaypoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.masterproject.data.ClosestStreetResult;
import com.mycompany.masterproject.data.GPXData;
import com.mycompany.masterproject.grid.Endpoint;

public class Graph {

    private final Map<Long, Node> adjacencyList;

    public static class Node {
        long nodeId;
        double lat; // Latitude
        double lon; // Longitude
        Map<Long, Edge> neighbors; // Key: neighbor's nodeId, Value: Edge metadata
    
        public Node(long nodeId, double lat, double lon) {
            this.nodeId = nodeId;
            this.lat = lat;
            this.lon = lon;
            this.neighbors = new HashMap<>();
        }
        public double getLat() {
            return lat;
        }
    
        public void addNeighbor(long neighborId, Edge edge) {
            neighbors.put(neighborId, edge);
        }
    }
    
    

    public Graph() {
        this.adjacencyList = new HashMap<>();
    }

    public void readFromJsonl(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip lines that start with '#'
                if (line.trim().startsWith("#")) {
                    continue;
                }
    
                JsonNode jsonNode = objectMapper.readTree(line);
    
                // Parse the main node details
                long nodeId = jsonNode.get("node_id").asLong();
                double lat = jsonNode.get("lat").asDouble();
                double lon = jsonNode.get("lon").asDouble();
    
                // Create or get the node from the adjacency list
                Node node = adjacencyList.computeIfAbsent(nodeId,
                    id -> new Node(nodeId, lat, lon)
                );
    
                // Parse neighbors
                JsonNode neighborsNode = jsonNode.get("neighbors");
                if (neighborsNode != null) {
                    for (Map.Entry<String, JsonNode> entry : iterable(neighborsNode.fields())) {
                        long neighborId = Long.parseLong(entry.getKey());
                        JsonNode edgeData = entry.getValue();
    
                        double distance = edgeData.get("distance").asDouble();
                        String streetType = edgeData.get("street_type").asText();
                        String maxSpeed = edgeData.get("maxspeed").asText();
                        int wayId = edgeData.get("way_id").asInt();
    
                        // Create an edge and add the neighbor
                        Edge edge = new Edge(distance, streetType, maxSpeed, wayId);
                        node.addNeighbor(neighborId, edge);
                    }
                }
            }
        }
    }
    
    // Helper method to iterate over JSON fields
    private static <T> Iterable<T> iterable(Iterator<T> iterator) {
        return () -> iterator;
    }    
    
    public Node getNode(long nodeId) {
        return adjacencyList.get(nodeId);
    }

    public Map<Long, Node> getAdjacencyList() {
        return adjacencyList;
    }

    public Map<String, Object> dijkstraWithPath(long startNodeId, long targetNodeId) {
        PriorityQueue<Map.Entry<Long, Double>> pq = new PriorityQueue<>(Map.Entry.comparingByValue());
        pq.add(Map.entry(startNodeId, 0.0));
    
        Map<Long, Double> distances = new HashMap<>();
        distances.put(startNodeId, 0.0);
    
        Map<Long, Long> predecessors = new HashMap<>();
    
        Set<Long> visited = new HashSet<>();
    
        while (!pq.isEmpty()) {
            Map.Entry<Long, Double> current = pq.poll();
            long currentNodeId = current.getKey();
            double currentDistance = current.getValue();
    
            if (visited.contains(currentNodeId)) continue;
            visited.add(currentNodeId);
    
            if (currentNodeId == targetNodeId) break;
    
            Node currentNode = getNode(currentNodeId);
            if (currentNode != null) {
                for (Map.Entry<Long, Edge> entry : currentNode.neighbors.entrySet()) {
                    if (entry.getKey() == currentNodeId) {
                        System.err.println("Self-loop detected: Node " + currentNodeId);
                    }
                    long neighborId = entry.getKey();
                    Edge edge = entry.getValue();
    
                    if (!visited.contains(neighborId)) {
                        double newDistance = currentDistance + edge.distance;
    
                        if (newDistance < distances.getOrDefault(neighborId, Double.POSITIVE_INFINITY)) {
                            distances.put(neighborId, newDistance);
                            predecessors.put(neighborId, currentNodeId);
                            pq.add(Map.entry(neighborId, newDistance));
                        }
                    }
                }
            }
        }
    
        // Reconstruct path
        List<Long> path = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        Long currentNodeId = targetNodeId;
        while (currentNodeId != null && predecessors.containsKey(currentNodeId)) {
            Long previousNodeId = predecessors.get(currentNodeId);
            path.add(0, currentNodeId);
    
            // Retrieve the edge between the previous and current nodes
            Node previousNode = getNode(previousNodeId);
            if (previousNode != null && previousNode.neighbors.containsKey(currentNodeId)) {
                Edge edge = previousNode.neighbors.get(currentNodeId);
                if (edge.distance == 0.0) {
                    System.err.println("Zero-length edge detected between nodes: " + previousNodeId + " and " + currentNodeId);
                }
                edges.add(0, edge);
            }
            
            currentNodeId = previousNodeId;
        }
    
        if (!path.isEmpty() && path.get(0) != startNodeId) {
            path.add(0, startNodeId);
        }
    
        // Prepare result
        Map<String, Object> result = new HashMap<>();
        result.put("distance", distances.getOrDefault(targetNodeId, Double.POSITIVE_INFINITY));
        result.put("path", path);
        result.put("edges", edges); // Include edges in the result
        return result;
    }
    

    @Override
    public String toString() {
        return "Graph{" +
                "adjacencyList=" + adjacencyList +
                '}';
    }

    // Reserved for generating unique negative node IDs for temporary nodes.
    private static long temporaryNodeId = -1;

    /**
     * Generates a unique negative node ID for temporary nodes.
     * This ensures no conflict with existing positive node IDs in the graph.
     *
     * @return A unique negative long ID.
     */
    private static long getNextTemporaryNodeId() {
        return temporaryNodeId--; // Decrement to ensure each ID is unique
    }

    /**
     * Adds a temporary node to the graph based on a TimedGeoPosition and connects it
     * to the endpoints of the given segment.
     *
     * @param graph The graph adjacency list.
     * @param timedGeoPosition The snapped position to be added.
     * @param segment The segment to which the temporary node will be connected.
     * @return The newly created temporary node.
     */    private Node addTemporaryNode(TimedGeoPosition timedGeoPosition, Segment segment) {

        final double EPSILON = 1e-6; // Adjust as needed

        // Generate a unique negative ID for the temporary node
        long tempNodeId = getNextTemporaryNodeId();
    
        double tempLat = timedGeoPosition.getPosition().getLatitude();
        double tempLon = timedGeoPosition.getPosition().getLongitude();
    
        // Check if the temporary node coincides with an endpoint
        for (Endpoint endpoint : segment.endpoints) {
            if (Math.abs(tempLat - endpoint.lat) < EPSILON && Math.abs(tempLon - endpoint.lon) < EPSILON) {
                //System.out.println("Temporary node coincides with endpoint: " + endpoint.nodeId);
                return adjacencyList.get(endpoint.nodeId); // Return the existing endpoint
            }
        }
    
        // Create the temporary node
        Node tempNode = new Node(tempNodeId, tempLat, tempLon);
    
        // Connect the temporary node to the endpoints of the segment
        for (Endpoint endpoint : segment.endpoints) {
            // Find the endpoint node in the graph
            Node endpointNode = adjacencyList.get(endpoint.nodeId);
            if (endpointNode != null) {
                // Extract the existing edge information between the endpoints
                Edge existingEdge = endpointNode.neighbors.get(segment.endpoints.get(1 - segment.endpoints.indexOf(endpoint)).nodeId);
                if (existingEdge == null) {
                    throw new IllegalStateException("No existing edge found between segment endpoints");
                }
    
                // Calculate the distance between the temporary node and the endpoint
                double distance = calculateHaversineDistance(tempLat, tempLon, endpoint.lat, endpoint.lon);
    
                // Skip creating a zero-length edge
                if (distance < EPSILON) {
                    System.err.println("Skipping zero-length edge for temporary node: " + tempNodeId);
                    continue;
                }
    
                // Create a new edge for the temporary connection using the same metadata
                Edge edgeToTemp = new Edge(distance, existingEdge.streetType, existingEdge.maxSpeed, existingEdge.wayId);
    
                // Add the connection in both directions
                tempNode.addNeighbor(endpoint.nodeId, edgeToTemp);
                endpointNode.addNeighbor(tempNodeId, edgeToTemp);
            }
        }
    
        // Add the temporary node to the graph
        adjacencyList.put(tempNodeId, tempNode);
    
        return tempNode; // Return the created temporary node
    }

    /**
     * Main function to run Dijkstra's algorithm between two ClosestStreetResults
     * by inserting temporary nodes into the graph.
     *
     * @param graph The graph adjacency list.
     * @param start The starting ClosestStreetResult.
     * @param target The target ClosestStreetResult.
     * @return The result of Dijkstra's algorithm, including the distance and path.
     */
    public Map<String, Object> dijkstraBetweenClosestStreetResults(ClosestStreetResult start, ClosestStreetResult target) {
        // Add a temporary node for the start position
        Node startNode = addTemporaryNode(start.getPosition(), start.getSegment());
    
        // Add a temporary node for the target position
        Node targetNode = addTemporaryNode(target.getPosition(), target.getSegment());
    
        // Run Dijkstra's algorithm between the two temporary nodes
        Map<String, Object> result = dijkstraWithPath(startNode.nodeId, targetNode.nodeId);
    
        return result; // Return the shortest path and distance
    }

    //Used to convert a path of node IDs to a GPXData object with the map matcher
    public GPXData convertPathToGPXData(List<Long> path, String name) {
        List<TimedGeoPosition> trackPoints = new ArrayList<>();
        Set<Waypoint> waypoints = new HashSet<>();
    
        // Iterate through the path to retrieve coordinates
        for (Long nodeId : path) {
            Node node = adjacencyList.get(nodeId);
            if (node == null) {
                System.err.println("Node " + nodeId + " does not exist in the graph.");
                continue;
            }
    
            // Convert the node to TimedGeoPosition (use a dummy timestamp if not provided)
            double lat = node.lat;
            double lon = node.lon;
            long timestamp = System.currentTimeMillis(); // Placeholder timestamp
    
            GeoPosition position = new GeoPosition(lat, lon);
            trackPoints.add(new TimedGeoPosition(position, timestamp));
            waypoints.add(new DefaultWaypoint(position)); // Create and add waypoints
        }
    
        // Return a new GPXData object
        return new GPXData(name, waypoints, trackPoints);
    }
    


    /**
     * Calculates the Haversine distance between two points on the Earth's surface.
     *
     * @param lat1 Latitude of the first point in degrees.
     * @param lon1 Longitude of the first point in degrees.
     * @param lat2 Latitude of the second point in degrees.
     * @param lon2 Longitude of the second point in degrees.
     * @return The distance between the two points in meters.
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371e3; // Earth's radius in meters

        // Convert degrees to radians
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        // Haversine formula
        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in meters
    }



    public static void main(String[] args) {
        String filePath = "/media/niklas/SSD2/geodata/streets.jsonl";
    
        Graph graph = new Graph();
        try {
            // Load the graph
            graph.readFromJsonl(filePath);
        } catch (IOException e) {
            System.err.println("Error reading JSONL file: " + e.getMessage());
        }


    }
    
}