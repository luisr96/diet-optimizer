package com.ramirez.macrosaver.controllers;

import com.ramirez.macrosaver.dto.OptimizationResponseDTO;
import com.ramirez.macrosaver.entity.Food;
import com.ramirez.macrosaver.services.FoodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/foods")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping()
    public List<Food> getAllFoods() {
        return foodService.getAllFoods();
    }

    @GetMapping("/search")
    public List<Food> getFoodsUnderPrice(@RequestParam Double maxPrice) {
        return foodService.getFoodsUnderPrice(maxPrice);
    }

    @GetMapping("/optimize")
    public ResponseEntity<OptimizationResponseDTO> optimizeFood(
            @RequestParam int calories,
            @RequestParam(defaultValue = "0") int lowerBound,
            @RequestParam(defaultValue = "0") int upperBound,
            @RequestParam(defaultValue = "1") int maxServings
    ) {
        OptimizationResponseDTO result = foodService.optimizeFoodSelection(calories, lowerBound, upperBound, maxServings);
        return ResponseEntity.ok(result);
    }
}
