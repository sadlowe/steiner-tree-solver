from __future__ import annotations
import math
import itertools
from typing import Optional

import numpy as np
from scipy.optimize import minimize


def distance(p, q) -> float:
    """Returns the Euclidean distance between two 2D points."""
    return float(np.linalg.norm(np.asarray(p, float) - np.asarray(q, float)))


def longueur_totale(edges: list[tuple[int, int]],
                    all_points: list[np.ndarray]) -> float:
    """
    Returns the total length of a set of edges.

    :param edges: list of index pairs (i, j) into all_points
    :param all_points: list of 2D coordinates [terminals..., steiner_points...]
    """
    return sum(distance(all_points[i], all_points[j]) for i, j in edges)


def _cross2d(u: np.ndarray, v: np.ndarray) -> float:
    """Returns the 2D scalar cross product of u and v."""
    return float(u[0] * v[1] - u[1] * v[0])


def fermat_point(a: np.ndarray,
                 b: np.ndarray,
                 c: np.ndarray,
                 tol: float = 1e-11,
                 max_iter: int = 50_000) -> Optional[np.ndarray]:
    """
    Computes the Fermat-Torricelli point F = argmin d(F,a) + d(F,b) + d(F,c).

    If any angle of triangle ABC is >= 120°, that vertex is returned directly.
    Returns None if the three points are collinear.
    Uses Weiszfeld iterations starting from the centroid.

    :param a: first point
    :param b: second point
    :param c: third point
    :param tol: convergence tolerance
    :param max_iter: maximum number of iterations
    :return: Fermat point, or None if degenerate
    """
    a, b, c = np.asarray(a, float), np.asarray(b, float), np.asarray(c, float)

    if abs(_cross2d(b - a, c - a)) < 1e-14:
        return None

    def _cos_at(vtx, p1, p2) -> float:
        u, v = p1 - vtx, p2 - vtx
        denom = np.linalg.norm(u) * np.linalg.norm(v)
        return float(np.dot(u, v) / denom) if denom > 1e-15 else 1.0

    cos120 = math.cos(math.radians(120.0))
    if _cos_at(a, b, c) <= cos120:
        return a.copy()
    if _cos_at(b, a, c) <= cos120:
        return b.copy()
    if _cos_at(c, a, b) <= cos120:
        return c.copy()

    pts = [a, b, c]
    f = (a + b + c) / 3.0

    for _ in range(max_iter):
        dists = [distance(f, p) for p in pts]

        if min(dists) < 1e-12:
            return pts[int(np.argmin(dists))].copy()

        w = [1.0 / d for d in dists]
        f_new = sum(wi * pi for wi, pi in zip(w, pts)) / sum(w)

        if distance(f_new, f) < tol:
            f = f_new
            break
        f = f_new

    return f


def _mst_4(terminals: list[np.ndarray]) -> tuple[float, list[tuple[int, int]]]:
    """
    Computes the MST of the 4 terminals using Prim's algorithm.

    :return: (total_length, list_of_edges)
    """
    in_tree: set[int] = {0}
    edges: list[tuple[int, int]] = []
    total = 0.0

    for _ in range(3):
        best_d, best_e = math.inf, (-1, -1)
        for i in in_tree:
            for j in range(4):
                if j not in in_tree:
                    d = distance(terminals[i], terminals[j])
                    if d < best_d:
                        best_d, best_e = d, (i, j)
        edges.append(best_e)
        in_tree.add(best_e[1])
        total += best_d

    return total, edges


def _eval_one_steiner(terminals: list[np.ndarray],
                      triplet: tuple[int, int, int],
                      lone: int,
                      attach: int) -> Optional[dict]:
    """
    Evaluates a topology with one Steiner point S = Fermat(triplet).
    The lone terminal connects directly to the given attach terminal.

    :return: result dict, or None if the triplet is collinear
    """
    a, b, c = [terminals[i] for i in triplet]
    s = fermat_point(a, b, c)
    if s is None:
        return None

    at_vertex = any(distance(s, terminals[i]) < 1e-8 for i in triplet)

    all_pts = terminals + [s]
    edges = [(4, triplet[0]), (4, triplet[1]), (4, triplet[2]), (lone, attach)]

    return {
        'length': longueur_totale(edges, all_pts),
        'edges_idx': edges,
        'all_points': all_pts,
        'steiner_points': [] if at_vertex else [s],
        'topology': (f'1 Steiner | triplet={triplet} '
                     f'| lone={lone} -> attach={attach}'),
    }


def _check_120(s: np.ndarray,
               neighbors: list[np.ndarray],
               tol_deg: float = 10.0) -> bool:
    """
    Checks that the three edges at Steiner point s form angles of approximately 120°.

    :param s: Steiner point
    :param neighbors: list of exactly 3 neighboring points
    :param tol_deg: angle tolerance in degrees
    """
    if len(neighbors) != 3:
        return False
    for u, v in itertools.combinations(neighbors, 2):
        d1 = float(np.linalg.norm(u - s))
        d2 = float(np.linalg.norm(v - s))
        if d1 < 1e-9 or d2 < 1e-9:
            return False
        cos_a = float(np.dot(u - s, v - s)) / (d1 * d2)
        angle = math.degrees(math.acos(max(-1.0, min(1.0, cos_a))))
        if abs(angle - 120.0) > tol_deg:
            return False
    return True


def _two_steiner_starts(a: np.ndarray, b: np.ndarray,
                        c: np.ndarray, d: np.ndarray) -> list[np.ndarray]:
    """
    Generates 4 starting points for the two-Steiner optimization.

    :return: list of flat arrays [s1x, s1y, s2x, s2y]
    """
    mab  = (a + b) / 2.0
    mcd  = (c + d) / 2.0
    ctr  = (a + b + c + d) / 4.0
    span = distance(mab, mcd)

    starts = []

    starts.append(np.r_[(mab * 2 + mcd) / 3.0, (mcd * 2 + mab) / 3.0])

    f1 = fermat_point(a, b, mcd)
    f2 = fermat_point(c, d, mab)
    if f1 is not None and f2 is not None:
        starts.append(np.r_[f1, f2])

    direction = mcd - mab
    if np.linalg.norm(direction) > 1e-9:
        perp = np.array([-direction[1], direction[0]])
        perp = perp / np.linalg.norm(perp) * 0.15 * span
    else:
        perp = np.array([0.1 * span, 0.0])
    starts.append(np.r_[ctr + perp, ctr - perp])

    starts.append(np.r_[0.65 * a + 0.35 * ctr, 0.65 * c + 0.35 * ctr])

    return starts


def _eval_two_steiner(terminals: list[np.ndarray],
                      pair1: tuple[int, int],
                      pair2: tuple[int, int]) -> Optional[dict]:
    """
    Evaluates a topology with two Steiner points S1 and S2.
    S1 connects to pair1 terminals and S2; S2 connects to pair2 terminals and S1.
    Positions are optimized by L-BFGS-B with multiple starting points.

    :return: result dict, or None if the topology is geometrically invalid
    """
    a, b = terminals[pair1[0]], terminals[pair1[1]]
    c, d = terminals[pair2[0]], terminals[pair2[1]]

    edges = [(4, pair1[0]), (4, pair1[1]), (4, 5),
             (5, pair2[0]), (5, pair2[1])]

    def objective(x: np.ndarray) -> float:
        s1, s2 = x[:2], x[2:]
        return longueur_totale(edges, terminals + [s1, s2])

    best = None
    for x0 in _two_steiner_starts(a, b, c, d):
        res = minimize(objective, x0, method='L-BFGS-B',
                       options={'ftol': 1e-14, 'gtol': 1e-10, 'maxiter': 10_000})
        if best is None or res.fun < best.fun:
            best = res

    if best is None:
        return None

    s1 = np.array(best.x[:2])
    s2 = np.array(best.x[2:])
    all_pts = terminals + [s1, s2]

    scale = max(
        distance(terminals[i], terminals[j])
        for i, j in itertools.combinations(range(4), 2)
    )
    eps = max(1e-8, 1e-4 * scale)

    if distance(s1, s2) < eps:
        return None
    if any(distance(s1, t) < eps for t in terminals):
        return None
    if any(distance(s2, t) < eps for t in terminals):
        return None

    s1_nb = [all_pts[pair1[0]], all_pts[pair1[1]], s2]
    s2_nb = [all_pts[pair2[0]], all_pts[pair2[1]], s1]
    if not (_check_120(s1, s1_nb) and _check_120(s2, s2_nb)):
        return None

    return {
        'length': float(best.fun),
        'edges_idx': edges,
        'all_points': all_pts,
        'steiner_points': [s1, s2],
        'topology': f'2 Steiner | paires {pair1} | {pair2}',
    }


def steiner_tree_4_points(points: list[tuple[float, float]]) -> dict:
    """
    Computes the optimal Euclidean Steiner tree for exactly 4 points.

    Enumerates all 16 candidate topologies (0, 1, or 2 Steiner points)
    and returns the one with the shortest total length.

    :param points: list of 4 (x, y) tuples
    :return: dict with keys 'length', 'edges', 'steiner_points', 'topology', 'all_points'
    :raises ValueError: if the input does not contain exactly 4 points
    """
    if len(points) != 4:
        raise ValueError("Exactement 4 points sont requis.")

    terminals = [np.asarray(p, dtype=float) for p in points]
    candidates: list[dict] = []

    mst_len, mst_edges = _mst_4(terminals)
    candidates.append({
        'length': mst_len,
        'edges_idx': mst_edges,
        'all_points': terminals[:],
        'steiner_points': [],
        'topology': 'MST (0 point de Steiner)',
    })

    for triplet in itertools.combinations(range(4), 3):
        lone = next(x for x in range(4) if x not in triplet)
        for attach in triplet:
            res = _eval_one_steiner(terminals, triplet, lone, attach)
            if res is not None:
                candidates.append(res)

    for pair1, pair2 in [((0, 1), (2, 3)), ((0, 2), (1, 3)), ((0, 3), (1, 2))]:
        res = _eval_two_steiner(terminals, pair1, pair2)
        if res is not None:
            candidates.append(res)

    best = min(candidates, key=lambda r: r['length'])

    return {
        'length': best['length'],
        'edges': [
            (best['all_points'][i], best['all_points'][j])
            for i, j in best['edges_idx']
        ],
        'steiner_points': best['steiner_points'],
        'topology': best['topology'],
        'all_points': best['all_points'],
    }


def afficher_resultat(res: dict, titre: str = '') -> None:
    """Prints the result in a readable format."""
    sep = '-' * 58
    print(sep)
    if titre:
        print(f'  {titre}')
    print(f'  Topologie  : {res["topology"]}')
    print(f'  Longueur   : {res["length"]:.8f}')
    n = len(res['steiner_points'])
    print(f'  Pts Steiner: {n}')
    for i, s in enumerate(res['steiner_points']):
        print(f'    S{i + 1} = ({s[0]:.6f}, {s[1]:.6f})')
    print(f'  Segments   : {len(res["edges"])}')
    for p1, p2 in res['edges']:
        print(f'    ({p1[0]:7.4f},{p1[1]:7.4f}) -> '
              f'({p2[0]:7.4f},{p2[1]:7.4f})   d={distance(p1, p2):.5f}')
    print(sep)


def visualiser(points: list[tuple], res: dict, titre: str = '') -> None:
    """Plots the solution using matplotlib."""
    try:
        import matplotlib.pyplot as plt
    except ImportError:
        print("matplotlib non disponible.")
        return

    fig, ax = plt.subplots(figsize=(7, 6))
    ax.set_aspect('equal')
    ax.set_title(titre or res['topology'], fontsize=11)

    for p1, p2 in res['edges']:
        ax.plot([p1[0], p2[0]], [p1[1], p2[1]], 'b-', lw=2.5, zorder=1)

    for i, (x, y) in enumerate(points):
        ax.scatter(x, y, s=120, color='steelblue', zorder=3)
        ax.annotate(f'T{i}', (x, y), xytext=(6, 6),
                    textcoords='offset points', fontsize=11, color='steelblue')

    for i, s in enumerate(res['steiner_points']):
        ax.scatter(s[0], s[1], s=140, color='crimson', marker='D', zorder=3)
        ax.annotate(f'S{i + 1}', (s[0], s[1]), xytext=(6, 6),
                    textcoords='offset points', fontsize=11, color='crimson')

    ax.set_xlabel(f'Longueur totale = {res["length"]:.6f}')
    plt.tight_layout()
    plt.show()


if __name__ == '__main__':
    afficher_resultat(
        steiner_tree_4_points([(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0)]),
        'Ex 1 -- Carre unite  (ref: 1+sqrt(3) = 2.73205080)'
    )

    afficher_resultat(
        steiner_tree_4_points([(1.0, 2.0), (4.0, 6.0), (7.0, 1.0), (3.0, 8.0)]),
        'Ex 2 -- Points quelconques'
    )

    afficher_resultat(
        steiner_tree_4_points([(0.0, 0.0), (10.0, 0.0), (5.0, 0.2), (5.0, -0.2)]),
        'Ex 3 -- Losange allonge'
    )

    afficher_resultat(
        steiner_tree_4_points([(0.0, 0.0), (3.0, 0.0), (3.0, 1.0), (0.0, 1.0)]),
        'Ex 4 -- Rectangle 3x1'
    )
