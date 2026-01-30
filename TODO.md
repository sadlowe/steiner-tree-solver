# TODO - Steiner Tree Solver

## [P0] Critical - Immediate Action Required

### Backend
- [ ] Implement true Steiner Tree algorithm for 4+ points (not MST)
- [ ] Handle edge cases: duplicate points, collinear points, epsilon proximity
- [ ] Add comprehensive unit tests for algorithm service
- [ ] Implement input validation and meaningful error responses

### Frontend
- [ ] Add point deletion functionality (right-click, keyboard shortcut)
- [ ] Add unit tests for components and services

---

## [P1] High Priority - Next Sprint

### Features
- [ ] Point drag-and-drop with real-time solution update
- [ ] Zoom and pan controls for canvas
- [ ] Coordinate input form (manual X, Y entry)
- [ ] Import/Export configurations (JSON, CSV)

### Visualization
- [ ] Step-by-step algorithm animation system
- [ ] Fermat point geometric construction visualization
- [ ] MST vs Steiner Tree comparison mode

### DevOps
- [ ] Docker containerization (backend + frontend)
- [ ] CI/CD pipeline with GitHub Actions
- [ ] Comprehensive README documentation

---

## [P2] Medium Priority - Backlog

### Educational Features
- [ ] Algorithm explanation panel with LaTeX formulas
- [ ] Interactive quiz/challenges for students
- [ ] Example gallery with real-world applications

### Improvements
- [ ] Mobile responsive design
- [ ] API documentation (OpenAPI/Swagger)
- [ ] User guide for students and teachers

---

## [DONE] Completed

- [x] Angular frontend with dark futuristic theme
- [x] Canvas component with point/edge rendering
- [x] Controls and InfoPanel components
- [x] SteinerService for backend API communication
- [x] Spring Boot REST API with CORS configuration
- [x] 2-point solution (direct line)
- [x] 3-point solution (Fermat point via Weiszfeld algorithm)
- [x] 4+ point solution (MST via Prim's algorithm) - temporary

---

## Timeline

| Week | Focus | Issues |
|------|-------|--------|
| 1-2 | Core Algorithm + Tests | #1, #3, #16, #19 |
| 3-4 | UX Essentials | #4, #5, #7, #17 |
| 5-6 | Educational Features | #9, #10, #12, #13 |
| 7-8 | Polish & Deploy | #6, #20, #21, #24 |
