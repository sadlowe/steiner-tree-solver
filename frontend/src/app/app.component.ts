import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Point, Edge, SteinerResult } from './models';
import { SteinerService } from './services';
import { CanvasComponent } from './components/canvas/canvas.component';
import { ControlsComponent } from './components/controls/controls.component';
import { InfoPanelComponent } from './components/info-panel/info-panel.component';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';

/**
 * Main application component for the Steiner Tree Solver.
 * Coordinates all child components and manages application state.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    CanvasComponent,
    ControlsComponent,
    InfoPanelComponent,
    HeaderComponent,
    FooterComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  /** Application title */
  title = 'Steiner Tree Solver';

  /** Array of user-placed terminal points (cities) */
  terminalPoints: Point[] = [];

  /** Array of computed Steiner points */
  steinerPoints: Point[] = [];

  /** Array of edges forming the Steiner tree solution */
  edges: Edge[] = [];

  /** Total length of the Steiner tree (sum of all edge lengths) */
  totalLength: number | null = null;

  /** Whether an API call is in progress */
  isLoading = false;

  /** Error message to display (null if no error) */
  errorMessage: string | null = null;

  constructor(private steinerService: SteinerService) {}

  /**
   * Checks if there is currently a solution displayed.
   */
  get hasSolution(): boolean {
    return this.edges.length > 0;
  }

  /**
   * Handles adding a new point when the user clicks on the canvas.
   * @param point The point coordinates from the canvas click
   */
  onPointAdded(point: Point): void {
    this.terminalPoints = [...this.terminalPoints, point];
    // Clear any existing solution when new points are added
    this.clearSolutionData();
  }

  /**
   * Sends the current points to the backend to compute the Steiner tree.
   */
  onSolve(): void {
    if (this.terminalPoints.length < 2) {
      this.errorMessage = 'Please add at least 2 points to solve.';
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

  /**
   * Handles successful API response.
   * @param result The Steiner tree solution from the backend
   */
  private handleSolveSuccess(result: SteinerResult): void {
    this.edges = result.edges;
    this.steinerPoints = result.steinerPoints || [];
    this.totalLength = result.totalLength;
    this.isLoading = false;
  }

  /**
   * Handles API error response.
   * @param error The error from the API call
   */
  private handleSolveError(error: Error): void {
    this.errorMessage = error.message;
    this.isLoading = false;
  }

  /**
   * Clears all points and the solution.
   */
  onClear(): void {
    this.terminalPoints = [];
    this.clearSolutionData();
    this.errorMessage = null;
  }

  /**
   * Clears only the solution, keeping the terminal points.
   */
  onClearSolution(): void {
    this.clearSolutionData();
  }

  /**
   * Clears the solution data (edges, Steiner points, total length).
   */
  private clearSolutionData(): void {
    this.edges = [];
    this.steinerPoints = [];
    this.totalLength = null;
  }

  /**
   * Dismisses the current error message.
   */
  dismissError(): void {
    this.errorMessage = null;
  }
}
