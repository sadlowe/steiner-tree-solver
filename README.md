# Arbre de Steiner et Bulles de Savon

> Projet universitaire — Visualisation interactive du problème de l'arbre de Steiner euclidien

---

## C'est quoi ce projet ?

Imagine que tu dois construire des routes pour relier plusieurs villes.
Tu veux que **la longueur totale des routes soit la plus courte possible**.

La solution évidente : relier chaque ville à ses voisines directement.
Mais il existe une solution encore meilleure : **ajouter des carrefours intermédiaires** qui ne correspondent à aucune ville.
Ces carrefours s'appellent les **points de Steiner**.

Ce projet est une application web interactive qui :
- Te permet de placer des points sur une carte
- Calcule automatiquement l'arbre de Steiner optimal
- Affiche les points de Steiner en violet et les routes en vert
- Explique l'algorithme utilisé de façon pédagogique

---

## Démonstration rapide

```
Tu places 4 points en forme de carré :

  P1 ─────── P2
  │           │
  │           │    ← Connexion directe (MST) : longueur = 3 unités
  │           │
  P4 ─────── P3

Mais avec 2 points de Steiner S1 et S2 :

  P1 ─── S1 ─── P2
          │           ← Arbre de Steiner : longueur = 2.73 unités
  P4 ─── S2 ─── P3

C'est 9% plus court !
```

---

## Le problème mathématique

### Pourquoi les angles valent toujours 120° ?

Dans un arbre de Steiner optimal, **les routes se rejoignent toujours à exactement 120°** aux points de Steiner.

Pourquoi ? Parce que c'est la condition d'équilibre des forces : si tu imagines des fils élastiques tirés vers chaque ville, le point d'équilibre (où la somme des tensions est nulle) est précisément le point de Steiner.

```
       Ville A
        /
       / ← angle 120°
      S
       \ ← angle 120°
        \
       Ville B

(et 120° vers la Ville C aussi)
```

C'est exactement ce que font les **bulles de savon** : elles trouvent naturellement ce point d'équilibre minimal par la physique !

---

## L'algorithme — Comment ça marche ?

### Cas 2 points

C'est simple : une ligne droite entre les deux points.
On ne peut pas faire mieux.

```
P1 ─────────────────── P2
```

---

### Cas 3 points — Le Point de Fermat-Torricelli

Pour 3 points, il peut y avoir **0 ou 1 point de Steiner**.

**Règle :** Si un angle du triangle formé par les 3 points est **supérieur ou égal à 120°**, alors ce sommet est déjà le point optimal — pas besoin de Steiner.

Sinon, on calcule le **Point de Fermat** : le point intérieur au triangle d'où les 3 villes sont vues à exactement 120° chacune.

**Comment le calculer ? — Algorithme de Weiszfeld (1937)**

C'est un algorithme itératif très simple :

```
1. Commence au centroïde des 3 points (leur "moyenne")
2. À chaque étape, calcule :

           P1/d(F,P1)  +  P2/d(F,P2)  +  P3/d(F,P3)
   F_new = ─────────────────────────────────────────────
              1/d(F,P1) +  1/d(F,P2)  +  1/d(F,P3)

   (d = distance, F = position courante du point)

3. Remplace F par F_new
4. Répète jusqu'à convergence (différence < 0.0000000001)
```

En clair : on fait une **moyenne pondérée** des 3 villes, où les villes les plus proches ont plus de poids. En répétant cela des milliers de fois, on converge vers le point parfait.

---

### Cas 4 points — La grande nouveauté du projet

C'est là où ça devient intéressant. Pour 4 points, il peut y avoir **0, 1 ou 2 points de Steiner**.

La plupart des projets similaires ne gèrent que 0 ou 1 point de Steiner — **nous gérons les 3 cas**.

#### Combien de topologies possibles ?

On doit comparer **16 configurations différentes** et garder la meilleure :

```
┌────────────────────────────────────────────────────────┐
│  0 point de Steiner  :  1 topologie                   │
│  → Arbre couvrant minimal (MST) — algorithme de Prim  │
│                                                        │
│  1 point de Steiner  : 12 topologies                  │
│  → Pour chaque groupe de 3 terminaux parmi 4 :        │
│     C(4,3) = 4 groupes × 3 connexions possibles = 12  │
│                                                        │
│  2 points de Steiner :  3 topologies                  │
│  → 3 façons de diviser {P1,P2,P3,P4} en 2 paires :   │
│     {P1,P2}|{P3,P4}  ou  {P1,P3}|{P2,P4}             │
│     ou  {P1,P4}|{P2,P3}                               │
└────────────────────────────────────────────────────────┘
```

#### Topologie à 1 point de Steiner

```
Structure :
  S = Point de Fermat de {P1, P2, P3}
  P4 se connecte directement à l'un de {P1, P2, P3}

  P1 ── S ── P2       Longueur = d(S,P1)+d(S,P2)+d(S,P3)+d(P4,P3)
        │
        P3 ── P4
```

> **Erreur classique à éviter :** connecter P4 directement au point de Steiner S.
> Cela donne un nœud de degré 4, ce qui viole la règle des 120° et n'est pas optimal.

#### Topologie à 2 points de Steiner

```
Structure :
  S1 connecté à {Pa, Pb, S2}   ← angles 120° en S1
  S2 connecté à {Pc, Pd, S1}   ← angles 120° en S2

  Pa ── S1 ── S2 ── Pc
        │           │
  Pb ───┘           └─── Pd
```

#### Comment trouver S1 et S2 ? — Weiszfeld Alterné

On étend l'algorithme de Weiszfeld pour 2 points simultanément :

```
Répéter jusqu'à convergence :

  1. Fixer S2, mettre à jour S1 :
     S1 = Weber({Pa, Pb, S2})   ← Weiszfeld avec 3 voisins

  2. Fixer S1, mettre à jour S2 :
     S2 = Weber({Pc, Pd, S1})   ← Weiszfeld avec 3 voisins
```

On fait ça 4 fois avec des points de départ différents (pour éviter les minima locaux), et on garde le meilleur résultat.

#### Validation géométrique

Après l'optimisation, on vérifie :
- Les angles en S1 et S2 sont bien ≈ 120° (tolérance ±10°)
- S1 et S2 ne sont pas confondus entre eux
- S1 et S2 ne coïncident pas avec un terminal

Si la topologie ne passe pas ces tests, elle est éliminée.

#### Résultat final

```
On compare les longueurs des 16 topologies validées.
La plus courte est retournée.

Exemple — Carré (0,0)(1,0)(1,1)(0,1) :
  MST                  : 3.000 unités
  Meilleur 1 Steiner   : 2.866 unités
  Meilleur 2 Steiner   : 2.732 unités  ← OPTIMAL (= 1 + √3)
```

---

## Architecture du projet

```
steiner-tree-solver/
│
├── frontend/                   ← Application web (Angular 17)
│   └── src/app/
│       ├── components/
│       │   ├── canvas/         ← La carte interactive (HTML Canvas)
│       │   ├── controls/       ← Boutons Résoudre / Effacer
│       │   ├── info-panel/     ← Statistiques et liste des points
│       │   ├── introduction/   ← Tutoriel interactif (5 slides)
│       │   └── activite-debranchee/  ← Page pédagogique lycée/collège
│       ├── models/             ← Types TypeScript (Point, Edge, Result)
│       └── services/           ← Appels HTTP vers le backend
│
├── backend/                    ← Serveur de calcul (Java Spring Boot)
│   └── src/main/java/.../
│       ├── controller/         ← API REST (POST /api/steiner/solve)
│       ├── service/            ← L'algorithme (SteinerTreeService.java)
│       └── model/              ← Point, Edge, SteinerResult
│
└── algorithms/                 ← Implémentation de référence (Python)
    └── steiner_4pts.py         ← Solveur exact, utilisé pour valider
```

### Comment les 3 parties communiquent

```
Utilisateur
    │
    │ clique sur le canvas
    ▼
Frontend Angular (port 4200)
    │
    │ POST /api/steiner/solve
    │ Body: [{x:228,y:171}, {x:575,y:171}, ...]
    ▼
Backend Java (port 8080)
    │
    │ Calcule les 16 topologies
    │ Retourne la meilleure
    ▼
Réponse JSON :
{
  "steinerPoints": [{"x":350,"y":220}, {"x":350,"y":330}],
  "edges": [{"start":{...}, "end":{...}}, ...],
  "totalLength": 584.3
}
    │
    ▼
Frontend affiche :
  • Points bleus  = terminaux (P1, P2, P3, P4)
  • Points violets = Steiner (S1, S2)
  • Lignes vertes  = arêtes
```

---

## Installation et lancement

### Prérequis

| Outil | Version | Utilisation |
|-------|---------|-------------|
| Java JDK | 17 ou 21 | Backend Spring Boot |
| Maven | 3.8+ | Build Java |
| Node.js | 18+ | Frontend Angular |
| npm | 9+ | Gestionnaire de paquets |
| Python | 3.10+ (optionnel) | Solveur de référence |

### Lancement du backend (Java)

```bash
cd steiner-tree-solver/backend

# Lancer le serveur
mvn spring-boot:run

# Le serveur démarre sur http://localhost:8080
# Test : http://localhost:8080/api/steiner/health
```

### Lancement du frontend (Angular)

```bash
cd steiner-tree-solver/frontend

# Installer les dépendances (première fois uniquement)
npm install

# Lancer l'application
npm start

# Ouvrir dans le navigateur : http://localhost:4200
```

### Lancement du solveur Python (optionnel)

```bash
# Installer les dépendances
pip install numpy scipy matplotlib

# Lancer les exemples
python algorithms/steiner_4pts.py
```

---

## Utilisation de l'application

### 1. Placer des points

Clique sur la zone noire pour ajouter des villes (points bleus).
Tu peux en mettre de 2 à autant que tu veux.

### 2. Résoudre

Clique sur le bouton **"Résoudre"** dans le panneau de droite.
Le backend calcule l'arbre de Steiner optimal et le frontend l'affiche.

### 3. Lire le résultat

```
Points bleus  (P1, P2...)  = tes villes (terminaux)
Points violets (S1, S2...) = points de Steiner ajoutés par l'algorithme
Lignes vertes              = les routes de la solution
```

Le panneau de droite affiche la longueur totale et les coordonnées de chaque point.

### 4. Tutoriel

Clique sur **"How it works"** dans le header pour ouvrir le tutoriel interactif en 5 étapes.

### 5. Activité débranchée

Clique sur **"Activité"** dans le header pour accéder à la page pédagogique : une carte interactive d'une ville avec un café, une école et un supermarché, pour comprendre intuitivement le point de Steiner.

---

## L'API REST

Le backend expose un seul endpoint :

### POST `/api/steiner/solve`

**Corps de la requête (JSON) :**
```json
[
  {"x": 100, "y": 200},
  {"x": 400, "y": 150},
  {"x": 350, "y": 450},
  {"x": 120, "y": 420}
]
```

**Réponse (JSON) :**
```json
{
  "terminalPoints": [
    {"x": 100, "y": 200},
    {"x": 400, "y": 150},
    {"x": 350, "y": 450},
    {"x": 120, "y": 420}
  ],
  "steinerPoints": [
    {"x": 253.4, "y": 274.1},
    {"x": 301.8, "y": 362.5}
  ],
  "edges": [
    {"start": {"x": 253.4, "y": 274.1}, "end": {"x": 100, "y": 200}},
    {"start": {"x": 253.4, "y": 274.1}, "end": {"x": 400, "y": 150}},
    {"start": {"x": 253.4, "y": 274.1}, "end": {"x": 301.8, "y": 362.5}},
    {"start": {"x": 301.8, "y": 362.5}, "end": {"x": 350, "y": 450}},
    {"start": {"x": 301.8, "y": 362.5}, "end": {"x": 120, "y": 420}}
  ],
  "totalLength": 684.27
}
```

### GET `/api/steiner/health`

Vérifie que le serveur est en marche.

---

## Le solveur Python — À quoi il sert ?

Le fichier `algorithms/steiner_4pts.py` est une **implémentation de référence** écrite en Python.

Il sert à :
1. **Valider le backend Java** — si les deux donnent le même résultat, le Java est correct
2. **Expérimenter facilement** — Python est plus rapide à modifier pour tester des idées
3. **Documenter l'algorithme** — le code est très commenté et lisible

**Utilisation rapide :**
```python
from algorithms.steiner_4pts import steiner_tree_4_points

result = steiner_tree_4_points([(0,0), (1,0), (1,1), (0,1)])

print(result['length'])           # 2.73205...  (= 1 + √3)
print(result['topology'])         # "2 Steiner | paires (0,1) | (2,3)"
print(result['steiner_points'])   # [array([0.5, 0.289]), array([0.5, 0.711])]

for p1, p2 in result['edges']:    # Segments prêts à tracer
    print(f"{p1} → {p2}")
```

---

## Lien avec la physique — Les bulles de savon

Ce projet s'appelle "Arbre de Steiner et **bulles de savon**" pour une raison :

Les bulles de savon trouvent **naturellement** le point de Steiner par la physique.

Si tu plonges 3 fils parallèles dans une solution savonneuse, le film de savon qui se forme entre eux crée automatiquement des jonctions à exactement **120°** — c'est le même point de Steiner que notre algorithme calcule !

```
Physique                  Mathématiques
─────────────────────     ─────────────────────────────
Film de savon         ←→  Arbre de Steiner
Tension de surface    ←→  Longueur totale à minimiser
Équilibre des forces  ←→  Condition des angles à 120°
Minimum d'énergie     ←→  Solution optimale
```

**La bulle de savon résout un problème d'optimisation sans aucun calcul.**
C'est l'un des liens les plus surprenants entre physique et mathématiques.

---

## Ce que vous apprenez avec ce projet

| Concept | Domaine |
|---------|---------|
| Graphes et arbres | Mathématiques / Informatique |
| Optimisation combinatoire | Algorithmes |
| Algorithme de Prim (MST) | Algorithmique classique |
| Algorithme de Weiszfeld | Optimisation numérique |
| Point de Fermat-Torricelli | Géométrie |
| Problème NP-difficile | Complexité |
| API REST | Développement web |
| Architecture full-stack | Génie logiciel |
| Lien physique-maths | Culture scientifique |

---

## Auteurs

Projet réalisé dans le cadre du cours **Algorithmique 2** — Université.

---

## Références

- [Problème de l'arbre de Steiner — Wikipedia](https://fr.wikipedia.org/wiki/Probl%C3%A8me_de_l%27arbre_de_Steiner)
- [Point de Fermat — Wikipedia](https://fr.wikipedia.org/wiki/Point_de_Fermat)
- [Algorithme de Weiszfeld — Wikipedia](https://en.wikipedia.org/wiki/Weiszfeld%27s_algorithm)
- [Vidéos d'Olivier Druet sur les bulles de savon](https://www.youtube.com/watch?v=YsuhZ61zvLo)
