# MacroSaver API

MacroSaver is a Spring Boot application designed to help you optimize your diet.

This API allows you to specify your target calories, and it will return a selection of food items that achieves that goal, while minimizing total cost, saturated fat, sodium, and added sugars (all the bad stuff).

To-do:
* Add more food in the database that it can select from
* Add tags like vegetarian, vegan, keto, etc. to filter for
  
## API Documentation

### Get optimized diet

`GET /api/v1/foods/optimize`

#### Parameters

- `calories` The target caloric intake. This parameter is required.
- `lowerBound` (optional): The lower bound for the target caloric intake. Defaults to `0`.
- `upperBound` (optional): The upper bound for the target caloric intake. Defaults to `0`.
- `maxServings` (optional): The maximum number of servings of something you're willing to eat. Defaults to `1`.

#### Example Requests

```http
GET https://macro-saver.onrender.com/api/v1/foods/optimize?calories=2300

{
    "dailyProtein": 172,
    "dailyCarbs": 209.22,
    "dailyFats": 96.41,
    "dailyCalories": 2392.57,
    "dailyPrice": 7.89,
    "selectedFoods": [
        {
            "foodName": "All Natural* 80% Lean/20% Fat Ground Beef Chuck, 2.25 lb Tray",
            "servings": 1
        },
        {
            "foodName": "Freshness Guaranteed Fresh Chicken Drumsticks, 5 lb Bag",
            "servings": 1
        },
        {
            "foodName": "Freshness Guaranteed Boneless Skinless Chicken Thighs, 2.75 - 4.0 lb Tray",
            "servings": 1
        },
        {
            "foodName": "Great Value Large White Eggs, 12 Count",
            "servings": 1
        },
        {
            "foodName": "Great Value Oats & Honey Granola, 11 oz",
            "servings": 1
        },
```
```http
GET https://macro-saver.onrender.com/api/v1/foods/optimize?calories=1100&upperBound=300&maxServings=2

{
    "dailyProtein": 82,
    "dailyCarbs": 123.09,
    "dailyFats": 44.92,
    "dailyCalories": 1224.64,
    "dailyPrice": 1.86,
    "selectedFoods": [
        {
            "foodName": "Freshness Guaranteed Fresh Chicken Drumsticks, 5 lb Bag",
            "servings": 2
        },
        {
            "foodName": "Great Value Lentils, 4 lb",
            "servings": 2
        },
        {
            "foodName": "Great Value Natural Brown Long Grain Rice, 32 oz",
            "servings": 1.9999999999999998
        },
        {
            "foodName": "Fresh Hass Avocados",
            "servings": 0.8530465949820778
        },
        {
            "foodName": "Boneless, Skinless Chicken Breasts, 4.7-6.1 lb Tray",
            "servings": 0.5376344086021501
        },
```
### Get all foods

```http
GET https://macro-saver.onrender.com/api/v1/foods

[
    {
        "id": 174,
        "name": "Fresh Red Cherries, 2.25 lb Bag",
        "servings": 6,
        "price": 5.56,
        "calories": 87,
        "protein": 2,
        "carbs": 22,
        "fats": 0,
        "saturatedFat": 0.05,
        "sodium": 0,
        "addedSugars": 17.7,
        "mealType": null,
        "vendor": {
            "id": 2,
            "name": "Walmart"
        }
    },
    ...
]
```

### Get all foods under a certain price
```http
GET https://macro-saver.onrender.com/api/v1/foods/search?maxPrice=1.75

[
    {
        "id": 196,
        "name": "Great Value Natural Brown Long Grain Rice, 32 oz",
        "servings": 20,
        "price": 1.64,
        "calories": 160,
        "protein": 4,
        "carbs": 34,
        "fats": 2,
        "saturatedFat": 0,
        "sodium": 0,
        "addedSugars": 0,
        "mealType": null,
        "vendor": {
            "id": 2,
            "name": "Walmart"
        }
    },
    {
        "id": 197,
        "name": "Fresh Hass Avocados",
        "servings": 3,
        "price": 0.68,
        "calories": 240,
        "protein": 3,
        "carbs": 13,
        "fats": 22,
        "saturatedFat": 3.19,
        "sodium": 10,
        "addedSugars": 1,
        "mealType": null,
        "vendor": {
            "id": 2,
            "name": "Walmart"
        }
    }
]
```

#### Notes

Algorithm used is Simplex for linear optimization.

It's common for resulting calories to be ~100 calories off. 

The app is opinionated to a 30/35/35 PCF split. 
