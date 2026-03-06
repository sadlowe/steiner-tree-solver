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
    {
      id: 'problem',
      title: 'Le problème',
      subtitle: 'Connecter des villes avec une longueur de route minimale'
    },
    {
      id: 'naive',
      title: 'Solution naïve',
      subtitle: 'Connexions directes entre toutes les villes'
    },
    {
      id: 'mst',
      title: 'Mieux : Arbre couvrant minimal',
      subtitle: 'Connexion sans cycles, mais pas encore optimal'
    },
    {
      id: 'steiner',
      title: 'Optimal : Arbre de Steiner',
      subtitle: 'Ajouter des points de jonction pour minimiser la longueur totale'
    },
    {
      id: 'fermat',
      title: 'Le point de Fermat',
      subtitle: 'Pour 3 villes, la jonction optimale a des angles de 120°'
    }
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
