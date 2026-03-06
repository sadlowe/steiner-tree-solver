package com.terra.numerica.steiner_tree_solver.controller;

import com.terra.numerica.steiner_tree_solver.model.Point;
import com.terra.numerica.steiner_tree_solver.model.SteinerResult;
import com.terra.numerica.steiner_tree_solver.service.SteinerTreeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/steiner")
@CrossOrigin(origins = "http://localhost:4200")
public class SteinerController {

    private final SteinerTreeService steinerTreeService;

    public SteinerController(SteinerTreeService steinerTreeService) {
        this.steinerTreeService = steinerTreeService;
    }

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

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Steiner Tree Solver API is running");
    }
}
