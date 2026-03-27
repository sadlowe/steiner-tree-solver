import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Point, Edge, SteinerResult } from './models';
import { SteinerService } from './services';
import { CanvasComponent } from './components/canvas/canvas.component';
import { ControlsComponent } from './components/controls/controls.component';
import { InfoPanelComponent } from './components/info-panel/info-panel.component';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { IntroductionComponent } from './components/introduction/introduction.component';

export type ConnectionMode = 'naive' | 'mst' | 'steiner';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    CanvasComponent,
    ControlsComponent,
    InfoPanelComponent,
    HeaderComponent,
    FooterComponent,
    IntroductionComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  @ViewChild(IntroductionComponent) introComponent!: IntroductionComponent;

  title = 'Steiner Tree Solver';

  terminalPoints: Point[] = [];
  selectedMode: ConnectionMode = 'steiner';

  // Steiner solution (backend)
  steinerPoints: Point[] = [];
  steinerEdges: Edge[] = [];
  steinerLength: number | null = null;

  // Connexion naïve (chaîne dans l'ordre de placement)
  naiveEdges: Edge[] = [];
  naiveLength: number | null = null;

  // MST (algorithme de Prim, côté client)
  mstEdges: Edge[] = [];
  mstLength: number | null = null;

  isLoading = false;
  errorMessage: string | null = null;

  constructor(private steinerService: SteinerService) {}

  // ── Getters d'affichage selon le mode sélectionné ────────────────────────

  get edges(): Edge[] {
    switch (this.selectedMode) {
      case 'naive':   return this.naiveEdges;
      case 'mst':     return this.mstEdges;
      case 'steiner': return this.steinerEdges;
    }
  }

  get totalLength(): number | null {
    switch (this.selectedMode) {
      case 'naive':   return this.naiveLength;
      case 'mst':     return this.mstLength;
      case 'steiner': return this.steinerLength;
    }
  }

  get displaySteinerPoints(): Point[] {
    return this.selectedMode === 'steiner' ? this.steinerPoints : [];
  }

  get edgeColor(): string {
    switch (this.selectedMode) {
      case 'naive':   return '#ef4444';
      case 'mst':     return '#f59e0b';
      case 'steiner': return '#10b981';
    }
  }

  get hasSolution(): boolean {
    return this.edges.length > 0;
  }

  get hasSteinerSolution(): boolean {
    return this.steinerEdges.length > 0;
  }

  // ── Gestionnaires d'événements ────────────────────────────────────────────

  onModeChange(mode: string): void {
    this.selectedMode = mode as ConnectionMode;
  }

  onPointAdded(point: Point): void {
    this.terminalPoints = [...this.terminalPoints, point];
    // Steiner périmé dès qu'un point change
    this.steinerEdges = [];
    this.steinerPoints = [];
    this.steinerLength = null;
    // Naïve et MST recalculés immédiatement (pas de backend)
    if (this.terminalPoints.length >= 2) {
      this.computeNaive();
      this.computeMST();
    }
  }

  onSolve(): void {
    if (this.terminalPoints.length < 2) {
      this.errorMessage = 'Ajoutez au moins 2 points pour résoudre.';
      return;
    }

    if (this.selectedMode === 'naive') {
      this.computeNaive();
      return;
    }

    if (this.selectedMode === 'mst') {
      this.computeMST();
      return;
    }

    // Mode Steiner : appel backend
    this.isLoading = true;
    this.errorMessage = null;
    this.steinerService.solve(this.terminalPoints).subscribe({
      next: (result: SteinerResult) => this.handleSolveSuccess(result),
      error: (error: Error)         => this.handleSolveError(error)
    });
  }

  onClear(): void {
    this.terminalPoints = [];
    this.naiveEdges = [];  this.naiveLength = null;
    this.mstEdges = [];    this.mstLength = null;
    this.steinerEdges = []; this.steinerPoints = []; this.steinerLength = null;
    this.errorMessage = null;
  }

  onClearSolution(): void {
    // Efface uniquement la solution Steiner (naïve et MST sont auto-calculés)
    this.steinerEdges = [];
    this.steinerPoints = [];
    this.steinerLength = null;
  }

  dismissError(): void {
    this.errorMessage = null;
  }

  // ── Algorithmes côté client ───────────────────────────────────────────────

  /** Connexion naïve : chaîne dans l'ordre de placement des points. */
  private computeNaive(): void {
    const pts = this.terminalPoints;
    const edgeList: Edge[] = [];
    let total = 0;
    for (let i = 0; i < pts.length - 1; i++) {
      edgeList.push({ start: pts[i], end: pts[i + 1] });
      const dx = pts[i + 1].x - pts[i].x;
      const dy = pts[i + 1].y - pts[i].y;
      total += Math.sqrt(dx * dx + dy * dy);
    }
    this.naiveEdges  = edgeList;
    this.naiveLength = total;
  }

  /** MST par algorithme de Prim (O(n²)). */
  private computeMST(): void {
    const pts = this.terminalPoints;
    const n = pts.length;
    const inMST  = new Array<boolean>(n).fill(false);
    const minDist = new Array<number>(n).fill(Infinity);
    const parent  = new Array<number>(n).fill(-1);
    minDist[0] = 0;

    for (let count = 0; count < n; count++) {
      let u = -1;
      for (let i = 0; i < n; i++)
        if (!inMST[i] && (u === -1 || minDist[i] < minDist[u])) u = i;
      if (u === -1) break;
      inMST[u] = true;
      for (let v = 0; v < n; v++) {
        if (!inMST[v]) {
          const dx = pts[v].x - pts[u].x;
          const dy = pts[v].y - pts[u].y;
          const d = Math.sqrt(dx * dx + dy * dy);
          if (d < minDist[v]) { minDist[v] = d; parent[v] = u; }
        }
      }
    }

    const edgeList: Edge[] = [];
    let total = 0;
    for (let i = 1; i < n; i++) {
      if (parent[i] !== -1) {
        edgeList.push({ start: pts[parent[i]], end: pts[i] });
        total += minDist[i];
      }
    }
    this.mstEdges  = edgeList;
    this.mstLength = total;
  }

  private handleSolveSuccess(result: SteinerResult): void {
    this.steinerEdges  = result.edges;
    this.steinerPoints = result.steinerPoints || [];
    this.steinerLength = result.totalLength;
    this.isLoading = false;
  }

  private handleSolveError(error: Error): void {
    this.errorMessage = error.message;
    this.isLoading = false;
  }
}
