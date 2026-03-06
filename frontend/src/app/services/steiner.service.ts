import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { Point, SteinerResult } from '../models';

@Injectable({
  providedIn: 'root'
})
export class SteinerService {
  private readonly apiUrl = 'http://localhost:8080/api/steiner';

  constructor(private http: HttpClient) {}

  solve(points: Point[]): Observable<SteinerResult> {
    return this.http.post<SteinerResult>(`${this.apiUrl}/solve`, points)
      .pipe(
        catchError(this.handleError)
      );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An error occurred while computing the Steiner tree.';

    if (error.status === 0) {
      errorMessage = 'Unable to connect to the backend server. Please ensure the server is running.';
    } else if (error.status === 400) {
      errorMessage = 'Invalid input: Please provide at least 2 points.';
    } else if (error.status === 500) {
      errorMessage = 'Server error: The computation failed. Please try again.';
    }

    console.error('Steiner API Error:', error);
    return throwError(() => new Error(errorMessage));
  }
}
