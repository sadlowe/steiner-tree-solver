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
            default:
                // For more than 3 points, use a simple MST approach for now
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
     * Simple MST (Minimum Spanning Tree) solution for more than 3 points.
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
