import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Destination {
  x: number;
  y: number;
  label: string;
  icon: string;
  color: string;
}

interface Residence {
  id: string;
  x: number;
  y: number;
  name: string;
  isSteiner: boolean;
}

interface ResidenceInfo extends Residence {
  distCafe: number;
  distEcole: number;
  distIntermarche: number;
  total: number;
}

@Component({
  selector: 'app-activite-debranchee',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activite-debranchee.component.html',
  styleUrl: './activite-debranchee.component.css'
})
export class ActiviteDebrancheeComponent {
  isVisible = false;
  selectedId: string | null = null;

  readonly cafe: Destination    = { x: 130, y: 100, label: 'Cafe',        icon: 'C', color: '#f59e0b' };
  readonly ecole: Destination   = { x: 560, y: 90,  label: 'Ecole',       icon: 'E', color: '#3b82f6' };
  readonly marche: Destination  = { x: 350, y: 440, label: 'Intermarche', icon: 'I', color: '#10b981' };

  readonly residences: Residence[] = [
    { id: 'steiner', x: 344, y: 218, name: 'Residence Steiner',      isSteiner: true  },
    { id: 'parc',    x: 160, y: 260, name: 'Residence du Parc',      isSteiner: false },
    { id: 'arts',    x: 540, y: 250, name: 'Residence des Arts',     isSteiner: false },
    { id: 'hugo',    x: 200, y: 400, name: 'Residence Victor Hugo',  isSteiner: false },
    { id: 'gare',    x: 490, y: 375, name: 'Residence de la Gare',   isSteiner: false },
    { id: 'centre',  x: 344, y: 130, name: 'Residence du Centre',    isSteiner: false },
  ];

  private dist(a: { x: number; y: number }, b: { x: number; y: number }): number {
    return Math.round(Math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2));
  }

  get enriched(): ResidenceInfo[] {
    return this.residences.map(r => {
      const dC = this.dist(r, this.cafe);
      const dE = this.dist(r, this.ecole);
      const dM = this.dist(r, this.marche);
      return { ...r, distCafe: dC, distEcole: dE, distIntermarche: dM, total: dC + dE + dM };
    });
  }

  get ranked(): ResidenceInfo[] {
    return [...this.enriched].sort((a, b) => a.total - b.total);
  }

  get selected(): ResidenceInfo | null {
    return this.enriched.find(r => r.id === this.selectedId) ?? null;
  }

  get minTotal(): number {
    return Math.min(...this.enriched.map(r => r.total));
  }

  select(id: string): void {
    this.selectedId = this.selectedId === id ? null : id;
  }

  open(): void {
    this.isVisible = true;
    this.selectedId = null;
  }

  close(): void {
    this.isVisible = false;
    this.selectedId = null;
  }
}
