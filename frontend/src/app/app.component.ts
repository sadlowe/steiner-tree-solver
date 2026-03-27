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
  steinerPoints: Point[] = [];
  edges: Edge[] = [];
  totalLength: number | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  constructor(private steinerService: SteinerService) {}

  get hasSolution(): boolean {
    return this.edges.length > 0;
  }

  onPointAdded(point: Point): void {
    this.terminalPoints = [...this.terminalPoints, point];
    this.clearSolutionData();
  }

  onSolve(): void {
    if (this.terminalPoints.length < 2) {
      this.errorMessage = 'Ajoutez au moins 2 points pour résoudre.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    this.steinerService.solve(this.terminalPoints).subscribe({
      next: (result: SteinerResult) => {
        this.handleSolveSuccess(result);
      },
      error: (error: Error) => {
        this.handleSolveError(error);
      }
    });
  }

  private handleSolveSuccess(result: SteinerResult): void {
    this.edges = result.edges;
    this.steinerPoints = result.steinerPoints || [];
    this.totalLength = result.totalLength;
    this.isLoading = false;
  }

  private handleSolveError(error: Error): void {
    this.errorMessage = error.message;
    this.isLoading = false;
  }

  onClear(): void {
    this.terminalPoints = [];
    this.clearSolutionData();
    this.errorMessage = null;
  }

  onClearSolution(): void {
    this.clearSolutionData();
  }

  private clearSolutionData(): void {
    this.edges = [];
    this.steinerPoints = [];
    this.totalLength = null;
  }

  dismissError(): void {
    this.errorMessage = null;
  }
}
