# Steiner Tree Solver

> Application web interactive pour calculer et visualiser l'arbre de Steiner euclidien — le réseau de longueur minimale reliant un ensemble de points dans le plan.

---

## Démarrage rapide

```bash
git clone https://github.com/hamidamediaz/steiner-tree-solver.git
cd steiner-tree-solver
make build
make up
```

Application disponible sur **http://localhost**

---

## 1. Présentation du projet

Le problème de l'arbre de Steiner consiste à connecter N points terminaux avec la distance totale minimale, en autorisant l'ajout de points intermédiaires appelés **points de Steiner**.

L'application permet de :
- Placer des points sur un canvas interactif
- Calculer l'arbre de Steiner optimal (2 à 5 points) ou le MST (6 points et plus)
- Comparer visuellement trois modes : **Naïf**, **MST**, **Steiner**
- Voir la longueur totale de chaque solution en temps réel

**Stack technique :**

| Couche | Technologie | Version |
|--------|-------------|---------|
| Frontend | Angular | 19 |
| Backend | Spring Boot (Java) | 3.5 / Java 17 |
| Reverse proxy | Nginx | stable-alpine |
| Conteneurisation | Docker + Compose | 24+ / 2.24+ |

---

## 2. Prérequis

| Outil | Version minimale | Vérification |
|-------|-----------------|--------------|
| Docker | 24+ | `docker --version` |
| Docker Compose | 2.24+ | `docker compose version` |
| Make | - | `make --version` |

> Sur Windows, utiliser **WSL 2** avec Docker Desktop (activer l'intégration WSL dans les paramètres Docker Desktop → Resources → WSL Integration).

---

## 3. Récupération du projet

```bash
git clone https://github.com/hamidamediaz/steiner-tree-solver.git
cd steiner-tree-solver
```

---

## 4. Build

Construit les images Docker du backend et du frontend :

```bash
make build
```

Commandes équivalentes sans Make :

```bash
docker build -t steiner-tree-solver-backend:1.0.0 ./backend
docker build -t steiner-tree-solver-frontend:1.0.0 ./frontend
```

---

## 5. Lancement en production

```bash
make up
```

Commande équivalente sans Make :

```bash
docker compose -f ./deploy/docker-compose.yaml up -d
```

**Ports exposés :**

| Service | Port | URL |
|---------|------|-----|
| Frontend (Nginx) | 80 | http://localhost |
| Backend (Spring Boot) | interne uniquement | non accessible directement |

> Le backend n'est pas exposé publiquement. Toutes les requêtes `/api` passent par Nginx.

---

## 6. Configuration

Le fichier de configuration se trouve dans `deploy/.env`.  
Il n'est pas commité — créer le depuis le modèle :

```bash
make setup
# ou manuellement :
cp deploy/.env.example deploy/.env
```

| Variable | Description | Valeur par défaut |
|----------|-------------|-------------------|
| `CORS_ALLOWED_ORIGINS` | Origines autorisées par le backend | `*` (toutes) |

> Sans fichier `.env`, l'application fonctionne avec les valeurs par défaut.

---

## 7. Structure de déploiement

Fichiers nécessaires pour déployer sur un serveur :

```
deploy/
├── docker-compose.yaml   # stack de production
├── .env.example          # modèle de configuration
└── .env                  # configuration locale (à créer)
```

**Workflow pour un déploiement sur serveur distant :**

```bash
# 1. Cloner le projet
git clone https://github.com/hamidamediaz/steiner-tree-solver.git
cd steiner-tree-solver

# 2. Créer la configuration
make setup

# 3. Se connecter au registry Docker
make login

# 4. Lancer la stack (pull automatique des images)
make up
```

---

## 8. Vérification et accès à l'application

### Étape 1 — Vérifier que les conteneurs sont bien démarrés

```bash
docker compose -f ./deploy/docker-compose.yaml ps
```

Les deux services doivent afficher le statut `running` ou `healthy` :

```
NAME                STATUS
deploy-backend-1    running (healthy)
deploy-frontend-1   running
```

### Étape 2 — Voir les logs en temps réel

```bash
make logs
# ou :
docker compose -f ./deploy/docker-compose.yaml logs -f
```

> Le backend (Spring Boot) prend environ **30 secondes** à démarrer. Attendre la ligne :
> `Started SteinerTreeSolverApplication in XX seconds`

### Étape 3 — Ouvrir l'application dans le navigateur

Ouvrir n'importe quel navigateur (Chrome, Firefox, Edge…) et aller à :

```
http://localhost
```

L'interface de l'application Steiner Tree Solver s'affiche. Vous pouvez :
1. **Cliquer sur le canvas** pour placer des points
2. **Cliquer sur "Solve"** pour calculer l'arbre de Steiner
3. **Voir le résultat** dessiné sur le canvas avec la longueur totale

### Étape 4 — Vérifier que le backend répond

Dans le navigateur ou avec curl :

```
http://localhost/api/steiner/health
```

Réponse attendue dans le navigateur :

```
Steiner Tree Solver API is running
```

> Si cette page répond, le backend est bien actif et l'application est entièrement fonctionnelle.

---

## 9. Arrêt et redémarrage

```bash
# Arrêter la stack
make down

# Redémarrer
make up

# Supprimer les images
make clean
```

---

## 10. Remarques importantes

- **Port 80** doit être libre sur la machine hôte
- Le backend démarre en **~30 secondes** (JVM) — le frontend attend automatiquement qu'il soit prêt (`healthcheck`)
- **6 points et plus** : l'algorithme utilisé est le MST (Prim), pas un Steiner exact
- Les images Docker sont hébergées sur `ghcr.io/sadlowe` — un `docker login ghcr.io` est requis si les images ne sont pas construites localement

---

## Référence des commandes Make

```bash
make help     # afficher l'aide
make setup    # créer deploy/.env depuis .env.example
make login    # connexion à ghcr.io
make build    # construire les images Docker
make push     # publier les images sur le registry
make up       # démarrer la stack
make down     # arrêter la stack
make logs     # afficher les logs
make clean    # arrêter et supprimer les images
```

---

## API REST

### `POST /api/steiner/solve`

```json
// Corps de la requête
[
  { "x": 100, "y": 200 },
  { "x": 400, "y": 100 },
  { "x": 300, "y": 450 }
]

// Réponse
{
  "terminalPoints": [...],
  "steinerPoints": [...],
  "edges": [...],
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

## Structure du projet

```
steiner-tree-solver/
│
├── backend/                        # API Spring Boot (Java 17)
│   ├── src/main/java/.../
│   │   ├── controller/             # Endpoints REST
│   │   ├── service/                # Algorithmes Steiner et MST
│   │   └── model/                  # Point, Edge, SteinerResult
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                       # Application Angular 19
│   ├── src/app/
│   │   ├── components/             # Canvas, contrôles, header, footer
│   │   ├── services/               # Appels HTTP vers le backend
│   │   └── models/                 # Interfaces TypeScript
│   ├── nginx.conf                  # Config reverse proxy
│   └── Dockerfile
│
├── deploy/                         # Fichiers de déploiement production
│   ├── docker-compose.yaml
│   ├── .env.example
│   └── .env                        # À créer (non commité)
│
├── algorithms/                     # Scripts Python de référence (scipy)
│   ├── steiner_4pts.py
│   └── steiner_5pts.py
│
├── docs/                           # Documentation du projet
│   ├── Rapport_algo_2_arbre_de_steinner-2026.pdf
│   └── fiche-technique.pdf
│
├── Makefile                        # Commandes de build et déploiement
└── README.md
```

---

## Documentation

| Document | Description | Lien |
|----------|-------------|------|
| Rapport | Rapport complet du projet Algorithmes 2 | [`docs/Rapport_algo_2_arbre_de_steinner-2026.pdf`](docs/Rapport_algo_2_arbre_de_steinner-2026.pdf) |
| Fiche technique | Fiche technique de l'application | [`docs/fiche-technique.pdf`](docs/fiche-technique.pdf) |

---

## Auteurs

Projet réalisé dans le cadre du cours **Algorithmes 2**.
