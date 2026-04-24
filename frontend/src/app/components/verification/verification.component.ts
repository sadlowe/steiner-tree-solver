import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Point, Edge, SteinerResult } from '../../models';
import { SteinerService } from '../../services';
import { CanvasComponent } from '../canvas/canvas.component';

interface PointFeedback {
  index: number;
  userPoint: Point;
  nearestReal: Point | null;
  distance: number;
  isCorrect: boolean;
}

@Component({
  selector: 'app-verification',
  standalone: true,
  imports: [CommonModule, CanvasComponent],
  templateUrl: './verification.component.html',
  styleUrl: './verification.component.css'
})
export class VerificationComponent implements OnDestroy {

  placingMode: 'terminal' | 'steiner' = 'terminal';

  terminalPoints: Point[] = [];
  userSteinerPoints: Point[] = [];

  realSteinerPoints: Point[] = [];
  realEdges: Edge[] = [];

  isVerified = false;
  isLoading = false;
  errorMessage: string | null = null;

  feedback: PointFeedback[] = [];
  correctCount = 0;

  // Un point utilisateur est considéré correct si à moins de 40px du point réel
  private readonly TOLERANCE = 40;

  private destroy$ = new Subject<void>();

  constructor(private steinerService: SteinerService, private router: Router) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get canVerify(): boolean {
    return this.terminalPoints.length >= 2 && !this.isVerified && !this.isLoading;
  }

  onPointAdded(point: Point): void {
    if (this.isVerified) return;
    if (this.placingMode === 'terminal') {
      this.terminalPoints = [...this.terminalPoints, point];
    } else {
      this.userSteinerPoints = [...this.userSteinerPoints, point];
    }
  }

  onVerify(): void {
    if (!this.canVerify) return;
    this.isLoading = true;
    this.errorMessage = null;

    this.steinerService.solve(this.terminalPoints)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: SteinerResult) => {
          this.realSteinerPoints = result.steinerPoints || [];
          this.realEdges = result.edges;
          this.computeFeedback();
          this.isVerified = true;
          this.isLoading = false;
        },
        error: (error: Error) => {
          this.errorMessage = error.message;
          this.isLoading = false;
        }
      });
  }

  onReset(): void {
    this.terminalPoints = [];
    this.userSteinerPoints = [];
    this.realSteinerPoints = [];
    this.realEdges = [];
    this.feedback = [];
    this.correctCount = 0;
    this.isVerified = false;
    this.isLoading = false;
    this.errorMessage = null;
    this.placingMode = 'terminal';
  }

  removeLastPoint(): void {
    if (this.isVerified) return;
    if (this.placingMode === 'terminal' && this.terminalPoints.length > 0) {
      this.terminalPoints = this.terminalPoints.slice(0, -1);
    } else if (this.placingMode === 'steiner' && this.userSteinerPoints.length > 0) {
      this.userSteinerPoints = this.userSteinerPoints.slice(0, -1);
    }
  }

  goHome(): void {
    this.router.navigate(['/']);
  }

  get scorePercent(): number {
    if (this.realSteinerPoints.length === 0) return 100;
    return Math.round((this.correctCount / this.realSteinerPoints.length) * 100);
  }

  get scoreColor(): string {
    if (this.scorePercent >= 80) return '#10b981';
    if (this.scorePercent >= 50) return '#f59e0b';
    return '#ef4444';
  }

  private computeFeedback(): void {
    const used = new Set<number>();
    this.feedback = this.userSteinerPoints.map((userPt, i) => {
      let minDist = Infinity;
      let nearestIdx = -1;
      this.realSteinerPoints.forEach((realPt, j) => {
        if (used.has(j)) return;
        const dx = userPt.x - realPt.x, dy = userPt.y - realPt.y;
        const d = Math.sqrt(dx * dx + dy * dy);
        if (d < minDist) { minDist = d; nearestIdx = j; }
      });

      const isCorrect = nearestIdx !== -1 && minDist <= this.TOLERANCE;
      if (isCorrect) used.add(nearestIdx);

      return {
        index: i + 1,
        userPoint: userPt,
        nearestReal: nearestIdx !== -1 ? this.realSteinerPoints[nearestIdx] : null,
        distance: minDist === Infinity ? 0 : minDist,
        isCorrect
      };
    });

    this.correctCount = this.feedback.filter(f => f.isCorrect).length;
  }
}
