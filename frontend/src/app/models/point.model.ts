/**
 * Represents a 2D point on the canvas.
 * Used for both user-placed points (cities) and computed Steiner points.
 */
export interface Point {
  /** X coordinate on the canvas */
  x: number;
  /** Y coordinate on the canvas */
  y: number;
}

/**
 * Extended point with additional metadata for rendering.
 * Distinguishes between user-placed points and Steiner points.
 */
export interface DisplayPoint extends Point {
  /** Unique identifier for the point */
  id: number;
  /** Whether this is a Steiner point (computed) or a terminal point (user-placed) */
  isSteinerPoint: boolean;
}
