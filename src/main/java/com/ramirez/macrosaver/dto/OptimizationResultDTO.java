package com.ramirez.macrosaver.dto;

import lombok.Data;

@Data
public class OptimizationResultDTO {
    private String itemName;
    private double servings;

    public OptimizationResultDTO(String itemName, double amount) {
        this.itemName = itemName;
        this.servings = amount;
    }
}
