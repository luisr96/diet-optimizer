package com.ramirez.macrosaver.repository;

import com.ramirez.macrosaver.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    List<Food> findByPriceLessThanEqual(Double price);
    List<Food> findByCaloriesLessThanEqualAndProteinGreaterThanEqualAndCarbsGreaterThanEqualAndFatsGreaterThanEqual(
            Integer calories, Integer protein, Integer carbs, Integer fats);
}
