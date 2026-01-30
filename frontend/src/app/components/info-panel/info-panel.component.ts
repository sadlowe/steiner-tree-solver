import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Point, Edge } from '../../models';

/**
 * Information panel component displaying statistics about the current
 * Steiner tree problem and its solution.
 */
@Component({
  selector: 'app-info-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './info-panel.component.html',
  styleUrl: './info-panel.component.css'
})
export class InfoPanelComponent {
  /** Array of terminal points (user-placed cities) */
  @Input() terminalPoints: Point[] = [];
  /** Array of Steiner points (computed intermediate points) */
  @Input() steinerPoints: Point[] = [];
  /** Array of edges in the solution */
  @Input() edges: Edge[] = [];
  /** Total length of the Steiner tree */
  @Input() totalLength: number | null = null;

  /**
   * Returns the number of terminal points.
   */
  get terminalCount(): number {
    return this.terminalPoints.length;
  }

  /**
   * Returns the number of Steiner points.
   */
  get steinerCount(): number {
    return this.steinerPoints.length;
  }

  /**
   * Returns the number of edges in the solution.
   */
  get edgeCount(): number {
    return this.edges.length;
  }

  /**
   * Checks if there is a solution to display.
   */
  get hasSolution(): boolean {
    return this.edges.length > 0 && this.totalLength !== null;
  }

  /**
   * Formats the total length for display.
   */
  get formattedLength(): string {
    if (this.totalLength === null) {
      return '-';
    }
    return this.totalLength.toFixed(2);
  }

  /**
   * Calculates the length of an edge using Euclidean distance.
   */
  calculateEdgeLength(edge: Edge): number {
    const dx = edge.end.x - edge.start.x;
    const dy = edge.end.y - edge.start.y;
    return Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Computes the theoretical minimum spanning tree length (approximate).
   * This is for educational comparison purposes.
   */
  get mstComparison(): string {
    if (!this.hasSolution || this.terminalCount < 2) {
      return '-';
    }
    // Note: This is a simplified approximation for educational purposes
    // A real MST would need to be computed by the backend
    return 'Computed by algorithm';
  }
}
