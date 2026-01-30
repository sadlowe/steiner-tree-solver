package com.terra.numerica.steiner_tree_solver.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of the Steiner Tree computation.
 */
public class SteinerResult {
    private List<Edge> edges;
    private double totalLength;
    private List<Point> steinerPoints;
    private List<Point> terminalPoints;

    public SteinerResult() {
        this.edges = new ArrayList<>();
        this.steinerPoints = new ArrayList<>();
        this.terminalPoints = new ArrayList<>();
        this.totalLength = 0;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public void addEdge(Edge edge) {
        this.edges.add(edge);
        this.totalLength += edge.getLength();
    }

    public double getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(double totalLength) {
        this.totalLength = totalLength;
    }

    public List<Point> getSteinerPoints() {
        return steinerPoints;
    }

    public void setSteinerPoints(List<Point> steinerPoints) {
        this.steinerPoints = steinerPoints;
    }

    public void addSteinerPoint(Point point) {
        this.steinerPoints.add(point);
    }

    public List<Point> getTerminalPoints() {
        return terminalPoints;
    }

    public void setTerminalPoints(List<Point> terminalPoints) {
        this.terminalPoints = terminalPoints;
    }
}
