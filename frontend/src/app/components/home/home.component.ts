import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Point, Edge, SteinerResult } from '../../models';
import { SteinerService } from '../../services';
import { CanvasComponent } from '../canvas/canvas.component';
import { ControlsComponent } from '../controls/controls.component';
import { InfoPanelComponent } from '../info-panel/info-panel.component';

export type ConnectionMode = 'naive' | 'mst' | 'steiner';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, CanvasComponent, ControlsComponent, InfoPanelComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnDestroy {

  terminalPoints: Point[] = [];
  selectedMode: ConnectionMode = 'steiner';

  steinerPoints: Point[] = [];
  steinerEdges: Edge[] = [];
  steinerLength: number | null = null;

  naiveEdges: Edge[] = [];
  naiveLength: number | null = null;

  mstEdges: Edge[] = [];
  mstLength: number | null = null;

  isLoading = false;
  errorMessage: string | null = null;

  private destroy$ = new Subject<void>();
  private errorTimeoutId: any = null;

  constructor(private steinerService: SteinerService) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.errorTimeoutId) clearTimeout(this.errorTimeoutId);
  }

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

  get hasSolution(): boolean { return this.edges.length > 0; }
  get hasSteinerSolution(): boolean { return this.steinerEdges.length > 0; }

  onModeChange(mode: string): void {
    if (!['naive', 'mst', 'steiner'].includes(mode)) return;
    this.selectedMode = mode as ConnectionMode;
    this.dismissError();
  }

  onPointAdded(point: Point): void {
    if (!point || typeof point.x !== 'number' || typeof point.y !== 'number') return;
    this.terminalPoints = [...this.terminalPoints, point];
    this.resetSteinerSolution();
    if (this.terminalPoints.length >= 2) {
      this.computeNaive();
      this.computeMST();
    }
  }

  onSolve(): void {
    if (this.terminalPoints.length < 2) {
      this.setError('Ajoutez au moins 2 points pour résoudre.');
      return;
    }
    if (this.selectedMode === 'naive') { this.computeNaive(); this.dismissError(); return; }
    if (this.selectedMode === 'mst')   { this.computeMST();   this.dismissError(); return; }

    this.isLoading = true;
    this.dismissError();
    this.steinerService.solve(this.terminalPoints)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: SteinerResult) => {
          this.steinerEdges  = result.edges;
          this.steinerPoints = result.steinerPoints || [];
          this.steinerLength = result.totalLength;
          this.isLoading = false;
        },
        error: (error: Error) => {
          this.errorMessage = error.message;
          this.isLoading = false;
        }
      });
  }

  onClear(): void {
    this.terminalPoints = [];
    this.naiveEdges = []; this.naiveLength = null;
    this.mstEdges = [];   this.mstLength = null;
    this.resetSteinerSolution();
    this.dismissError();
  }

  onClearSolution(): void { this.resetSteinerSolution(); this.dismissError(); }

  dismissError(): void {
    if (this.errorTimeoutId) { clearTimeout(this.errorTimeoutId); this.errorTimeoutId = null; }
    this.errorMessage = null;
  }

  private resetSteinerSolution(): void {
    this.steinerEdges = []; this.steinerPoints = []; this.steinerLength = null;
  }

  private setError(message: string): void {
    this.dismissError();
    this.errorMessage = message;
    this.errorTimeoutId = setTimeout(() => this.dismissError(), 5000);
  }

  private computeNaive(): void {
    const pts = this.terminalPoints;
    const edges: Edge[] = [];
    let total = 0;
    for (let i = 0; i < pts.length - 1; i++) {
      edges.push({ start: pts[i], end: pts[i + 1] });
      const dx = pts[i + 1].x - pts[i].x, dy = pts[i + 1].y - pts[i].y;
      total += Math.sqrt(dx * dx + dy * dy);
    }
    this.naiveEdges = edges; this.naiveLength = total;
  }

  private computeMST(): void {
    const pts = this.terminalPoints;
    const n = pts.length;
    const inMST = new Array<boolean>(n).fill(false);
    const minDist = new Array<number>(n).fill(Infinity);
    const parent = new Array<number>(n).fill(-1);
    minDist[0] = 0;
    for (let count = 0; count < n; count++) {
      let u = -1;
      for (let i = 0; i < n; i++)
        if (!inMST[i] && (u === -1 || minDist[i] < minDist[u])) u = i;
      if (u === -1) break;
      inMST[u] = true;
      for (let v = 0; v < n; v++) {
        if (!inMST[v]) {
          const dx = pts[v].x - pts[u].x, dy = pts[v].y - pts[u].y;
          const d = Math.sqrt(dx * dx + dy * dy);
          if (d < minDist[v]) { minDist[v] = d; parent[v] = u; }
        }
      }
    }
    const edges: Edge[] = [];
    let total = 0;
    for (let i = 1; i < n; i++) {
      if (parent[i] !== -1) { edges.push({ start: pts[parent[i]], end: pts[i] }); total += minDist[i]; }
    }
    this.mstEdges = edges; this.mstLength = total;
  }
}
