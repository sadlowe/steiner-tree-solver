# 🌳 Steiner Tree Solver - Project Roadmap

## 📋 Milestones Overview

| Milestone | Description | Priority | Status |
|-----------|-------------|----------|--------|
| **M1** | Core Algorithm Implementation | 🔴 High | 🟡 In Progress |
| **M2** | UI/UX Enhancements | 🟠 Medium | ⬜ Not Started |
| **M3** | Step-by-Step Visualization | 🟠 Medium | ⬜ Not Started |
| **M4** | Educational Features | 🟢 Low | ⬜ Not Started |
| **M5** | Testing & Quality | 🔴 High | ⬜ Not Started |
| **M6** | Deployment & Documentation | 🟢 Low | ⬜ Not Started |

---

## 🎯 Milestone 1: Core Algorithm Implementation

### ✅ Completed
- [x] Basic 2-point solution (straight line)
- [x] 3-point solution with Fermat point (Weiszfeld algorithm)
- [x] Simple MST for 4+ points (Prim's algorithm)

### 📝 Issues

#### Issue #1: Implement true Steiner Tree algorithm for 4+ points
**Priority:** 🔴 High  
**Labels:** `enhancement`, `algorithm`, `backend`  
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

#### Issue #2: Optimize Fermat point computation
**Priority:** 🟡 Medium  
**Labels:** `optimization`, `algorithm`, `backend`  
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

#### Issue #3: Handle edge cases and validation
**Priority:** 🔴 High  
**Labels:** `bug`, `backend`  
**Description:**  
Add proper handling for edge cases:

**Tasks:**
- [ ] Duplicate points detection
- [ ] Collinear points handling
- [ ] Very close points (within epsilon)
- [ ] Points outside canvas bounds
- [ ] Maximum number of points limit

---

## 🎨 Milestone 2: UI/UX Enhancements

### 📝 Issues

#### Issue #4: Add point deletion functionality
**Priority:** 🔴 High  
**Labels:** `enhancement`, `frontend`, `UX`  
**Description:**  
Users should be able to delete individual points.

**Tasks:**
- [ ] Right-click context menu on points
- [ ] Delete button in point list (info panel)
- [ ] Keyboard shortcut (Delete/Backspace when point selected)
- [ ] Undo/Redo functionality

---

#### Issue #5: Add point dragging functionality
**Priority:** 🟠 Medium  
**Labels:** `enhancement`, `frontend`, `UX`  
**Description:**  
Allow users to drag points to new positions and see the solution update in real-time.

**Tasks:**
- [ ] Implement mouse drag detection on points
- [ ] Update point position during drag
- [ ] Re-compute solution on drag end (or live if performant)
- [ ] Visual feedback during drag

---

#### Issue #6: Add zoom and pan functionality
**Priority:** 🟡 Medium  
**Labels:** `enhancement`, `frontend`  
**Description:**  
For large point sets, users need to zoom and pan the canvas.

**Tasks:**
- [ ] Mouse wheel zoom (centered on cursor)
- [ ] Click and drag to pan
- [ ] Zoom controls (+/- buttons)
- [ ] Fit-to-content button
- [ ] Mini-map for navigation (optional)

---

#### Issue #7: Add coordinate input form
**Priority:** 🟡 Medium  
**Labels:** `enhancement`, `frontend`, `UX`  
**Description:**  
Allow users to input precise coordinates instead of clicking.

**Tasks:**
- [ ] Input form for X, Y coordinates
- [ ] Batch import (CSV, JSON)
- [ ] Preset examples (square, triangle, pentagon, etc.)
- [ ] Random point generator

---

#### Issue #8: Improve mobile responsiveness
**Priority:** 🟢 Low  
**Labels:** `enhancement`, `frontend`, `responsive`  
**Description:**  
Make the app fully usable on tablets and mobile devices.

**Tasks:**
- [ ] Touch events for adding points
- [ ] Responsive sidebar (collapsible)
- [ ] Touch-friendly buttons
- [ ] Pinch-to-zoom

---

## 🎬 Milestone 3: Step-by-Step Visualization

### 📝 Issues

#### Issue #9: Implement algorithm animation system
**Priority:** 🟠 Medium  
**Labels:** `enhancement`, `frontend`, `animation`  
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

#### Issue #10: Visualize Fermat point construction
**Priority:** 🟠 Medium  
**Labels:** `enhancement`, `frontend`, `educational`  
**Description:**  
Show the geometric construction of the Fermat point:
1. Draw equilateral triangles on each side
2. Connect opposite vertices
3. Intersection is the Fermat point

**Tasks:**
- [ ] Draw construction lines (dashed, different color)
- [ ] Animate step-by-step
- [ ] Show angle measurements (120°)
- [ ] Toggle visibility of construction

---

#### Issue #11: Visualize MST construction (Prim's algorithm)
**Priority:** 🟡 Medium  
**Labels:** `enhancement`, `frontend`, `educational`  
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

## 📚 Milestone 4: Educational Features

### 📝 Issues

#### Issue #12: Add algorithm explanation panel
**Priority:** 🟡 Medium  
**Labels:** `enhancement`, `frontend`, `educational`  
**Description:**  
Create a collapsible panel explaining the algorithm being used.

**Tasks:**
- [ ] Markdown/HTML content for each algorithm
- [ ] Mathematical formulas (LaTeX rendering)
- [ ] Diagrams and illustrations
- [ ] Links to resources (Wikipedia, papers)

---

#### Issue #13: Add comparison mode (MST vs Steiner)
**Priority:** 🟠 Medium  
**Labels:** `enhancement`, `frontend`, `educational`  
**Description:**  
Show side-by-side or overlay comparison of MST vs Steiner Tree.

**Tasks:**
- [ ] Compute both MST and Steiner Tree
- [ ] Display both solutions (toggle or side-by-side)
- [ ] Show length difference (percentage saved)
- [ ] Highlight Steiner points

---

#### Issue #14: Add interactive quiz/challenges
**Priority:** 🟢 Low  
**Labels:** `enhancement`, `frontend`, `gamification`  
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

#### Issue #15: Add history/examples gallery
**Priority:** 🟢 Low  
**Labels:** `enhancement`, `frontend`  
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

## 🧪 Milestone 5: Testing & Quality

### 📝 Issues

#### Issue #16: Add backend unit tests
**Priority:** 🔴 High  
**Labels:** `testing`, `backend`  
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

#### Issue #17: Add frontend unit tests
**Priority:** 🔴 High  
**Labels:** `testing`, `frontend`  
**Description:**  
Unit tests for Angular components and services.

**Tasks:**
- [ ] Canvas component tests
- [ ] Controls component tests
- [ ] Info panel component tests
- [ ] Steiner service tests (mock HTTP)
- [ ] Model tests

---

#### Issue #18: Add E2E tests
**Priority:** 🟡 Medium  
**Labels:** `testing`, `e2e`  
**Description:**  
End-to-end tests with Cypress or Playwright.

**Tasks:**
- [ ] Test point addition flow
- [ ] Test solve flow
- [ ] Test clear functionality
- [ ] Test error handling
- [ ] Visual regression tests

---

#### Issue #19: Add input validation and error handling
**Priority:** 🔴 High  
**Labels:** `bug`, `backend`, `frontend`  
**Description:**  
Robust error handling throughout the application.

**Tasks:**
- [ ] Backend: validate input JSON
- [ ] Backend: meaningful error messages
- [ ] Frontend: display backend errors
- [ ] Frontend: loading states
- [ ] Frontend: retry mechanism

---

## 🚀 Milestone 6: Deployment & Documentation

### 📝 Issues

#### Issue #20: Create Docker setup
**Priority:** 🟡 Medium  
**Labels:** `devops`, `docker`  
**Description:**  
Containerize the application for easy deployment.

**Tasks:**
- [ ] Dockerfile for backend
- [ ] Dockerfile for frontend
- [ ] docker-compose.yml
- [ ] Environment configuration
- [ ] Health checks

---

#### Issue #21: Write comprehensive README
**Priority:** 🟠 Medium  
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

#### Issue #22: Add API documentation (OpenAPI/Swagger)
**Priority:** 🟢 Low  
**Labels:** `documentation`, `backend`  
**Description:**  
Auto-generate API documentation.

**Tasks:**
- [ ] Add SpringDoc/Swagger dependency
- [ ] Annotate controller methods
- [ ] Configure Swagger UI
- [ ] Export OpenAPI spec

---

#### Issue #23: Create user guide
**Priority:** 🟢 Low  
**Labels:** `documentation`  
**Description:**  
Step-by-step guide for users (students, teachers).

**Tasks:**
- [ ] Getting started guide
- [ ] Feature tutorials
- [ ] Algorithm explanations
- [ ] FAQ section

---

#### Issue #24: Setup CI/CD pipeline
**Priority:** 🟡 Medium  
**Labels:** `devops`, `CI/CD`  
**Description:**  
Automated testing and deployment.

**Tasks:**
- [ ] GitHub Actions workflow
- [ ] Run tests on PR
- [ ] Build Docker images
- [ ] Deploy to staging (optional)
- [ ] Release automation

---

## 📊 Summary

| Category | Total Issues | High Priority | Medium Priority | Low Priority |
|----------|-------------|---------------|-----------------|--------------|
| Algorithm | 3 | 2 | 1 | 0 |
| UI/UX | 5 | 1 | 3 | 1 |
| Visualization | 3 | 0 | 2 | 1 |
| Educational | 4 | 0 | 2 | 2 |
| Testing | 4 | 2 | 1 | 1 |
| DevOps/Docs | 5 | 0 | 3 | 2 |
| **Total** | **24** | **5** | **12** | **7** |

---

## 🏃 Suggested Sprint Plan

### Sprint 1 (Week 1-2): Core Improvements
- Issue #1: True Steiner Tree for 4+ points
- Issue #3: Edge cases handling
- Issue #16: Backend unit tests
- Issue #19: Error handling

### Sprint 2 (Week 3-4): UX Essentials
- Issue #4: Point deletion
- Issue #5: Point dragging
- Issue #7: Coordinate input
- Issue #17: Frontend unit tests

### Sprint 3 (Week 5-6): Educational Features
- Issue #9: Animation system
- Issue #10: Fermat point visualization
- Issue #12: Algorithm explanations
- Issue #13: MST vs Steiner comparison

### Sprint 4 (Week 7-8): Polish & Deploy
- Issue #6: Zoom and pan
- Issue #20: Docker setup
- Issue #21: README
- Issue #24: CI/CD pipeline

---

## 🏷️ Labels Reference

| Label | Color | Description |
|-------|-------|-------------|
| `enhancement` | 🟢 Green | New feature |
| `bug` | 🔴 Red | Bug fix |
| `algorithm` | 🟣 Purple | Algorithm related |
| `frontend` | 🔵 Blue | Frontend changes |
| `backend` | 🟠 Orange | Backend changes |
| `testing` | 🟡 Yellow | Testing related |
| `documentation` | ⚪ White | Documentation |
| `devops` | 🟤 Brown | DevOps/Infrastructure |
| `educational` | 🩵 Cyan | Educational features |
| `UX` | 🩷 Pink | User experience |
