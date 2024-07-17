package com.ramirez.macrosaver.services;

import com.ramirez.macrosaver.dto.FoodItemDTO;
import com.ramirez.macrosaver.dto.OptimizationResponseDTO;
import com.ramirez.macrosaver.dto.SelectedFoodsDTO;
import com.ramirez.macrosaver.entity.Food;
import com.ramirez.macrosaver.repository.FoodRepository;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final FoodRepository foodRepository;

    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    public List<Food> getFoodsUnderPrice(Double price) {
        return foodRepository.findByPriceLessThanEqual(price);
    }

    public OptimizationResponseDTO optimizeFoodSelection(int targetCalories, int lowerBound, int upperBound, int maxServings) {
        List<FoodItemDTO> foodItems = foodRepository.findAllAsDTO();
        List<FoodItemDTO> normalizedFoodItems = normalizePrice(foodItems);

        // Objective function: minimize price
        double[] costCoefficients = normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getPrice).toArray();
        LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(costCoefficients, 0);

        Collection<LinearConstraint> constraints = new ArrayList<>();

        // Constraints for caloric range
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(item -> item.getCalories().doubleValue()).toArray(), Relationship.GEQ, targetCalories - lowerBound));
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(item -> item.getCalories().doubleValue()).toArray(), Relationship.LEQ, targetCalories + upperBound));

        // Constraints for macronutrient distribution
        int[] macroSplit = getMacrosFromCalories(targetCalories);
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(item -> item.getProtein().doubleValue()).toArray(), Relationship.GEQ, macroSplit[0]));
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(item -> item.getCarbs().doubleValue()).toArray(), Relationship.GEQ, macroSplit[1]));
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(item -> item.getFats().doubleValue()).toArray(), Relationship.GEQ, macroSplit[2]));

        // Constraints for minimizing saturated fat, sodium, and added sugars
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getSaturatedFat).toArray(), Relationship.LEQ, targetCalories * 0.10));
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getSodium).toArray(), Relationship.LEQ, MAX_SODIUM_MG));
        constraints.add(new LinearConstraint(normalizedFoodItems.stream().mapToDouble(FoodItemDTO::getAddedSugars).toArray(), Relationship.LEQ, MAX_ADDED_SUGARS_G));

        // Constraints for maximum servings per food item
        for (int i = 0; i < normalizedFoodItems.size(); i++) {
            double[] servingConstraint = new double[normalizedFoodItems.size()];
            servingConstraint[i] = 1.0;
            constraints.add(new LinearConstraint(servingConstraint, Relationship.LEQ, maxServings));
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

    private List<FoodItemDTO> normalizePrice(List<FoodItemDTO> foodItems) {
        List<FoodItemDTO> normalizedItems = new ArrayList<>();

        for (FoodItemDTO item : foodItems) {
            if (item.getServings() != null && BigDecimal.valueOf(item.getServings()).compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal normalizedPrice = BigDecimal.valueOf(item.getPrice())
                        .divide(BigDecimal.valueOf(item.getServings()), RoundingMode.HALF_UP);

                FoodItemDTO normalizedItem = new FoodItemDTO(
                        item.getName(),
                        1,
                        normalizedPrice.doubleValue(),
                        item.getCalories(),
                        item.getProtein(),
                        item.getCarbs(),
                        item.getFats(),
                        item.getSaturatedFat(),
                        item.getSodium(),
                        item.getAddedSugars()
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

        List<SelectedFoodsDTO> result = new ArrayList<>();
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;
        BigDecimal totalFats = BigDecimal.ZERO;
        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;

        double[] selectedQuantities = solution.getPoint();
        for (int i = 0; i < foodItems.size(); i++) {
            System.out.println(foodItems.get(i).getName() + ": " + selectedQuantities[i]);
            BigDecimal servings = BigDecimal.valueOf(selectedQuantities[i]);
            result.add(new SelectedFoodsDTO(foodItems.get(i).getName(), servings.doubleValue()));

            totalProtein = totalProtein.add(BigDecimal.valueOf(foodItems.get(i).getProtein()).multiply(servings));
            totalCarbs = totalCarbs.add(BigDecimal.valueOf(foodItems.get(i).getCarbs()).multiply(servings));
            totalFats = totalFats.add(BigDecimal.valueOf(foodItems.get(i).getFats()).multiply(servings));
            totalCalories = totalCalories.add(BigDecimal.valueOf(foodItems.get(i).getCalories()).multiply(servings));
            totalPrice = totalPrice.add(BigDecimal.valueOf(foodItems.get(i).getPrice()).multiply(servings));
        }

        totalProtein = roundBigDecimal(totalProtein);
        totalCarbs = roundBigDecimal(totalCarbs);
        totalFats = roundBigDecimal(totalFats);
        totalCalories = totalProtein.multiply(BigDecimal.valueOf(4))
                .add(totalCarbs.multiply(BigDecimal.valueOf(4)))
                .add(totalFats.multiply(BigDecimal.valueOf(9)));
        totalPrice = roundBigDecimal(totalPrice);

        result.sort((o1, o2) -> Double.compare(o2.getServings(), o1.getServings()));

        return new OptimizationResponseDTO(totalProtein.doubleValue(), totalCarbs.doubleValue(), totalFats.doubleValue(), totalCalories.doubleValue(), totalPrice.doubleValue(), result);
    }

    private BigDecimal roundBigDecimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public List<Food> getAllFoods() {
        return foodRepository.findAll();
    }
}
