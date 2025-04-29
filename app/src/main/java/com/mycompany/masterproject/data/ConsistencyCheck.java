// File: ConsistencyCheck.java
package com.mycompany.masterproject.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import com.mycompany.masterproject.analysis.PathAnalyzer;
import com.mycompany.masterproject.gpx.GPXLoader;
import com.mycompany.masterproject.graph.Edge;
import com.mycompany.masterproject.graph.Graph;
import com.mycompany.masterproject.graph.TimedGeoPosition;
import com.mycompany.masterproject.grid.StreetGrid;
import com.mycompany.masterproject.grid.StreetGridLoader;
import com.mycompany.masterproject.util.WitnessReconstructor;

import java.util.function.Function;


/**
 * Represents a road path segment with length, speed limit, road type, feasible speed interval, and slack.
 */
class PathSegment {
    private double length; // Length of the segment in meters
    private double speedLimit; // Maximum allowed speed on the segment in m/s

    // Constructor
    public PathSegment(double length, double speedLimit) {
        if (length < 0) {
            throw new IllegalArgumentException("PathSegment length must be non-negative.");
        }
        if (speedLimit < 0) {
            throw new IllegalArgumentException("Speed limit must be non-negative.");
        }
        this.length = length;
        this.speedLimit = speedLimit;
    }

    // Getters
    public double getLength() {
        return length;
    }

    public double getSpeedLimit() {
        return speedLimit;
    }


    @Override
    public String toString() {
        return "PathSegment{" +
                "length=" + length +
                ", speedLimit=" + speedLimit +
                '}';
    }
}

class Route {
    private List<PathSegment> pathSegments;

    // Constructor
    public Route() {
        this.pathSegments = new ArrayList<>();
    }

    // Adds a PathSegment to the route
    public void addPathSegment(PathSegment pathSegment) {
        if (pathSegment == null) {
            throw new IllegalArgumentException("PathSegment cannot be null.");
        }
        this.pathSegments.add(pathSegment);
    }

    // Getters
    public List<PathSegment> getPathSegments() {
        return pathSegments;
    }

    @Override
    public String toString() {
        return "Route{pathSegments=" + pathSegments + "}";
    }
}

/**
 * Represents a piecewise function U(x) that defines the upper bound of feasible speeds along a route.
 */
class UFunction {

    // List of all segment pieces
    private final List<SegmentPiece> pieces;

    // Enum for function types
    public enum FunctionType {
        CONSTANT, ACCELERATION, DECELERATION
    }

    // Class representing a single piece of the U(x) function
    public static class SegmentPiece {
        double startX; // Starting position of the segment
        double endX;   // Ending position of the segment
        double initialSpeed; // Initial speed at the start of the segment
        double endSpeed; // Speed limit or the maximum speed for this piece
        FunctionType functionType; // Type of the function (CONSTANT, ACCELERATION, DECELERATION)
        double acceleration; // Acceleration or deceleration value, if applicable

        public SegmentPiece(double startX, double endX,double initialSpeed, double endSpeed, FunctionType functionType, double acceleration) {
            /**
            System.out.println("Creating SegmentPiece with startX=" + startX + ", endX=" + endX +
                   ", endSpeed=" + endSpeed + ", functionType=" + functionType +
                   ", acceleration=" + acceleration);
            */

            if (startX < 0 || endX <= startX || endSpeed < 0) {
                //System.out.println("startX: " + startX + " endX: " + endX + " maxSpeed: " + endSpeed);
                //throw new IllegalArgumentException("Invalid segment boundaries or speed.");
            }
            this.startX = startX;
            this.endX = endX;
            this.initialSpeed = initialSpeed;
            this.endSpeed = endSpeed;
            this.functionType = functionType;
            this.acceleration = acceleration; // Used for ACCELERATION and DECELERATION
        }

        @Override
        public String toString() {
            return "SegmentPiece{" +
                    "startX=" + startX +
                    ", endX=" + endX +
                    ", initialSpeed=" + initialSpeed +
                    ", endSpeed=" + endSpeed +
                    ", functionType=" + functionType +
                    ", acceleration=" + acceleration +
                    '}';
        }
    }

    // Constructor
    public UFunction() {
        this.pieces = new ArrayList<>();
    }
    
    /**
     * Adds a segment piece to the U(x) function.
     *
     * @param startX       Starting position of the segment.
     * @param endX         Ending position of the segment.
     * @param maxSpeed     Maximum speed for the segment.
     * @param functionType Type of function (CONSTANT, ACCELERATION, DECELERATION).
     * @param acceleration Acceleration or deceleration value (used only for ACCELERATION/DECELERATION).
     */
    public void addSegment(double startX, double endX, double initialSpeed, double maxSpeed, FunctionType functionType, double acceleration) {
        SegmentPiece piece = new SegmentPiece(startX, endX, initialSpeed, maxSpeed, functionType, acceleration);
        this.pieces.add(piece);
    }
    public void addSegment(SegmentPiece segmentPiece) {
        this.addSegment(segmentPiece.startX, segmentPiece.endX, segmentPiece.initialSpeed, segmentPiece.endSpeed, segmentPiece.functionType, segmentPiece.acceleration);
    }

    /**
     * Retrieves the speed at a given position x based on the segment the position falls into.
     *
     * @param x Position for which the speed is calculated.
     * @return Speed U(x) for the given position.
     */
    public double getSpeed(double x) {
        for (SegmentPiece piece : pieces) {
            if (x >= piece.startX && x <= piece.endX) {
                switch (piece.functionType) {
                    case CONSTANT:
                        return piece.endSpeed; // Constant speed
                    case ACCELERATION:
                        return calculateSpeedForwards(piece.startX, x, piece.initialSpeed, piece.acceleration); // Handle acceleration
                    case DECELERATION:
                        return calculateSpeedForwards(piece.startX, x, piece.initialSpeed, piece.acceleration); // Handle deceleration
                    default:
                        throw new IllegalStateException("Unexpected function type: " + piece.functionType);
                }
            }
        }
        throw new IllegalArgumentException("Position x is out of range of the U(x) function.");
    }
    
    private double calculateSpeedForwards(double xStart, double xEnd, double vStart, double accel) {
        double distance = xEnd - xStart;
        double radicant = Math.pow(vStart, 2) + 2 * accel * distance;
        if(Math.abs(radicant)< 1e-6){
            radicant=0.0;
        }
        double speed = Math.sqrt(radicant); // Basic kinematic equation
        return speed;
    }
    
    private double calculateSpeedBackwards(double xStart, double xEnd, double vEnd, double accel) {
        double distance = xEnd - xStart;
        double speed = Math.sqrt(Math.abs(Math.pow(vEnd, 2) - 2 * accel * distance)); // Basic kinematic equation with deceleration, we use the abs, since numerically tiny differences can occur
        if(speed< 1e-6){
            return 0;
        }else{
            return speed;
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UFunction{\n");
        for (int i = pieces.size() - 1; i >= 0; i--) {
            sb.append("  ").append(pieces.get(i).toString()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    //This function is for integrating over the U(x) function
    public double integrateOneOverU() {
        double totalTime = 0.0;
    
        for (UFunction.SegmentPiece piece : this.pieces) {
            double startX = piece.startX;
            double endX = piece.endX;
            double vInitial = piece.initialSpeed;
            double acceleration = piece.acceleration;
    
            double segmentTime = 0.0;
            switch (piece.functionType) {
                case CONSTANT:
                    // For constant-speed segments
                    if (vInitial < 0) {
                        throw new IllegalArgumentException("Constant speed cannot be zero or negative.");
                    }
                    segmentTime = (endX - startX) / vInitial;
                    break;
    
                case ACCELERATION:
                    // For accelerating segments
                    if (vInitial < 0 || acceleration <= 0) {
                        System.out.println("vInitial: " + vInitial + ", acceleration: " + acceleration);
                        throw new IllegalArgumentException("Invalid parameters for acceleration segment.");
                    }
                    segmentTime = (Math.sqrt(vInitial * vInitial + 2 * acceleration * (endX-startX)) / acceleration)-((Math.sqrt(vInitial * vInitial)) / acceleration);
                    break;
    
                case DECELERATION:
                    // For decelerating segments
                    if (vInitial < 0 || acceleration >= 0) {
                        throw new IllegalArgumentException("Invalid parameters for deceleration segment.");
                    }
                    double deltaDistance = endX - startX; // Distance between the points
                    double vInitialSquared = vInitial * vInitial; // Square of the initial speed
                    double sqrtInitialSpeed = Math.sqrt(vInitialSquared); // Square root of initial speed
                    double firstTerm = vInitialSquared + 2 * acceleration * deltaDistance; // First term in the equation
                    if (firstTerm < 1e-6) {
                        firstTerm = 0; // Prevent negative values due to numerical errors                        
                    }
                    // Compute the total speed after acceleration over the distance
                    double sqrtFinalSpeed = Math.sqrt(firstTerm);

                    // Compute the time for the segment
                    segmentTime = (sqrtFinalSpeed / acceleration) - (sqrtInitialSpeed / acceleration);
                    break;
    
                default:
                    throw new IllegalStateException("Unexpected segment type: " + piece.functionType);
            }
    
            totalTime += segmentTime;
        }
        //System.out.println("Total time: " + totalTime);
        return totalTime;
    }
    
    // This function calculates the intersection point between U(x) and a given segment
    public double findIntersection(SegmentPiece segment) {
        // Iterate through each segment piece in U(x)
        for (SegmentPiece uSegment : this.pieces) {
            // Determine intersection based on segment types
            double intersectionX = calculateIntersection(segment, uSegment);
            // Check if the intersection point is within the bounds of both segments
            if (intersectionX >= Math.max(segment.startX, uSegment.startX) 
                && intersectionX <= Math.min(segment.endX, uSegment.endX)) {
                return intersectionX; // Valid intersection found
            }
        }
    
        // No intersection found
        return -1.0;
    }
    // Helper function to calculate intersection point between two segments
    private double calculateIntersection(SegmentPiece s1, SegmentPiece s2) {
        // Handle CONSTANT speed segments
        if (s1.functionType == UFunction.FunctionType.CONSTANT && s2.functionType == UFunction.FunctionType.CONSTANT) {
            if (s1.endSpeed == s2.endSpeed) {
                return Double.NaN; // Parallel, no intersection
            }
            // Calculate intersection x where 1/v(s1) = 1/v(s2)
            return (s2.startX - s1.startX) + (s1.endSpeed - s2.endSpeed);
        }
    
        // Handle ACCELERATION or DECELERATION segments
        if ((s1.functionType == UFunction.FunctionType.ACCELERATION || s1.functionType == UFunction.FunctionType.DECELERATION)
                && (s2.functionType == UFunction.FunctionType.ACCELERATION || s2.functionType == UFunction.FunctionType.DECELERATION)) {
            double a1 = s1.acceleration;
            double a2 = s2.acceleration;
            double v1 = s1.initialSpeed;
            double v2 = s2.initialSpeed;
    
            // Solve for x: v1^2 + 2a1(x - startX1) = v2^2 + 2a2(x - startX2)
            double discriminant = Math.pow(v1, 2) - Math.pow(v2, 2) 
                                + 2 * a2 * s2.startX - 2 * a1 * s1.startX;
    
            if (a1 != a2) {
                return discriminant / (2 * (a2 - a1)); // Intersection x
            } else {
                return Double.NaN; // Parallel, no intersection
            }
        }
    
        // Handle mixed types (e.g., CONSTANT with ACCELERATION/DECELERATION)
        if (s1.functionType == UFunction.FunctionType.CONSTANT) {
            // Solve using s1.maxSpeed and s2 acceleration
            return solveMixedIntersection(s1, s2);
        } else if (s2.functionType == UFunction.FunctionType.CONSTANT) {
            // Solve using s2.maxSpeed and s1 acceleration
            return solveMixedIntersection(s2, s1);
        }
    
        return Double.NaN; // No valid intersection for unsupported types
    }
    // Mixed intersection solver: CONSTANT with ACCELERATION/DECELERATION
    private double solveMixedIntersection(SegmentPiece constant, SegmentPiece changing) {
        double a = changing.acceleration;
        double vChanging = changing.initialSpeed;
        double vConstant = constant.endSpeed;
    
        // Solve for x: vChanging^2 + 2a(x - changing.startX) = vConstant^2
        double discriminant = (Math.pow(vChanging, 2) - Math.pow(vConstant, 2) - 2 * a * changing.startX);
        if (a != 0) {
            double intersectionX = discriminant / (-2 * a);
            if (intersectionX >= Math.max(constant.startX, changing.startX) 
                && intersectionX <= Math.min(constant.endX, changing.endX)) {
                return intersectionX;
            }
        }
        return Double.NaN; // No valid intersection
    }

    public UFunction getUFunctionUpTo(double x) {
        UFunction truncatedUFunction = new UFunction(); // New function to store segments up to x
        ListIterator<SegmentPiece> iterator = this.pieces.listIterator(this.pieces.size());
        while(iterator.hasPrevious()) {
            SegmentPiece segment = iterator.previous();
            if (x >= segment.startX && x <= segment.endX) {
                // Found the segment that intersects x, truncate it
                double truncatedSpeed = calculateSpeedForwards(segment.startX, x, segment.initialSpeed, segment.acceleration);
                SegmentPiece truncatedSegment = new SegmentPiece(
                    segment.startX, // Start remains the same
                    x,             // End is adjusted to x
                    segment.initialSpeed,
                    truncatedSpeed, 
                    segment.functionType, 
                    segment.acceleration
                );
                truncatedUFunction.addSegment(truncatedSegment);
                break; // Stop further iterations after truncation
            } else if (x > segment.endX) {
                // Entire segment is before x, retain it
                truncatedUFunction.addSegment(segment);
            } else {
                // Segment is completely beyond x, stop adding further segments
                break;
            }
        }
    
        return truncatedUFunction;
    }

    /**
     * Computes the feasible speed interval [v_down_f, v_up_f] at the end of the route.
     *
     * @param vDown    The initial lower speed bound (v_down).
     * @param vUp      The initial upper speed bound (v_up).
     * @param aUp      The maximum acceleration (a_up > 0).
     * @param aDown    The maximum deceleration (a_down < 0).
     * @param L        The total length of the route.
     * @param deltaT   The total allowed travel time (Δt).
     * @return         An array containing [v_down_f, v_up_f], or [NaN, NaN] if no feasible solution exists.
     */
    public double[] computeSpeedInterval(double vDown, double vUp, double aDown, double aUp, double deltaT) {
        if(Double.isNaN(vUp) || Double.isNaN(vDown) || Double.isNaN(aUp) || Double.isNaN(aDown) || Double.isNaN(deltaT)){
            System.out.println("Invalid input values.");
            return new double[]{Double.NaN, Double.NaN};
        }
        // Initialize final speed bounds
        double vUpF = Double.NaN;
        double vDownF = Double.NaN;
        double L = this.pieces.getFirst().endX; //Remember that this list is in reverse order
    
        // -------------------------------
        // Compute Maximum Final Speed (v_up_f)
        // -------------------------------
    
        // Step 1: Integrate 1/U(x) over the entire route to get T_U
        double T_U = this.integrateOneOverU();
        
    
        // Step 2: Check feasibility with U(x)
        if (T_U > deltaT) {
            //System.out.println("Required Time: " + T_U + ", available Time: " + deltaT);
            return new double[]{Double.NaN, Double.NaN};
        }
    
        // Step 3: Extract final speed from U(x)
        double vU = this.getSpeed(L);
    
        // Step 4: Construct auxiliary curves d(x) and e(x) as Segment Pieces
        double decelEnd = (Math.pow(vDown, 2) - Math.pow(0, 2)) / (2 * aUp);
        double decelEndFinal = Math.min(L, decelEnd);
        double accelStart = L + ((vU * vU) / (2 * aDown));
        double accelStartFinal = Math.max(0, accelStart);

        //Calculate the speed at the end of the deceleration segment and the start of the acceleration segment
        double decelSpeedCutoff = calculateSpeedForwards(0, decelEndFinal, vDown, aDown);
        double accelStartSpeed = calculateSpeedBackwards(accelStartFinal, L, vU, aUp);
        SegmentPiece d =  new SegmentPiece(0, decelEndFinal, vDown, decelSpeedCutoff, UFunction.FunctionType.DECELERATION, aDown);
        SegmentPiece e =  new SegmentPiece(accelStartFinal, L, accelStartSpeed, vU, UFunction.FunctionType.ACCELERATION, aUp);    

        UFunction dFunction = new UFunction();
        dFunction.addSegment(d);
        UFunction eFunction = new UFunction();
        eFunction.addSegment(e);

        boolean dReachesZero = (d.endSpeed == 0);
        boolean eReachesZero = (e.initialSpeed== 0);
    
        // Step 5: Check intersection of d(x) and e(x)
        double intersectionPoint = findIntersection(d, e);

        if (intersectionPoint >= 0 && intersectionPoint <= L) {
    
            UFunction witness = constructMaxWitness(d, e, intersectionPoint);
            double T_max_de = witness.integrateOneOverU();
    
            if (T_max_de >= deltaT) {
                vUpF = vU;
            } else {
                if (dReachesZero) {
                    vUpF = calculateVFinalUpFixed(vDown, aDown, aUp, deltaT, L, decelEndFinal, d, e);
                } else {
                    double T_d = dFunction.integrateOneOverU();
    
                    if (T_d > deltaT) {
                        vUpF = calculateVFinalUpFixed(vDown, aDown, aUp, deltaT, L, decelEndFinal, d, e);
                    } else {//We don't have enough distance to slow down enough
                        vUpF = Double.NaN;
                    }
                }
            }
        } else {//d(x) and e(x) do not intersect
            if (dReachesZero && eReachesZero) {
                // Case 1: Both curves reach zero
                vUpF = vU;
            } else if (eFunction.getSpeed(0) > dFunction.getSpeed(0)) {
                // Case 2: e(x) reaches x=0 above d(x)
                double T_e = eFunction.integrateOneOverU();

                if (T_e >= deltaT) {
                    // e(x) is slow enough
                    vUpF = vU;
                } else {
                    // same procedure as above
                    if (dReachesZero) {
                        vUpF = calculateVFinalUpFixed(vDown, aDown, aUp, deltaT, L, decelEndFinal, d, e);
                    } else {
                        double T_d = dFunction.integrateOneOverU();
        
                        if (T_d > deltaT) {
                            vUpF = calculateVFinalUpFixed(vDown, aDown, aUp, deltaT, L, decelEndFinal, d, e);
                        } else {
                            vUpF = Double.NaN;
                        }
                    }
                }
            }
        }

        // -------------------------------
        // Compute Minimum Final Speed (v_down_f)
        // -------------------------------
    

        double maxVStart = calculateSpeedBackwards(0, L, 0, aDown);
        SegmentPiece f = new SegmentPiece(0, L, maxVStart, 0, UFunction.FunctionType.DECELERATION, aDown);

    
        intersectionPoint = findIntersection(f);
        double xIntMin = intersectionPoint >= 0 && intersectionPoint <= L ? intersectionPoint : -1;
    
        double witnessTimeMin;
        if (xIntMin >= 0) {//U(x) and f(x) intersect
            UFunction witness = this.getUFunctionUpTo(xIntMin);
            double newVStart = calculateSpeedBackwards(xIntMin, L, 0, aDown);
            SegmentPiece fPiece = new SegmentPiece(xIntMin, L, newVStart, 0, UFunction.FunctionType.DECELERATION, aDown);
            witness.addSegment(fPiece);
            witnessTimeMin = witness.integrateOneOverU();
        } else {//U(x) and f(x) do not intersect
            UFunction witness = new UFunction();
            witness.addSegment(f);
            witnessTimeMin = witness.integrateOneOverU();
        }
    
        if (witnessTimeMin <= deltaT) {
            vDownF = 0.0;//We reach speed zero before the time is up and wait at the end.
        } else {
            //We don't have enough space to slow down and must find an exact witness.
            vDownF = calculateVDownFinal(vDown, aDown, deltaT, L);
        }
    
    
        if (!Double.isNaN(vUpF) && !Double.isNaN(vDownF)) {
            return new double[]{vDownF, vUpF};
        } else {
            return new double[]{Double.NaN, Double.NaN};
        }
    }
    

    
    /**
     * Calculates the intersection point of two speed curves defined by SegmentPiece instances.
     *
     * @param s1 The first SegmentPiece representing the first curve.
     * @param s2 The second SegmentPiece representing the second curve.
     * @return   The x-coordinate where the two curves intersect within their segments, or null if no intersection exists.
     */
    public static double findIntersection(SegmentPiece s1, SegmentPiece s2) {
        // Extract accelerations, initial speeds, and start positions from both segments
        double a1 = s1.acceleration;
        double a2 = s2.acceleration;
        double v1 = s1.initialSpeed;
        double v2 = s2.initialSpeed;
        double x1 = s1.startX;
        double x2 = s2.startX;
    
        // Check if both segments have the same acceleration
        if (a1 == a2) {
            if (v1 == v2) {
                // If accelerations and initial speeds are identical, the curves overlap infinitely
                return -1; // Indicating infinite intersections
            } else {
                // If accelerations are the same but speeds differ, they are parallel and do not intersect
                return -1; // No intersection
            }
        }
    
        // Solve for the intersection point using the kinematic equations
        // x = (v2^2 - v1^2 + 2 * a1 * x1 - 2 * a2 * x2) / (2 * (a1 - a2))
        double numerator = Math.pow(v2, 2) - Math.pow(v1, 2) + 2 * a1 * x1 - 2 * a2 * x2;
        double denominator = 2 * (a1 - a2);
        double x = numerator / denominator;
    
        // Check if the intersection lies within the overlapping range of the segments
        double overlapStart = Math.max(s1.startX, s2.startX);
        double overlapEnd = Math.min(s1.endX, s2.endX);
    
        if (x >= overlapStart && x <= overlapEnd) {
            // Valid intersection within both segments
            return x;
        } else {
            // Intersection does not lie within the valid overlapping range of the segments
            return -1;
        }
    }
    

    // Function for constructing the maximum witness profile
    UFunction constructMaxWitness(SegmentPiece d, SegmentPiece e, double xInt) {
        // Implement witness construction by taking max(d, e) up to xInt
        UFunction uFunction= new UFunction();

        double endSpeedOfD=calculateSpeedForwards(d.startX, xInt,d.initialSpeed,d.acceleration);
        // Add d up until the IntersectionPoint
        if(xInt!=0){
            uFunction.addSegment(new SegmentPiece(0, xInt, d.initialSpeed, endSpeedOfD , UFunction.FunctionType.DECELERATION, d.acceleration));
        }
        // Add e from the IntersectionPoint
        if(xInt!=this.pieces.getFirst().endX){
            uFunction.addSegment(new SegmentPiece(xInt, this.pieces.getFirst().endX, endSpeedOfD, calculateSpeedForwards(xInt, this.pieces.getFirst().endX, endSpeedOfD, e.acceleration), UFunction.FunctionType.ACCELERATION, e.acceleration));
        }

        return uFunction;
    }

    // Function for solving for v_up_f using the closed-form formula
    public static double calculateVFinalUpWrong(double vDown, double aDown, double aUp, double deltaT, double totalLength) {
        System.out.println("Inputs" + vDown + " " + aDown + " " + aUp + " " + deltaT + " " + totalLength);
        // Step 1: Compute x'
        double numerator1 = Math.pow(vDown + aUp * deltaT, 2) * (-2 * totalLength + 2 * vDown * deltaT + aUp * Math.pow(deltaT, 2));
        double denominator1 = aUp - aDown;

        // Step 2: Compute the square root term
        double sqrtTerm = Math.sqrt(numerator1 / denominator1);

        // Step 3: Compute the second numerator and denominator for the second term
        double numerator2 = 2 * aUp * totalLength
                            + 2 * aDown * vDown * deltaT
                            - 2 * Math.pow(aUp, 2) * Math.pow(deltaT, 2)
                            + aUp * deltaT * (-4 * vDown + aDown * deltaT);
        double denominator2 = 2 * (aUp - aDown);

        // Step 4: Compute x'
        double xPrime = -sqrtTerm - (numerator2 / denominator2);

        // Step 5: Compute v_up_f
        double vFinalSquared = Math.pow(vDown, 2) + 2 * aDown * xPrime + 2 * aUp * (totalLength - xPrime);
        double vFinalUp = Math.sqrt(vFinalSquared);

        // Step 2: Compute v↑,f
        return vFinalUp;
    }

    public double calculateVFinalUpFixed(double vDown, double aDown, double aUp, double deltaT, double totalLength, double decelEndFinal, SegmentPiece d, SegmentPiece e) {
        double xPrime = calculateXPrime(decelEndFinal, vDown, aDown, aUp, deltaT, totalLength);
        double vUpF = Double.NaN;
        //Calculate if XPrime is a viable solution
        UFunction witness= constructMaxWitness(d, e, xPrime);
        
        double witnessArea = witness.integrateOneOverU();
        if(Math.abs(witnessArea-deltaT)<1e-6){//In this case xPrime is a real solution
            vUpF = witness.getSpeed(totalLength);
        }else{
            witness= constructMaxWitness(d, e, decelEndFinal);//In this case we have to stop at the end of the deceleration segment and wait
            vUpF = witness.getSpeed(totalLength);
        }
        return vUpF;
    }

    public static double calculateXPrime(double decelEnd, double vDown, double aDown, double aUp, double deltaT, double totalLength){
        // Define the function to minimize
        Function<Double, Double> equation = (xPrime) -> {
            // First term
            double T1 = (Math.sqrt(vDown * vDown + 2 * aDown * xPrime) - vDown) / aDown;

            // Second term
            double T2 = (Math.sqrt(vDown * vDown + 2 * aDown * xPrime + (2 * totalLength - 2 * xPrime) * aUp)
                         - Math.sqrt(vDown * vDown + 2 * aDown * xPrime)) / aUp;

            // Total time difference
            return Math.abs(T1 + T2 - deltaT);
        };

        // Perform a bounded search using a golden section search
        double tolerance = 1e-6; // Convergence tolerance
        double result = goldenSectionSearch(equation, 0, decelEnd, tolerance);

        // Return the result or NaN if no valid solution is found
        return Double.isNaN(result) ? Double.NaN : result;
    }

    //Find a solution to the equation using the golden section search method
    private static double goldenSectionSearch(Function<Double, Double> func, double lowerBound, double upperBound, double tolerance) {
        double gr = (Math.sqrt(5) + 1) / 2; // Golden ratio

        double c = upperBound - (upperBound - lowerBound) / gr;
        double d = lowerBound + (upperBound - lowerBound) / gr;

        while (Math.abs(upperBound - lowerBound) > tolerance) {
            if (func.apply(c) < func.apply(d)) {
                upperBound = d;
            } else {
                lowerBound = c;
            }
            c = upperBound - (upperBound - lowerBound) / gr;
            d = lowerBound + (upperBound - lowerBound) / gr;
        }

        return (upperBound + lowerBound) / 2; // Midpoint of the final interval
    }

    private double calculateVDownFinal(double vDown, double aDown, double deltaT, double L) {

        // Step 1: Initialize binary search bounds for vEnd
        double vEndLower = 0;
        double vEndUpper = this.getSpeed(L);  // Maximum speed at the end of U(x)
        double vEndCurrent = (vEndLower + vEndUpper) / 2;
        double tolerance = 1e-6;  // Precision for matching deltaT
        double travelTime = Double.MAX_VALUE;  // Initialize travel time
        SegmentPiece f = null;
        // Step 4: Perform binary search
        int iteration = 0;
        while (Math.abs(travelTime - deltaT) > tolerance) {
            iteration++;
            // Step 4.1: Construct f(x) for the current vEnd
            double maxVStart = calculateSpeedBackwards(0, L, vEndCurrent, aDown);
    
            f = new SegmentPiece(0, L, maxVStart, vEndCurrent, UFunction.FunctionType.DECELERATION, aDown);
            //System.out.println("SegmentPiece f: " + f);
    
            // Step 4.2: Find the intersection point
            double xIntMin = findIntersection(f);
            //System.out.println("Intersection Point: " + xIntMin);
    
            // Step 4.3: Calculate the total travel time for U(x) + f(x)
            UFunction witness = this.getUFunctionUpTo(xIntMin);
            
    
            // Add new f(x) to the witness
            f = new SegmentPiece(xIntMin, L, calculateSpeedBackwards(xIntMin, L, vEndCurrent, aDown), vEndCurrent, UFunction.FunctionType.DECELERATION, aDown);
            witness.addSegment(f);
            // Integrate to calculate travel time
            travelTime = witness.integrateOneOverU();
    
            // Step 4.4: Adjust vEnd based on the travel time
            if (travelTime > deltaT) {
                vEndLower = vEndCurrent;
            } else {
                vEndUpper = vEndCurrent;
            }
    
            // Update vEndCurrent to the midpoint
            vEndCurrent = (vEndLower + vEndUpper) / 2;
            // Optional: Add an iteration limit to prevent infinite loops
            if (iteration > 100) {
                //System.out.println("Iteration limit reached.");
                break;
            }
        }
    
        // Step 5: Return the final adjusted vEnd
        return vEndCurrent;
    }
    
}

/**
 * Constructs the U(x) function from a list of PathSegments.
 *
 * @param pathSegments List of PathSegment objects.
 * @param vStart       Initial speed at the start of the route (m/s).
 * @param aUp          Maximum acceleration bound (m/s²).
 * @param aDown        Maximum deceleration bound (negative value, m/s²).
 * @return UFunction representing the piecewise upper bound of speeds.
 */
class UFunctionBuilder {

    /**
     * Constructs the U(x) function from a list of PathSegments.
     *
     * @param pathSegments List of PathSegment objects.
     * @param vStartMax       Initial speed at the start of the route (m/s).
     * @param aUp          Maximum acceleration bound (m/s²).
     * @param aDown        Maximum deceleration bound (negative value, m/s²).
     * @return UFunction representing the piecewise upper bound of speeds.
     */
    public static UFunction sisToUFunction(List<PathSegment> pathSegments, double vStartMin, double vStartMax, double a) {
        if (vStartMax < 0 || a <= 0) {
            throw new IllegalArgumentException("Initial speed must be non-negative, and acceleration/deceleration must be valid.");
        }
        double aUp = a;
        double aDown = -a;
    
        Stack<UFunction.SegmentPiece> stack = new Stack<>();
        double currentSpeed = vStartMax;
        double currentX = 0;
    
        for (int i = 0; i < pathSegments.size(); i++) {
            PathSegment segment = pathSegments.get(i);
            double vMax = segment.getSpeedLimit();
            double length = segment.getLength();
            double segmentEndX = currentX + length;
        
            if (currentSpeed == vMax) {
                // Case 1: Maintain constant speed
                stack.push(new UFunction.SegmentPiece(currentX, segmentEndX,currentSpeed, vMax, UFunction.FunctionType.CONSTANT, 0));
                currentX = segmentEndX;
            } else if (currentSpeed < vMax) {
                // Case 2: Acceleration required
                double accelDistance = (vMax * vMax - currentSpeed * currentSpeed) / (2 * aUp);
        
                if (accelDistance <= length) {
                    // Acceleration completes within the segment
                    stack.push(new UFunction.SegmentPiece(currentX, currentX + accelDistance,currentSpeed, vMax, UFunction.FunctionType.ACCELERATION, aUp));
        
                    // Update state
                    currentSpeed = vMax;
                    currentX += accelDistance;
        
                    // Add remaining part of the segment as constant-speed (if applicable)
                    if (currentX < segmentEndX) {
                        stack.push(new UFunction.SegmentPiece(currentX, segmentEndX, currentSpeed, vMax, UFunction.FunctionType.CONSTANT, 0));
                        currentX = segmentEndX;
                    }
                } else {
                    // Acceleration spans the entire segment
                    double newSpeed = Math.sqrt(currentSpeed * currentSpeed + 2 * aUp * length);
                    stack.push(new UFunction.SegmentPiece(currentX, segmentEndX, currentSpeed, newSpeed, UFunction.FunctionType.ACCELERATION, aUp));
                    currentSpeed=newSpeed;
                    currentX = segmentEndX;
                }
            } else { // currentSpeed > vMax
                // Case 3: Deceleration required
                double vFinal = vMax;
                double xDecelEnd = segmentEndX - length ; // Deceleration ends at the current segment's start
                boolean intersectionFound = false;
        
                while (!stack.isEmpty()) {
                    UFunction.SegmentPiece lastPiece = stack.pop();
                    double lastStartX = lastPiece.startX;
                    double lastEndX = lastPiece.endX;
                    double lastInitialSpeed = lastPiece.initialSpeed;
        
                    // Check intersection of deceleration curve with the last segment
                    double intersectionX = (vFinal * vFinal - lastInitialSpeed * lastInitialSpeed 
                                            + 2 * lastPiece.acceleration * lastStartX 
                                            - 2 * aDown * xDecelEnd)
                                            / (2 * (lastPiece.acceleration - aDown));
        
                    if (intersectionX >= lastStartX && intersectionX <= lastEndX) {
                        //Calculate the deceleration speed at the intersection point
                        double vAtIntersection = Math.sqrt(lastInitialSpeed * lastInitialSpeed + 2 * lastPiece.acceleration * (intersectionX - lastStartX));

                        // Intersection found: split the segment
                        if (intersectionX > lastStartX) {
                            // Truncate the previous segment up to the intersection point
                            stack.push(new UFunction.SegmentPiece(lastStartX, intersectionX,lastInitialSpeed, vAtIntersection, lastPiece.functionType, lastPiece.acceleration));
                        }

            
                        // Add the deceleration segment from the intersection to the end of the current segment
                        stack.push(new UFunction.SegmentPiece(intersectionX, xDecelEnd,vAtIntersection, vFinal, UFunction.FunctionType.DECELERATION, aDown));
                        // Add the continue speed of the current segment
                        stack.push(new UFunction.SegmentPiece(xDecelEnd, segmentEndX,vFinal, vFinal, UFunction.FunctionType.CONSTANT, 0));


                        // Update the deceleration start point
                        intersectionFound = true;
                        break;
                    }
                }
        
                // Handle case where no intersection was found
                if (!intersectionFound) {
                    // Check deceleration curve speed at x=0
                    double speedAtZero = Math.sqrt(vFinal * vFinal - 2 * aDown * xDecelEnd);
                    if (speedAtZero < vStartMin || (vFinal * vFinal - 2 * aDown * xDecelEnd)<0) {
                        //System.out.println("No valid witness found: Deceleration curve does not match initial speed at x=0.");
                        return null;
                    }else{
                        //To Do fix this here
                        if(xDecelEnd!=0){
                            stack.push(new UFunction.SegmentPiece(0, xDecelEnd,speedAtZero, vFinal, UFunction.FunctionType.DECELERATION, aDown));
                            stack.push(new UFunction.SegmentPiece(xDecelEnd, segmentEndX,vFinal, vFinal, UFunction.FunctionType.CONSTANT, 0));
                        }
                        else{
                            stack.push(new UFunction.SegmentPiece(0, segmentEndX,vFinal, vFinal, UFunction.FunctionType.CONSTANT, 0));
                        }
                    }
                }
        
                // Update current speed and position
                currentSpeed = vMax;
                currentX = segmentEndX;
            }
        
        }
            
                // Combine stack into UFunction
                UFunction uFunction = new UFunction();
                while (!stack.isEmpty()) {
                    UFunction.SegmentPiece piece = stack.pop();
                    uFunction.addSegment(piece.startX, piece.endX,piece.initialSpeed, piece.endSpeed, piece.functionType, piece.acceleration);
                }
            
                return uFunction;
    }

    public static void main(String[] args) {
        // Example segments
        List<PathSegment> pathSegments = new ArrayList<>();
        pathSegments.add(new PathSegment(150, 40));
        pathSegments.add(new PathSegment(10, 10));
        pathSegments.add(new PathSegment(100, 10)); // Requires deceleration

        double vStartMin = 0;  // Initial speed
        double vStartMax = 30; // Initial speed bound
        double a = 5.0; // Acceleration bound

        UFunction uFunction = sisToUFunction(pathSegments, vStartMin, vStartMax, a);
        System.out.println(uFunction);
        double[] speedInterval = uFunction.computeSpeedInterval(vStartMin, vStartMax, -a, a, 17.5);
        System.out.println(speedInterval[0] + " " + speedInterval[1]);
    }
}


/**
 * Tests for the core data structures.
 */
public class ConsistencyCheck {

    /**
     * Converts a list of edges to a list of path segments.
     * @param edges     List of edges to convert
     * @param streetToSpeed    Dictionary mapping street types to speed limits
     * @return
     */
    public static List<PathSegment> convertEdgesToPathSegments(List<Edge> edges, double slack, HashMap<String, Integer> streetToSpeed) {
        List<PathSegment> pathSegments = new ArrayList<>();
        if (edges == null || edges.isEmpty()) {
            return pathSegments; // Return empty list for null or empty input
        }
    
        // Initialize the first segment properties
        double totalLength = edges.get(0).distance;
        double speedLimit = parseSpeed(edges.get(0),slack , streetToSpeed);
    
        for (int i = 1; i < edges.size(); i++) {
            Edge currentEdge = edges.get(i);
    
            // Determine the speed for the current edge
            double currentSpeedLimit = parseSpeed(currentEdge,slack, streetToSpeed);
    
            // Check if current edge has the same speed limit as the previous one
            if (currentSpeedLimit == speedLimit) {
                // Extend the current segment
                totalLength += currentEdge.distance;
            } else {
                // Finalize the current segment and start a new one
                pathSegments.add(new PathSegment(totalLength, speedLimit));
    
                // Reset for the new segment
                totalLength = currentEdge.distance;
                speedLimit = currentSpeedLimit;
            }
        }
    
        // Add the last segment
        pathSegments.add(new PathSegment(totalLength, speedLimit));
    
        return pathSegments;
    }
    
    /**
     * Parses the speed from an edge. If the speed is not a valid number (e.g., NaN, unclassified, unknown),
     * fetch the speed from the streetToSpeed map.
     */
    private static double parseSpeed(Edge edge, double  slack, HashMap<String, Integer> streetToSpeed) {
        double speed;
    
        // Try to parse the maxSpeed value as a number
        try {
            speed = Double.parseDouble(edge.maxSpeed);
            speed *= slack; // Adjust for safety margin
            speed /= 3.6; // Convert km/h to m/s
        } catch (NumberFormatException e) {
            speed = Double.NaN; // Mark as invalid if parsing fails
        }
    
        // If speed is NaN or maxSpeed is not a valid number, look up in streetToSpeed map
        if (Double.isNaN(speed)) {
            speed = slack*streetToSpeed.getOrDefault(edge.streetType, 0) / 3.6; // Convert km/h to m/s
        }
    
        return speed;
    }


  @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
     
        //Dictionary for mapping streetstypes to speedlimits
        // Create the dictionary
        HashMap<String, Integer> streetToSpeed = new HashMap<>();

        // Add entries
        streetToSpeed.put("motorway", 400);
        streetToSpeed.put("motorway_link", 150);
        streetToSpeed.put("trunk", 100);
        streetToSpeed.put("trunk_link", 100);
        streetToSpeed.put("primary", 100);
        streetToSpeed.put("primary_link", 100);
        streetToSpeed.put("secondary", 50);
        streetToSpeed.put("secondary_link", 50);
        streetToSpeed.put("tertiary", 50);
        streetToSpeed.put("residential", 30);
        streetToSpeed.put("unclassified", 50);

        //The relevant input paramters for the test
        double[] initialSpeedInterval = {0,5};
        int numberOfTrackedIntervalls = 20;
        int c = 2;
        double slack = 1;
        double timebuffer = 2;
        //For Debugging
        StringBuilder logBuilder = new StringBuilder(); // Accumulate the output

        // Create GPXLoader instance
        GPXLoader gpxLoader = new GPXLoader();

        // Load the relevant files
        File gpxFile = new File("./TestData/test.gpx");
        
        String streetGridString = "./input/grid.jsonl";
        String streetGraphString = "./input/graph.jsonl";
        
        long startTime1 = System.nanoTime();
        GPXData gpxData = gpxLoader.loadGPXTrack(gpxFile);
        long endTime1 = System.nanoTime();
        System.out.println("Time to load GPX: " + (endTime1 - startTime1) / 1_000_000.0 + " ms");

        // Load the StreetGrid
        long startTime2 = System.nanoTime();
        StreetGrid streetGrid = StreetGridLoader.loadStreetGrid(streetGridString);
        long endTime2 = System.nanoTime();
        System.out.println("Time to load grid: " + (endTime2 - startTime2) / 1_000_000.0 + " ms");
        
        Graph graph = new Graph();
        try {
            // Load the graph
            long startTime3 = System.nanoTime();
            graph.readFromJsonl(streetGraphString);
            long endTime3 = System.nanoTime();
             System.out.println("Time to load graph: " + (endTime3 - startTime3) / 1_000_000.0 + " ms");
        } catch (IOException e) {
            System.err.println("Error reading JSONL file: " + e.getMessage());
        }

        long startTime4 = System.nanoTime();

        List<PathSegment> SIS = new ArrayList<>();

        //Generate the List of TrackPoints
        List<TimedGeoPosition> trackPoints = gpxData.getTrackPoints();
        
        

        //We initialize the first DataPoint to have no predecessor and the initial speed interval
        
        //Initialize Intervall Datastructure
        DataPoint[] dataPoints = new DataPoint[trackPoints.size()];
        dataPoints[0] = new DataPoint(trackPoints.get(0),numberOfTrackedIntervalls);

        //Create the dummy references for the first TrackPoint
        List<ClosestStreetResult> currentCandidates = streetGrid.findClosestStreets(trackPoints.get(0),c);
        int i=0;
        for (ClosestStreetResult currentCandidate : currentCandidates) {
            CandidateInterval candidateInterval =  new CandidateInterval(initialSpeedInterval, null, currentCandidate,0);
            CandidateData candidateData = new CandidateData(currentCandidate,numberOfTrackedIntervalls);
            candidateData.addInterval(candidateInterval);
            dataPoints[0].setCandidate(i, candidateData);
            i++;
        }
        
        //For each TrackPoint
        for (i = 0; i < trackPoints.size() - 1; i++) {
            boolean foundValidInterval = false;
            TimedGeoPosition current = trackPoints.get(i);
            TimedGeoPosition next = trackPoints.get(i + 1);
            logBuilder.append("\n");
            logBuilder.append("\n");
            logBuilder.append("Current: ").append(current.getPosition()).append("\n");
            logBuilder.append("Next: ").append(next.getPosition()).append("\n");

            // Create for each Geoposition c candidates
            currentCandidates = streetGrid.findClosestStreets(current, c);
            List<ClosestStreetResult> nextCandidates = streetGrid.findClosestStreets(next, c);

            // Initiate New DataPoint with its c Candidates
            dataPoints[i + 1] = new DataPoint(next, c);

            // Find the c^2 routes between the two candidates and create a segment for each pair
            for (ClosestStreetResult currentCandidate : currentCandidates) {

                logBuilder.append("Current Candidate: ").append(currentCandidate.getPosition().getPosition()).append("\n");

                // Extract all the CandidateIntervals for the outgoing node
                ArrayList<CandidateInterval> candidateIntervals = dataPoints[i].getCandidateIntervall(currentCandidate);
                if (candidateIntervals == null){
                    logBuilder.append("No intervals found for Current Candidate: ").append(currentCandidate.getPosition().getPosition()).append("\n");
                    continue;
                }


                // Extract all relevant Intervals for the currentCandidate
                for (ClosestStreetResult nextCandidate : nextCandidates) {
                    logBuilder.append("Next Candidate: ").append(nextCandidate.getPosition().getPosition()).append("\n");

                    // Create or Access the interval List for the nextCandidate
                    CandidateData candidateData = dataPoints[i + 1].getCandidateData(nextCandidate, numberOfTrackedIntervalls);

                    // Check if the two candidates are the same
                    if (currentCandidate.getPosition().getPosition().getLatitude() == nextCandidate.getPosition().getPosition().getLatitude() &&
                        currentCandidate.getPosition().getPosition().getLongitude() == nextCandidate.getPosition().getPosition().getLongitude()) {    
                        logBuilder.append("Same Candidate: ").append("\n");
                        for (CandidateInterval interval : candidateIntervals) {

                            // Set the SpeedInterval to the Current Interval's values
                            double[] speedInterval = interval.getInterval();
                            // Create a new interval with the new speed interval
                            CandidateInterval newInterval = new CandidateInterval(speedInterval, interval, nextCandidate,0);

                            // Add the new interval to the interval List
                            candidateData.addInterval(newInterval);
                        }
                        continue;
                    }

                    if (candidateIntervals.size() == 0) {
                        logBuilder.append("No intervals found for Current Candidate: ").append(currentCandidate.getPosition().getPosition()).append("\n");
                        continue;
                        
                    }

                    // Generate the shortest path between two candidate locations
                    Map<String, Object> result = graph.dijkstraBetweenClosestStreetResults(nextCandidate, currentCandidate);
                    double[] routeEvaluation = PathAnalyzer.analyzePath(result);
                    logBuilder.append("Routelength: ").append(routeEvaluation[0]).append("\n");

                    double scorelength = routeEvaluation[0]+routeEvaluation[1]*100;


                    // Create the SIS from the shortest path
                    SIS = convertEdgesToPathSegments((List<Edge>) result.get("edges"), slack, streetToSpeed);
                    

                    if (SIS.size() == 0) {
                        logBuilder.append("No route found").append("\n");
                        continue;
                    }

                    // For each interval in the interval List
                    for (CandidateInterval interval : candidateIntervals) {

                        logBuilder.append("Current Interval: ").append(interval.getInterval()[0] + " " + interval.getInterval()[1]).append("\n");

                        // Set the SpeedInterval to the Current Interval's values
                        double[] speedInterval = interval.getInterval();

                        // Create U(x) from the SIS
                        UFunction uFunction = UFunctionBuilder.sisToUFunction(SIS, speedInterval[0], speedInterval[1], 5);
                        if (uFunction == null) {
                            logBuilder.append("Invalid UFunction, skipping...").append("\n");
                            continue;                            
                        }
                        double[] tempSpeedInterval = uFunction.computeSpeedInterval(speedInterval[0], speedInterval[1], -5, 5, ((next.getTimestamp() - current.getTimestamp()) / 1000) + timebuffer);

                        if (Double.isNaN(tempSpeedInterval[0]) || Double.isNaN(tempSpeedInterval[1])) {
                            logBuilder.append("Invalid Temp Speed Interval, skipping...").append("\n");
                            continue;
                        }
                        // Create a new interval with the new speed interval
                        CandidateInterval newInterval = new CandidateInterval(tempSpeedInterval, interval, nextCandidate,scorelength);

                        // Add the new interval to the interval List
                        candidateData.addInterval2(newInterval);
                        logBuilder.append("Try to add Intervall: ").append(newInterval).append("\n");
                        foundValidInterval = true;
                    }
                }
            }
            if(!foundValidInterval){
                logBuilder.append("No valid interval found for TrackPoint: ").append(trackPoints.get(i).getPosition()).append("\n");
                // Write the accumulated log to a file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("./CHECK.txt"))) {
                    writer.write(logBuilder.toString());
                } catch (IOException e) {
                    System.err.println("Error writing log to file: " + e.getMessage());
                }
                break;
            }
        }

        boolean foundValidInterval = false;
        int j =0;
        for (CandidateData candidate : dataPoints[dataPoints.length-1].getAllCandidates()) {
            System.out.println(candidate);
            if (candidate != null) {
                for (CandidateInterval interval : candidate.getIntervals()) {
                    double[] speedInterval = interval.getInterval();
                    if (speedInterval != null &&
                        !Double.isNaN(speedInterval[0]) &&
                        !Double.isNaN(speedInterval[1]) &&
                        speedInterval[0] <= speedInterval[1]) {
                        foundValidInterval = true;
                        WitnessReconstructor reconstructor = new WitnessReconstructor();
                        reconstructor.reconstructAndExport(interval, "./viableRoutes/" +  j + ".gpx" , graph);
                        //reconstructor.reconstructAndExport2(interval, "./viableRoutes/" +  j + ".gpx", graph);
                        j++;
                    }
                }
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./CHECK.txt"))) {
            writer.write(logBuilder.toString());
        } catch (IOException e) {
            System.err.println("Error writing log to file: " + e.getMessage());
        }
        System.out.println("Found valid interval: " + foundValidInterval);
        long endTime4 = System.nanoTime();
        System.out.println("Time to perform MapMatch: " + (endTime4 - startTime4) / 1_000_000.0 + " ms");
        
    }
}