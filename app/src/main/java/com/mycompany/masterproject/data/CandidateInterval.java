package com.mycompany.masterproject.data;

public class CandidateInterval {
    private double[] interval; // The speed interval [v_min, v_max]
    private CandidateInterval predecessor; // The previous interval in the sequence
    private ClosestStreetResult candidate; // The candidate street point
    private double score; // The score of the interval

    // Constructor
    public CandidateInterval(double[] interval, CandidateInterval predecessor, ClosestStreetResult candidate, double score) {
        this.interval = interval;
        this.predecessor = predecessor;
        this.candidate = candidate;
        this.score = score;
    }

    // Getters and Setters
    public double[] getInterval() {
        return interval;
    }
    public CandidateInterval getPredecessor() {
        return predecessor;
    }
    public double getScore() {
        return score;
    }
    public ClosestStreetResult getCandidate() {
        return candidate;
    }

    public void setPredecessor(CandidateInterval predecessor) {
        this.predecessor = predecessor;
    }

    @Override
    public String toString() {
        return "CandidateInterval{" +
                "interval=" + interval[0] + " " + interval[1] +
                '}';
    }
}
