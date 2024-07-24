package com.ramirez.macrosaver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodItemDTO {
    private String name;
    private Integer servings;

    private Double price;

    private Integer calories;
    private Integer protein;
    private Integer carbs;
    private Integer fats;

    private Double saturatedFat;
    private Double sodium;
    private Double addedSugars;

    private String vendor;
}
