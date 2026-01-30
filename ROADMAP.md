# Steiner Tree Solver - Project Roadmap

## Milestones Overview

| ID | Milestone | Priority | Status |
|----|-----------|----------|--------|
| M1 | Core Algorithm Implementation | HIGH | IN PROGRESS |
| M2 | UI/UX Enhancements | MEDIUM | NOT STARTED |
| M3 | Step-by-Step Visualization | MEDIUM | NOT STARTED |
| M4 | Educational Features | LOW | NOT STARTED |
| M5 | Testing & Quality | HIGH | NOT STARTED |
| M6 | Deployment & Documentation | LOW | NOT STARTED |

---

## [M1] Core Algorithm Implementation

### Completed
- [x] Basic 2-point solution (straight line)
- [x] 3-point solution with Fermat point (Weiszfeld algorithm)
- [x] Simple MST for 4+ points (Prim's algorithm)

### Issues

#### #1 - Implement true Steiner Tree algorithm for 4+ points
**Priority:** HIGH  
**Labels:** `enhancement` `algorithm` `backend`  

**Description:**  
The current implementation uses MST (Minimum Spanning Tree) for 4+ points, which is NOT optimal. We need to implement a proper Steiner Tree algorithm that adds Steiner points to minimize total network length.

**Tasks:**
- [ ] Research Steiner Tree heuristics (e.g., Smith's algorithm, GeoSteiner)
- [ ] Implement recursive decomposition for 4 points
- [ ] Implement general n-point Steiner Tree approximation
- [ ] Add comparison with MST to show improvement

**Acceptance Criteria:**
- For 4 points forming a square, the algorithm should find the optimal "X" pattern with 2 Steiner points
- Total length should be less than MST in most cases

---

#### #2 - Optimize Fermat point computation
**Priority:** MEDIUM  
**Labels:** `optimization` `algorithm` `backend`  

**Description:**  
The current Weiszfeld algorithm works but could be improved with:
- Better initial guess (not just centroid)
- Adaptive step size
- Early termination criteria

**Tasks:**
- [ ] Implement geometric construction method as alternative
- [ ] Add convergence monitoring
- [ ] Handle edge cases (collinear points)

---

#### #3 - Handle edge cases and validation
**Priority:** HIGH  
**Labels:** `bug` `backend`  

**Description:**  
Add proper handling for edge cases:

**Tasks:**
- [ ] Duplicate points detection
- [ ] Collinear points handling
- [ ] Very close points (within epsilon)
- [ ] Points outside canvas bounds
- [ ] Maximum number of points limit

---

## [M2] UI/UX Enhancements

### Issues

#### #4 - Add point deletion functionality
**Priority:** HIGH  
**Labels:** `enhancement` `frontend` `UX`  

**Description:**  
Users should be able to delete individual points.

**Tasks:**
- [ ] Right-click context menu on points
- [ ] Delete button in point list (info panel)
- [ ] Keyboard shortcut (Delete/Backspace when point selected)
- [ ] Undo/Redo functionality

---

#### #5 - Add point dragging functionality
**Priority:** MEDIUM  
**Labels:** `enhancement` `frontend` `UX`  

**Description:**  
Allow users to drag points to new positions and see the solution update in real-time.

**Tasks:**
- [ ] Implement mouse drag detection on points
- [ ] Update point position during drag
- [ ] Re-compute solution on drag end (or live if performant)
- [ ] Visual feedback during drag

---

#### #6 - Add zoom and pan functionality
**Priority:** MEDIUM  
**Labels:** `enhancement` `frontend`  

**Description:**  
For large point sets, users need to zoom and pan the canvas.

**Tasks:**
- [ ] Mouse wheel zoom (centered on cursor)
- [ ] Click and drag to pan
- [ ] Zoom controls (+/- buttons)
- [ ] Fit-to-content button
- [ ] Mini-map for navigation (optional)

---

#### #7 - Add coordinate input form
**Priority:** MEDIUM  
**Labels:** `enhancement` `frontend` `UX`  

**Description:**  
Allow users to input precise coordinates instead of clicking.

**Tasks:**
- [ ] Input form for X, Y coordinates
- [ ] Batch import (CSV, JSON)
- [ ] Preset examples (square, triangle, pentagon, etc.)
- [ ] Random point generator

---

#### #8 - Improve mobile responsiveness
**Priority:** LOW  
**Labels:** `enhancement` `frontend` `responsive`  

**Description:**  
Make the app fully usable on tablets and mobile devices.

**Tasks:**
- [ ] Touch events for adding points
- [ ] Responsive sidebar (collapsible)
- [ ] Touch-friendly buttons
- [ ] Pinch-to-zoom

---

## [M3] Step-by-Step Visualization

### Issues

#### #9 - Implement algorithm animation system
**Priority:** MEDIUM  
**Labels:** `enhancement` `frontend` `animation`  

**Description:**  
Create a system to animate the algorithm step-by-step for educational purposes.

**Tasks:**
- [ ] Define animation step data structure
- [ ] Backend: return intermediate steps (not just final result)
- [ ] Frontend: animation timeline component
- [ ] Play/Pause/Step controls
- [ ] Speed slider
- [ ] Step description text

---

#### #10 - Visualize Fermat point construction
**Priority:** MEDIUM  
**Labels:** `enhancement` `frontend` `educational`  

**Description:**  
Show the geometric construction of the Fermat point:
1. Draw equilateral triangles on each side
2. Connect opposite vertices
3. Intersection is the Fermat point

**Tasks:**
- [ ] Draw construction lines (dashed, different color)
- [ ] Animate step-by-step
- [ ] Show angle measurements (120 deg)
- [ ] Toggle visibility of construction

---

#### #11 - Visualize MST construction (Prim's algorithm)
**Priority:** MEDIUM  
**Labels:** `enhancement` `frontend` `educational`  

**Description:**  
Animate the MST building process showing:
- Current frontier
- Edge being considered
- Edge being added

**Tasks:**
- [ ] Highlight current vertex
- [ ] Show candidate edges
- [ ] Animate edge addition
- [ ] Show running total length

---

## [M4] Educational Features

### Issues

#### #12 - Add algorithm explanation panel
**Priority:** MEDIUM  
**Labels:** `enhancement` `frontend` `educational`  

**Description:**  
Create a collapsible panel explaining the algorithm being used.

**Tasks:**
- [ ] Markdown/HTML content for each algorithm
- [ ] Mathematical formulas (LaTeX rendering)
- [ ] Diagrams and illustrations
- [ ] Links to resources (Wikipedia, papers)

---

#### #13 - Add comparison mode (MST vs Steiner)
**Priority:** MEDIUM  
**Labels:** `enhancement` `frontend` `educational`  

**Description:**  
Show side-by-side or overlay comparison of MST vs Steiner Tree.

**Tasks:**
- [ ] Compute both MST and Steiner Tree
- [ ] Display both solutions (toggle or side-by-side)
- [ ] Show length difference (percentage saved)
- [ ] Highlight Steiner points

---

#### #14 - Add interactive quiz/challenges
**Priority:** LOW  
**Labels:** `enhancement` `frontend` `gamification`  

**Description:**  
Create interactive challenges for students:
- "Guess where the Steiner point should be"
- "Which solution is shorter?"
- "Find the optimal configuration"

**Tasks:**
- [ ] Challenge data structure
- [ ] Scoring system
- [ ] Hints system
- [ ] Progress tracking (local storage)

---

#### #15 - Add history/examples gallery
**Priority:** LOW  
**Labels:** `enhancement` `frontend`  

**Description:**  
Showcase interesting Steiner Tree examples:
- Historical problems
- Real-world applications (road networks, circuit design)
- Famous configurations

**Tasks:**
- [ ] Curate example configurations
- [ ] Load example functionality
- [ ] Description for each example
- [ ] Thumbnail previews

---

## [M5] Testing & Quality

### Issues

#### #16 - Add backend unit tests
**Priority:** HIGH  
**Labels:** `testing` `backend`  

**Description:**  
Comprehensive unit tests for the algorithm service.

**Tasks:**
- [ ] Test 2-point case
- [ ] Test 3-point cases (all angle scenarios)
- [ ] Test 4+ point MST
- [ ] Test edge cases (duplicate, collinear)
- [ ] Performance tests (timing)
- [ ] Property-based tests

---

#### #17 - Add frontend unit tests
**Priority:** HIGH  
**Labels:** `testing` `frontend`  

**Description:**  
Unit tests for Angular components and services.

**Tasks:**
- [ ] Canvas component tests
- [ ] Controls component tests
- [ ] Info panel component tests
- [ ] Steiner service tests (mock HTTP)
- [ ] Model tests

---

#### #18 - Add E2E tests
**Priority:** MEDIUM  
**Labels:** `testing` `e2e`  

**Description:**  
End-to-end tests with Cypress or Playwright.

**Tasks:**
- [ ] Test point addition flow
- [ ] Test solve flow
- [ ] Test clear functionality
- [ ] Test error handling
- [ ] Visual regression tests

---

#### #19 - Add input validation and error handling
**Priority:** HIGH  
**Labels:** `bug` `backend` `frontend`  

**Description:**  
Robust error handling throughout the application.

**Tasks:**
- [ ] Backend: validate input JSON
- [ ] Backend: meaningful error messages
- [ ] Frontend: display backend errors
- [ ] Frontend: loading states
- [ ] Frontend: retry mechanism

---

## [M6] Deployment & Documentation

### Issues

#### #20 - Create Docker setup
**Priority:** MEDIUM  
**Labels:** `devops` `docker`  

**Description:**  
Containerize the application for easy deployment.

**Tasks:**
- [ ] Dockerfile for backend
- [ ] Dockerfile for frontend
- [ ] docker-compose.yml
- [ ] Environment configuration
- [ ] Health checks

---

#### #21 - Write comprehensive README
**Priority:** MEDIUM  
**Labels:** `documentation`  

**Description:**  
Update README with:
- Project description
- Screenshots/GIFs
- Installation instructions
- Usage guide
- Architecture overview
- Contributing guidelines

---

#### #22 - Add API documentation (OpenAPI/Swagger)
**Priority:** LOW  
**Labels:** `documentation` `backend`  

**Description:**  
Auto-generate API documentation.

**Tasks:**
- [ ] Add SpringDoc/Swagger dependency
- [ ] Annotate controller methods
- [ ] Configure Swagger UI
- [ ] Export OpenAPI spec

---

#### #23 - Create user guide
**Priority:** LOW  
**Labels:** `documentation`  

**Description:**  
Step-by-step guide for users (students, teachers).

**Tasks:**
- [ ] Getting started guide
- [ ] Feature tutorials
- [ ] Algorithm explanations
- [ ] FAQ section

---

#### #24 - Setup CI/CD pipeline
**Priority:** MEDIUM  
**Labels:** `devops` `CI/CD`  

**Description:**  
Automated testing and deployment.

**Tasks:**
- [ ] GitHub Actions workflow
- [ ] Run tests on PR
- [ ] Build Docker images
- [ ] Deploy to staging (optional)
- [ ] Release automation

---

## Summary

| Category | Total | High | Medium | Low |
|----------|-------|------|--------|-----|
| Algorithm | 3 | 2 | 1 | 0 |
| UI/UX | 5 | 1 | 3 | 1 |
| Visualization | 3 | 0 | 3 | 0 |
| Educational | 4 | 0 | 2 | 2 |
| Testing | 4 | 3 | 1 | 0 |
| DevOps/Docs | 5 | 0 | 3 | 2 |
| **Total** | **24** | **6** | **13** | **5** |

---

## Sprint Plan

### Sprint 1 (Week 1-2): Core Improvements
- #1: True Steiner Tree for 4+ points
- #3: Edge cases handling
- #16: Backend unit tests
- #19: Error handling

### Sprint 2 (Week 3-4): UX Essentials
- #4: Point deletion
- #5: Point dragging
- #7: Coordinate input
- #17: Frontend unit tests

### Sprint 3 (Week 5-6): Educational Features
- #9: Animation system
- #10: Fermat point visualization
- #12: Algorithm explanations
- #13: MST vs Steiner comparison

### Sprint 4 (Week 7-8): Polish & Deploy
- #6: Zoom and pan
- #20: Docker setup
- #21: README
- #24: CI/CD pipeline

---

## Labels Reference

| Label | Description |
|-------|-------------|
| `enhancement` | New feature or improvement |
| `bug` | Bug fix or defect |
| `algorithm` | Algorithm related changes |
| `frontend` | Frontend/Angular changes |
| `backend` | Backend/Spring Boot changes |
| `testing` | Testing related |
| `documentation` | Documentation updates |
| `devops` | DevOps/Infrastructure |
| `educational` | Educational features |
| `UX` | User experience improvements |
