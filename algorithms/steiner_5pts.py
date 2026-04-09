from __future__ import annotations
import math
import itertools
from typing import Optional

import numpy as np
from scipy.optimize import minimize


def distance(p, q) -> float:
    """Returns the Euclidean distance between two 2D points."""
    return float(np.linalg.norm(np.asarray(p, float) - np.asarray(q, float)))


def total_length(edges: list[tuple[int, int]],
                 all_points: list[np.ndarray]) -> float:
    """
    Returns the total length of a set of edges.

    :param edges: list of index pairs (i, j) into all_points
    :param all_points: list of 2D coordinates [terminals[0..4], steiner_pts...]
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


def _mst_5(terminals: list[np.ndarray]) -> tuple[float, list[tuple[int, int]]]:
    """
    Computes the MST of 5 terminals using Prim's algorithm.

    :return: (total_length, list_of_edges)
    """
    n = len(terminals)
    in_tree: set[int] = {0}
    edges: list[tuple[int, int]] = []
    total = 0.0

    for _ in range(n - 1):
        best_d, best_e = math.inf, (-1, -1)
        for i in in_tree:
            for j in range(n):
                if j not in in_tree:
                    d = distance(terminals[i], terminals[j])
                    if d < best_d:
                        best_d, best_e = d, (i, j)
        edges.append(best_e)
        in_tree.add(best_e[1])
        total += best_d

    return total, edges


def generate_topologies_5_points() -> list[dict]:
    """
    Generates all candidate topologies for a Steiner tree on 5 terminals.

    Each topology is a dict with keys:
      - 'k'         : number of Steiner points (1 to 3)
      - 'edges_idx' : edge list as index pairs (0-4 = terminals, 5-7 = Steiner points)
      - 'label'     : human-readable description

    Index convention:
        0..4  → terminals T0..T4
        5     → S0 (first Steiner point)
        6     → S1 (second Steiner point)
        7     → S2 (third Steiner point)

    Topologies generated:
      k=1 : ~150 topologies (10 triplets × attachments of the 2 remaining terminals)
      k=2 :  ~75 topologies (Type I: S0-S1 direct + tail; Type II: T_mid between S0 and S1)
      k=3 :   15 topologies (chain S0-S1-S2, the only valid structure for k=3, n=5)
    """
    topologies: list[dict] = []

    for triplet in itertools.combinations(range(5), 3):
        ti0, ti1, ti2 = triplet
        lones = [x for x in range(5) if x not in triplet]
        l0, l1 = lones

        tri_nodes = [ti0, ti1, ti2]

        for attach_l0 in tri_nodes:
            for attach_l1 in tri_nodes + [l0]:
                edges = [(5, ti0), (5, ti1), (5, ti2),
                         (l0, attach_l0), (l1, attach_l1)]
                topologies.append({
                    'k': 1,
                    'edges_idx': edges,
                    'label': (f'1S | triplet=({ti0},{ti1},{ti2}) '
                              f'| l0={l0}→{attach_l0} | l1={l1}→{attach_l1}'),
                })

        for attach_l1 in tri_nodes:
            edges = [(5, ti0), (5, ti1), (5, ti2),
                     (l1, attach_l1), (l0, l1)]
            topologies.append({
                'k': 1,
                'edges_idx': edges,
                'label': (f'1S | triplet=({ti0},{ti1},{ti2}) '
                          f'| l1={l1}→{attach_l1} | l0={l0}→l1'),
            })

    for tail in range(5):
        core = [x for x in range(5) if x != tail]
        ta, tb, tc, td = core

        for (pa, pb), (pc, pd) in [
            ((ta, tb), (tc, td)),
            ((ta, tc), (tb, td)),
            ((ta, td), (tb, tc)),
        ]:
            for attach_tail in [pa, pb, pc, pd]:
                edges = [(5, pa), (5, pb), (5, 6),
                         (6, pc), (6, pd),
                         (tail, attach_tail)]
                topologies.append({
                    'k': 2,
                    'edges_idx': edges,
                    'label': (f'2S TypeI | S0=({pa},{pb}) S1=({pc},{pd}) '
                              f'| queue={tail}→{attach_tail}'),
                })

    for mid in range(5):
        others = [x for x in range(5) if x != mid]
        ta, tb, tc, td = others
        for (pa, pb), (pc, pd) in [
            ((ta, tb), (tc, td)),
            ((ta, tc), (tb, td)),
            ((ta, td), (tb, tc)),
        ]:
            edges = [(5, pa), (5, pb), (5, mid),
                     (6, mid), (6, pc), (6, pd)]
            topologies.append({
                'k': 2,
                'edges_idx': edges,
                'label': (f'2S TypeII | T_mid={mid} '
                          f'| S0=({pa},{pb}) S1=({pc},{pd})'),
            })

    for tc in range(5):
        leaves = [x for x in range(5) if x != tc]
        ta, tb, td, te = leaves

        for (pa, pb), (pd, pe) in [
            ((ta, tb), (td, te)),
            ((ta, td), (tb, te)),
            ((ta, te), (tb, td)),
        ]:
            edges = [(5, pa), (5, pb), (5, 6),
                     (6, tc), (6, 7),
                     (7, pd), (7, pe)]
            topologies.append({
                'k': 3,
                'edges_idx': edges,
                'label': (f'3S | S1_mid={tc} '
                          f'| S0=({pa},{pb}) S2=({pd},{pe})'),
            })

    return topologies


def _steiner_inits(terminals: list[np.ndarray],
                   edges_idx: list[tuple[int, int]],
                   k: int) -> list[np.ndarray]:
    """
    Generates multiple starting points for the k Steiner point optimization.

    Three strategies are used:
      - centroid of terminal neighbors of each Steiner point
      - Fermat point of terminal neighbors (when 3 terminal neighbors exist)
      - random perturbations around the global centroid (fixed seed 42)

    :return: list of flat arrays of size 2k
    """
    n_total = 5 + k
    nbrs: list[list[int]] = [[] for _ in range(k)]
    for i, j in edges_idx:
        if 5 <= i < n_total and j < 5:
            nbrs[i - 5].append(j)
        if 5 <= j < n_total and i < 5:
            nbrs[j - 5].append(i)

    global_ctr = sum(terminals) / len(terminals)
    starts: list[np.ndarray] = []

    x0_parts = []
    for si in range(k):
        term_nbrs = [terminals[j] for j in nbrs[si]]
        cx = sum(term_nbrs) / len(term_nbrs) if term_nbrs else global_ctr
        x0_parts.append(cx)
    starts.append(np.concatenate(x0_parts))

    x1_parts = []
    for si in range(k):
        term_nbrs = [terminals[j] for j in nbrs[si]]
        f = None
        if len(term_nbrs) >= 3:
            f = fermat_point(term_nbrs[0], term_nbrs[1], term_nbrs[2])
        if f is None and term_nbrs:
            f = sum(term_nbrs) / len(term_nbrs)
        if f is None:
            f = global_ctr
        x1_parts.append(f)
    starts.append(np.concatenate(x1_parts))

    rng = np.random.default_rng(42)
    scale = max(
        distance(terminals[i], terminals[j])
        for i, j in itertools.combinations(range(5), 2)
    ) * 0.1
    for _ in range(3):
        parts = [global_ctr + rng.standard_normal(2) * scale for _ in range(k)]
        starts.append(np.concatenate(parts))

    return starts


def optimize_steiner_points(terminals: list[np.ndarray],
                             edges_idx: list[tuple[int, int]],
                             k: int) -> Optional[dict]:
    """
    Optimizes the positions of k Steiner points using L-BFGS-B (scipy).

    Multiple starting points are tried to avoid local minima.
    The result is rejected if any Steiner point coincides with a terminal
    or with another Steiner point within the degeneracy threshold.

    :param terminals: list of 5 terminal points
    :param edges_idx: topology edge list
    :param k: number of Steiner points
    :return: result dict, or None if the topology is geometrically invalid
    """
    scale = max(
        distance(terminals[i], terminals[j])
        for i, j in itertools.combinations(range(5), 2)
    )
    eps = max(1e-6, 5e-3 * scale)

    def objective(x: np.ndarray) -> float:
        steiner_pts = [x[2 * si:2 * si + 2] for si in range(k)]
        all_pts = terminals + steiner_pts
        return total_length(edges_idx, all_pts)

    inits = _steiner_inits(terminals, edges_idx, k)
    best_res = None

    for x0 in inits:
        try:
            res = minimize(objective, x0, method='L-BFGS-B',
                           options={'ftol': 1e-14, 'gtol': 1e-10,
                                    'maxiter': 20_000})
        except Exception:
            continue
        if best_res is None or res.fun < best_res.fun:
            best_res = res

    if best_res is None:
        return None

    steiner_pts = [np.array(best_res.x[2 * si:2 * si + 2]) for si in range(k)]
    all_pts = terminals + steiner_pts

    for si, s in enumerate(steiner_pts):
        if any(distance(s, t) < eps for t in terminals):
            return None
        for sj, s2 in enumerate(steiner_pts):
            if si != sj and distance(s, s2) < eps:
                return None

    return {
        'length': float(best_res.fun),
        'edges_idx': edges_idx,
        'all_points': all_pts,
        'steiner_points': steiner_pts,
    }


def _eval_one_steiner(terminals: list[np.ndarray],
                      topo: dict) -> Optional[dict]:
    """
    Evaluates a topology with one Steiner point using the exact Fermat point.

    S0 is computed analytically as the Fermat-Torricelli point of its three
    terminal neighbors, which guarantees the exact minimum for this topology.

    :return: result dict, or None if the topology is invalid
    """
    edges_idx = topo['edges_idx']

    triplet_ids = []
    for i, j in edges_idx:
        if i == 5 and j < 5:
            triplet_ids.append(j)
        elif j == 5 and i < 5:
            triplet_ids.append(i)

    if len(triplet_ids) != 3:
        return None

    a, b, c = [terminals[idx] for idx in triplet_ids]
    s = fermat_point(a, b, c)
    if s is None:
        return None

    scale = max(distance(terminals[i], terminals[j])
                for i, j in itertools.combinations(range(5), 2))
    eps = max(1e-6, 5e-3 * scale)
    if any(distance(s, terminals[idx]) < eps for idx in triplet_ids):
        return None

    all_pts = terminals + [s]
    return {
        'length': total_length(edges_idx, all_pts),
        'edges_idx': edges_idx,
        'all_points': all_pts,
        'steiner_points': [s],
    }


def solve_steiner_5(points: list[tuple[float, float]]) -> dict:
    """
    Computes the optimal Euclidean Steiner tree for exactly 5 points.

    Enumerates all candidate topologies with 0 to 3 Steiner points and
    returns the one with the shortest total length.

    :param points: list of 5 (x, y) tuples
    :return: dict with keys 'length', 'edges', 'steiner_points', 'topology', 'all_points', 'k'
    :raises ValueError: if the input does not contain exactly 5 points
    """
    if len(points) != 5:
        raise ValueError("Exactement 5 points sont requis.")

    terminals = [np.asarray(p, dtype=float) for p in points]
    candidates: list[dict] = []

    mst_len, mst_edges = _mst_5(terminals)
    candidates.append({
        'length': mst_len,
        'edges_idx': mst_edges,
        'all_points': terminals[:],
        'steiner_points': [],
        'topology': 'MST (0 point de Steiner)',
        'k': 0,
    })

    topologies = generate_topologies_5_points()

    for topo in topologies:
        k = topo['k']

        if k == 1:
            res = _eval_one_steiner(terminals, topo)
        else:
            res = optimize_steiner_points(terminals, topo['edges_idx'], k)

        if res is not None:
            candidates.append({
                'length': res['length'],
                'edges_idx': res['edges_idx'],
                'all_points': res['all_points'],
                'steiner_points': res['steiner_points'],
                'topology': topo['label'],
                'k': k,
            })

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
        'k': best['k'],
    }


def afficher_resultat(res: dict, titre: str = '') -> None:
    """Prints the result in a readable format."""
    sep = '-' * 62
    print(sep)
    if titre:
        print(f'  {titre}')
    print(f'  Topologie  : {res["topology"]}')
    print(f'  k Steiner  : {res["k"]}')
    print(f'  Longueur   : {res["length"]:.8f}')
    print(f'  Pts Steiner: {len(res["steiner_points"])}')
    for i, s in enumerate(res['steiner_points']):
        print(f'    S{i + 1} = ({s[0]:.6f}, {s[1]:.6f})')
    print(f'  Segments   : {len(res["edges"])}')
    for p1, p2 in res['edges']:
        print(f'    ({p1[0]:7.4f},{p1[1]:7.4f}) -> '
              f'({p2[0]:7.4f},{p2[1]:7.4f})   d={distance(p1, p2):.5f}')
    print(sep)


def plot_solution(points: list[tuple], res: dict, titre: str = '') -> None:
    """
    Plots the solution using matplotlib.

    Terminal points are shown in steel blue, Steiner points in crimson.
    """
    try:
        import matplotlib.pyplot as plt
        import matplotlib.patches as mpatches
    except ImportError:
        print("matplotlib non disponible — pip install matplotlib")
        return

    fig, ax = plt.subplots(figsize=(8, 7))
    ax.set_aspect('equal')
    ax.set_title(titre or res['topology'], fontsize=10)

    for p1, p2 in res['edges']:
        ax.plot([p1[0], p2[0]], [p1[1], p2[1]],
                color='steelblue', lw=2.5, zorder=1)

    for i, (x, y) in enumerate(points):
        ax.scatter(x, y, s=140, color='steelblue', zorder=3)
        ax.annotate(f'T{i + 1}', (x, y), xytext=(7, 7),
                    textcoords='offset points', fontsize=11, color='steelblue',
                    fontweight='bold')

    for i, s in enumerate(res['steiner_points']):
        ax.scatter(s[0], s[1], s=160, color='crimson', marker='D', zorder=3)
        ax.annotate(f'S{i + 1}', (s[0], s[1]), xytext=(7, 7),
                    textcoords='offset points', fontsize=11, color='crimson',
                    fontweight='bold')

    ax.set_xlabel(
        f'Longueur totale = {res["length"]:.6f}  |  '
        f'{res["k"]} point(s) de Steiner  |  {res["topology"][:60]}',
        fontsize=9
    )

    t_patch = mpatches.Patch(color='steelblue', label='Terminal')
    s_patch = mpatches.Patch(color='crimson', label='Steiner')
    ax.legend(handles=[t_patch, s_patch], loc='upper right', fontsize=9)

    plt.tight_layout()
    plt.show()


if __name__ == '__main__':
    import math as _math

    penta = [(round(_math.cos(2 * _math.pi * i / 5), 6),
              round(_math.sin(2 * _math.pi * i / 5), 6))
             for i in range(5)]
    afficher_resultat(solve_steiner_5(penta), 'Ex 1 — Pentagone regulier (rayon 1)')

    afficher_resultat(
        solve_steiner_5([(0.0, 0.0), (4.0, 0.0), (4.0, 4.0),
                         (0.0, 4.0), (2.0, 2.0)]),
        'Ex 2 — Carre 4x4 + centre'
    )

    afficher_resultat(
        solve_steiner_5([(0.0, 0.0), (1.0, 0.0), (2.0, 0.0),
                         (3.0, 0.0), (4.0, 0.0)]),
        'Ex 3 — 5 points colineaires (MST attendu)'
    )

    afficher_resultat(
        solve_steiner_5([(1.0, 2.0), (5.0, 1.0), (8.0, 4.0),
                         (6.0, 8.0), (2.0, 7.0)]),
        'Ex 4 — Points quelconques'
    )

    afficher_resultat(
        solve_steiner_5([(0.0, 0.0), (2.0, 0.0), (4.0, 0.0),
                         (1.0, 2.0), (3.0, 2.0)]),
        'Ex 5 — W en etoile (k=3 probable)'
    )
