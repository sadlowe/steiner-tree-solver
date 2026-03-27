"""
steiner_4pts.py
===============================================================================
Solveur de l'arbre de Steiner euclidien pour exactement 4 points dans le plan.

CONTEXTE DU PROJET
------------------
Ce script fait partie du projet "Arbre de Steiner et bulles de savon".
Il implémente une solution géométrique exacte pour le problème de Steiner
euclidien à 4 terminaux, en complément du backend Java de l'application.

Il peut servir à :
  - Valider les résultats du backend Spring Boot
  - Démontrer les algorithmes lors d'une présentation
  - Explorer les topologies possibles pour un ensemble de points

PROBLÈME RÉSOLU
---------------
Étant donné 4 points dans le plan (les "terminaux"), trouver l'arbre de longueur
minimale qui les connecte tous, en autorisant l'ajout de nouveaux points
intermédiaires (les "points de Steiner").

Propriété clé : dans la solution optimale, les arêtes se rejoignent toujours
à exactement 120° aux points de Steiner (Point de Fermat-Torricelli).

STRATÉGIE
---------
Pour n = 4 terminaux, un arbre de Steiner optimal admet au plus n − 2 = 2
points de Steiner. On énumère exhaustivement les 16 topologies candidates :

  • 0 Steiner  (  1 topologie) : arbre couvrant minimal (MST) des 4 terminaux
  • 1 Steiner  ( 12 topologies): triplet de 3 terminaux -> point de Fermat,
                                  le 4e terminal se raccroche a l'un d'eux
  • 2 Steiner  (  3 topologies): partition {a,b}|{c,d}, S1 et S2 optimisés
                                  numériquement par L-BFGS-B (scipy)

DÉPENDANCES
-----------
    pip install numpy scipy matplotlib

UTILISATION RAPIDE
------------------
    from steiner_4pts import steiner_tree_4_points

    result = steiner_tree_4_points([(0,0), (1,0), (1,1), (0,1)])
    print(result['length'])          # 2.73205...  (= 1 + sqrt(3))
    print(result['steiner_points'])  # [S1, S2]
    for p1, p2 in result['edges']:   # segments prets a tracer
        ...

===============================================================================
"""

from __future__ import annotations
import math
import itertools
from typing import Optional

import numpy as np
from scipy.optimize import minimize


# ===============================================================================
# 1. Fonctions de base
# ===============================================================================

def distance(p, q) -> float:
    """Distance euclidienne entre deux points 2D."""
    return float(np.linalg.norm(np.asarray(p, float) - np.asarray(q, float)))


def longueur_totale(edges: list[tuple[int, int]],
                    all_points: list[np.ndarray]) -> float:
    """
    Longueur totale d'un ensemble de segments.

    Parameters
    ----------
    edges      : liste de paires d'indices (i, j) dans all_points
    all_points : liste de coordonnées 2D, [terminaux..., steiner_points...]
    """
    return sum(distance(all_points[i], all_points[j]) for i, j in edges)


def _cross2d(u: np.ndarray, v: np.ndarray) -> float:
    """
    Produit vectoriel 2D (scalaire).
    Remplace np.cross pour la compatibilité NumPy >= 2.0.
    """
    return float(u[0] * v[1] - u[1] * v[0])


# ===============================================================================
# 2. Point de Fermat-Torricelli — itérations de Weiszfeld
# ===============================================================================

def fermat_point(a: np.ndarray,
                 b: np.ndarray,
                 c: np.ndarray,
                 tol: float = 1e-11,
                 max_iter: int = 50_000) -> Optional[np.ndarray]:
    """
    Calcule le point de Fermat-Torricelli F = argmin d(F,a) + d(F,b) + d(F,c).

    Règle géométrique fondamentale
    --------------------------------
    Si un angle du triangle ABC est >= 120 deg, alors F est ce sommet.
    Ajouter un point de Steiner en ce sommet n'améliorerait pas la longueur.
    Sinon, F est l'unique point intérieur où les trois arêtes forment 120 deg.

    Algorithme : itérations de Weiszfeld (1937)
    -------------------------------------------
        F_{k+1} = sum_i (P_i / d(F_k, P_i))
                  --------------------------
                  sum_i (1  / d(F_k, P_i))

    Convergence linéaire garantie si F ne coïncide pas avec un P_i.

    Retourne None si les trois points sont colinéaires (triangle dégénéré).
    """
    a, b, c = np.asarray(a, float), np.asarray(b, float), np.asarray(c, float)

    # Cas dégénéré : triangle plat (aire ≈ 0)
    if abs(_cross2d(b - a, c - a)) < 1e-14:
        return None

    def _cos_at(vtx, p1, p2) -> float:
        u, v = p1 - vtx, p2 - vtx
        denom = np.linalg.norm(u) * np.linalg.norm(v)
        return float(np.dot(u, v) / denom) if denom > 1e-15 else 1.0

    # cos(120 deg) = -0.5 ; angle >= 120 deg  <=>  cosinus <= -0.5
    cos120 = math.cos(math.radians(120.0))
    if _cos_at(a, b, c) <= cos120:
        return a.copy()
    if _cos_at(b, a, c) <= cos120:
        return b.copy()
    if _cos_at(c, a, b) <= cos120:
        return c.copy()

    # Itérations de Weiszfeld depuis le centroïde
    pts = [a, b, c]
    f = (a + b + c) / 3.0

    for _ in range(max_iter):
        dists = [distance(f, p) for p in pts]

        if min(dists) < 1e-12:
            # F coincide quasi exactement avec un sommet
            return pts[int(np.argmin(dists))].copy()

        w = [1.0 / d for d in dists]
        f_new = sum(wi * pi for wi, pi in zip(w, pts)) / sum(w)

        if distance(f_new, f) < tol:
            f = f_new
            break
        f = f_new

    return f


# ===============================================================================
# 3. Arbre couvrant minimal — algorithme de Prim (O(n²), suffisant pour n=4)
# ===============================================================================

def _mst_4(terminals: list[np.ndarray]) -> tuple[float, list[tuple[int, int]]]:
    """MST des 4 terminaux. Renvoie (longueur_totale, liste_aretes)."""
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


# ===============================================================================
# 4. Topologies avec 1 point de Steiner (12 cas)
# ===============================================================================

def _eval_one_steiner(terminals: list[np.ndarray],
                      triplet: tuple[int, int, int],
                      lone: int,
                      attach: int) -> Optional[dict]:
    """
    Évalue la topologie à 1 point de Steiner :
      S = point de Fermat des 3 terminaux du triplet
      Le terminal `lone` se raccorde directement au terminal `attach`

    Structure de l'arbre (5 noeuds, 4 aretes) :
      S -> triplet[0]
      S -> triplet[1]
      S -> triplet[2]
      lone -> attach

    Retourne None si le triplet est colinéaire (Fermat indéfini).
    """
    a, b, c = [terminals[i] for i in triplet]
    s = fermat_point(a, b, c)
    if s is None:
        return None

    at_vertex = any(distance(s, terminals[i]) < 1e-8 for i in triplet)

    all_pts = terminals + [s]   # index 4 = S
    edges = [(4, triplet[0]), (4, triplet[1]), (4, triplet[2]), (lone, attach)]

    return {
        'length': longueur_totale(edges, all_pts),
        'edges_idx': edges,
        'all_points': all_pts,
        'steiner_points': [] if at_vertex else [s],
        'topology': (f'1 Steiner | triplet={triplet} '
                     f'| lone={lone} -> attach={attach}'),
    }


# ===============================================================================
# 5. Topologies avec 2 points de Steiner (3 cas) — optimisation numérique
# ===============================================================================

def _check_120(s: np.ndarray,
               neighbors: list[np.ndarray],
               tol_deg: float = 10.0) -> bool:
    """
    Vérifie que les 3 arêtes issues du point de Steiner S
    forment des angles d'environ 120 deg entre elles.
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
    Génère 4 points de départ pour l'optimisation (S1, S2).
    S1 doit être entre a, b et la zone {c,d} ; S2 inversement.
    """
    mab  = (a + b) / 2.0
    mcd  = (c + d) / 2.0
    ctr  = (a + b + c + d) / 4.0
    span = distance(mab, mcd)

    starts = []

    # Init 1 : milieux décalés vers le centroïde
    starts.append(np.r_[(mab * 2 + mcd) / 3.0, (mcd * 2 + mab) / 3.0])

    # Init 2 : Fermat(a, b, mcd) et Fermat(c, d, mab)
    f1 = fermat_point(a, b, mcd)
    f2 = fermat_point(c, d, mab)
    if f1 is not None and f2 is not None:
        starts.append(np.r_[f1, f2])

    # Init 3 : perturbation orthogonale du centroïde
    direction = mcd - mab
    if np.linalg.norm(direction) > 1e-9:
        perp = np.array([-direction[1], direction[0]])
        perp = perp / np.linalg.norm(perp) * 0.15 * span
    else:
        perp = np.array([0.1 * span, 0.0])
    starts.append(np.r_[ctr + perp, ctr - perp])

    # Init 4 : décalé vers les paires respectives
    starts.append(np.r_[0.65 * a + 0.35 * ctr, 0.65 * c + 0.35 * ctr])

    return starts


def _eval_two_steiner(terminals: list[np.ndarray],
                      pair1: tuple[int, int],
                      pair2: tuple[int, int]) -> Optional[dict]:
    """
    Évalue la topologie à 2 points de Steiner :
      S1 relié à pair1[0], pair1[1] et S2   (angles 120 deg)
      S2 relié à pair2[0], pair2[1] et S1   (angles 120 deg)

    Les positions de S1 et S2 sont optimisées par L-BFGS-B (scipy)
    avec 4 initialisations pour éviter les minima locaux.

    Retourne None si la solution est géométriquement invalide
    (Steiner confondu avec un terminal, ou angles trop éloignés de 120 deg).
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

    # Seuil de dégénérescence = 0.01% de la plus grande distance entre terminaux
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


# ===============================================================================
# 6. Fonction principale
# ===============================================================================

def steiner_tree_4_points(points: list[tuple[float, float]]) -> dict:
    """
    Arbre de Steiner euclidien optimal pour exactement 4 points dans le plan.

    Parameters
    ----------
    points : liste de 4 tuples (x, y)

    Returns
    -------
    dict :
        'length'         -- float           : longueur totale minimale
        'edges'          -- list[(arr, arr)]: segments directement tracables
        'steiner_points' -- list[ndarray]   : coordonnées des points de Steiner
        'topology'       -- str             : description de la topologie choisie
        'all_points'     -- list[ndarray]   : [terminaux..., steiner_points...]

    Exemple
    -------
        result = steiner_tree_4_points([(0,0), (1,0), (1,1), (0,1)])
        # result['length']  ->  2.73205080...  (= 1 + sqrt(3))
        # result['topology'] -> '2 Steiner | paires (0, 1) | (2, 3)'
    """
    if len(points) != 4:
        raise ValueError("Exactement 4 points sont requis.")

    terminals = [np.asarray(p, dtype=float) for p in points]
    candidates: list[dict] = []

    # --- 0 point de Steiner : MST ------------------------------------------
    mst_len, mst_edges = _mst_4(terminals)
    candidates.append({
        'length': mst_len,
        'edges_idx': mst_edges,
        'all_points': terminals[:],
        'steiner_points': [],
        'topology': 'MST (0 point de Steiner)',
    })

    # --- 1 point de Steiner : C(4,3) x 3 = 12 topologies ------------------
    for triplet in itertools.combinations(range(4), 3):
        lone = next(x for x in range(4) if x not in triplet)
        for attach in triplet:
            res = _eval_one_steiner(terminals, triplet, lone, attach)
            if res is not None:
                candidates.append(res)

    # --- 2 points de Steiner : 3 partitions en 2 paires --------------------
    for pair1, pair2 in [((0, 1), (2, 3)), ((0, 2), (1, 3)), ((0, 3), (1, 2))]:
        res = _eval_two_steiner(terminals, pair1, pair2)
        if res is not None:
            candidates.append(res)

    # --- Sélection de la meilleure topologie --------------------------------
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


# ===============================================================================
# 7. Affichage
# ===============================================================================

def afficher_resultat(res: dict, titre: str = '') -> None:
    """Affiche le résultat de façon lisible dans le terminal."""
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


# ===============================================================================
# 8. Visualisation (optionnelle, nécessite matplotlib)
# ===============================================================================

def visualiser(points: list[tuple], res: dict, titre: str = '') -> None:
    """Trace la solution avec matplotlib."""
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


# ===============================================================================
# 9. Exemples d'utilisation
# ===============================================================================

if __name__ == '__main__':

    # Exemple 1 : carre unite
    # Solution exacte : 1 + sqrt(3) = 2.73205080...
    afficher_resultat(
        steiner_tree_4_points([(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0)]),
        'Ex 1 -- Carre unite  (ref: 1+sqrt(3) = 2.73205080)'
    )

    # Exemple 2 : points quelconques
    afficher_resultat(
        steiner_tree_4_points([(1.0, 2.0), (4.0, 6.0), (7.0, 1.0), (3.0, 8.0)]),
        'Ex 2 -- Points quelconques'
    )

    # Exemple 3 : losange allonge (MST probablement optimal)
    afficher_resultat(
        steiner_tree_4_points([(0.0, 0.0), (10.0, 0.0), (5.0, 0.2), (5.0, -0.2)]),
        'Ex 3 -- Losange allonge'
    )

    # Exemple 4 : rectangle 3x1
    afficher_resultat(
        steiner_tree_4_points([(0.0, 0.0), (3.0, 0.0), (3.0, 1.0), (0.0, 1.0)]),
        'Ex 4 -- Rectangle 3x1'
    )
