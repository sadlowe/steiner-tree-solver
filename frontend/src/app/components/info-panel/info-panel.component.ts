import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Point, Edge } from '../../models';

@Component({
  selector: 'app-info-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './info-panel.component.html',
  styleUrl: './info-panel.component.css'
})
export class InfoPanelComponent {
  @Input() terminalPoints: Point[] = [];
  @Input() steinerPoints: Point[] = [];
  @Input() edges: Edge[] = [];
  @Input() totalLength: number | null = null;

  // Données de comparaison
  @Input() naiveLength: number | null = null;
  @Input() mstLength: number | null = null;
  @Input() steinerLength: number | null = null;
  @Input() selectedMode: string = 'steiner';

  get terminalCount(): number { return this.terminalPoints.length; }
  get steinerCount(): number  { return this.steinerPoints.length; }
  get edgeCount(): number     { return this.edges.length; }

  get hasSolution(): boolean {
    return this.edges.length > 0 && this.totalLength !== null;
  }

  get hasComparison(): boolean {
    return this.naiveLength !== null || this.mstLength !== null || this.steinerLength !== null;
  }

  get formattedLength(): string {
    return this.totalLength !== null ? this.totalLength.toFixed(2) : '—';
  }

  formatLen(v: number | null): string {
    return v !== null ? v.toFixed(2) : '—';
  }

  /** Pourcentage de réduction de target par rapport à reference. */
  savingsPct(target: number | null, reference: number | null): string {
    if (target === null || reference === null || reference === 0) return '';
    const pct = ((reference - target) / reference) * 100;
    if (pct <= 0.05) return '';
    return `−${pct.toFixed(0)} %`;
  }

  /** Largeur de barre proportionnelle au max (pour la visualisation). */
  barWidth(value: number | null): string {
    const max = Math.max(this.naiveLength ?? 0, this.mstLength ?? 0, this.steinerLength ?? 0);
    if (!value || max === 0) return '0%';
    return `${(value / max) * 100}%`;
  }
}
