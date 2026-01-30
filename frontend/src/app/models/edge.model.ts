import { Point } from './point.model';

/**
 * Represents an edge (connection) between two points in the Steiner tree.
 * Each edge connects exactly two points.
 */
export interface Edge {
  /** Starting point of the edge */
  start: Point;
  /** Ending point of the edge */
  end: Point;
}

/**
 * Extended edge with computed length for display purposes.
 */
export interface DisplayEdge extends Edge {
  /** Euclidean length of the edge */
  length: number;
}
