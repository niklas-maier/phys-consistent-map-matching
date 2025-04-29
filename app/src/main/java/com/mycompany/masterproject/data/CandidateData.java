package com.mycompany.masterproject.data;

import java.util.ArrayList;

public class CandidateData {
    private ClosestStreetResult candidate; // The candidate street point
    private ArrayList<CandidateInterval> intervals; // Top k intervals
    private int maxLength; // Maximum number of intervals

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public CandidateData(ClosestStreetResult candidate, int maxIntervals) {
        this.candidate = candidate;
        this.intervals = new ArrayList(maxIntervals);
        this.maxLength=maxIntervals;
    }

    public ClosestStreetResult getCandidate() {
        return candidate;
    }

    public void addInterval(CandidateInterval interval) {
        CandidateInterval worstInterval = null; // Track the worst interval
        int worstIndex = -1; // Index of the worst interval
        double worstScore = Double.NEGATIVE_INFINITY; // Initialize to a very low score
    
        for (int i = 0; i < intervals.size(); i++) {
            CandidateInterval existing = intervals.get(i);
            double[] currentInterval = interval.getInterval();
            double[] existingInterval = existing.getInterval();
    
            // Track the worst interval for potential replacement
            if (existing.getScore() > worstScore) {
                worstScore = existing.getScore();
                worstInterval = existing;
                worstIndex = i;
            }
    
            // Case 2: The current interval lies completely within the existing interval
            if (currentInterval[0] > existingInterval[0] && currentInterval[1] < existingInterval[1]) {
                //System.out.println("Current interval lies within the existing interval");
                return; // Do not add, stop
            }
            /* */
            if (currentInterval[0] == existingInterval[0] && currentInterval[1] == existingInterval[1]) {
                //System.out.println("Current interval envelops the existing interval");
                if(existing.getPredecessor() == interval.getPredecessor()){
                    return; // Do not add, stop
                }
            } 
    
            // Case 3: The existing interval lies completely within the current interval
            if (existingInterval[0] > currentInterval[0] && existingInterval[1] < currentInterval[1]) {
                // Replace only if they share the same predecessor
                if (existing.getPredecessor() == interval.getPredecessor()) {
                    intervals.set(i, interval); // Replace the existing interval
                    //System.out.println("Replaced existing interval (shared predecessor): " + 
                    //                   existingInterval[0] + " " + existingInterval[1] + " with " +
                    //                   currentInterval[0] + " " + currentInterval[1]);
                    return; // Stop
                } else {
                    //System.out.println("Existing interval is enveloped, but predecessors differ. Not replacing.");
                    continue; // Keep the existing interval
                }
            }
    
            // Case 4: If the interval has the same predecessor
            if (existing.getPredecessor() == interval.getPredecessor()) {
                // Replace only if the new interval has a better score
                if (interval.getScore() < existing.getScore()) {
                    intervals.set(i, interval);
                    //System.out.println("Replaced interval with the same predecessor: " +
                    //                   "Old Score: " + existing.getScore() + ", New Score: " + interval.getScore());
                    return;
                } else {
                    //System.out.println("Existing interval with the same predecessor has a better score");
                    return; // Stop if the new interval isn't better
                }
            }
    
            // Case 5: The intervals overlap but one does not contain the other
            if ((currentInterval[0] < existingInterval[1] && currentInterval[1] > existingInterval[0]) ||
                    (existingInterval[0] < currentInterval[1] && existingInterval[1] > currentInterval[0])) {
                // Overlap exists, move on to the next interval
                continue;
            }
        }
    
        // Case 1: At the end of the list, if there is space, append the interval
        if (intervals.size() < maxLength) {
            intervals.add(interval);
            //System.out.println("Added new interval " + interval.getInterval()[0] + " " + interval.getInterval()[1]);
            return;
        }
    
        // Replace the worst interval if the new interval has a better score
        if (interval.getScore() < worstScore) {
            intervals.set(worstIndex, interval);
            //System.out.println("Replaced worst interval with new better-scoring interval: " +
            //                   "Old Score: " + worstScore + ", New Score: " + interval.getScore());
            return;
        }
    
        //System.out.println("No space available to add new interval");
    }

    public void addInterval2(CandidateInterval interval) {
        CandidateInterval worstInterval = null; // Track the worst interval
        int worstIndex = -1; // Index of the worst interval
        double worstScore = Double.NEGATIVE_INFINITY; // Initialize to a very low score
    
        for (int i = 0; i < intervals.size(); i++) {
            CandidateInterval existing = intervals.get(i);
            double[] currentInterval = interval.getInterval();
            double[] existingInterval = existing.getInterval();
    
            // Track the worst interval for potential replacement
            if (existing.getScore() > worstScore) {
                worstScore = existing.getScore();
                worstInterval = existing;
                worstIndex = i;
            }
    
            // Case 1: If the new interval shares the same predecessor
            if (existing.getPredecessor() == interval.getPredecessor()) {
                // Prefer the interval with the lower score
                if (interval.getScore() < existing.getScore()) {
                    intervals.set(i, interval); // Replace with better score
                    //System.out.println("Replaced interval with the same predecessor: " +
                    //                   "Old Score: " + existing.getScore() + ", New Score: " + interval.getScore());
                    return;
                } else {
                    //System.out.println("Existing interval with the same predecessor has a better score");
                    return; // Stop if the new interval isn't better
                }
            }
    
            // Case 2: Compare intervals based on score and size
            if (currentInterval[0] <= existingInterval[1] && currentInterval[1] >= existingInterval[0]) {
                // Overlap exists, prefer the lower score
                if (interval.getScore() < existing.getScore()) {
                    intervals.set(i, interval); // Replace with the better-scoring interval
                    //System.out.println("Replaced overlapping interval: " +
                    //                   "Old Score: " + existing.getScore() + ", New Score: " + interval.getScore());
                    return;
                }
            }
        }
    
        // Case 3: At the end of the list, if there is space, append the interval
        if (intervals.size() < maxLength) {
            intervals.add(interval);
            //System.out.println("Added new interval " + interval.getInterval()[0] + " " + interval.getInterval()[1]);
            return;
        }
    
        // Case 4: Replace the worst interval if the new interval has a better score
        if (interval.getScore() < worstScore) {
            intervals.set(worstIndex, interval);
            //System.out.println("Replaced worst interval with new better-scoring interval: " +
            //                   "Old Score: " + worstScore + ", New Score: " + interval.getScore());
            return;
        }
    
        //System.out.println("No space available to add new interval");
    }
    
    

    public ArrayList<CandidateInterval> getIntervals() {
        return intervals;
    }
}

