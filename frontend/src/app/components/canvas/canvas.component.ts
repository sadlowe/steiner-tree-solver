import { Component, ElementRef, ViewChild, AfterViewInit, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Point, Edge } from '../../models';

/**
 * Canvas component for rendering the Steiner tree visualization.
 * Handles mouse interactions for adding points and draws the complete solution.
 */
@Component({
  selector: 'app-canvas',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './canvas.component.html',
  styleUrl: './canvas.component.css'
})
export class CanvasComponent implements AfterViewInit, OnChanges {
  /** Reference to the HTML canvas element */
  @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  /** Width of the canvas in pixels */
  @Input() width = 800;
  /** Height of the canvas in pixels */
  @Input() height = 600;
  /** Array of user-placed terminal points */
  @Input() terminalPoints: Point[] = [];
  /** Array of computed Steiner points */
  @Input() steinerPoints: Point[] = [];
  /** Array of edges forming the Steiner tree */
  @Input() edges: Edge[] = [];
  /** Whether adding points is enabled */
  @Input() isAddingEnabled = true;

  /** Emitted when a new point is added by clicking on the canvas */
  @Output() pointAdded = new EventEmitter<Point>();

  /** 2D rendering context of the canvas */
  private ctx!: CanvasRenderingContext2D;

  /** Radius of terminal points (cities) */
  private readonly TERMINAL_RADIUS = 8;
  /** Radius of Steiner points */
  private readonly STEINER_RADIUS = 6;
  /** Color for terminal points - Cyan */
  private readonly TERMINAL_COLOR = '#00bfff';
  /** Color for Steiner points - Purple */
  private readonly STEINER_COLOR = '#a855f7';
  /** Color for edges - Teal/Green */
  private readonly EDGE_COLOR = '#10b981';
  /** Width of edge lines */
  private readonly EDGE_WIDTH = 2;
  /** Background color of the canvas - Dark */
  private readonly BACKGROUND_COLOR = '#0a0e17';
  /** Grid color - Subtle */
  private readonly GRID_COLOR = 'rgba(148, 163, 184, 0.08)';
  /** Grid spacing in pixels */
  private readonly GRID_SPACING = 50;

  ngAfterViewInit(): void {
    this.initCanvas();
    this.draw();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Redraw when inputs change
    if (this.ctx) {
      this.draw();
    }
  }

  /**
   * Initializes the canvas context and sets up the drawing environment.
   */
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

  /**
   * Handles mouse click events on the canvas.
   * Converts click coordinates to canvas coordinates and emits a new point.
   */
  onCanvasClick(event: MouseEvent): void {
    if (!this.isAddingEnabled) {
      return;
    }

    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();

    // Calculate the scale factor between canvas logical size and display size
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;

    // Convert screen coordinates to canvas coordinates
    const x = Math.round((event.clientX - rect.left) * scaleX);
    const y = Math.round((event.clientY - rect.top) * scaleY);

    // Emit the new point
    this.pointAdded.emit({ x, y });
  }

  /**
   * Main drawing method. Clears the canvas and redraws all elements.
   */
  draw(): void {
    this.clearCanvas();
    this.drawGrid();
    this.drawEdges();
    this.drawSteinerPoints();
    this.drawTerminalPoints();
  }

  /**
   * Clears the entire canvas with the background color.
   */
  private clearCanvas(): void {
    this.ctx.fillStyle = this.BACKGROUND_COLOR;
    this.ctx.fillRect(0, 0, this.width, this.height);
  }

  /**
   * Draws a subtle grid to help users place points.
   */
  private drawGrid(): void {
    this.ctx.strokeStyle = this.GRID_COLOR;
    this.ctx.lineWidth = 1;

    // Draw vertical lines
    for (let x = 0; x <= this.width; x += this.GRID_SPACING) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, this.height);
      this.ctx.stroke();
    }

    // Draw horizontal lines
    for (let y = 0; y <= this.height; y += this.GRID_SPACING) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, y);
      this.ctx.lineTo(this.width, y);
      this.ctx.stroke();
    }
  }

  /**
   * Draws all edges of the Steiner tree with glow effect.
   */
  private drawEdges(): void {
    for (const edge of this.edges) {
      // Draw glow effect (wider, semi-transparent line behind)
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

  /**
   * Draws all terminal points (user-placed cities).
   */
  private drawTerminalPoints(): void {
    for (let i = 0; i < this.terminalPoints.length; i++) {
      const point = this.terminalPoints[i];
      this.drawPoint(point, this.TERMINAL_RADIUS, this.TERMINAL_COLOR, `P${i + 1}`);
    }
  }

  /**
   * Draws all Steiner points (computed intermediate points).
   */
  private drawSteinerPoints(): void {
    for (let i = 0; i < this.steinerPoints.length; i++) {
      const point = this.steinerPoints[i];
      this.drawPoint(point, this.STEINER_RADIUS, this.STEINER_COLOR, `S${i + 1}`);
    }
  }

  /**
   * Draws a single point with a label.
   * @param point The point to draw
   * @param radius The radius of the point circle
   * @param color The fill color of the point
   * @param label The label to display next to the point
   */
  private drawPoint(point: Point, radius: number, color: string, label: string): void {
    // Draw glow effect
    this.ctx.beginPath();
    this.ctx.arc(point.x, point.y, radius + 6, 0, Math.PI * 2);
    const gradient = this.ctx.createRadialGradient(point.x, point.y, radius, point.x, point.y, radius + 10);
    gradient.addColorStop(0, color + '50');
    gradient.addColorStop(1, 'transparent');
    this.ctx.fillStyle = gradient;
    this.ctx.fill();

    // Draw the circle
    this.ctx.beginPath();
    this.ctx.arc(point.x, point.y, radius, 0, Math.PI * 2);
    this.ctx.fillStyle = color;
    this.ctx.fill();

    // Draw a subtle border for better visibility
    this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
    this.ctx.lineWidth = 1.5;
    this.ctx.stroke();

    // Draw the label
    this.ctx.fillStyle = '#f1f5f9';
    this.ctx.font = '600 11px Inter, system-ui, sans-serif';
    this.ctx.textAlign = 'left';
    this.ctx.textBaseline = 'middle';
    this.ctx.fillText(label, point.x + radius + 6, point.y);

    // Draw coordinates below the label
    this.ctx.font = '10px monospace';
    this.ctx.fillStyle = '#64748b';
    this.ctx.fillText(`(${point.x}, ${point.y})`, point.x + radius + 6, point.y + 13);
  }

  /**
   * Returns the cursor style based on whether adding points is enabled.
   */
  get cursorStyle(): string {
    return this.isAddingEnabled ? 'crosshair' : 'default';
  }
}
