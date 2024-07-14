package com.ramirez.macrosaver.services;

import com.ramirez.macrosaver.dto.FoodItemDTO;
import com.ramirez.macrosaver.dto.OptimizationResponseDTO;
import com.ramirez.macrosaver.dto.OptimizationResultDTO;
import com.ramirez.macrosaver.entity.Food;
import com.ramirez.macrosaver.repository.FoodRepository;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class FoodService {

    private static final int CALORIES_PER_GRAM_PROTEIN = 4;
    private static final int CALORIES_PER_GRAM_CARBS = 4;
    private static final int CALORIES_PER_GRAM_FATS = 9;

    private static final double PERCENTAGE_PROTEIN = 0.30;
    private static final double PERCENTAGE_CARBS = 0.35;
    private static final double PERCENTAGE_FATS = 0.35;

    private static final int MAX_SODIUM_MG = 1500;
    private static final int MAX_ADDED_SUGARS_G = 25;

    private static final int MAX_SERVINGS = 3;

    private final FoodRepository foodRepository;

    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    public List<Food> getFoodsUnderPrice(Double price) {
        return foodRepository.findByPriceLessThanEqual(price);
    }

    public OptimizationResponseDTO optimizeFoodSelection(int targetCalories, int lowerBound, int upperBound) {
        List<FoodItemDTO> foodItems = foodRepository.findAllNormalized();
        List<FoodItemDTO> normalizedFoodItems = normalizeToOneServing(foodItems);

        // Objective function: minimize price
        double[] costCoefficients = normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getPrice).toArray();
        LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(costCoefficients, 0);

        // Constraints for nutritional requirements
        Collection<LinearConstraint> constraints = new ArrayList<>();
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getCalories).toArray(), Relationship.GEQ, targetCalories - lowerBound)); // Lower bound
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getCalories).toArray(), Relationship.LEQ, targetCalories + upperBound)); // Upper bound

        int[] macroSplit = getMacrosFromCalories(targetCalories);
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getProtein).toArray(), Relationship.GEQ, macroSplit[0]));
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getCarbs).toArray(), Relationship.GEQ, macroSplit[1]));
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getFats).toArray(), Relationship.GEQ, macroSplit[2]));

        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getSaturatedFat).toArray(), Relationship.LEQ, targetCalories * 0.10));
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getSodium).toArray(), Relationship.LEQ, MAX_SODIUM_MG));
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getAddedSugars).toArray(), Relationship.LEQ, MAX_ADDED_SUGARS_G));

        // Maximum servings for each food item
        for (int i = 0; i < normalizedFoodItems.size(); i++) {
            double[] servingConstraint = new double[normalizedFoodItems.size()];
            servingConstraint[i] = 1.0;
            constraints.add(new LinearConstraint(servingConstraint, Relationship.LEQ, MAX_SERVINGS));
        }

        // Solver configuration
        LinearConstraintSet constraintSet = new LinearConstraintSet(constraints);
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution;
        try {
            solution = solver.optimize(objectiveFunction, constraintSet, GoalType.MINIMIZE, new NonNegativeConstraint(true));
        } catch (NoFeasibleSolutionException e) {
            throw new NoFeasibleSolutionException();
        }

        return calculateTotals(solution, normalizedFoodItems);
    }

    private List<FoodItemDTO> normalizeToOneServing(List<FoodItemDTO> foodItems) {
        List<FoodItemDTO> normalizedItems = new ArrayList<>();

        for (FoodItemDTO item : foodItems) {
            if (item.getServings() != null && item.getServings() != 0) {
                FoodItemDTO normalizedItem = new FoodItemDTO(
                        item.getName(),
                        1, // Always normalize to 1 serving
                        item.getPrice() / item.getServings(),
                        item.getCalories() / item.getServings(),
                        item.getProtein() / item.getServings(),
                        item.getCarbs() / item.getServings(),
                        item.getFats() / item.getServings(),
                        item.getSaturatedFat() / item.getServings(),
                        item.getSodium() / item.getServings(),
                        item.getAddedSugars() / item.getServings()
                );
                normalizedItems.add(normalizedItem);
            }
        }

        return normalizedItems;
    }

    private int[] getMacrosFromCalories(int calories) {
        int proteinGrams = (int) (calories * PERCENTAGE_PROTEIN / CALORIES_PER_GRAM_PROTEIN);
        int carbsGrams = (int) (calories * PERCENTAGE_CARBS / CALORIES_PER_GRAM_CARBS);
        int fatsGrams = (int) (calories * PERCENTAGE_FATS / CALORIES_PER_GRAM_FATS);
        return new int[]{proteinGrams, carbsGrams, fatsGrams};
    }

    private OptimizationResponseDTO calculateTotals(PointValuePair solution, List<FoodItemDTO> foodItems) {
        if (solution == null) {
            throw new NoFeasibleSolutionException();
        }

        List<OptimizationResultDTO> result = new ArrayList<>();
        double totalProtein = 0.0;
        double totalCarbs = 0.0;
        double totalFats = 0.0;
        double totalCalories = 0.0;
        double totalPrice = 0.0;

        double[] selectedQuantities = solution.getPoint();
        for (int i = 0; i < foodItems.size(); i++) {
            double servings = selectedQuantities[i];
            result.add(new OptimizationResultDTO(foodItems.get(i).getName(), servings));

            totalProtein += foodItems.get(i).getProtein() * servings;
            totalCarbs += foodItems.get(i).getCarbs() * servings;
            totalFats += foodItems.get(i).getFats() * servings;
            totalCalories += foodItems.get(i).getCalories() * servings;
            totalPrice += foodItems.get(i).getPrice() * servings;
        }

        totalProtein = roundDouble(totalProtein);
        totalCarbs = roundDouble(totalCarbs);
        totalFats = roundDouble(totalFats);
        totalCalories = roundDouble(totalCalories);
        totalPrice = roundDouble(totalPrice);

        return new OptimizationResponseDTO(totalProtein, totalCarbs, totalFats, totalCalories, totalPrice, result);
    }

    private Double roundDouble(Double value) {
        DecimalFormat df = new DecimalFormat("#.##");
        return Double.parseDouble(df.format(value));
    }
}
