package com.ramirez.macrosaver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimizationResponseDTO {
    private double dailyProtein;
    private double dailyCarbs;
    private double dailyFats;
    private double dailyCalories;
    private double dailyPrice;
    private List<SelectedFoodsDTO> selectedFoods;
}
