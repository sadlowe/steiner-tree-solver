# Steiner Tree Solver

Une application web interactive pour calculer et visualiser l'**arbre de Steiner euclidien** — le réseau de longueur minimale reliant un ensemble de points dans le plan.

---

## Présentation

Le problème de l'arbre de Steiner consiste à connecter $n$ points terminaux en utilisant le moins de longueur possible, en autorisant l'ajout de nouveaux points intermédiaires appelés **points de Steiner**. Dans la solution optimale, les arêtes se rejoignent toujours à exactement **120°** en chaque point de Steiner (point de Fermat-Torricelli).

L'application permet de :
- Placer des points sur un canvas interactif
- Calculer l'arbre de Steiner optimal (2 à 5 points) ou une heuristique Steiner (6 points et plus)
- Comparer visuellement trois modes : **Naïf**, **MST**, **Steiner**
- Voir la longueur totale de chaque solution en temps réel

---

## Stack technique

| Couche | Technologie | Version |
|--------|-------------|---------|
| Frontend | Angular | 19 |
| Backend | Spring Boot (Java) | 3.5 / Java 17 |
| Build backend | Maven | 3.8+ |
| Build frontend | Angular CLI | 19 |
| Communication | REST API JSON | HTTP |

---

## Architecture

```
steiner-tree-solver/
├── backend/
│   └── src/main/java/.../
│       ├── controller/
│       │   └── SteinerController.java      # POST /api/steiner/solve
│       ├── service/
│       │   └── SteinerTreeService.java     # Algorithmes principaux
│       └── model/
│           ├── Point.java
│           ├── Edge.java
│           └── SteinerResult.java
│
├── frontend/
│   └── src/app/
│       ├── components/
│       │   ├── canvas/                     # Canvas interactif
│       │   ├── controls/                   # Boutons et modes
│       │   ├── info-panel/                 # Longueur et statistiques
│       │   ├── header/
│       │   └── footer/
│       ├── services/
│       │   └── steiner.service.ts          # Appels HTTP vers le backend
│       └── models/                         # Point, Edge, SteinerResult
│
├── deploy/
│   ├── docker-compose.yaml                 # Stack de production
│   ├── .env.example                        # Modèle de configuration
│   └── .env                               # Configuration locale (non commité)
│
└── algorithms/                             # Scripts Python de référence
    ├── steiner_4pts.py                     # Solveur exact 4 points (scipy)
    └── steiner_5pts.py                     # Solveur exact 5 points (scipy)
```

---

## Algorithmes

### 2 points
Segment direct entre les deux terminaux.

### 3 points — Point de Fermat-Torricelli
Si un angle du triangle est ≥ 120°, ce sommet est le hub optimal.
Sinon, le point de Fermat est calculé par **itérations de Weiszfeld** jusqu'à convergence.

### 4 points — Énumération exhaustive (16 topologies)

| Famille | Structure | Nb. topologies | Méthode |
|---------|-----------|----------------|---------|
| 0 Steiner | MST (Prim) | 1 | Exact |
| 1 Steiner | Fermat(triplet) + terminal isolé | 12 | Weiszfeld |
| 2 Steiner | Partition en 2 paires | 3 | Weiszfeld alterné |

### 5 points — Énumération exhaustive (~241 topologies)

| Famille | Structure | Nb. topologies | Méthode |
|---------|-----------|----------------|---------|
| 0 Steiner | MST | 1 | Prim |
| 1 Steiner | Fermat(triplet) + 2 terminaux raccrochés | ~150 | Weiszfeld |
| 2 Steiner Type I | S0–S1 directs + queue | 60 | Weiszfeld alterné |
| 2 Steiner Type II | Terminal milieu entre S0 et S1 | 15 | Fermat exact |
| 3 Steiner | Chaîne S0–S1–S2 (unique structure valide) | 15 | Weiszfeld 3 blocs |

### 6 points et plus — Heuristique Steiner
Le MST est calculé puis amélioré itérativement par insertion de points de Fermat aux angles > 120°.

---

## Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Docker | 24+ |
| Docker Compose | 2.24+ |
| make | - |

Pour le développement local sans Docker :

| Outil | Version minimale |
|-------|-----------------|
| Java | 17 |
| Maven | 3.8 |
| Node.js | 18 |
| npm | 9 |

---

## Lancement en développement

### 1. Cloner le dépôt

```bash
git clone https://github.com/hamidamediaz/steiner-tree-solver.git
cd steiner-tree-solver
```

### 2. Lancer le backend

```bash
cd backend
./mvnw spring-boot:run
```

Le serveur démarre sur **http://localhost:8080**.

### 3. Lancer le frontend

```bash
cd frontend
npm install
npm start
```

L'application est accessible sur **http://localhost:4200**.

---

## Déploiement avec Docker

### Cas 1 — Build local puis lancement (développeur)

```bash
# 1. Construire les images localement
make build

# 2. Démarrer la stack
make up
```

L'application est disponible sur **http://localhost**.

### Cas 2 — Déploiement sur un serveur distant

```bash
# 1. Cloner le dépôt sur le serveur
git clone https://github.com/hamidamediaz/steiner-tree-solver.git
cd steiner-tree-solver

# 2. Créer le fichier de configuration
make setup
# Puis éditer deploy/.env si nécessaire :
# CORS_ALLOWED_ORIGINS=https://votre-domaine.com

# 3. Se connecter au registry Docker
make login
# Saisir votre nom d'utilisateur et token GHCR quand demandé

# 4. Démarrer la stack (les images sont pullées depuis le registry)
make up
```

L'application est disponible sur **http://IP-DU-SERVEUR**.

### Référence des commandes Make

```bash
make help    # Afficher l'aide
make setup   # Créer deploy/.env depuis .env.example
make login   # Se connecter à ghcr.io
make build   # Construire les images Docker
make push    # Pousser les images vers le registry
make up      # Démarrer la stack
make down    # Arrêter la stack
make logs    # Afficher les logs en temps réel
make clean   # Arrêter et supprimer les images
```

### Configuration

La variable `CORS_ALLOWED_ORIGINS` dans `deploy/.env` contrôle les origines autorisées par le backend.
Par défaut (sans `.env`), toutes les origines sont acceptées (`*`).

| Variable | Description | Exemple |
|----------|-------------|---------|
| `CORS_ALLOWED_ORIGINS` | Origine(s) autorisée(s) par le backend | `https://mon-domaine.com` |

---

## API REST

### `POST /api/steiner/solve`

Calcule l'arbre de Steiner pour une liste de points.

**Corps de la requête :**
```json
[
  { "x": 100, "y": 200 },
  { "x": 400, "y": 100 },
  { "x": 300, "y": 450 }
]
```

**Réponse :**
```json
{
  "terminalPoints": [{ "x": 100, "y": 200 }, ...],
  "steinerPoints":  [{ "x": 267, "y": 284 }],
  "edges": [
    { "start": { "x": 267, "y": 284 }, "end": { "x": 100, "y": 200 } }
  ],
  "totalLength": 523.41
}
```

| Code | Signification |
|------|--------------|
| 200 | Succès |
| 400 | Moins de 2 points fournis |
| 500 | Erreur interne |

### `GET /api/steiner/health`

Vérifie que le backend est actif.

---

## Scripts Python (référence)

Les scripts dans `algorithms/` permettent de valider les résultats du backend avec une implémentation indépendante utilisant `scipy`.

**Prérequis :**
```bash
pip install numpy scipy matplotlib
```

**Exemple :**
```python
from algorithms.steiner_4pts import steiner_tree_4_points

result = steiner_tree_4_points([(0,0), (1,0), (1,1), (0,1)])
print(result['length'])    # 2.73205...  (= 1 + √3)
print(result['topology'])  # '2 Steiner | paires (0, 1) | (2, 3)'
```

---

## Auteurs

Projet réalisé dans le cadre du cours **Algorithmes 2**.
