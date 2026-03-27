"""
steiner_5pts.py
===============================================================================
Solveur de l'arbre de Steiner euclidien pour exactement 5 points dans le plan.

CONTEXTE DU PROJET
------------------
Ce script fait partie du projet "Arbre de Steiner et bulles de savon".
Il implémente une solution numérique exacte-sur-toutes-topologies pour le
problème de Steiner euclidien à 5 terminaux.

Il peut servir à :
  - Valider les résultats du backend Spring Boot
  - Démontrer les algorithmes lors d'une présentation
  - Explorer les topologies possibles pour un ensemble de points

PROBLÈME RÉSOLU
---------------
Étant donné 5 points dans le plan (les "terminaux"), trouver l'arbre de longueur
minimale qui les connecte tous, en autorisant l'ajout de nouveaux points
intermédiaires (les "points de Steiner").

Propriété clé : dans la solution optimale, les arêtes se rejoignent toujours
à exactement 120° aux points de Steiner (Point de Fermat-Torricelli).

POURQUOI AU PLUS n − 2 POINTS DE STEINER ?
-------------------------------------------
Un arbre sur n + k nœuds a exactement n + k − 1 arêtes.
Dans un arbre de Steiner optimal, chaque point de Steiner a degré exactement 3
(si son degré était 2, on pourrait supprimer cet intermédiaire sans allonger
l'arbre ; si son degré était ≥ 4, on pourrait le remplacer par deux Steiner
voisins plus courts). La somme des degrés vaut 2(n + k − 1). Les n terminaux
ont degré ≥ 1 donc contribuent au moins n. Les k Steiner contribuent 3k.
D'où : 2(n + k − 1) ≥ n + 3k → n ≥ k + 2 → k ≤ n − 2.
Pour n = 5 : au plus 3 points de Steiner.

STRATÉGIE
---------
Pour n = 5 terminaux, un arbre de Steiner optimal admet au plus n − 2 = 3
points de Steiner. On énumère exhaustivement toutes les topologies candidates :

  • 0 Steiner  (  1 topologie) : arbre couvrant minimal (MST) des 5 terminaux
  • 1 Steiner  (~150 topologies): triplet de 3 terminaux → point de Fermat,
                                   les 2 terminaux restants se raccordent à l'arbre
  • 2 Steiner  ( ~75 topologies): deux types de structures
                                   - Type I  : S0-S1 directs, 2+2 terminaux + 1 queue
                                   - Type II : T_milieu entre S0 et S1, 2+2 terminaux
  • 3 Steiner  ( 15 topologies) : topologie complète en chaîne S0-S1-S2
                                   (la seule structure arborescente à 3 Steiner)

POURQUOI LA STRUCTURE EN CHAÎNE POUR k = 3 ?
---------------------------------------------
Pour k = 3 et n = 5, l'arbre a 8 nœuds et 7 arêtes. Chaque Steiner a degré 3.
La seule topologie arborescente où 3 nœuds ont degré 3 et 5 nœuds ont degré ≥ 1
est la chaîne S0-S1-S2 où S0 et S2 ont chacun 2 feuilles terminales et S1
(milieu) a 1 feuille terminale. Il y a exactement 5 choix pour T_milieu (qui
va sur S1) × C(4,2)/2 = 3 façons de répartir les 4 autres → 15 topologies.

OPTIMISATION
------------
Pour k ≥ 2, les positions des points de Steiner sont optimisées par L-BFGS-B
(scipy) avec plusieurs initialisations. Pour k = 1, le point de Fermat-Torricelli
est calculé exactement par les itérations de Weiszfeld (convergence garantie
si aucun angle du triangle n'est ≥ 120°).

DÉPENDANCES
-----------
    pip install numpy scipy matplotlib

UTILISATION RAPIDE
------------------
    from steiner_5pts import steiner_tree_5_points

    result = steiner_tree_5_points([(0,0),(4,0),(2,4),(0,4),(4,4)])
    print(result['length'])
    print(result['topology'])
    for p1, p2 in result['edges']:
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


def total_length(edges: list[tuple[int, int]],
                 all_points: list[np.ndarray]) -> float:
    """
    Longueur totale d'un ensemble de segments.

    Parameters
    ----------
    edges      : liste de paires d'indices (i, j) dans all_points
    all_points : liste de coordonnées 2D [terminaux[0..4], steiner_pts...]
    """
    return sum(distance(all_points[i], all_points[j]) for i, j in edges)


def _cross2d(u: np.ndarray, v: np.ndarray) -> float:
    """Produit vectoriel 2D (scalaire)."""
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

    POURQUOI 120° ?
    ---------------
    Le gradient de f(F) = d(F,a) + d(F,b) + d(F,c) vaut :
        ∇f = (F-a)/‖F-a‖ + (F-b)/‖F-b‖ + (F-c)/‖F-c‖
    Au minimum, ∇f = 0, ce qui impose que les trois vecteurs unitaires
    sortants se compensent, c'est-à-dire forment des angles de 120° entre eux.

    RÈGLE DU SOMMET (angle ≥ 120°)
    --------------------------------
    Si un angle du triangle ABC est ≥ 120°, alors F est ce sommet.
    En ajoutant un Steiner en cet endroit on ne gagnerait rien.

    ALGORITHME DE WEISZFELD (1937)
    -------------------------------
        F_{k+1} = Σ_i (P_i / d(F_k, P_i))
                  --------------------------
                  Σ_i (1  / d(F_k, P_i))

    Convergence linéaire garantie si F ne coïncide pas avec un P_i.

    Retourne None si le triangle est dégénéré (points colinéaires).
    """
    a, b, c = np.asarray(a, float), np.asarray(b, float), np.asarray(c, float)

    if abs(_cross2d(b - a, c - a)) < 1e-14:
        return None  # triangle dégénéré (colinéaire)

    def _cos_at(vtx, p1, p2) -> float:
        u, v = p1 - vtx, p2 - vtx
        denom = np.linalg.norm(u) * np.linalg.norm(v)
        return float(np.dot(u, v) / denom) if denom > 1e-15 else 1.0

    cos120 = math.cos(math.radians(120.0))  # = -0.5
    if _cos_at(a, b, c) <= cos120:
        return a.copy()
    if _cos_at(b, a, c) <= cos120:
        return b.copy()
    if _cos_at(c, a, b) <= cos120:
        return c.copy()

    pts = [a, b, c]
    f = (a + b + c) / 3.0  # démarrage au centroïde

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


# ===============================================================================
# 3. Arbre couvrant minimal — algorithme de Prim (O(n²), suffisant pour n=5)
# ===============================================================================

def _mst_5(terminals: list[np.ndarray]) -> tuple[float, list[tuple[int, int]]]:
    """
    MST des 5 terminaux par algorithme de Prim.
    Retourne (longueur_totale, liste_aretes).

    Complexité O(n²) : acceptable ici car n = 5.
    Propriété : MST ≤ 2 × Steiner_optimal (borne classique).
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


# ===============================================================================
# 4. Génération des topologies
# ===============================================================================

def generate_topologies_5_points() -> list[dict]:
    """
    Génère toutes les topologies candidates pour un arbre de Steiner sur
    5 terminaux. Chaque topologie est un dictionnaire :
      {
        'k'         : int             — nombre de points de Steiner (0 à 3)
        'edges_idx' : list[(int,int)] — indices d'arêtes (0-4 = terminaux, 5-7 = Steiner)
        'label'     : str             — description lisible
      }

    Convention des indices dans edges_idx :
        0, 1, 2, 3, 4  →  terminaux T0..T4
        5              →  S0 (1er point de Steiner)
        6              →  S1 (2e point de Steiner)
        7              →  S2 (3e point de Steiner)

    Topologies générées :
      k=0 :  1 topologie  (MST, calculé séparément)
      k=1 : ~150 topologies (10 triplets × ~15 attachements des 2 terminaux restants)
      k=2 :  ~75 topologies (Type I : S0-S1 direct + queue ; Type II : T_milieu entre S0 et S1)
      k=3 :  15 topologies  (chaîne S0-S1-S2, seule structure complète possible)
    """
    topologies: list[dict] = []

    # -----------------------------------------------------------------------
    # k = 1 : un point de Steiner S0 (indice 5)
    #   S0 est le point de Fermat d'un triplet de 3 terminaux.
    #   Les 2 terminaux restants (l0, l1 avec l0 < l1) doivent être connectés
    #   à l'arbre partiel {S0, Ti0, Ti1, Ti2} sans créer de cycle.
    #   Règle : S0 est déjà à degré 3 → l0 et l1 s'accrochent aux terminaux
    #   du triplet, ou l'un s'accroche à l'autre (chaîne).
    # -----------------------------------------------------------------------
    for triplet in itertools.combinations(range(5), 3):
        ti0, ti1, ti2 = triplet
        lones = [x for x in range(5) if x not in triplet]
        l0, l1 = lones  # l0 < l1 toujours (itertools.combinations garantit l'ordre)

        # Nœuds du triplet disponibles pour l'accrochage (S0 est plein)
        tri_nodes = [ti0, ti1, ti2]

        # l0 s'accroche à un nœud du triplet, l1 s'accroche au triplet ou à l0
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

        # l1 s'accroche à un nœud du triplet et l0 s'accroche à l1 (chaîne inversée)
        for attach_l1 in tri_nodes:
            edges = [(5, ti0), (5, ti1), (5, ti2),
                     (l1, attach_l1), (l0, l1)]
            topologies.append({
                'k': 1,
                'edges_idx': edges,
                'label': (f'1S | triplet=({ti0},{ti1},{ti2}) '
                          f'| l1={l1}→{attach_l1} | l0={l0}→l1 (chaîne)'),
            })

    # -----------------------------------------------------------------------
    # k = 2 : deux points de Steiner S0 (indice 5) et S1 (indice 6)
    #
    # TYPE I — S0 et S1 sont directement connectés, chacun raccorde 2 terminaux.
    #   Le 5e terminal (queue) s'accroche à l'un des 4 terminaux directs.
    #   Structure : S0(-Ta,-Tb,-S1), S1(-Tc,-Td,-S0), queue→attachement
    #   Pourquoi cette structure ? Dans un arbre optimal, chaque Steiner a degré
    #   exactement 3. Avec l'arête S0-S1 : chaque Steiner a 2 terminaux feuilles.
    #   Total : 5 arêtes pour 4 terminaux + S0 + S1 = 6 nœuds, il manque 1 arête
    #   pour connecter le 5e terminal → il s'accroche à un terminal existant.
    # -----------------------------------------------------------------------
    for tail in range(5):
        core = [x for x in range(5) if x != tail]
        ta, tb, tc, td = core

        # 3 partitions non ordonnées de {ta,tb,tc,td} en 2 paires pour S0 et S1
        for (pa, pb), (pc, pd) in [
            ((ta, tb), (tc, td)),
            ((ta, tc), (tb, td)),
            ((ta, td), (tb, tc)),
        ]:
            # queue peut s'accrocher à pa, pb, pc ou pd
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

    # TYPE II — Un terminal T_mid est placé entre S0 et S1 sur la chaîne.
    #   Structure : S0(-Ta,-Tb,-T_mid), T_mid(-S0,-S1), S1(-T_mid,-Tc,-Td)
    #   T_mid a degré 2 (autorisé pour un terminal), S0 et S1 ont degré 3.
    #   Tous les 5 terminaux sont utilisés → pas de queue nécessaire.
    #   Nombre de topologies : 5 (choix de T_mid) × C(4,2)/2 = 15
    # -----------------------------------------------------------------------
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

    # -----------------------------------------------------------------------
    # k = 3 : trois points de Steiner S0 (5), S1 (6), S2 (7)
    #
    # UNIQUE STRUCTURE — chaîne S0-S1-S2 :
    #   S0 ← T_a, T_b, S1        (S0 relie 2 terminaux feuilles)
    #   S1 ← S0, T_c, S2        (S1 relie 1 terminal feuille)
    #   S2 ← S1, T_d, T_e       (S2 relie 2 terminaux feuilles)
    #
    # C'est la SEULE topologie arborescente avec k=3 et n=5 :
    #   - 3 Steiner de degré 3 + 5 terminaux = 8 nœuds, 7 arêtes
    #   - la somme des degrés = 2×7=14 = 3×3 + 5×1 = 14 ✓
    #   - l'unique arbre sur 3 nœuds de degré 3 est la chaîne (pas d'étoile
    #     à 3 branches : un nœud central de degré 3 ne peut relier que 3 nœuds
    #     de degré 1 sans connecter les Steiner entre eux)
    #
    # Comptage des 15 topologies :
    #   5 choix pour T_c (le terminal "milieu" connecté à S1)
    #   × C(4,2) / 2 = 3 façons de diviser les 4 restants en paires
    #   = 15 topologies
    #
    # Division par 2 pour la symétrie S0 ↔ S2 :
    #   Retourner la chaîne S0-S1-S2 → S2-S1-S0 donne le même arbre.
    #   On utilise des partitions non ordonnées pour éviter les doublons.
    # -----------------------------------------------------------------------
    for tc in range(5):
        leaves = [x for x in range(5) if x != tc]
        ta, tb, td, te = leaves

        # Partitions non ordonnées de {ta,tb,td,te} en 2 paires (S0 et S2)
        for (pa, pb), (pd, pe) in [
            ((ta, tb), (td, te)),
            ((ta, td), (tb, te)),
            ((ta, te), (tb, td)),
        ]:
            #  S0(5) – S1(6) – S2(7)
            #  S0 raccorde T_pa, T_pb
            #  S1 raccorde T_tc (terminal milieu)
            #  S2 raccorde T_pd, T_pe
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


# ===============================================================================
# 5. Initialisations des points de Steiner
# ===============================================================================

def _steiner_inits(terminals: list[np.ndarray],
                   edges_idx: list[tuple[int, int]],
                   k: int) -> list[np.ndarray]:
    """
    Génère plusieurs points de départ pour les k points de Steiner.

    Stratégie :
      - Init 0 : centroïde des voisins de chaque Steiner (départ naturel)
      - Init 1 : point de Fermat des voisins terminaux de chaque Steiner
      - Init 2 : centroïde global + perturbation aléatoire fixe (graine 42)
    """
    # Construire la liste des voisins de chaque Steiner
    n_total = 5 + k
    nbrs: list[list[int]] = [[] for _ in range(k)]
    for i, j in edges_idx:
        if 5 <= i < n_total and j < 5:
            nbrs[i - 5].append(j)
        if 5 <= j < n_total and i < 5:
            nbrs[j - 5].append(i)
        # Voisinage entre Steiner : chaque point de Steiner aura aussi des voisins Steiner
        # mais pour l'initialisation on ne regarde que les terminaux

    global_ctr = sum(terminals) / len(terminals)

    starts: list[np.ndarray] = []

    # Init 0 : centroïde des voisins terminaux
    x0_parts = []
    for si in range(k):
        term_nbrs = [terminals[j] for j in nbrs[si]]
        if term_nbrs:
            cx = sum(term_nbrs) / len(term_nbrs)
        else:
            cx = global_ctr
        x0_parts.append(cx)
    starts.append(np.concatenate(x0_parts))

    # Init 1 : point de Fermat des voisins terminaux (si 3 voisins terminaux)
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

    # Init 2 : perturbations autour du centroïde global
    rng = np.random.default_rng(42)
    scale = max(
        distance(terminals[i], terminals[j])
        for i, j in itertools.combinations(range(5), 2)
    ) * 0.1
    for _ in range(3):
        parts = [global_ctr + rng.standard_normal(2) * scale for _ in range(k)]
        starts.append(np.concatenate(parts))

    return starts


# ===============================================================================
# 6. Optimisation des points de Steiner par L-BFGS-B
# ===============================================================================

def optimize_steiner_points(terminals: list[np.ndarray],
                             edges_idx: list[tuple[int, int]],
                             k: int) -> Optional[dict]:
    """
    Optimise les positions des k points de Steiner par L-BFGS-B (scipy).

    Principe de l'optimisation alternante de Weiszfeld / L-BFGS-B
    -------------------------------------------------------------
    Pour les topologies à k ≥ 2, les Steiner sont couplés (S0 voisin de S1).
    On minimise simultanément f(S0, S1, ...) = longueur totale de l'arbre.
    L-BFGS-B calcule le gradient automatiquement par différences finies et
    converge vers un minimum local.

    Plusieurs initialisations (voir _steiner_inits) permettent d'explorer
    différents bassins d'attraction et d'éviter les minima locaux.

    Validation géométrique
    ----------------------
    Après optimisation, on vérifie que :
      - aucun Steiner n'est confondu avec un terminal (seuil eps)
      - aucun Steiner n'est confondu avec un autre Steiner (seuil eps)
    Ces dégénérescences indiquent que cette topologie n'est pas valide pour
    cette configuration de terminaux (la solution optimale est une topologie
    avec moins de Steiner).

    Retourne None si la solution est géométriquement invalide.
    """
    scale = max(
        distance(terminals[i], terminals[j])
        for i, j in itertools.combinations(range(5), 2)
    )
    # Seuil de dégénérescence : 0.5% de la plus grande distance entre terminaux
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

    # Validation : vérifier qu'aucun Steiner n'est dégénéré
    all_pts = terminals + steiner_pts
    for si, s in enumerate(steiner_pts):
        # Confondu avec un terminal ?
        if any(distance(s, t) < eps for t in terminals):
            return None
        # Confondu avec un autre Steiner ?
        for sj, s2 in enumerate(steiner_pts):
            if si != sj and distance(s, s2) < eps:
                return None

    return {
        'length': float(best_res.fun),
        'edges_idx': edges_idx,
        'all_points': all_pts,
        'steiner_points': steiner_pts,
    }


# ===============================================================================
# 7. Topologie k = 1 : calcul exact par Fermat
# ===============================================================================

def _eval_one_steiner(terminals: list[np.ndarray],
                      topo: dict) -> Optional[dict]:
    """
    Évalue une topologie à 1 point de Steiner.
    S0 est le point de Fermat exact du triplet indiqué dans les arêtes.

    Pour k = 1, S0 a exactement 3 voisins terminaux (son triplet).
    Sa position est calculée analytiquement par Weiszfeld (pas d'optimisation
    numérique nécessaire), ce qui garantit le minimum exact pour cette topologie.
    """
    edges_idx = topo['edges_idx']

    # Identifier les 3 voisins de S0 (indice 5) dans les arêtes
    triplet_ids = []
    for i, j in edges_idx:
        if i == 5 and j < 5:
            triplet_ids.append(j)
        elif j == 5 and i < 5:
            triplet_ids.append(i)

    if len(triplet_ids) != 3:
        return None  # topologie invalide pour k=1

    a, b, c = [terminals[idx] for idx in triplet_ids]
    s = fermat_point(a, b, c)
    if s is None:
        return None

    # Si S coïncide avec un sommet du triplet, la topologie est dégénérée
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


# ===============================================================================
# 8. Fonction principale
# ===============================================================================

def solve_steiner_5(points: list[tuple[float, float]]) -> dict:
    """
    Arbre de Steiner euclidien optimal pour exactement 5 points dans le plan.

    Parameters
    ----------
    points : liste de 5 tuples (x, y)

    Returns
    -------
    dict :
        'length'         -- float           : longueur totale minimale
        'edges'          -- list[(arr,arr)] : segments directement traçables
        'steiner_points' -- list[ndarray]   : coordonnées des points de Steiner
        'topology'       -- str             : description de la topologie choisie
        'all_points'     -- list[ndarray]   : [terminaux[0..4], steiner_pts...]
        'k'              -- int             : nombre de points de Steiner

    Exemple
    -------
        result = solve_steiner_5([(0,0),(4,0),(8,0),(2,4),(6,4)])
        print(result['length'])
        print(result['topology'])
    """
    if len(points) != 5:
        raise ValueError("Exactement 5 points sont requis.")

    terminals = [np.asarray(p, dtype=float) for p in points]
    candidates: list[dict] = []

    # --- k = 0 : MST -------------------------------------------------------
    mst_len, mst_edges = _mst_5(terminals)
    candidates.append({
        'length': mst_len,
        'edges_idx': mst_edges,
        'all_points': terminals[:],
        'steiner_points': [],
        'topology': 'MST (0 point de Steiner)',
        'k': 0,
    })

    # --- k = 1, 2, 3 : énumération des topologies --------------------------
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

    # --- Sélection du meilleur candidat ------------------------------------
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


# ===============================================================================
# 9. Affichage texte
# ===============================================================================

def afficher_resultat(res: dict, titre: str = '') -> None:
    """Affiche le résultat de façon lisible dans le terminal."""
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


# ===============================================================================
# 10. Visualisation (nécessite matplotlib)
# ===============================================================================

def plot_solution(points: list[tuple], res: dict, titre: str = '') -> None:
    """
    Trace la solution avec matplotlib.

    Légende :
      - Bleu acier   : terminaux (T1..T5)
      - Losange rouge : points de Steiner (S1..S3)
      - Ligne bleue  : arêtes de l'arbre
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

    # Arêtes
    for p1, p2 in res['edges']:
        ax.plot([p1[0], p2[0]], [p1[1], p2[1]],
                color='steelblue', lw=2.5, zorder=1)

    # Terminaux
    for i, (x, y) in enumerate(points):
        ax.scatter(x, y, s=140, color='steelblue', zorder=3)
        ax.annotate(f'T{i + 1}', (x, y), xytext=(7, 7),
                    textcoords='offset points', fontsize=11, color='steelblue',
                    fontweight='bold')

    # Points de Steiner
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


# ===============================================================================
# 11. Exemples d'utilisation
# ===============================================================================

if __name__ == '__main__':

    # Exemple 1 : pentagone régulier
    # Les points sont disposés en pentagone régulier de rayon 1
    import math as _math
    penta = [(round(_math.cos(2 * _math.pi * i / 5), 6),
              round(_math.sin(2 * _math.pi * i / 5), 6))
             for i in range(5)]
    afficher_resultat(
        solve_steiner_5(penta),
        'Ex 1 — Pentagone régulier (rayon 1)'
    )

    # Exemple 2 : grille 2×2 + centre
    afficher_resultat(
        solve_steiner_5([(0.0, 0.0), (4.0, 0.0), (4.0, 4.0),
                         (0.0, 4.0), (2.0, 2.0)]),
        'Ex 2 — Carré 4×4 + centre'
    )

    # Exemple 3 : 5 points colinéaires (dégénéré — MST attendu)
    afficher_resultat(
        solve_steiner_5([(0.0, 0.0), (1.0, 0.0), (2.0, 0.0),
                         (3.0, 0.0), (4.0, 0.0)]),
        'Ex 3 — 5 points colinéaires (MST attendu)'
    )

    # Exemple 4 : points quelconques issus d'une vraie instance
    afficher_resultat(
        solve_steiner_5([(1.0, 2.0), (5.0, 1.0), (8.0, 4.0),
                         (6.0, 8.0), (2.0, 7.0)]),
        'Ex 4 — Points quelconques'
    )

    # Exemple 5 : W en étoile (k=3 attendu)
    afficher_resultat(
        solve_steiner_5([(0.0, 0.0), (2.0, 0.0), (4.0, 0.0),
                         (1.0, 2.0), (3.0, 2.0)]),
        'Ex 5 — W en étoile (k=3 probable)'
    )
