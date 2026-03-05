package com.terra.numerica.steiner_tree_solver.service;

import com.terra.numerica.steiner_tree_solver.model.Edge;
import com.terra.numerica.steiner_tree_solver.model.Point;
import com.terra.numerica.steiner_tree_solver.model.SteinerResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for computing Steiner Trees.
 *
 * The Steiner Tree problem asks: given a set of terminal points,
 * find the shortest network of edges that connects all of them,
 * possibly adding new "Steiner points" to reduce total length.
 */
@Service
public class SteinerTreeService {

    /**
     * Solves the Steiner Tree problem for the given points.
     *
     * @param points List of terminal points to connect
     * @return SteinerResult containing edges, Steiner points, and total length
     */
    public SteinerResult solve(List<Point> points) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("At least 2 points are required");
        }

        switch (points.size()) {
            case 2:
                return solveForTwoPoints(points);
            case 3:
                return solveForThreePoints(points);
            case 4:
                return solveForFourPoints(points);
            default:
                // For more than 4 points, use a simple MST approach for now
                return solveWithMST(points);
        }
    }

    /**
     * Solution for 2 points: simply connect them with a straight line.
     * No Steiner point needed.
     */
    private SteinerResult solveForTwoPoints(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);

        Point p1 = points.get(0);
        Point p2 = points.get(1);

        result.addEdge(new Edge(p1, p2));

        return result;
    }

    /**
     * Solution for 3 points: compute the Fermat point (Steiner point).
     *
     * The Fermat point is the point that minimizes the sum of distances
     * to all three vertices. If all angles of the triangle are less than 120°,
     * the Fermat point is inside the triangle. Otherwise, it's at the vertex
     * with the largest angle (>= 120°).
     */
    private SteinerResult solveForThreePoints(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);

        Point p1 = points.get(0);
        Point p2 = points.get(1);
        Point p3 = points.get(2);

        // Calculate angles of the triangle
        double angle1 = calculateAngle(p2, p1, p3); // Angle at p1
        double angle2 = calculateAngle(p1, p2, p3); // Angle at p2
        double angle3 = calculateAngle(p1, p3, p2); // Angle at p3

        // If any angle >= 120°, the optimal solution is a star from that vertex
        double threshold = Math.toRadians(120);

        if (angle1 >= threshold) {
            // Connect p2 and p3 to p1
            result.addEdge(new Edge(p1, p2));
            result.addEdge(new Edge(p1, p3));
        } else if (angle2 >= threshold) {
            // Connect p1 and p3 to p2
            result.addEdge(new Edge(p2, p1));
            result.addEdge(new Edge(p2, p3));
        } else if (angle3 >= threshold) {
            // Connect p1 and p2 to p3
            result.addEdge(new Edge(p3, p1));
            result.addEdge(new Edge(p3, p2));
        } else {
            // All angles < 120°: compute Fermat point
            Point fermatPoint = computeFermatPoint(p1, p2, p3);
            result.addSteinerPoint(fermatPoint);

            // Connect all terminal points to the Fermat point
            result.addEdge(new Edge(fermatPoint, p1));
            result.addEdge(new Edge(fermatPoint, p2));
            result.addEdge(new Edge(fermatPoint, p3));
        }

        return result;
    }

    /**
     * Calculates the angle at vertex B in triangle ABC.
     *
     * @param a Point A
     * @param b Point B (vertex of the angle)
     * @param c Point C
     * @return Angle in radians
     */
    private double calculateAngle(Point a, Point b, Point c) {
        double ba_x = a.getX() - b.getX();
        double ba_y = a.getY() - b.getY();
        double bc_x = c.getX() - b.getX();
        double bc_y = c.getY() - b.getY();

        double dotProduct = ba_x * bc_x + ba_y * bc_y;
        double magnitudeBA = Math.sqrt(ba_x * ba_x + ba_y * ba_y);
        double magnitudeBC = Math.sqrt(bc_x * bc_x + bc_y * bc_y);

        if (magnitudeBA == 0 || magnitudeBC == 0) {
            return 0;
        }

        double cosAngle = dotProduct / (magnitudeBA * magnitudeBC);
        // Clamp to [-1, 1] to avoid numerical errors
        cosAngle = Math.max(-1, Math.min(1, cosAngle));

        return Math.acos(cosAngle);
    }

    /**
     * Computes the Fermat point (Torricelli point) of a triangle.
     *
     * The Fermat point is where all three angles to the vertices are 120°.
     * We use an iterative method (Weiszfeld's algorithm) to find it.
     */
    private Point computeFermatPoint(Point p1, Point p2, Point p3) {
        // Start from the centroid
        double x = (p1.getX() + p2.getX() + p3.getX()) / 3;
        double y = (p1.getY() + p2.getY() + p3.getY()) / 3;

        // Weiszfeld's algorithm - iterative optimization
        for (int i = 0; i < 100; i++) {
            Point current = new Point(x, y);

            double d1 = current.distanceTo(p1);
            double d2 = current.distanceTo(p2);
            double d3 = current.distanceTo(p3);

            // Avoid division by zero
            if (d1 < 1e-10) return p1;
            if (d2 < 1e-10) return p2;
            if (d3 < 1e-10) return p3;

            double w1 = 1.0 / d1;
            double w2 = 1.0 / d2;
            double w3 = 1.0 / d3;
            double totalWeight = w1 + w2 + w3;

            double newX = (p1.getX() * w1 + p2.getX() * w2 + p3.getX() * w3) / totalWeight;
            double newY = (p1.getY() * w1 + p2.getY() * w2 + p3.getY() * w3) / totalWeight;

            // Check convergence
            if (Math.abs(newX - x) < 1e-10 && Math.abs(newY - y) < 1e-10) {
                break;
            }

            x = newX;
            y = newY;
        }

        return new Point(x, y);
    }

    /**
     * Solution for 4 points: try all combinations of 3 points with Steiner point,
     * then connect the 4th point to the Steiner point.
     *
     * For each combination of 3 points:
     * 1. Compute the Steiner solution for those 3 points
     * 2. Connect the 4th point to the Steiner point (if exists) or to the optimal vertex
     * 3. Verify angle constraints (120° rule at Steiner points)
     * 4. Choose the configuration with minimum total length
     */
    private SteinerResult solveForFourPoints(List<Point> points) {
        Point p1 = points.get(0);
        Point p2 = points.get(1);
        Point p3 = points.get(2);
        Point p4 = points.get(3);

        SteinerResult bestResult = null;
        double bestLength = Double.MAX_VALUE;

        // Try all 4 combinations: each time we pick 3 points and connect the 4th
        // Combination 1: Use p1, p2, p3 as base triangle, connect p4
        SteinerResult result1 = tryThreePointsWithFourth(p1, p2, p3, p4);
        if (result1 != null && result1.getTotalLength() < bestLength) {
            bestResult = result1;
            bestLength = result1.getTotalLength();
        }

        // Combination 2: Use p1, p2, p4 as base triangle, connect p3
        SteinerResult result2 = tryThreePointsWithFourth(p1, p2, p4, p3);
        if (result2 != null && result2.getTotalLength() < bestLength) {
            bestResult = result2;
            bestLength = result2.getTotalLength();
        }

        // Combination 3: Use p1, p3, p4 as base triangle, connect p2
        SteinerResult result3 = tryThreePointsWithFourth(p1, p3, p4, p2);
        if (result3 != null && result3.getTotalLength() < bestLength) {
            bestResult = result3;
            bestLength = result3.getTotalLength();
        }

        // Combination 4: Use p2, p3, p4 as base triangle, connect p1
        SteinerResult result4 = tryThreePointsWithFourth(p2, p3, p4, p1);
        if (result4 != null && result4.getTotalLength() < bestLength) {
            bestResult = result4;
            bestLength = result4.getTotalLength();
        }

        // If no valid solution found (shouldn't happen), fall back to MST
        if (bestResult == null) {
            return solveWithMST(points);
        }

        // Set the terminal points
        bestResult.setTerminalPoints(points);
        return bestResult;
    }

    /**
     * Try building a Steiner tree using 3 points as base and connecting the 4th point.
     *
     * @param p1 First point of base triangle
     * @param p2 Second point of base triangle
     * @param p3 Third point of base triangle
     * @param p4 Fourth point to connect
     * @return SteinerResult or null if configuration is invalid
     */
    private SteinerResult tryThreePointsWithFourth(Point p1, Point p2, Point p3, Point p4) {
        SteinerResult result = new SteinerResult();

        // Calculate angles of the base triangle
        double angle1 = calculateAngle(p2, p1, p3);
        double angle2 = calculateAngle(p1, p2, p3);
        double angle3 = calculateAngle(p1, p3, p2);
        double threshold = Math.toRadians(120);

        Point steinerPoint = null;
        boolean hasBaseSteinerPoint = false;

        // Case 1: One of the angles >= 120° in base triangle
        if (angle1 >= threshold) {
            // p1 is the hub for p2 and p3
            result.addEdge(new Edge(p1, p2));
            result.addEdge(new Edge(p1, p3));

            // Connect p4 to the hub p1
            result.addEdge(new Edge(p1, p4));

        } else if (angle2 >= threshold) {
            // p2 is the hub for p1 and p3
            result.addEdge(new Edge(p2, p1));
            result.addEdge(new Edge(p2, p3));

            // Connect p4 to the hub p2
            result.addEdge(new Edge(p2, p4));

        } else if (angle3 >= threshold) {
            // p3 is the hub for p1 and p2
            result.addEdge(new Edge(p3, p1));
            result.addEdge(new Edge(p3, p2));

            // Connect p4 to the hub p3
            result.addEdge(new Edge(p3, p4));

        } else {
            // Case 2: All angles < 120°, compute Fermat point for base triangle
            steinerPoint = computeFermatPoint(p1, p2, p3);
            hasBaseSteinerPoint = true;

            result.addSteinerPoint(steinerPoint);
            result.addEdge(new Edge(steinerPoint, p1));
            result.addEdge(new Edge(steinerPoint, p2));
            result.addEdge(new Edge(steinerPoint, p3));

            // Now connect p4 to the Steiner point
            // But first, check if the angle constraint is satisfied
            // At the Steiner point, we need to verify that adding p4 maintains valid angles

            // Calculate the angle that p4 would make with existing connections
            // For a valid Steiner point with 4 connections, angles should be around 90° or
            // we might need a second Steiner point

            // For simplicity, we connect p4 directly to the first Steiner point
            // A more sophisticated approach would check if we need a second Steiner point
            result.addEdge(new Edge(steinerPoint, p4));

            // Verify angle constraints at the Steiner point with 4 edges
            // If angles are too small, this configuration might not be optimal
            if (!verifyFourWayAngles(steinerPoint, p1, p2, p3, p4)) {
                // If angles are invalid, try connecting p4 to closest terminal point instead
                result = tryConnectionToClosestTerminal(p1, p2, p3, p4, steinerPoint);
            }
        }

        return result;
    }

    /**
     * Verify that angles at a Steiner point with 4 connections are reasonable.
     * For a 4-way junction, ideal angles would be 90° each, but we allow some flexibility.
     *
     * @return true if the configuration is acceptable
     */
    private boolean verifyFourWayAngles(Point steiner, Point p1, Point p2, Point p3, Point p4) {
        // Calculate all 4 angles at the Steiner point
        double angle1 = calculateAngle(p1, steiner, p2);
        double angle2 = calculateAngle(p2, steiner, p3);
        double angle3 = calculateAngle(p3, steiner, p4);
        double angle4 = calculateAngle(p4, steiner, p1);

        // For a 4-way Steiner point, angles should be close to 90° (π/2)
        // We allow angles between 60° and 120° as acceptable
        double minAngle = Math.toRadians(60);
        double maxAngle = Math.toRadians(120);

        return (angle1 >= minAngle && angle1 <= maxAngle) ||
               (angle2 >= minAngle && angle2 <= maxAngle) ||
               (angle3 >= minAngle && angle3 <= maxAngle) ||
               (angle4 >= minAngle && angle4 <= maxAngle);
    }

    /**
     * Alternative approach: create a solution by connecting p4 to the closest terminal point
     * of the base triangle instead of to the Steiner point.
     */
    private SteinerResult tryConnectionToClosestTerminal(Point p1, Point p2, Point p3, Point p4, Point steiner) {
        SteinerResult result = new SteinerResult();

        // Add the base Steiner point and its connections
        result.addSteinerPoint(steiner);
        result.addEdge(new Edge(steiner, p1));
        result.addEdge(new Edge(steiner, p2));
        result.addEdge(new Edge(steiner, p3));

        // Find closest terminal point to p4
        double dist1 = p4.distanceTo(p1);
        double dist2 = p4.distanceTo(p2);
        double dist3 = p4.distanceTo(p3);

        Point closest = p1;
        if (dist2 < dist1 && dist2 < dist3) {
            closest = p2;
        } else if (dist3 < dist1 && dist3 < dist2) {
            closest = p3;
        }

        // Connect p4 to the closest terminal point
        result.addEdge(new Edge(closest, p4));

        return result;
    }

    /**
     * Simple MST (Minimum Spanning Tree) solution for more than 4 points.
     * Uses Prim's algorithm.
     *
     * Note: This is NOT an optimal Steiner tree, just a simple MST.
     * A true Steiner tree could be shorter by adding Steiner points.
     */
    private SteinerResult solveWithMST(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);

        int n = points.size();
        boolean[] inMST = new boolean[n];
        double[] minDist = new double[n];
        int[] parent = new int[n];

        // Initialize
        for (int i = 0; i < n; i++) {
            minDist[i] = Double.MAX_VALUE;
            parent[i] = -1;
        }
        minDist[0] = 0;

        // Prim's algorithm
        for (int count = 0; count < n; count++) {
            // Find minimum distance vertex not in MST
            int u = -1;
            double minVal = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!inMST[i] && minDist[i] < minVal) {
                    minVal = minDist[i];
                    u = i;
                }
            }

            if (u == -1) break;
            inMST[u] = true;

            // Update distances
            for (int v = 0; v < n; v++) {
                if (!inMST[v]) {
                    double dist = points.get(u).distanceTo(points.get(v));
                    if (dist < minDist[v]) {
                        minDist[v] = dist;
                        parent[v] = u;
                    }
                }
            }
        }

        // Build edges from parent array
        for (int i = 1; i < n; i++) {
            if (parent[i] != -1) {
                result.addEdge(new Edge(points.get(parent[i]), points.get(i)));
            }
        }

        return result;
    }
}
