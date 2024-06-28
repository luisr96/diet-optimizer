package com.ramirez.macrosaver.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer servings;
    private Integer calories;
    private Integer protein;
    private Integer carbs;
    private Integer fats;
    private Double price;

    @ManyToOne
    @JoinColumn(name = "meal_type_id")
    private MealType mealType;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;
}
