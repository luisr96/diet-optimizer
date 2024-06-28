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

    public OptimizationResponseDTO optimizeFoodSelection(int targetCalories, int caloricTolerance) {
        List<FoodItemDTO> foodItems = List.of(
                new FoodItemDTO("Apple", 52, 0.3, 14, 0.2, 0.50, 0.03, 1, 10),
                new FoodItemDTO("Banana", 89, 1.1, 23, 0.3, 0.30, 0.11, 1, 12),
                new FoodItemDTO("Chicken Breast", 165, 31, 0, 3.6, 3.00, 1.02, 70, 0),
                new FoodItemDTO("Broccoli", 34, 2.8, 7, 0.4, 0.80, 0.04, 33, 1),
                new FoodItemDTO("Almonds", 579, 21, 22, 49, 1.50, 4.59, 1, 4),
                new FoodItemDTO("Oatmeal", 68, 2.4, 12, 1.4, 0.20, 0.24, 2, 0),
                new FoodItemDTO("Eggs", 155, 13, 1.1, 11, 0.20, 3.31, 124, 0),
                new FoodItemDTO("Salmon", 208, 20, 0, 13, 10.00, 3.1, 75, 0),
                new FoodItemDTO("Spinach", 23, 2.9, 3.6, 0.4, 0.50, 0.07, 79, 0),
                new FoodItemDTO("Sweet Potato", 86, 1.6, 20, 0.1, 0.60, 0.02, 72, 6),
                new FoodItemDTO("Quinoa", 120, 4.1, 21, 1.9, 0.90, 0.23, 5, 1),
                new FoodItemDTO("Greek Yogurt", 59, 10, 3.6, 0.4, 1.20, 0.19, 36, 7),
                new FoodItemDTO("Blueberries", 57, 0.7, 14, 0.3, 0.40, 0.03, 1, 10),
                new FoodItemDTO("Brown Rice", 123, 2.6, 25, 1, 0.50, 0.14, 2, 0),
                new FoodItemDTO("Avocado", 160, 2, 9, 15, 1.50, 2.13, 7, 0),
                new FoodItemDTO("Carrots", 41, 0.9, 10, 0.2, 0.30, 0.03, 69, 5),
                new FoodItemDTO("Lentils", 116, 9, 20, 0.4, 0.80, 0.05, 2, 0),
                new FoodItemDTO("Turkey", 135, 30, 0, 1, 3.00, 0.45, 10, 0),
                new FoodItemDTO("Walnuts", 654, 15, 14, 65, 1.50, 6.13, 2, 1),
                new FoodItemDTO("Orange", 47, 0.9, 12, 0.1, 0.50, 0.02, 1, 9)
        );

        // Objective function: minimize price
        double[] costCoefficients = foodItems.stream().mapToDouble(FoodItemDTO::getPrice).toArray();
        LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(costCoefficients, 0);

        int[] macroSplit = getMacrosFromCalories(targetCalories);

        // Constraints for nutritional requirements
        Collection<LinearConstraint> constraints = new ArrayList<>();
        constraints.add(new LinearConstraint(foodItems.stream().mapToDouble(FoodItemDTO::getCalories).toArray(), Relationship.GEQ, targetCalories - caloricTolerance)); // Lower bound
        constraints.add(new LinearConstraint(foodItems.stream().mapToDouble(FoodItemDTO::getCalories).toArray(), Relationship.LEQ, targetCalories + caloricTolerance)); // Upper bound

        constraints.add(new LinearConstraint(foodItems.stream().mapToDouble(FoodItemDTO::getProtein).toArray(), Relationship.GEQ, macroSplit[0]));
        constraints.add(new LinearConstraint(foodItems.stream().mapToDouble(FoodItemDTO::getCarbs).toArray(), Relationship.GEQ, macroSplit[1]));
        constraints.add(new LinearConstraint(foodItems.stream().mapToDouble(FoodItemDTO::getFats).toArray(), Relationship.GEQ, macroSplit[2]));

        constraints.add(new LinearConstraint(foodItems.stream().mapToDouble(FoodItemDTO::getSaturatedFat).toArray(), Relationship.LEQ, targetCalories * 0.10));
        constraints.add(new LinearConstraint(foodItems.stream().mapToDouble(FoodItemDTO::getSodium).toArray(), Relationship.LEQ, MAX_SODIUM_MG));
        constraints.add(new LinearConstraint(foodItems.stream().mapToDouble(FoodItemDTO::getAddedSugars).toArray(), Relationship.LEQ, MAX_ADDED_SUGARS_G));

        // Maximum servings for each food item
        for (int i = 0; i < foodItems.size(); i++) {
            double[] servingConstraint = new double[foodItems.size()];
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
            System.out.println("No feasible solution found.");
            return new OptimizationResponseDTO(new ArrayList<>(), 0, 0, 0, 0);
        }

        return calculateTotals(solution, foodItems);
    }

    private int[] getMacrosFromCalories(int calories) {
        int proteinGrams = (int) (calories * PERCENTAGE_PROTEIN / CALORIES_PER_GRAM_PROTEIN);
        int carbsGrams = (int) (calories * PERCENTAGE_CARBS / CALORIES_PER_GRAM_CARBS);
        int fatsGrams = (int) (calories * PERCENTAGE_FATS / CALORIES_PER_GRAM_FATS);
        return new int[]{proteinGrams, carbsGrams, fatsGrams};
    }

    private OptimizationResponseDTO calculateTotals(PointValuePair solution, List<FoodItemDTO> foodItems) {
        List<OptimizationResultDTO> result = new ArrayList<>();
        double totalProtein = 0.0;
        double totalCarbs = 0.0;
        double totalFats = 0.0;
        double totalCalories = 0.0;

        if (solution != null) {
            double[] selectedQuantities = solution.getPoint();
            for (int i = 0; i < foodItems.size(); i++) {
                double servings = selectedQuantities[i];
                result.add(new OptimizationResultDTO(foodItems.get(i).getName(), servings));

                totalProtein += foodItems.get(i).getProtein() * servings;
                totalCarbs += foodItems.get(i).getCarbs() * servings;
                totalFats += foodItems.get(i).getFats() * servings;
                totalCalories += foodItems.get(i).getCalories() * servings;
            }
        }

        // Round totals to 2 decimal places
        DecimalFormat df = new DecimalFormat("#.##");
        totalProtein = Double.parseDouble(df.format(totalProtein));
        totalCarbs = Double.parseDouble(df.format(totalCarbs));
        totalFats = Double.parseDouble(df.format(totalFats));
        totalCalories = Double.parseDouble(df.format(totalCalories));

        return new OptimizationResponseDTO(result, totalProtein, totalCarbs, totalFats, totalCalories);
    }
}
