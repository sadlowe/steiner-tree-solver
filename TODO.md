# ✅ TODO - Steiner Tree Solver

## 🔴 Priorité Haute (À faire immédiatement)

### Backend
- [ ] **Algorithme Steiner Tree pour 4+ points** - Implémenter un vrai algorithme (pas juste MST)
- [ ] **Gestion des cas limites** - Points dupliqués, colinéaires, très proches
- [ ] **Tests unitaires backend** - Couvrir tous les cas de l'algorithme
- [ ] **Validation des entrées** - Vérifier les données reçues de l'API

### Frontend
- [ ] **Suppression de points** - Permettre de supprimer des points individuels
- [ ] **Tests unitaires frontend** - Tester les composants et services

---

## 🟠 Priorité Moyenne (Sprint suivant)

### Fonctionnalités UX
- [ ] **Glisser-déposer les points** - Modifier la position des points
- [ ] **Zoom et pan** - Pour les grands ensembles de points
- [ ] **Saisie de coordonnées** - Formulaire pour entrer X, Y manuellement
- [ ] **Import/Export** - Charger/sauvegarder des configurations (JSON, CSV)

### Visualisation
- [ ] **Animation pas-à-pas** - Montrer l'algorithme étape par étape
- [ ] **Construction du point de Fermat** - Visualiser la construction géométrique
- [ ] **Comparaison MST vs Steiner** - Afficher les deux solutions

### DevOps
- [ ] **Docker** - Conteneuriser l'application
- [ ] **CI/CD** - Pipeline GitHub Actions
- [ ] **README** - Documentation complète du projet

---

## 🟢 Priorité Basse (Plus tard)

### Fonctionnalités éducatives
- [ ] **Panel d'explications** - Expliquer l'algorithme utilisé
- [ ] **Quiz interactifs** - Challenges pour les étudiants
- [ ] **Galerie d'exemples** - Configurations prédéfinies intéressantes

### Amélirations
- [ ] **Responsive mobile** - Support tablettes et téléphones
- [ ] **Documentation API** - Swagger/OpenAPI
- [ ] **Guide utilisateur** - Tutoriels pour étudiants/enseignants

---

## ✅ Terminé

- [x] Frontend Angular avec thème sombre futuriste
- [x] Composant Canvas avec dessin des points et arêtes
- [x] Composants Controls et InfoPanel
- [x] Service API pour communiquer avec le backend
- [x] Backend Spring Boot avec API REST
- [x] Solution pour 2 points (ligne droite)
- [x] Solution pour 3 points (point de Fermat)
- [x] Solution MST pour 4+ points (temporaire)
- [x] Configuration CORS pour le développement

---

## 📅 Prochaines étapes suggérées

1. **Cette semaine**: Issues #1, #3, #16 (Algorithme + Tests backend)
2. **Semaine prochaine**: Issues #4, #17 (Suppression points + Tests frontend)
3. **Dans 2 semaines**: Issues #9, #10 (Animation + Visualisation)
