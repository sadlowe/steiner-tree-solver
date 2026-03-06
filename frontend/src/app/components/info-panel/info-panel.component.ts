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

  get terminalCount(): number {
    return this.terminalPoints.length;
  }

  get steinerCount(): number {
    return this.steinerPoints.length;
  }

  get edgeCount(): number {
    return this.edges.length;
  }

  get hasSolution(): boolean {
    return this.edges.length > 0 && this.totalLength !== null;
  }

  get formattedLength(): string {
    if (this.totalLength === null) {
      return '-';
    }
    return this.totalLength.toFixed(2);
  }

  calculateEdgeLength(edge: Edge): number {
    const dx = edge.end.x - edge.start.x;
    const dy = edge.end.y - edge.start.y;
    return Math.sqrt(dx * dx + dy * dy);
  }

  get mstComparison(): string {
    if (!this.hasSolution || this.terminalCount < 2) {
      return '-';
    }
    return 'Computed by algorithm';
  }
}
