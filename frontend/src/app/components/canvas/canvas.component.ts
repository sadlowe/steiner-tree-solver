import { Component, ElementRef, ViewChild, AfterViewInit, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Point, Edge } from '../../models';

@Component({
  selector: 'app-canvas',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './canvas.component.html',
  styleUrl: './canvas.component.css'
})
export class CanvasComponent implements AfterViewInit, OnChanges {
  @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  @Input() width = 800;
  @Input() height = 600;
  @Input() terminalPoints: Point[] = [];
  @Input() steinerPoints: Point[] = [];
  @Input() edges: Edge[] = [];
  @Input() isAddingEnabled = true;

  @Output() pointAdded = new EventEmitter<Point>();

  private ctx!: CanvasRenderingContext2D;

  private readonly TERMINAL_RADIUS = 8;
  private readonly STEINER_RADIUS = 6;
  private readonly TERMINAL_COLOR = '#00bfff';
  private readonly STEINER_COLOR = '#a855f7';
  private readonly EDGE_COLOR = '#10b981';
  private readonly EDGE_WIDTH = 2;
  private readonly BACKGROUND_COLOR = '#0a0e17';
  private readonly GRID_COLOR = 'rgba(148, 163, 184, 0.08)';
  private readonly GRID_SPACING = 50;

  ngAfterViewInit(): void {
    this.initCanvas();
    this.draw();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.ctx) {
      this.draw();
    }
  }

  private initCanvas(): void {
    const canvas = this.canvasRef.nativeElement;
    canvas.width = this.width;
    canvas.height = this.height;

    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('Unable to get 2D context from canvas');
    }
    this.ctx = context;
  }

  onCanvasClick(event: MouseEvent): void {
    if (!this.isAddingEnabled) {
      return;
    }

    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();

    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;

    const x = Math.round((event.clientX - rect.left) * scaleX);
    const y = Math.round((event.clientY - rect.top) * scaleY);

    this.pointAdded.emit({ x, y });
  }

  draw(): void {
    this.clearCanvas();
    this.drawGrid();
    this.drawEdges();
    this.drawSteinerPoints();
    this.drawTerminalPoints();
  }

  private clearCanvas(): void {
    this.ctx.fillStyle = this.BACKGROUND_COLOR;
    this.ctx.fillRect(0, 0, this.width, this.height);
  }

  private drawGrid(): void {
    this.ctx.strokeStyle = this.GRID_COLOR;
    this.ctx.lineWidth = 1;

    for (let x = 0; x <= this.width; x += this.GRID_SPACING) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, this.height);
      this.ctx.stroke();
    }

    for (let y = 0; y <= this.height; y += this.GRID_SPACING) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, y);
      this.ctx.lineTo(this.width, y);
      this.ctx.stroke();
    }
  }

  private drawEdges(): void {
    for (const edge of this.edges) {
      this.ctx.strokeStyle = this.EDGE_COLOR + '40';
      this.ctx.lineWidth = this.EDGE_WIDTH + 6;
      this.ctx.lineCap = 'round';
      this.ctx.beginPath();
      this.ctx.moveTo(edge.start.x, edge.start.y);
      this.ctx.lineTo(edge.end.x, edge.end.y);
      this.ctx.stroke();

      // Draw main edge line
      this.ctx.strokeStyle = this.EDGE_COLOR;
      this.ctx.lineWidth = this.EDGE_WIDTH;
      this.ctx.beginPath();
      this.ctx.moveTo(edge.start.x, edge.start.y);
      this.ctx.lineTo(edge.end.x, edge.end.y);
      this.ctx.stroke();
    }
  }

  private drawTerminalPoints(): void {
    for (let i = 0; i < this.terminalPoints.length; i++) {
      const point = this.terminalPoints[i];
      this.drawPoint(point, this.TERMINAL_RADIUS, this.TERMINAL_COLOR, `P${i + 1}`);
    }
  }

  private drawSteinerPoints(): void {
    for (let i = 0; i < this.steinerPoints.length; i++) {
      const point = this.steinerPoints[i];
      this.drawPoint(point, this.STEINER_RADIUS, this.STEINER_COLOR, `S${i + 1}`);
    }
  }

  private drawPoint(point: Point, radius: number, color: string, label: string): void {
    this.ctx.beginPath();
    this.ctx.arc(point.x, point.y, radius + 6, 0, Math.PI * 2);
    const gradient = this.ctx.createRadialGradient(point.x, point.y, radius, point.x, point.y, radius + 10);
    gradient.addColorStop(0, color + '50');
    gradient.addColorStop(1, 'transparent');
    this.ctx.fillStyle = gradient;
    this.ctx.fill();

    this.ctx.beginPath();
    this.ctx.arc(point.x, point.y, radius, 0, Math.PI * 2);
    this.ctx.fillStyle = color;
    this.ctx.fill();

    this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
    this.ctx.lineWidth = 1.5;
    this.ctx.stroke();

    this.ctx.fillStyle = '#f1f5f9';
    this.ctx.font = '600 11px Inter, system-ui, sans-serif';
    this.ctx.textAlign = 'left';
    this.ctx.textBaseline = 'middle';
    this.ctx.fillText(label, point.x + radius + 6, point.y);

    this.ctx.font = '10px monospace';
    this.ctx.fillStyle = '#64748b';
    this.ctx.fillText(`(${point.x}, ${point.y})`, point.x + radius + 6, point.y + 13);
  }

  get cursorStyle(): string {
    return this.isAddingEnabled ? 'crosshair' : 'default';
  }
}
