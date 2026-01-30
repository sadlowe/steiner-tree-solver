import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Introduction component with educational illustrations
 * explaining the Steiner Tree problem to the public.
 */
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
      title: 'The Problem',
      subtitle: 'Connect cities with minimum road length'
    },
    {
      id: 'naive',
      title: 'Naive Solution',
      subtitle: 'Direct connections between all cities'
    },
    {
      id: 'mst',
      title: 'Better: Minimum Spanning Tree',
      subtitle: 'Connect without cycles, but still not optimal'
    },
    {
      id: 'steiner',
      title: 'Optimal: Steiner Tree',
      subtitle: 'Add junction points to minimize total length'
    },
    {
      id: 'fermat',
      title: 'The Fermat Point',
      subtitle: 'For 3 cities, the optimal junction has 120° angles'
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
