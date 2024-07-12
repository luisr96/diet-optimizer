package com.ramirez.macrosaver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimizationResponseDTO {
    private double totalProtein;
    private double totalCarbs;
    private double totalFats;
    private double totalCalories;
    private double totalPrice;
    private List<OptimizationResultDTO> foods;
}
