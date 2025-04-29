package com.mycompany.masterproject.graph;


import java.util.*;

import com.mycompany.masterproject.grid.Endpoint;

public class Segment {
    public int way_id;
    public List<Endpoint> endpoints;

    public Segment(int way_id, List<Endpoint> endpoints) {
        this.way_id = way_id;
        this.endpoints = endpoints;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true; // Same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null or different class

        Segment other = (Segment) obj;

        // Compare `way_id` and `endpoints` for equality
        return this.way_id == other.way_id && 
               Objects.equals(this.endpoints, other.endpoints);
    }
}

