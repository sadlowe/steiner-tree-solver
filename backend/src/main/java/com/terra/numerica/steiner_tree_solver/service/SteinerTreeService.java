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

/**
 * Service de calcul de l'arbre de Steiner euclidien.
 *
 * ALGORITHME POUR 4 POINTS
 * ─────────────────────────
 * Un arbre de Steiner sur n terminaux peut contenir au plus n−2 points de Steiner.
 * Pour n=4, on énumère exhaustivement les 16 topologies candidates :
 *
 *   • 0 point de Steiner  ( 1 topologie) : arbre couvrant minimal (MST)
 *   • 1 point de Steiner  (12 topologies): triplet → point de Fermat,
 *                                          le 4e terminal se raccorde à l'un des 3 autres
 *   • 2 points de Steiner ( 3 topologies): partition {a,b}|{c,d},
 *                                          S1 et S2 optimisés par Weiszfeld alterné
 *
 * La topologie de longueur minimale valide est retournée.
 *
 * ALGORITHME POUR N >= 5 POINTS
 * ──────────────────────────────
 * Heuristique d'insertion itérative de points de Fermat :
 * Partir du MST puis insérer des points de Steiner (Fermat) là où ils
 * réduisent la longueur totale, jusqu'à convergence.
 */
@Service
public class SteinerTreeService {

    public SteinerResult solve(List<Point> points) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("At least 2 points are required");
        }
        switch (points.size()) {
            case 2:  return solveForTwoPoints(points);
            case 3:  return solveForThreePoints(points);
            case 4:  return solveForFourPoints(points);
            default: return solveWithSteinerHeuristic(points);
        }
    }

    // =========================================================================
    // 2 POINTS : segment direct
    // =========================================================================

    private SteinerResult solveForTwoPoints(List<Point> points) {
        SteinerResult result = new SteinerResult();
        result.setTerminalPoints(points);
        result.addEdge(new Edge(points.get(0), points.get(1)));
        return result;
    }

    // =========================================================================
    // 3 POINTS : point de Fermat-Torricelli
    // Si un angle >= 120°, ce sommet est le point optimal (pas de Steiner utile).
    // Sinon, le point de Fermat divise les 3 arêtes à exactement 120°.
    // =========================================================================

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

    // =========================================================================
    // 4 POINTS : énumération complète des 16 topologies
    // =========================================================================

    private SteinerResult solveForFourPoints(List<Point> points) {
        Point[] p = {
            points.get(0), points.get(1),
            points.get(2), points.get(3)
        };

        SteinerResult best = solveWithMST(points);
        best.setTerminalPoints(points);
        double bestLength = best.getTotalLength();

        // ── 1 point de Steiner : 4 triplets × 3 attaches = 12 topologies ────
        //
        // Structure :
        //   S = Fermat(triplet[0], triplet[1], triplet[2])
        //   terminal lone → terminal attach  (attach ∈ triplet)
        //
        // IMPORTANT : le terminal lone se connecte à UN TERMINAL du triplet,
        // PAS au point de Steiner. Un point de Steiner n'a que 3 arêtes (120°).
        int[][] triplets = { {0,1,2}, {0,1,3}, {0,2,3}, {1,2,3} };
        int[]   lones    = {  3,       2,       1,       0       };

        for (int t = 0; t < 4; t++) {
            int[] tri = triplets[t];
            int   lone = lones[t];

            Point ta = p[tri[0]], tb = p[tri[1]], tc = p[tri[2]];
            Point fermat = computeFermatPoint(ta, tb, tc);
            if (fermat == null) continue;   // triplet colinéaire, topologie invalide

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

        // ── 2 points de Steiner : 3 partitions en 2 paires ──────────────────
        //
        // Structure (exemple partition {0,1}|{2,3}) :
        //   S1 connecté à T0, T1, S2   (angles 120°)
        //   S2 connecté à T2, T3, S1   (angles 120°)
        //
        // Optimisation par itérations de Weiszfeld alternées (sans dépendance externe).
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

    // =========================================================================
    // OPTIMISATION DES 2 POINTS DE STEINER
    //
    // Topologie : S1 ↔ {Ta, Tb, S2}    S2 ↔ {Tc, Td, S1}
    //
    // On minimise f(S1,S2) = d(S1,Ta)+d(S1,Tb)+d(S1,S2)+d(S2,Tc)+d(S2,Td)
    //
    // Algorithme : Weiszfeld alterné (coordinate descent).
    //   1. Fixer S2, mettre à jour S1 = Weber({Ta, Tb, S2}) par Weiszfeld
    //   2. Fixer S1, mettre à jour S2 = Weber({Tc, Td, S1}) par Weiszfeld
    //   3. Répéter jusqu'à convergence
    //
    // Plusieurs initialisations pour éviter les minima locaux.
    // =========================================================================

    private SteinerResult evalTwoSteinerTopology(Point[] p, int a, int b, int c, int d) {
        Point ta = p[a], tb = p[b], tc = p[c], td = p[d];

        double cx = (ta.getX() + tb.getX() + tc.getX() + td.getX()) / 4.0;
        double cy = (ta.getY() + tb.getY() + tc.getY() + td.getY()) / 4.0;
        double mabX = (ta.getX() + tb.getX()) / 2.0;
        double mabY = (ta.getY() + tb.getY()) / 2.0;
        double mcdX = (tc.getX() + td.getX()) / 2.0;
        double mcdY = (tc.getY() + td.getY()) / 2.0;

        // 4 points de départ différents
        double[][] inits = {
            // Init 1 : milieux des deux paires
            { mabX, mabY, mcdX, mcdY },
            // Init 2 : milieux décalés vers le centroïde
            { (mabX * 2 + mcdX) / 3.0, (mabY * 2 + mcdY) / 3.0,
              (mcdX * 2 + mabX) / 3.0, (mcdY * 2 + mabY) / 3.0 },
            // Init 3 : centroïde ± perturbation vers chaque paire
            { (ta.getX() + tb.getX() + cx) / 3.0, (ta.getY() + tb.getY() + cy) / 3.0,
              (tc.getX() + td.getX() + cx) / 3.0, (tc.getY() + td.getY() + cy) / 3.0 },
            // Init 4 : décalé vers les terminaux respectifs
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

        // Validation : points de Steiner non confondus avec terminaux/entre eux
        double scale = maxPairDistance(ta, tb, tc, td);
        double eps   = Math.max(1e-8, 1e-4 * scale);

        if (dist(s1x, s1y, s2x, s2y) < eps) return null;
        for (Point t : new Point[]{ ta, tb, tc, td }) {
            if (dist(s1x, s1y, t.getX(), t.getY()) < eps) return null;
            if (dist(s2x, s2y, t.getX(), t.getY()) < eps) return null;
        }

        // Validation des angles ≈ 120° aux deux points de Steiner
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
     * Optimise S1 et S2 par itérations de Weiszfeld alternées.
     *
     * Chaque étape :
     *   S1 ← Weber({Ta, Tb, S2}) :  S1 = (Ta/d(S1,Ta) + Tb/d(S1,Tb) + S2/d(S1,S2))
     *                                     / (1/d(S1,Ta) + 1/d(S1,Tb) + 1/d(S1,S2))
     *   S2 ← Weber({Tc, Td, S1}) :  même formule avec Tc, Td, S1
     */
    private double[] optimizeTwoSteiner(
            Point ta, Point tb, Point tc, Point td,
            double s1x, double s1y, double s2x, double s2y) {

        for (int iter = 0; iter < 50_000; iter++) {
            double ps1x = s1x, ps1y = s1y;
            double ps2x = s2x, ps2y = s2y;

            // Mise à jour de S1 : Weber({Ta, Tb, S2})
            double d1a  = dist(s1x, s1y, ta.getX(), ta.getY());
            double d1b  = dist(s1x, s1y, tb.getX(), tb.getY());
            double d1s2 = dist(s1x, s1y, s2x, s2y);
            if (d1a < 1e-12 || d1b < 1e-12 || d1s2 < 1e-12) break;

            double w1a = 1.0 / d1a, w1b = 1.0 / d1b, w1s2 = 1.0 / d1s2;
            double ws1 = w1a + w1b + w1s2;
            s1x = (ta.getX() * w1a + tb.getX() * w1b + s2x * w1s2) / ws1;
            s1y = (ta.getY() * w1a + tb.getY() * w1b + s2y * w1s2) / ws1;

            // Mise à jour de S2 : Weber({Tc, Td, S1})
            double d2c  = dist(s2x, s2y, tc.getX(), tc.getY());
            double d2d  = dist(s2x, s2y, td.getX(), td.getY());
            double d2s1 = dist(s2x, s2y, s1x, s1y);
            if (d2c < 1e-12 || d2d < 1e-12 || d2s1 < 1e-12) break;

            double w2c = 1.0 / d2c, w2d = 1.0 / d2d, w2s1 = 1.0 / d2s1;
            double ws2 = w2c + w2d + w2s1;
            s2x = (tc.getX() * w2c + td.getX() * w2d + s1x * w2s1) / ws2;
            s2y = (tc.getY() * w2c + td.getY() * w2d + s1y * w2s1) / ws2;

            // Critère de convergence
            if (Math.abs(s1x - ps1x) < 1e-10 && Math.abs(s1y - ps1y) < 1e-10
             && Math.abs(s2x - ps2x) < 1e-10 && Math.abs(s2y - ps2y) < 1e-10) {
                break;
            }
        }

        return new double[]{ s1x, s1y, s2x, s2y };
    }

    // =========================================================================
    // UTILITAIRES GÉOMÉTRIQUES
    // =========================================================================

    /**
     * Point de Fermat-Torricelli de trois points par itérations de Weiszfeld.
     * Retourne null si les points sont colinéaires.
     * Retourne directement le sommet si son angle est >= 120°.
     */
    private Point computeFermatPoint(Point p1, Point p2, Point p3) {
        double threshold = Math.toRadians(120.0) - 1e-9;
        if (calculateAngle(p2, p1, p3) >= threshold) return p1;
        if (calculateAngle(p1, p2, p3) >= threshold) return p2;
        if (calculateAngle(p1, p3, p2) >= threshold) return p3;

        // Triangle plat → pas de point de Fermat défini
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

    /** Angle en b dans le triangle (a, b, c). */
    private double calculateAngle(Point a, Point b, Point c) {
        double bax = a.getX() - b.getX(), bay = a.getY() - b.getY();
        double bcx = c.getX() - b.getX(), bcy = c.getY() - b.getY();
        double dot = bax * bcx + bay * bcy;
        double mag = Math.sqrt(bax*bax + bay*bay) * Math.sqrt(bcx*bcx + bcy*bcy);
        if (mag < 1e-15) return 0;
        return Math.acos(Math.max(-1.0, Math.min(1.0, dot / mag)));
    }

    /**
     * Vérifie que les 3 arêtes du point de Steiner s (vers n1, n2, n3)
     * forment des angles d'environ 120° (tolérance ± 10°).
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

    private double maxPairDistance(Point ta, Point tb, Point tc, Point td) {
        Point[] pts = { ta, tb, tc, td };
        double max = 0;
        for (int i = 0; i < 4; i++)
            for (int j = i + 1; j < 4; j++)
                max = Math.max(max, pts[i].distanceTo(pts[j]));
        return max;
    }

    // =========================================================================
    // N >= 5 POINTS : heuristique de Steiner par insertion itérative de points
    // de Fermat
    //
    // Algorithme déterministe en trois phases :
    //
    //   Phase 1 – Initialisation par le MST
    //     Construire l'arbre couvrant minimal des n terminaux.
    //     Le MST est une 2-approximation de l'arbre de Steiner.
    //
    //   Phase 2 – Insertion itérative de points de Steiner
    //     Pour chaque paire d'arêtes (v,a) et (v,b) partageant un nœud v :
    //       a. Calculer F = point de Fermat de (a, v, b)
    //       b. Si d(F,a) + d(F,v) + d(F,b) < d(v,a) + d(v,b)
    //          → Retirer arêtes (v,a) et (v,b)
    //          → Ajouter arêtes (F,a), (F,v), (F,b)
    //          → Enregistrer F comme point de Steiner
    //     Répéter jusqu'à convergence (aucune amélioration possible).
    //
    //   Phase 3 – Construction du résultat
    //     Retourner les arêtes et les points de Steiner insérés.
    //
    // Propriétés :
    //   • Déterministe — pas de hasard, convergence garantie
    //   • Longueur résultante ≤ longueur MST
    //   • La propriété 120° est satisfaite aux points de Steiner insérés
    //   • Complexité : O(n² × passes) — pratique pour n ≤ quelques centaines
    // =========================================================================

    private SteinerResult solveWithSteinerHeuristic(List<Point> points) {
        int n = points.size();

        // Représentation de travail (les terminaux occupent les indices 0..n-1)
        List<double[]> nodes = new ArrayList<>();
        for (Point p : points) nodes.add(new double[]{p.getX(), p.getY()});

        // Phase 1 : MST initial
        List<int[]> edges = buildMSTEdgeIndices(nodes);

        // Phase 2 : Insertion itérative de points de Fermat
        boolean improved = true;
        int maxPasses = 5 * n; // Limite de sécurité

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

                        double oldCost = distXY(pa, pv) + distXY(pv, pb);
                        double newCost = distXY(F, pa) + distXY(F, pv) + distXY(F, pb);

                        if (newCost < oldCost - 1e-8) {
                            // Vérifier que F n'est pas confondu avec un nœud existant
                            boolean tooClose = false;
                            for (double[] nd : nodes) {
                                if (distXY(F, nd) < 1e-6) { tooClose = true; break; }
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
        }

        // Phase 3 : Construction du résultat
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

    // ── Helpers pour l'heuristique ────────────────────────────────────────────

    /** MST de Prim sur une liste de nœuds. Retourne les arêtes comme paires d'indices. */
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

    /** Construit la liste d'adjacence à partir d'une liste d'arêtes (paires d'indices). */
    private Map<Integer, List<Integer>> buildAdj(int n, List<int[]> edges) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int[] e : edges) {
            adj.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
            adj.computeIfAbsent(e[1], k -> new ArrayList<>()).add(e[0]);
        }
        return adj;
    }

    /** Supprime l'arête non-orientée (u, v) de la liste. */
    private void removeEdgeFromList(List<int[]> edges, int u, int v) {
        edges.removeIf(e -> (e[0] == u && e[1] == v) || (e[0] == v && e[1] == u));
    }

    /** Distance euclidienne entre deux points 2D (tableaux double[2]). */
    private double distXY(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Point de Fermat-Torricelli de trois points 2D (tableaux double[2]).
     *
     * Si un angle du triangle est >= 120°, ce sommet est retourné.
     * Si le triangle est dégénéré, retourne null.
     * Sinon, calcule par itérations de Weiszfeld depuis le centroïde.
     */
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

    /** Angle en degrés au sommet {@code vertex} dans le triangle (a, vertex, c). */
    private double angleDeg2D(double[] a, double[] vertex, double[] c) {
        double[] va = {a[0]-vertex[0], a[1]-vertex[1]};
        double[] vc = {c[0]-vertex[0], c[1]-vertex[1]};
        double dot = va[0]*vc[0] + va[1]*vc[1];
        double mag = Math.sqrt(va[0]*va[0]+va[1]*va[1]) * Math.sqrt(vc[0]*vc[0]+vc[1]*vc[1]);
        if (mag < 1e-15) return 0;
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot / mag))));
    }

    // =========================================================================
    // MST — algorithme de Prim pour n >= 5 points (utilisé en interne)
    // =========================================================================

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
