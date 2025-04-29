package com.mycompany.masterproject.util;

public class VehicleCalculationsHelper {

    /**
     * Calculates the final speed of a vehicle after traversing a segment.
     *
     * @param initialSpeed Initial speed of the vehicle in meters/second.
     * @param acceleration Acceleration of the vehicle in meters/second^2 (can be negative for deceleration).
     * @param length       Length of the segment in meters.
     * @return Final speed of the vehicle in meters/second.
     */
    public static double calculateFinalSpeed(double initialSpeed, double acceleration, double length) {
        if (initialSpeed < 0 || length < 0) {
            throw new IllegalArgumentException("Initial speed and segment length must be non-negative.");
        }

        double finalSpeedSquared = Math.pow(initialSpeed, 2) + 2 * acceleration * length;
        return Math.sqrt(Math.max(finalSpeedSquared, 0));
    }

    public static void main(String[] args) {
        // Example usage of the helper methods
        double initialSpeed = 10.0; // m/s
        double acceleration = 2.0;  // m/s^2
        double length = 50.0;       // meters

        System.out.printf("Final speed: %.2f m/s%n", calculateFinalSpeed(initialSpeed, acceleration, length));
    }
}
