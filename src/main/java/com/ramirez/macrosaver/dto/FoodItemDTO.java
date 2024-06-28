package com.ramirez.macrosaver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodItemDTO {
    private String name;
    private double calories;
    private double protein;
    private double carbs;
    private double fats;
    private double price;
    private double saturatedFat;
    private double sodium;
    private double addedSugars;
}
