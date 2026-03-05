# Steiner Tree Solver - AI Coding Agent Instructions

## Project Overview
Full-stack educational application demonstrating the Steiner Tree problem - finding the shortest network connecting points by potentially adding intermediate "Steiner points". Uses Spring Boot (Java 17) backend with Angular 19 frontend.

## Architecture & Data Flow

### Component Boundaries
- **Frontend** (`frontend/`): Angular 19 standalone components, communicates via HTTP to backend
- **Backend** (`backend/`): Spring Boot 3.5.10 REST API, stateless computation service
- **Data Exchange**: JSON over HTTP (port 8080 → 4200 CORS-enabled)

### Request Flow
1. User clicks canvas → `CanvasComponent` emits `Point` → `AppComponent` adds to `terminalPoints[]`
2. User clicks "Solve" → `SteinerService.solve(points)` POSTs to `/api/steiner/solve`
3. `SteinerController` validates → `SteinerTreeService.solve()` computes algorithm
4. Returns `SteinerResult` (edges, steinerPoints, totalLength) → Frontend renders solution

### Algorithm Implementation
Located in `SteinerTreeService.java`, uses case-based approach:
- **2 points**: Direct line (trivial)
- **3 points**: Fermat point via Weiszfeld's algorithm (iterative optimization) OR star from vertex with angle ≥120°
- **4+ points**: Currently uses MST with Prim's algorithm (**NOT optimal** - see ROADMAP.md #1)

**Critical**: The 4+ point solution is a placeholder. Any changes should note this uses MST, not true Steiner Tree approximations.

## Development Workflows

### Running the Application
```powershell
# Backend (Spring Boot)
cd backend
.\mvnw spring-boot:run  # Starts on http://localhost:8080

# Frontend (Angular)
cd frontend
npm install  # First time only
npm start    # Starts on http://localhost:4200
```

### Building for Production
```powershell
# Backend
cd backend
.\mvnw clean package  # Creates JAR in target/

# Frontend
cd frontend
npm run build  # Creates dist/ output
```

### Testing
- **Backend**: `.\mvnw test` (Minimal tests exist - see TODO.md)
- **Frontend**: `npm test` (Karma/Jasmine - minimal coverage)

## Key Conventions & Patterns

### Model Synchronization
Backend and frontend share identical data structures with different languages:
- `Point`: `{x: double, y: double}` (Java) ↔ `{x: number, y: number}` (TypeScript)
- `Edge`: `{from: Point, to: Point, length: double}` (calculated in Java)
- `SteinerResult`: Contains `edges[]`, `steinerPoints[]`, `terminalPoints[]`, `totalLength`

**Critical**: Keep these models in sync. Changes to backend models require frontend interface updates.

### Standalone Components Pattern
Frontend uses Angular 19 standalone components (no NgModule):
- All components explicitly import dependencies in `@Component.imports`
- See `AppComponent` for typical pattern: imports CommonModule + child components directly
- Use `providedIn: 'root'` for services (e.g., `SteinerService`)

### State Management
**No global state library** - state flows unidirectionally:
- `AppComponent` is the single source of truth for `terminalPoints`, `steinerPoints`, `edges`
- Data flows down via `@Input()` properties to child components
- Events flow up via `@Output()` EventEmitters (e.g., `pointAdded`, `solve`, `clear`)

### Canvas Rendering
`CanvasComponent` uses native HTML5 Canvas API:
- Grid drawn first (subtle background), then edges, then points
- **Color coding**: Terminal points = cyan (`#00bfff`), Steiner points = purple (`#a855f7`), Edges = teal (`#10b981`)
- Coordinate system: Canvas native (0,0 = top-left), no transformations applied
- Redraw on every `ngOnChanges()` - entire canvas cleared and redrawn (simple but inefficient)

## Project-Specific Gotchas

### CORS Configuration
Backend hardcodes `http://localhost:4200` in two places:
1. `application.properties` (Spring global CORS)
2. `@CrossOrigin` annotation in `SteinerController.java`

**Must update both** if changing frontend port.

### Weiszfeld Algorithm
The Fermat point calculation (`computeFermatPoint()`) uses 100 iterations hardcoded:
- Starts from triangle centroid as initial guess
- No explicit convergence check (epsilon-based) - assumes 100 iterations suffice
- Known limitation: Can fail for collinear points (see TODO.md #3)

### Dark Theme Dependencies
UI uses hardcoded colors matching "futuristic dark" theme:
- Background: `#0a0e17` (near-black)
- Grid: `rgba(148, 163, 184, 0.08)` (subtle gray)
- Changing canvas colors requires updates in `CanvasComponent` constants (no CSS variables)

## Current Limitations (see ROADMAP.md & TODO.md)

**Algorithm**: 4+ points use MST (suboptimal). True Steiner Tree with recursive decomposition is P0 priority.

**Missing Features**: No point deletion, no drag-and-drop, no undo/redo, no edge case validation (duplicates, collinear points).

**Testing**: Minimal unit test coverage on both frontend and backend.

## File Locations Reference
- Algorithm logic: `backend/src/main/java/com/terra/numerica/steiner_tree_solver/service/SteinerTreeService.java`
- API endpoint: `backend/src/main/java/com/terra/numerica/steiner_tree_solver/controller/SteinerController.java`
- Models: `backend/src/main/java/com/terra/numerica/steiner_tree_solver/model/*.java` | `frontend/src/app/models/*.ts`
- Canvas rendering: `frontend/src/app/components/canvas/canvas.component.ts`
- State management: `frontend/src/app/app.component.ts`
- HTTP service: `frontend/src/app/services/steiner.service.ts`
