package com.mycompany.masterproject.data;

import java.util.ArrayList;

import com.mycompany.masterproject.graph.TimedGeoPosition;

public class DataPoint {
    private TimedGeoPosition position; // The original GPS point
    private CandidateData[] candidates; // Array of candidates

    public DataPoint(TimedGeoPosition position, int numCandidates) {
        this.position = position;
        this.candidates = new CandidateData[numCandidates]; // Fixed size
    }

    public TimedGeoPosition getPosition() {
        return position;
    }

    public ArrayList<CandidateInterval> getCandidateIntervall(ClosestStreetResult candidate) {
        for (CandidateData data : candidates) {
            if (data != null && data.getCandidate().equals(candidate)) {
                return data.getIntervals();
            }
        }
        System.out.println("Candidate not found");
        return null;
    }

    public void setCandidate(int index, CandidateData candidate) {
        candidates[index] = candidate;
    }

    public CandidateData[] getAllCandidates() {
        return candidates;
    }

    public CandidateData getCandidateData(ClosestStreetResult candidate, int maxIntervals) {
        // Check if the candidate already exists
        for (CandidateData data : candidates) {
            if (data != null && data.getCandidate().equals(candidate)) {
                return data; // Return the existing candidate
            }
        }
    
        // If not found, create a new CandidateData
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i] == null) { // Find the first empty slot
                candidates[i] = new CandidateData(candidate, maxIntervals); // Adjust max intervals as needed
                return candidates[i];
            }
        }
    
        // Optional: Throw an exception or handle the case where the array is full
        throw new IllegalStateException("No space available to add new candidate");
    }
}

