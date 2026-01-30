import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Controls component providing action buttons for the Steiner tree solver.
 * Allows users to solve, clear, and manage the canvas state.
 */
@Component({
  selector: 'app-controls',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './controls.component.html',
  styleUrl: './controls.component.css'
})
export class ControlsComponent {
  /** Number of points currently on the canvas */
  @Input() pointCount = 0;
  /** Whether a solve operation is in progress */
  @Input() isLoading = false;
  /** Whether there is a solution currently displayed */
  @Input() hasSolution = false;

  /** Emitted when the user clicks the Solve button */
  @Output() solve = new EventEmitter<void>();
  /** Emitted when the user clicks the Clear button */
  @Output() clear = new EventEmitter<void>();
  /** Emitted when the user clicks the Clear Solution button */
  @Output() clearSolution = new EventEmitter<void>();

  /**
   * Handles the Solve button click.
   * Only emits if there are at least 2 points.
   */
  onSolve(): void {
    if (this.canSolve) {
      this.solve.emit();
    }
  }

  /**
   * Handles the Clear All button click.
   */
  onClear(): void {
    this.clear.emit();
  }

  /**
   * Handles the Clear Solution button click.
   */
  onClearSolution(): void {
    this.clearSolution.emit();
  }

  /**
   * Determines if the Solve button should be enabled.
   * Requires at least 2 points and no loading state.
   */
  get canSolve(): boolean {
    return this.pointCount >= 2 && !this.isLoading;
  }

  /**
   * Returns the appropriate text for the Solve button.
   */
  get solveButtonText(): string {
    if (this.isLoading) {
      return 'Computing...';
    }
    return this.hasSolution ? 'Solve Again' : 'Solve Steiner Tree';
  }
}
