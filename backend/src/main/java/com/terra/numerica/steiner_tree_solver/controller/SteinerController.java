package com.terra.numerica.steiner_tree_solver.controller;

import com.terra.numerica.steiner_tree_solver.model.Point;
import com.terra.numerica.steiner_tree_solver.model.SteinerResult;
import com.terra.numerica.steiner_tree_solver.service.SteinerTreeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for the Steiner Tree API.
 */
@RestController
@RequestMapping("/api/steiner")
@CrossOrigin(origins = "http://localhost:4200") // Allow Angular frontend
public class SteinerController {

    private final SteinerTreeService steinerTreeService;

    public SteinerController(SteinerTreeService steinerTreeService) {
        this.steinerTreeService = steinerTreeService;
    }

    /**
     * Solves the Steiner Tree problem for the given points.
     *
     * @param points List of terminal points to connect
     * @return SteinerResult with edges, Steiner points, and total length
     */
    @PostMapping("/solve")
    public ResponseEntity<SteinerResult> solve(@RequestBody List<Point> points) {
        if (points == null || points.size() < 2) {
            return ResponseEntity.badRequest().build();
        }

        try {
            SteinerResult result = steinerTreeService.solve(points);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Steiner Tree Solver API is running");
    }
}
