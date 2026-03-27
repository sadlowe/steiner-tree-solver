import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-introduction',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './introduction.component.html',
  styleUrl: './introduction.component.css'
})
export class IntroductionComponent {
  isVisible = true;
  currentSlide = 0;

  slides = [
    { id: 'problem',    title: 'Le problème',                      subtitle: 'Connecter des villes avec une longueur de route minimale' },
    { id: 'naive',      title: 'Approche naïve',                   subtitle: 'Connexions directes entre toutes les villes' },
    { id: 'mst',        title: 'Arbre couvrant minimal',           subtitle: 'Connexion sans cycles' },
    { id: 'steiner',    title: 'Arbre de Steiner',                 subtitle: 'Les points de Steiner réduisent la longueur totale' },
    { id: 'algo-2',     title: 'Algorithme — 2 villes',            subtitle: 'La ligne droite' },
    { id: 'algo-3',     title: 'Algorithme — 3 villes (général)',  subtitle: 'Le point de Fermat, angles < 120°' },
    { id: 'algo-3-sp',  title: 'Algorithme — 3 villes (particulier)', subtitle: 'Un angle ≥ 120°' },
    { id: 'algo-4',     title: 'Algorithme — 4 villes',            subtitle: 'Deux points de Steiner' },
  ];

  nextSlide(): void {
    if (this.currentSlide < this.slides.length - 1) {
      this.currentSlide++;
    }
  }

  prevSlide(): void {
    if (this.currentSlide > 0) {
      this.currentSlide--;
    }
  }

  goToSlide(index: number): void {
    this.currentSlide = index;
  }

  close(): void {
    this.isVisible = false;
  }

  open(): void {
    this.isVisible = true;
    this.currentSlide = 0;
  }
}
