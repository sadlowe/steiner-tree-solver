import { Edge } from './edge.model';
import { Point } from './point.model';

/**
 * Result returned by the backend after computing the Steiner tree.
 * Contains all the information needed to render the solution.
 */
export interface SteinerResult {
  /** List of edges forming the Steiner tree */
  edges: Edge[];
  /** Total length of all edges in the tree (sum of Euclidean distances) */
  totalLength: number;
  /** Steiner points (additional points computed to minimize total length) */
  steinerPoints: Point[];
  /** Original terminal points (user-placed cities) */
  terminalPoints: Point[];
}

/**
 * Request payload sent to the backend API.
 */
export interface SteinerRequest {
  /** Array of points to connect with minimum total distance */
  points: Point[];
}
