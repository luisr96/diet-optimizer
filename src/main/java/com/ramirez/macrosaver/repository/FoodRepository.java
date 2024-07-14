package com.ramirez.macrosaver.repository;

import com.ramirez.macrosaver.dto.FoodItemDTO;
import com.ramirez.macrosaver.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    List<Food> findByPriceLessThanEqual(Double price);

    @Query("SELECT new com.ramirez.macrosaver.dto.FoodItemDTO(" +
            "f.name, " +
            "f.servings, " +
            "f.price / f.servings, " +
            "f.calories / f.servings, " +
            "f.protein / f.servings, " +
            "f.carbs / f.servings, " +
            "f.fats / f.servings, " +
            "f.saturatedFat / f.servings, " +
            "f.sodium / f.servings, " +
            "f.addedSugars / f.servings) " +
            "FROM Food f")
    List<FoodItemDTO> findAllNormalized();

}
