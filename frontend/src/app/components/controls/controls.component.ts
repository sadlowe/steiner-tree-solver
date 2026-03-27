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
  @Input() hasSteinerSolution = false;
  @Input() selectedMode: string = 'steiner';

  @Output() solve = new EventEmitter<void>();
  @Output() clear = new EventEmitter<void>();
  @Output() clearSolution = new EventEmitter<void>();
  @Output() modeChange = new EventEmitter<string>();

  onSolve(): void {
    if (this.canSolve) this.solve.emit();
  }

  onClear(): void {
    this.clear.emit();
  }

  onClearSolution(): void {
    this.clearSolution.emit();
  }

  onModeChange(mode: string): void {
    this.modeChange.emit(mode);
  }

  get canSolve(): boolean {
    return this.pointCount >= 2 && !this.isLoading;
  }

  get solveButtonText(): string {
    if (this.isLoading) return 'Calcul en cours...';
    switch (this.selectedMode) {
      case 'naive':   return 'Afficher connexion naïve';
      case 'mst':     return 'Calculer le MST';
      case 'steiner': return this.hasSteinerSolution
                        ? 'Recalculer l\'arbre de Steiner'
                        : 'Calculer l\'arbre de Steiner';
      default:        return 'Résoudre';
    }
  }
}
