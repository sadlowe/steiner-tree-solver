import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-presentation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './presentation.component.html',
  styleUrls: ['./presentation.component.css']
})
export class PresentationComponent {
  @Output() closePres = new EventEmitter<void>();

  activeSection = 0;

  sections = [
    { label: 'Introduction',  short: 'Intro' },
    { label: '2 Points',      short: '2 pts' },
    { label: '3 Points',      short: '3 pts' },
    { label: '4 Points',      short: '4 pts' },
    { label: '5 Points',      short: '5 pts' },
    { label: '6+ Points',     short: '6+ pts' },
    { label: 'Comparaison',   short: 'Compa.' },
  ];

  goTo(i: number) { this.activeSection = i; }
  prev()  { if (this.activeSection > 0) this.activeSection--; }
  next()  { if (this.activeSection < this.sections.length - 1) this.activeSection++; }
  close() { this.closePres.emit(); }
}
