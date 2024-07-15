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
            "f.price, " +
            "f.calories, " +
            "f.protein, " +
            "f.carbs, " +
            "f.fats, " +
            "f.saturatedFat, " +
            "f.sodium, " +
            "f.addedSugars) " +
            "FROM Food f")
    List<FoodItemDTO> findAllAsDTO();

}
