package com.ramirez.macrosaver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectedFoodsDTO {
    private String foodName;
    private double servings;
    private double price;
    private int calories;
    private int protein;
    private int carbs;
    private int fats;
    private double saturatedFat;
    private double sodium;
    private double addedSugars;
    private String vendor;
}
