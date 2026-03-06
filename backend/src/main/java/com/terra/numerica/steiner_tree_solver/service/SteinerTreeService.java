package com.terra.numerica.steiner_tree_solver.service;

import com.terra.numerica.steiner_tree_solver.model.Edge;
import com.terra.numerica.steiner_tree_solver.model.Point;
import com.terra.numerica.steiner_tree_solver.model.SteinerResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SteinerTreeService {

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
                return solveWithMST(points);
        }
    }

    private SteinerResult solveForTwoPoints(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);

        Point p1 = points.get(0);
        Point p2 = points.get(1);

        result.addEdge(new Edge(p1, p2));

        return result;
    }

    private SteinerResult solveForThreePoints(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);

        Point p1 = points.get(0);
        Point p2 = points.get(1);
        Point p3 = points.get(2);

        double angle1 = calculateAngle(p2, p1, p3);
        double angle2 = calculateAngle(p1, p2, p3);
        double angle3 = calculateAngle(p1, p3, p2);

        double threshold = Math.toRadians(120);

        if (angle1 >= threshold) {
            result.addEdge(new Edge(p1, p2));
            result.addEdge(new Edge(p1, p3));
        } else if (angle2 >= threshold) {
            result.addEdge(new Edge(p2, p1));
            result.addEdge(new Edge(p2, p3));
        } else if (angle3 >= threshold) {
            result.addEdge(new Edge(p3, p1));
            result.addEdge(new Edge(p3, p2));
        } else {
            Point fermatPoint = computeFermatPoint(p1, p2, p3);
            result.addSteinerPoint(fermatPoint);

            result.addEdge(new Edge(fermatPoint, p1));
            result.addEdge(new Edge(fermatPoint, p2));
            result.addEdge(new Edge(fermatPoint, p3));
        }

        return result;
    }

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
        cosAngle = Math.max(-1, Math.min(1, cosAngle));

        return Math.acos(cosAngle);
    }

    private Point computeFermatPoint(Point p1, Point p2, Point p3) {
        double x = (p1.getX() + p2.getX() + p3.getX()) / 3;
        double y = (p1.getY() + p2.getY() + p3.getY()) / 3;

        for (int i = 0; i < 100; i++) {
            Point current = new Point(x, y);

            double d1 = current.distanceTo(p1);
            double d2 = current.distanceTo(p2);
            double d3 = current.distanceTo(p3);

            if (d1 < 1e-10) return p1;
            if (d2 < 1e-10) return p2;
            if (d3 < 1e-10) return p3;

            double w1 = 1.0 / d1;
            double w2 = 1.0 / d2;
            double w3 = 1.0 / d3;
            double totalWeight = w1 + w2 + w3;

            double newX = (p1.getX() * w1 + p2.getX() * w2 + p3.getX() * w3) / totalWeight;
            double newY = (p1.getY() * w1 + p2.getY() * w2 + p3.getY() * w3) / totalWeight;

            if (Math.abs(newX - x) < 1e-10 && Math.abs(newY - y) < 1e-10) {
                break;
            }

            x = newX;
            y = newY;
        }

        return new Point(x, y);
    }

    private SteinerResult solveForFourPoints(List<Point> points) {
        Point p1 = points.get(0);
        Point p2 = points.get(1);
        Point p3 = points.get(2);
        Point p4 = points.get(3);

        SteinerResult bestResult = null;
        double bestLength = Double.MAX_VALUE;

        SteinerResult result1 = tryThreePointsWithFourth(p1, p2, p3, p4);
        if (result1 != null && result1.getTotalLength() < bestLength) {
            bestResult = result1;
            bestLength = result1.getTotalLength();
        }

        SteinerResult result2 = tryThreePointsWithFourth(p1, p2, p4, p3);
        if (result2 != null && result2.getTotalLength() < bestLength) {
            bestResult = result2;
            bestLength = result2.getTotalLength();
        }

        SteinerResult result3 = tryThreePointsWithFourth(p1, p3, p4, p2);
        if (result3 != null && result3.getTotalLength() < bestLength) {
            bestResult = result3;
            bestLength = result3.getTotalLength();
        }

        SteinerResult result4 = tryThreePointsWithFourth(p2, p3, p4, p1);
        if (result4 != null && result4.getTotalLength() < bestLength) {
            bestResult = result4;
            bestLength = result4.getTotalLength();
        }

        if (bestResult == null) {
            return solveWithMST(points);
        }

        bestResult.setTerminalPoints(points);
        return bestResult;
    }

    private SteinerResult tryThreePointsWithFourth(Point p1, Point p2, Point p3, Point p4) {
        SteinerResult result = new SteinerResult();

        double angle1 = calculateAngle(p2, p1, p3);
        double angle2 = calculateAngle(p1, p2, p3);
        double angle3 = calculateAngle(p1, p3, p2);
        double threshold = Math.toRadians(120);

        Point steinerPoint = null;
        boolean hasBaseSteinerPoint = false;

        if (angle1 >= threshold) {
            result.addEdge(new Edge(p1, p2));
            result.addEdge(new Edge(p1, p3));

            result.addEdge(new Edge(p1, p4));

        } else if (angle2 >= threshold) {
            result.addEdge(new Edge(p2, p1));
            result.addEdge(new Edge(p2, p3));

            result.addEdge(new Edge(p2, p4));

        } else if (angle3 >= threshold) {
            result.addEdge(new Edge(p3, p1));
            result.addEdge(new Edge(p3, p2));

            result.addEdge(new Edge(p3, p4));

        } else {
            steinerPoint = computeFermatPoint(p1, p2, p3);
            hasBaseSteinerPoint = true;

            result.addSteinerPoint(steinerPoint);
            result.addEdge(new Edge(steinerPoint, p1));
            result.addEdge(new Edge(steinerPoint, p2));
            result.addEdge(new Edge(steinerPoint, p3));

            result.addEdge(new Edge(steinerPoint, p4));

            if (!verifyFourWayAngles(steinerPoint, p1, p2, p3, p4)) {
                result = tryConnectionToClosestTerminal(p1, p2, p3, p4, steinerPoint);
            }
        }

        return result;
    }

    private boolean verifyFourWayAngles(Point steiner, Point p1, Point p2, Point p3, Point p4) {
        double angle1 = calculateAngle(p1, steiner, p2);
        double angle2 = calculateAngle(p2, steiner, p3);
        double angle3 = calculateAngle(p3, steiner, p4);
        double angle4 = calculateAngle(p4, steiner, p1);

        double minAngle = Math.toRadians(60);
        double maxAngle = Math.toRadians(120);

        return (angle1 >= minAngle && angle1 <= maxAngle) ||
               (angle2 >= minAngle && angle2 <= maxAngle) ||
               (angle3 >= minAngle && angle3 <= maxAngle) ||
               (angle4 >= minAngle && angle4 <= maxAngle);
    }

    private SteinerResult tryConnectionToClosestTerminal(Point p1, Point p2, Point p3, Point p4, Point steiner) {
        SteinerResult result = new SteinerResult();

        result.addSteinerPoint(steiner);
        result.addEdge(new Edge(steiner, p1));
        result.addEdge(new Edge(steiner, p2));
        result.addEdge(new Edge(steiner, p3));

        double dist1 = p4.distanceTo(p1);
        double dist2 = p4.distanceTo(p2);
        double dist3 = p4.distanceTo(p3);

        Point closest = p1;
        if (dist2 < dist1 && dist2 < dist3) {
            closest = p2;
        } else if (dist3 < dist1 && dist3 < dist2) {
            closest = p3;
        }

        result.addEdge(new Edge(closest, p4));

        return result;
    }

    private SteinerResult solveWithMST(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);

        int n = points.size();
        boolean[] inMST = new boolean[n];
        double[] minDist = new double[n];
        int[] parent = new int[n];

        for (int i = 0; i < n; i++) {
            minDist[i] = Double.MAX_VALUE;
            parent[i] = -1;
        }
        minDist[0] = 0;

        for (int count = 0; count < n; count++) {
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

        for (int i = 1; i < n; i++) {
            if (parent[i] != -1) {
                result.addEdge(new Edge(points.get(parent[i]), points.get(i)));
            }
        }

        return result;
    }
}
