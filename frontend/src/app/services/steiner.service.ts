import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { Point, SteinerResult } from '../models';

/**
 * Service responsible for communicating with the Steiner Tree backend API.
 * Handles all HTTP requests related to solving the Steiner tree problem.
 */
@Injectable({
  providedIn: 'root'
})
export class SteinerService {
  /** Base URL for the backend API */
  private readonly apiUrl = 'http://localhost:8080/api/steiner';

  constructor(private http: HttpClient) {}

  /**
   * Sends a list of points to the backend and receives the computed Steiner tree.
   *
   * @param points Array of terminal points (cities) to connect
   * @returns Observable containing the Steiner tree solution
   *
   * @example
   * this.steinerService.solve([{x: 0, y: 0}, {x: 100, y: 100}])
   *   .subscribe(result => console.log(result));
   */
  solve(points: Point[]): Observable<SteinerResult> {
    return this.http.post<SteinerResult>(`${this.apiUrl}/solve`, points)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Handles HTTP errors and provides meaningful error messages.
   * @param error The HTTP error response
   * @returns Observable that emits an error with a user-friendly message
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An error occurred while computing the Steiner tree.';

    if (error.status === 0) {
      // Network error or backend not available
      errorMessage = 'Unable to connect to the backend server. Please ensure the server is running.';
    } else if (error.status === 400) {
      // Bad request - invalid input
      errorMessage = 'Invalid input: Please provide at least 2 points.';
    } else if (error.status === 500) {
      // Server error
      errorMessage = 'Server error: The computation failed. Please try again.';
    }

    console.error('Steiner API Error:', error);
    return throwError(() => new Error(errorMessage));
  }
}
