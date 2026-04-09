package com.terra.numerica.steiner_tree_solver.service;

import com.terra.numerica.steiner_tree_solver.model.Edge;
import com.terra.numerica.steiner_tree_solver.model.Point;
import com.terra.numerica.steiner_tree_solver.model.SteinerResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SteinerTreeService {

    /**
     * Computes the Euclidean Steiner tree for the given terminal points.
     *
     * @param points list of terminal points (minimum 2)
     * @return the Steiner tree result containing edges and optional Steiner points
     * @throws IllegalArgumentException if fewer than 2 points are provided
     */
    public SteinerResult solve(List<Point> points) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("At least 2 points are required");
        }
        switch (points.size()) {
            case 2:  return solveForTwoPoints(points);
            case 3:  return solveForThreePoints(points);
            case 4:  return solveForFourPoints(points);
            case 5:  return solveForFivePoints(points);
            default: return solveWithSteinerHeuristic(points);
        }
    }

    /**
     * Returns a direct edge between the two terminal points.
     */
    private SteinerResult solveForTwoPoints(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);
        result.addEdge(new Edge(points.get(0), points.get(1)));
        return result;
    }

    /**
     * Computes the Steiner tree for 3 points using the Fermat-Torricelli point.
     * If any angle is at least 120°, that vertex is used directly as the hub.
     */
    private SteinerResult solveForThreePoints(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);

        Point p1 = points.get(0);
        Point p2 = points.get(1);
        Point p3 = points.get(2);

        double threshold = Math.toRadians(120.0);

        if (calculateAngle(p2, p1, p3) >= threshold) {
            result.addEdge(new Edge(p1, p2));
            result.addEdge(new Edge(p1, p3));
        } else if (calculateAngle(p1, p2, p3) >= threshold) {
            result.addEdge(new Edge(p2, p1));
            result.addEdge(new Edge(p2, p3));
        } else if (calculateAngle(p1, p3, p2) >= threshold) {
            result.addEdge(new Edge(p3, p1));
            result.addEdge(new Edge(p3, p2));
        } else {
            Point fermat = computeFermatPoint(p1, p2, p3);
            result.addSteinerPoint(fermat);
            result.addEdge(new Edge(fermat, p1));
            result.addEdge(new Edge(fermat, p2));
            result.addEdge(new Edge(fermat, p3));
        }

        return result;
    }

    /**
     * Computes the Steiner tree for 4 points by exhaustive enumeration of all
     * valid topologies (0, 1, or 2 Steiner points) and returns the shortest one.
     */
    private SteinerResult solveForFourPoints(List<Point> points) {
        Point[] p = {
            points.get(0), points.get(1),
            points.get(2), points.get(3)
        };

        SteinerResult best = solveWithMST(points);
        best.setTerminalPoints(points);
        double bestLength = best.getTotalLength();

        int[][] triplets = { {0,1,2}, {0,1,3}, {0,2,3}, {1,2,3} };
        int[]   lones    = {  3,       2,       1,       0       };

        for (int t = 0; t < 4; t++) {
            int[] tri = triplets[t];
            int   lone = lones[t];

            Point ta = p[tri[0]], tb = p[tri[1]], tc = p[tri[2]];
            Point fermat = computeFermatPoint(ta, tb, tc);
            if (fermat == null) continue;

            boolean atVertex = fermat.distanceTo(ta) < 1e-8
                            || fermat.distanceTo(tb) < 1e-8
                            || fermat.distanceTo(tc) < 1e-8;

            for (int attach : tri) {
                SteinerResult candidate = new SteinerResult();
                candidate.setTerminalPoints(points);

                if (!atVertex) {
                    candidate.addSteinerPoint(fermat);
                }
                candidate.addEdge(new Edge(fermat, ta));
                candidate.addEdge(new Edge(fermat, tb));
                candidate.addEdge(new Edge(fermat, tc));
                candidate.addEdge(new Edge(p[lone], p[attach]));

                if (candidate.getTotalLength() < bestLength) {
                    best = candidate;
                    bestLength = candidate.getTotalLength();
                }
            }
        }

        int[][][] partitions = {
            {{0, 1}, {2, 3}},
            {{0, 2}, {1, 3}},
            {{0, 3}, {1, 2}}
        };

        for (int[][] partition : partitions) {
            int a = partition[0][0], b = partition[0][1];
            int c = partition[1][0], d = partition[1][1];

            SteinerResult candidate = evalTwoSteinerTopology(p, a, b, c, d);
            if (candidate == null) continue;

            candidate.setTerminalPoints(points);
            if (candidate.getTotalLength() < bestLength) {
                best = candidate;
                bestLength = candidate.getTotalLength();
            }
        }

        return best;
    }

    /**
     * Evaluates a topology with two Steiner points S1 and S2 where
     * S1 connects to {Ta, Tb, S2} and S2 connects to {Tc, Td, S1}.
     * Uses alternating Weiszfeld iterations with multiple starting points.
     *
     * @return the best result found, or {@code null} if the topology is invalid
     */
    private SteinerResult evalTwoSteinerTopology(Point[] p, int a, int b, int c, int d) {
        Point ta = p[a], tb = p[b], tc = p[c], td = p[d];

        double cx = (ta.getX() + tb.getX() + tc.getX() + td.getX()) / 4.0;
        double cy = (ta.getY() + tb.getY() + tc.getY() + td.getY()) / 4.0;
        double mabX = (ta.getX() + tb.getX()) / 2.0;
        double mabY = (ta.getY() + tb.getY()) / 2.0;
        double mcdX = (tc.getX() + td.getX()) / 2.0;
        double mcdY = (tc.getY() + td.getY()) / 2.0;

        double[][] inits = {
            { mabX, mabY, mcdX, mcdY },
            { (mabX * 2 + mcdX) / 3.0, (mabY * 2 + mcdY) / 3.0,
              (mcdX * 2 + mabX) / 3.0, (mcdY * 2 + mabY) / 3.0 },
            { (ta.getX() + tb.getX() + cx) / 3.0, (ta.getY() + tb.getY() + cy) / 3.0,
              (tc.getX() + td.getX() + cx) / 3.0, (tc.getY() + td.getY() + cy) / 3.0 },
            { 0.65 * ta.getX() + 0.35 * cx, 0.65 * ta.getY() + 0.35 * cy,
              0.65 * tc.getX() + 0.35 * cx, 0.65 * tc.getY() + 0.35 * cy }
        };

        double   bestLen    = Double.MAX_VALUE;
        double[] bestCoords = null;

        for (double[] init : inits) {
            double[] coords = optimizeTwoSteiner(
                ta, tb, tc, td,
                init[0], init[1], init[2], init[3]
            );
            if (coords == null) continue;

            double s1x = coords[0], s1y = coords[1];
            double s2x = coords[2], s2y = coords[3];
            double len = dist(s1x, s1y, ta.getX(), ta.getY())
                       + dist(s1x, s1y, tb.getX(), tb.getY())
                       + dist(s1x, s1y, s2x, s2y)
                       + dist(s2x, s2y, tc.getX(), tc.getY())
                       + dist(s2x, s2y, td.getX(), td.getY());

            if (len < bestLen) {
                bestLen    = len;
                bestCoords = coords;
            }
        }

        if (bestCoords == null) return null;

        double s1x = bestCoords[0], s1y = bestCoords[1];
        double s2x = bestCoords[2], s2y = bestCoords[3];

        double scale = maxPairDistance(ta, tb, tc, td);
        double eps   = Math.max(1e-8, 1e-4 * scale);

        if (dist(s1x, s1y, s2x, s2y) < eps) return null;
        for (Point t : new Point[]{ ta, tb, tc, td }) {
            if (dist(s1x, s1y, t.getX(), t.getY()) < eps) return null;
            if (dist(s2x, s2y, t.getX(), t.getY()) < eps) return null;
        }

        Point s1 = new Point(s1x, s1y);
        Point s2 = new Point(s2x, s2y);
        if (!checkAngles120(s1, ta, tb, s2)) return null;
        if (!checkAngles120(s2, tc, td, s1)) return null;

        SteinerResult result = new SteinerResult();
        result.addSteinerPoint(s1);
        result.addSteinerPoint(s2);
        result.addEdge(new Edge(s1, ta));
        result.addEdge(new Edge(s1, tb));
        result.addEdge(new Edge(s1, s2));
        result.addEdge(new Edge(s2, tc));
        result.addEdge(new Edge(s2, td));
        return result;
    }

    /**
     * Optimizes S1 and S2 using alternating Weiszfeld (coordinate descent) iterations.
     *
     * @return {@code double[]{s1x, s1y, s2x, s2y}} after convergence
     */
    private double[] optimizeTwoSteiner(
            Point ta, Point tb, Point tc, Point td,
            double s1x, double s1y, double s2x, double s2y) {

        for (int iter = 0; iter < 50_000; iter++) {
            double ps1x = s1x, ps1y = s1y;
            double ps2x = s2x, ps2y = s2y;

            double d1a  = dist(s1x, s1y, ta.getX(), ta.getY());
            double d1b  = dist(s1x, s1y, tb.getX(), tb.getY());
            double d1s2 = dist(s1x, s1y, s2x, s2y);
            if (d1a < 1e-12 || d1b < 1e-12 || d1s2 < 1e-12) break;

            double w1a = 1.0 / d1a, w1b = 1.0 / d1b, w1s2 = 1.0 / d1s2;
            double ws1 = w1a + w1b + w1s2;
            s1x = (ta.getX() * w1a + tb.getX() * w1b + s2x * w1s2) / ws1;
            s1y = (ta.getY() * w1a + tb.getY() * w1b + s2y * w1s2) / ws1;

            double d2c  = dist(s2x, s2y, tc.getX(), tc.getY());
            double d2d  = dist(s2x, s2y, td.getX(), td.getY());
            double d2s1 = dist(s2x, s2y, s1x, s1y);
            if (d2c < 1e-12 || d2d < 1e-12 || d2s1 < 1e-12) break;

            double w2c = 1.0 / d2c, w2d = 1.0 / d2d, w2s1 = 1.0 / d2s1;
            double ws2 = w2c + w2d + w2s1;
            s2x = (tc.getX() * w2c + td.getX() * w2d + s1x * w2s1) / ws2;
            s2y = (tc.getY() * w2c + td.getY() * w2d + s1y * w2s1) / ws2;

            if (Math.abs(s1x - ps1x) < 1e-10 && Math.abs(s1y - ps1y) < 1e-10
             && Math.abs(s2x - ps2x) < 1e-10 && Math.abs(s2y - ps2y) < 1e-10) {
                break;
            }
        }

        return new double[]{ s1x, s1y, s2x, s2y };
    }

    /**
     * Computes the Fermat-Torricelli point of three points using Weiszfeld iterations.
     * Returns the vertex directly if one of its angles is at least 120°.
     * Returns {@code null} if the points are collinear.
     */
    private Point computeFermatPoint(Point p1, Point p2, Point p3) {
        double threshold = Math.toRadians(120.0) - 1e-9;
        if (calculateAngle(p2, p1, p3) >= threshold) return p1;
        if (calculateAngle(p1, p2, p3) >= threshold) return p2;
        if (calculateAngle(p1, p3, p2) >= threshold) return p3;

        double cross = (p2.getX() - p1.getX()) * (p3.getY() - p1.getY())
                     - (p2.getY() - p1.getY()) * (p3.getX() - p1.getX());
        if (Math.abs(cross) < 1e-10) return null;

        double x = (p1.getX() + p2.getX() + p3.getX()) / 3.0;
        double y = (p1.getY() + p2.getY() + p3.getY()) / 3.0;

        for (int i = 0; i < 50_000; i++) {
            double d1 = dist(x, y, p1.getX(), p1.getY());
            double d2 = dist(x, y, p2.getX(), p2.getY());
            double d3 = dist(x, y, p3.getX(), p3.getY());

            if (d1 < 1e-12) return p1;
            if (d2 < 1e-12) return p2;
            if (d3 < 1e-12) return p3;

            double w1 = 1.0/d1, w2 = 1.0/d2, w3 = 1.0/d3, wt = w1+w2+w3;
            double nx = (p1.getX()*w1 + p2.getX()*w2 + p3.getX()*w3) / wt;
            double ny = (p1.getY()*w1 + p2.getY()*w2 + p3.getY()*w3) / wt;

            if (Math.abs(nx - x) < 1e-10 && Math.abs(ny - y) < 1e-10) {
                x = nx; y = ny;
                break;
            }
            x = nx; y = ny;
        }

        return new Point(x, y);
    }

    /**
     * Returns the angle at vertex {@code b} in the triangle (a, b, c), in radians.
     */
    private double calculateAngle(Point a, Point b, Point c) {
        double bax = a.getX() - b.getX(), bay = a.getY() - b.getY();
        double bcx = c.getX() - b.getX(), bcy = c.getY() - b.getY();
        double dot = bax * bcx + bay * bcy;
        double mag = Math.sqrt(bax*bax + bay*bay) * Math.sqrt(bcx*bcx + bcy*bcy);
        if (mag < 1e-15) return 0;
        return Math.acos(Math.max(-1.0, Math.min(1.0, dot / mag)));
    }

    /**
     * Checks that the three edges at Steiner point {@code s} form angles of approximately 120°
     * (tolerance ±10°).
     */
    private boolean checkAngles120(Point s, Point n1, Point n2, Point n3) {
        double tol    = Math.toRadians(10.0);
        double target = Math.toRadians(120.0);
        return Math.abs(calculateAngle(n1, s, n2) - target) < tol
            && Math.abs(calculateAngle(n2, s, n3) - target) < tol
            && Math.abs(calculateAngle(n1, s, n3) - target) < tol;
    }

    private double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Returns the maximum pairwise distance among the four given points. */
    private double maxPairDistance(Point ta, Point tb, Point tc, Point td) {
        Point[] pts = { ta, tb, tc, td };
        double max = 0;
        for (int i = 0; i < 4; i++)
            for (int j = i + 1; j < 4; j++)
                max = Math.max(max, pts[i].distanceTo(pts[j]));
        return max;
    }

    /**
     * Computes the Steiner tree for 5 points by exhaustive enumeration of all
     * topologies with 0, 1, 2, or 3 Steiner points.
     */
    private SteinerResult solveForFivePoints(List<Point> points) {
        Point[] p = {
            points.get(0), points.get(1), points.get(2),
            points.get(3), points.get(4)
        };

        SteinerResult best = solveWithMST(points);
        best.setTerminalPoints(points);
        double bestLen = best.getTotalLength();

        int[][] triplets = {
            {0,1,2},{0,1,3},{0,1,4},
            {0,2,3},{0,2,4},{0,3,4},
            {1,2,3},{1,2,4},{1,3,4},
            {2,3,4}
        };
        int[][] PP = {{0,1,2,3},{0,2,1,3},{0,3,1,2}};

        double scale5 = maxPairDist5(p);
        double eps5   = Math.max(1e-6, 1e-3 * scale5);

        for (int[] tri : triplets) {
            int ti0=tri[0], ti1=tri[1], ti2=tri[2];
            int[] ln = lonesOf5(tri);
            int l0=ln[0], l1=ln[1];

            Point fermat = computeFermatPoint(p[ti0], p[ti1], p[ti2]);
            if (fermat == null) continue;

            boolean atVtx = fermat.distanceTo(p[ti0]) < 1e-6
                         || fermat.distanceTo(p[ti1]) < 1e-6
                         || fermat.distanceTo(p[ti2]) < 1e-6;

            for (int a0 : tri) {
                for (int a1 : new int[]{ti0, ti1, ti2, l0}) {
                    SteinerResult c = new SteinerResult();
                    c.setTerminalPoints(points);
                    if (!atVtx) c.addSteinerPoint(fermat);
                    c.addEdge(new Edge(fermat, p[ti0]));
                    c.addEdge(new Edge(fermat, p[ti1]));
                    c.addEdge(new Edge(fermat, p[ti2]));
                    c.addEdge(new Edge(p[l0], p[a0]));
                    c.addEdge(new Edge(p[l1], p[a1]));
                    if (c.getTotalLength() < bestLen) { best=c; bestLen=c.getTotalLength(); }
                }
            }
            for (int a1 : tri) {
                SteinerResult c = new SteinerResult();
                c.setTerminalPoints(points);
                if (!atVtx) c.addSteinerPoint(fermat);
                c.addEdge(new Edge(fermat, p[ti0]));
                c.addEdge(new Edge(fermat, p[ti1]));
                c.addEdge(new Edge(fermat, p[ti2]));
                c.addEdge(new Edge(p[l1], p[a1]));
                c.addEdge(new Edge(p[l0], p[l1]));
                if (c.getTotalLength() < bestLen) { best=c; bestLen=c.getTotalLength(); }
            }
        }

        for (int tail = 0; tail < 5; tail++) {
            int[] core = coreOf5(tail);
            for (int[] pp : PP) {
                int pa=core[pp[0]], pb=core[pp[1]], pc=core[pp[2]], pd=core[pp[3]];
                double[][] inits = twoSteinerInits(p[pa], p[pb], p[pc], p[pd]);
                double bestSubLen = Double.MAX_VALUE;
                double[] bestCoords = null;
                for (double[] init : inits) {
                    double[] coords = optimizeTwoSteiner(
                        p[pa], p[pb], p[pc], p[pd],
                        init[0], init[1], init[2], init[3]);
                    if (coords == null) continue;
                    double s0x=coords[0],s0y=coords[1],s1x=coords[2],s1y=coords[3];
                    double len = dist(s0x,s0y,p[pa].getX(),p[pa].getY())
                               + dist(s0x,s0y,p[pb].getX(),p[pb].getY())
                               + dist(s0x,s0y,s1x,s1y)
                               + dist(s1x,s1y,p[pc].getX(),p[pc].getY())
                               + dist(s1x,s1y,p[pd].getX(),p[pd].getY());
                    if (len < bestSubLen) { bestSubLen=len; bestCoords=coords; }
                }
                if (bestCoords == null) continue;
                double s0x=bestCoords[0],s0y=bestCoords[1];
                double s1x=bestCoords[2],s1y=bestCoords[3];
                if (dist(s0x,s0y,s1x,s1y) < eps5) continue;
                boolean s0Bad=false, s1Bad=false;
                for (int idx : new int[]{pa,pb,pc,pd,tail}) {
                    if (dist(s0x,s0y,p[idx].getX(),p[idx].getY()) < eps5) s0Bad=true;
                    if (dist(s1x,s1y,p[idx].getX(),p[idx].getY()) < eps5) s1Bad=true;
                }
                if (s0Bad || s1Bad) continue;
                for (int att : core) {
                    double totalLen = bestSubLen + p[tail].distanceTo(p[att]);
                    if (totalLen < bestLen) {
                        bestLen = totalLen;
                        Point s0=new Point(s0x,s0y), s1=new Point(s1x,s1y);
                        SteinerResult c = new SteinerResult();
                        c.setTerminalPoints(points);
                        c.addSteinerPoint(s0); c.addSteinerPoint(s1);
                        c.addEdge(new Edge(s0,p[pa])); c.addEdge(new Edge(s0,p[pb]));
                        c.addEdge(new Edge(s0,s1));
                        c.addEdge(new Edge(s1,p[pc])); c.addEdge(new Edge(s1,p[pd]));
                        c.addEdge(new Edge(p[tail], p[att]));
                        best = c;
                    }
                }
            }
        }

        for (int mid = 0; mid < 5; mid++) {
            int[] oth = coreOf5(mid);
            for (int[] pp : PP) {
                int pa=oth[pp[0]], pb=oth[pp[1]], pc=oth[pp[2]], pd=oth[pp[3]];
                Point s0 = computeFermatPoint(p[pa], p[pb], p[mid]);
                Point s1 = computeFermatPoint(p[pc], p[pd], p[mid]);
                if (s0==null || s1==null) continue;
                boolean s0ok = s0.distanceTo(p[pa])>eps5
                            && s0.distanceTo(p[pb])>eps5
                            && s0.distanceTo(p[mid])>eps5;
                boolean s1ok = s1.distanceTo(p[pc])>eps5
                            && s1.distanceTo(p[pd])>eps5
                            && s1.distanceTo(p[mid])>eps5;
                SteinerResult c = new SteinerResult();
                c.setTerminalPoints(points);
                if (s0ok) c.addSteinerPoint(s0);
                if (s1ok) c.addSteinerPoint(s1);
                c.addEdge(new Edge(s0, p[pa])); c.addEdge(new Edge(s0, p[pb]));
                c.addEdge(new Edge(s0, p[mid]));
                c.addEdge(new Edge(s1, p[mid]));
                c.addEdge(new Edge(s1, p[pc])); c.addEdge(new Edge(s1, p[pd]));
                if (c.getTotalLength() < bestLen) { best=c; bestLen=c.getTotalLength(); }
            }
        }

        for (int tc = 0; tc < 5; tc++) {
            int[] lv = coreOf5(tc);
            for (int[] pp : PP) {
                int pa=lv[pp[0]], pb=lv[pp[1]], pd=lv[pp[2]], pe=lv[pp[3]];
                SteinerResult c = evalThreeSteiner5(p, pa, pb, tc, pd, pe, points, eps5);
                if (c==null) continue;
                if (c.getTotalLength() < bestLen) { best=c; bestLen=c.getTotalLength(); }
            }
        }

        return best;
    }

    /**
     * Evaluates the chain topology S0–S1–S2 for 5 terminals where
     * S0 connects to {Ta, Tb, S1}, S1 connects to {S0, Tc, S2}, and S2 connects to {S1, Td, Te}.
     *
     * @return the optimized result, or {@code null} if the topology is invalid
     */
    private SteinerResult evalThreeSteiner5(Point[] p,
            int pa, int pb, int tc, int pd, int pe,
            List<Point> allPoints, double eps) {

        Point ta=p[pa], tb=p[pb], tmid=p[tc], td=p[pd], te=p[pe];
        double gx=(ta.getX()+tb.getX()+tmid.getX()+td.getX()+te.getX())/5;
        double gy=(ta.getY()+tb.getY()+tmid.getY()+td.getY()+te.getY())/5;
        double mabx=(ta.getX()+tb.getX())/2, maby=(ta.getY()+tb.getY())/2;
        double mdex=(td.getX()+te.getX())/2, mdey=(td.getY()+te.getY())/2;

        Point f0 = computeFermatPoint(ta, tb, tmid);
        Point f2 = computeFermatPoint(td, te, tmid);

        double[][] inits = {
            {mabx,maby,  gx,gy,  mdex,mdey},
            {f0!=null?f0.getX():mabx, f0!=null?f0.getY():maby,
             gx, gy,
             f2!=null?f2.getX():mdex, f2!=null?f2.getY():mdey},
            {(ta.getX()+gx)/2,(ta.getY()+gy)/2,
             (tmid.getX()+gx)/2,(tmid.getY()+gy)/2,
             (td.getX()+gx)/2,(td.getY()+gy)/2}
        };

        double bestSubLen = Double.MAX_VALUE;
        double[] bestCoords = null;
        for (double[] init : inits) {
            double[] coords = optimizeThreeSteiner(
                ta, tb, tmid, td, te,
                init[0],init[1], init[2],init[3], init[4],init[5]);
            if (coords==null) continue;
            double s0x=coords[0],s0y=coords[1],s1x=coords[2];
            double s1y=coords[3],s2x=coords[4],s2y=coords[5];
            double len = dist(s0x,s0y,ta.getX(),ta.getY())
                       + dist(s0x,s0y,tb.getX(),tb.getY())
                       + dist(s0x,s0y,s1x,s1y)
                       + dist(s1x,s1y,tmid.getX(),tmid.getY())
                       + dist(s1x,s1y,s2x,s2y)
                       + dist(s2x,s2y,td.getX(),td.getY())
                       + dist(s2x,s2y,te.getX(),te.getY());
            if (len < bestSubLen) { bestSubLen=len; bestCoords=coords; }
        }
        if (bestCoords==null) return null;

        double s0x=bestCoords[0],s0y=bestCoords[1];
        double s1x=bestCoords[2],s1y=bestCoords[3];
        double s2x=bestCoords[4],s2y=bestCoords[5];

        if (dist(s0x,s0y,s1x,s1y)<eps || dist(s1x,s1y,s2x,s2y)<eps
         || dist(s0x,s0y,s2x,s2y)<eps) return null;
        for (Point t : new Point[]{ta,tb,tmid,td,te}) {
            if (dist(s0x,s0y,t.getX(),t.getY())<eps) return null;
            if (dist(s1x,s1y,t.getX(),t.getY())<eps) return null;
            if (dist(s2x,s2y,t.getX(),t.getY())<eps) return null;
        }

        Point s0=new Point(s0x,s0y), s1=new Point(s1x,s1y), s2=new Point(s2x,s2y);
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(allPoints);
        result.addSteinerPoint(s0); result.addSteinerPoint(s1); result.addSteinerPoint(s2);
        result.addEdge(new Edge(s0,ta)); result.addEdge(new Edge(s0,tb));
        result.addEdge(new Edge(s0,s1));
        result.addEdge(new Edge(s1,tmid));
        result.addEdge(new Edge(s1,s2));
        result.addEdge(new Edge(s2,td)); result.addEdge(new Edge(s2,te));
        return result;
    }

    /**
     * Optimizes the three Steiner points S0, S1, S2 using alternating Weiszfeld iterations.
     *
     * @return {@code double[]{s0x, s0y, s1x, s1y, s2x, s2y}} after convergence
     */
    private double[] optimizeThreeSteiner(
            Point ta, Point tb, Point tc, Point td, Point te,
            double s0x, double s0y, double s1x, double s1y, double s2x, double s2y) {

        for (int iter = 0; iter < 50_000; iter++) {
            double ps0x=s0x,ps0y=s0y,ps1x=s1x,ps1y=s1y,ps2x=s2x,ps2y=s2y;

            double d0a=dist(s0x,s0y,ta.getX(),ta.getY());
            double d0b=dist(s0x,s0y,tb.getX(),tb.getY());
            double d0s1=dist(s0x,s0y,s1x,s1y);
            if (d0a<1e-12||d0b<1e-12||d0s1<1e-12) break;
            double wt0=1/d0a+1/d0b+1/d0s1;
            s0x=(ta.getX()/d0a+tb.getX()/d0b+s1x/d0s1)/wt0;
            s0y=(ta.getY()/d0a+tb.getY()/d0b+s1y/d0s1)/wt0;

            double d1s0=dist(s1x,s1y,s0x,s0y);
            double d1c=dist(s1x,s1y,tc.getX(),tc.getY());
            double d1s2=dist(s1x,s1y,s2x,s2y);
            if (d1s0<1e-12||d1c<1e-12||d1s2<1e-12) break;
            double wt1=1/d1s0+1/d1c+1/d1s2;
            s1x=(s0x/d1s0+tc.getX()/d1c+s2x/d1s2)/wt1;
            s1y=(s0y/d1s0+tc.getY()/d1c+s2y/d1s2)/wt1;

            double d2s1=dist(s2x,s2y,s1x,s1y);
            double d2d=dist(s2x,s2y,td.getX(),td.getY());
            double d2e=dist(s2x,s2y,te.getX(),te.getY());
            if (d2s1<1e-12||d2d<1e-12||d2e<1e-12) break;
            double wt2=1/d2s1+1/d2d+1/d2e;
            s2x=(s1x/d2s1+td.getX()/d2d+te.getX()/d2e)/wt2;
            s2y=(s1y/d2s1+td.getY()/d2d+te.getY()/d2e)/wt2;

            if (Math.abs(s0x-ps0x)<1e-10&&Math.abs(s0y-ps0y)<1e-10
             && Math.abs(s1x-ps1x)<1e-10&&Math.abs(s1y-ps1y)<1e-10
             && Math.abs(s2x-ps2x)<1e-10&&Math.abs(s2y-ps2y)<1e-10) break;
        }
        return new double[]{s0x,s0y,s1x,s1y,s2x,s2y};
    }

    /** Returns the 2 indices in {0..4} not present in the given triplet. */
    private int[] lonesOf5(int[] triplet) {
        int[] lones = new int[2]; int k=0;
        outer: for (int i=0; i<5; i++) {
            for (int t : triplet) if (t==i) continue outer;
            lones[k++]=i;
        }
        return lones;
    }

    /** Returns the 4 indices in {0..4} that are not equal to {@code excl}. */
    private int[] coreOf5(int excl) {
        int[] core = new int[4]; int k=0;
        for (int i=0; i<5; i++) if (i!=excl) core[k++]=i;
        return core;
    }

    /** Returns the maximum pairwise distance among the given points. */
    private double maxPairDist5(Point[] p) {
        double max=0;
        for (int i=0; i<p.length; i++)
            for (int j=i+1; j<p.length; j++)
                max=Math.max(max, p[i].distanceTo(p[j]));
        return max;
    }

    /** Returns multiple starting points for the two-Steiner optimization. */
    private double[][] twoSteinerInits(Point ta, Point tb, Point tc, Point td) {
        double mabX=(ta.getX()+tb.getX())/2, mabY=(ta.getY()+tb.getY())/2;
        double mcdX=(tc.getX()+td.getX())/2, mcdY=(tc.getY()+td.getY())/2;
        double cx=(ta.getX()+tb.getX()+tc.getX()+td.getX())/4;
        double cy=(ta.getY()+tb.getY()+tc.getY()+td.getY())/4;
        return new double[][] {
            {mabX,mabY,mcdX,mcdY},
            {(mabX*2+mcdX)/3,(mabY*2+mcdY)/3,(mcdX*2+mabX)/3,(mcdY*2+mabY)/3},
            {(ta.getX()+tb.getX()+cx)/3,(ta.getY()+tb.getY()+cy)/3,
             (tc.getX()+td.getX()+cx)/3,(tc.getY()+td.getY()+cy)/3},
            {0.65*ta.getX()+0.35*cx, 0.65*ta.getY()+0.35*cy,
             0.65*tc.getX()+0.35*cx, 0.65*tc.getY()+0.35*cy}
        };
    }

    /**
     * Heuristic Steiner tree for n ≥ 6 points.
     * Starts from the MST and iteratively inserts Fermat points where they reduce total length.
     */
    private SteinerResult solveWithSteinerHeuristic(List<Point> points) {
        int n = points.size();

        List<double[]> nodes = new ArrayList<>();
        for (Point p : points) nodes.add(new double[]{p.getX(), p.getY()});

        double scale = 0;
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                scale = Math.max(scale, distXY(nodes.get(i), nodes.get(j)));
        if (scale < 1e-9) scale = 1.0;

        final double minSep = Math.max(1.0, scale * 0.01);

        List<int[]> edges = buildMSTEdgeIndices(nodes);

        boolean improved = true;
        int maxPasses = 5 * n;

        while (improved && maxPasses-- > 0) {
            improved = false;

            Map<Integer, List<Integer>> adj = buildAdj(nodes.size(), edges);

            outerLoop:
            for (int v = 0; v < nodes.size(); v++) {
                List<Integer> nbrs = adj.getOrDefault(v, Collections.emptyList());
                if (nbrs.size() < 2) continue;

                for (int i = 0; i < nbrs.size(); i++) {
                    for (int j = i + 1; j < nbrs.size(); j++) {
                        int a = nbrs.get(i);
                        int b = nbrs.get(j);

                        double[] pa = nodes.get(a);
                        double[] pv = nodes.get(v);
                        double[] pb = nodes.get(b);

                        double[] F = fermat2D(pa, pv, pb);
                        if (F == null) continue;

                        double oldCost    = distXY(pa, pv) + distXY(pv, pb);
                        double newCost    = distXY(F, pa)  + distXY(F, pv) + distXY(F, pb);
                        double improvement = oldCost - newCost;

                        if (improvement < Math.max(1e-6, oldCost * 5e-4)) continue;

                        boolean tooClose = false;
                        for (int ndIdx = n; ndIdx < nodes.size(); ndIdx++) {
                            if (distXY(F, nodes.get(ndIdx)) < minSep) {
                                tooClose = true; break;
                            }
                        }
                        if (tooClose) continue;

                        int fIdx = nodes.size();
                        nodes.add(F);
                        removeEdgeFromList(edges, v, a);
                        removeEdgeFromList(edges, v, b);
                        edges.add(new int[]{fIdx, a});
                        edges.add(new int[]{fIdx, v});
                        edges.add(new int[]{fIdx, b});

                        improved = true;
                        break outerLoop;
                    }
                }
            }
        }

        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);

        for (int[] e : edges) {
            double[] p1 = nodes.get(e[0]);
            double[] p2 = nodes.get(e[1]);
            result.addEdge(new Edge(new Point(p1[0], p1[1]), new Point(p2[0], p2[1])));
        }

        for (int i = n; i < nodes.size(); i++) {
            double[] p = nodes.get(i);
            result.addSteinerPoint(new Point(p[0], p[1]));
        }

        return result;
    }

    private List<int[]> buildMSTEdgeIndices(List<double[]> nodes) {
        int n = nodes.size();
        boolean[] inMST  = new boolean[n];
        double[]  minDist = new double[n];
        int[]     parent  = new int[n];
        Arrays.fill(minDist, Double.MAX_VALUE);
        Arrays.fill(parent, -1);
        minDist[0] = 0;

        for (int count = 0; count < n; count++) {
            int u = -1;
            for (int i = 0; i < n; i++)
                if (!inMST[i] && (u == -1 || minDist[i] < minDist[u])) u = i;
            if (u == -1) break;
            inMST[u] = true;
            for (int v = 0; v < n; v++) {
                if (!inMST[v]) {
                    double d = distXY(nodes.get(u), nodes.get(v));
                    if (d < minDist[v]) { minDist[v] = d; parent[v] = u; }
                }
            }
        }

        List<int[]> edgeList = new ArrayList<>();
        for (int i = 1; i < n; i++)
            if (parent[i] != -1) edgeList.add(new int[]{parent[i], i});
        return edgeList;
    }

    private Map<Integer, List<Integer>> buildAdj(int n, List<int[]> edges) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int[] e : edges) {
            adj.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
            adj.computeIfAbsent(e[1], k -> new ArrayList<>()).add(e[0]);
        }
        return adj;
    }

    private void removeEdgeFromList(List<int[]> edges, int u, int v) {
        edges.removeIf(e -> (e[0] == u && e[1] == v) || (e[0] == v && e[1] == u));
    }

    private double distXY(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double[] fermat2D(double[] a, double[] b, double[] c) {
        if (angleDeg2D(b, a, c) >= 120.0 - 1e-6) return a.clone();
        if (angleDeg2D(a, b, c) >= 120.0 - 1e-6) return b.clone();
        if (angleDeg2D(a, c, b) >= 120.0 - 1e-6) return c.clone();

        double cross = (b[0]-a[0])*(c[1]-a[1]) - (b[1]-a[1])*(c[0]-a[0]);
        if (Math.abs(cross) < 1e-12) return null;

        double[] f = {(a[0]+b[0]+c[0])/3.0, (a[1]+b[1]+c[1])/3.0};
        double[][] pts = {a, b, c};

        for (int iter = 0; iter < 50_000; iter++) {
            double[] prev = f.clone();
            double wx = 0, wy = 0, wt = 0;
            for (double[] p : pts) {
                double d = distXY(f, p);
                if (d < 1e-12) return p.clone();
                double w = 1.0 / d;
                wx += w * p[0]; wy += w * p[1]; wt += w;
            }
            f = new double[]{wx / wt, wy / wt};
            if (distXY(f, prev) < 1e-10) break;
        }
        return f;
    }

    private double angleDeg2D(double[] a, double[] vertex, double[] c) {
        double[] va = {a[0]-vertex[0], a[1]-vertex[1]};
        double[] vc = {c[0]-vertex[0], c[1]-vertex[1]};
        double dot = va[0]*vc[0] + va[1]*vc[1];
        double mag = Math.sqrt(va[0]*va[0]+va[1]*va[1]) * Math.sqrt(vc[0]*vc[0]+vc[1]*vc[1]);
        if (mag < 1e-15) return 0;
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot / mag))));
    }

    private SteinerResult solveWithMST(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);

        int n = points.size();
        boolean[] inMST  = new boolean[n];
        double[]  minDist = new double[n];
        int[]     parent  = new int[n];

        for (int i = 0; i < n; i++) { minDist[i] = Double.MAX_VALUE; parent[i] = -1; }
        minDist[0] = 0;

        for (int count = 0; count < n; count++) {
            int u = -1; double minVal = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!inMST[i] && minDist[i] < minVal) { minVal = minDist[i]; u = i; }
            }
            if (u == -1) break;
            inMST[u] = true;

            for (int v = 0; v < n; v++) {
                if (!inMST[v]) {
                    double d = points.get(u).distanceTo(points.get(v));
                    if (d < minDist[v]) { minDist[v] = d; parent[v] = u; }
                }
            }
        }

        for (int i = 1; i < n; i++) {
            if (parent[i] != -1)
                result.addEdge(new Edge(points.get(parent[i]), points.get(i)));
        }

        return result;
    }
}
