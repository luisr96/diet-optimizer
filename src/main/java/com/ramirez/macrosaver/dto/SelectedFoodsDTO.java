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
}
