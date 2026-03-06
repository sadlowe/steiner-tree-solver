export interface Point {
  x: number;
  y: number;
}

export interface DisplayPoint extends Point {
  id: number;
  isSteinerPoint: boolean;
}
