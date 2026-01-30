package com.terra.numerica.steiner_tree_solver.model;

/**
 * Represents an edge (connection) between two points.
 */
public class Edge {
    private Point start;
    private Point end;

    public Edge() {}

    public Edge(Point start, Point end) {
        this.start = start;
        this.end = end;
    }

    public Point getStart() {
        return start;
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public Point getEnd() {
        return end;
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    /**
     * Calculates the length of this edge.
     */
    public double getLength() {
        return start.distanceTo(end);
    }

    @Override
    public String toString() {
        return String.format("Edge[%s -> %s]", start, end);
    }
}
