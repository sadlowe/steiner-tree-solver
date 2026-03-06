import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-controls',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './controls.component.html',
  styleUrl: './controls.component.css'
})
export class ControlsComponent {
  @Input() pointCount = 0;
  @Input() isLoading = false;
  @Input() hasSolution = false;

  @Output() solve = new EventEmitter<void>();
  @Output() clear = new EventEmitter<void>();
  @Output() clearSolution = new EventEmitter<void>();

  onSolve(): void {
    if (this.canSolve) {
      this.solve.emit();
    }
  }

  onClear(): void {
    this.clear.emit();
  }

  onClearSolution(): void {
    this.clearSolution.emit();
  }

  get canSolve(): boolean {
    return this.pointCount >= 2 && !this.isLoading;
  }

  get solveButtonText(): string {
    if (this.isLoading) {
      return 'Calcul en cours...';
    }
    return this.hasSolution ? 'Résoudre à nouveau' : 'Résoudre l\'arbre de Steiner';
  }
}
