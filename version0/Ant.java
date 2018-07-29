package version0;

import bugwars.*;

public class Ant {

    private MemoryManager manager;
    private UnitController uc;

    public Ant(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        tryHarvest();
        tryMove();
        tryHarvest();
    }

    private void tryMove() {
        int maxAmount = 0;
        Location foodLoc = manager.getIdleFoodLocation();
        if (manager.food.length != 0) {
            FoodInfo bestFood = manager.food[0];

            for (FoodInfo food : manager.food) {
                if (food.food > maxAmount) {
                    maxAmount = food.food;
                    bestFood = food;
                }
            }

            if (!manager.myLocation.isEqual(bestFood.location)) {
                Direction bestDirection = manager.myLocation.directionTo(bestFood.location);
                if (uc.canMove(bestDirection)) {
                    uc.move(bestDirection);
                    return;
                }
            }
        } else if (foodLoc.x != 0 ||foodLoc.y != 0){
            manager.path.moveTo(foodLoc);
            return;
        }

        Direction randomDirections[] = manager.shuffle(manager.dirs);
        for (Direction dir : randomDirections) {
            if (uc.canMove(dir)) {
                uc.move(dir);
                return;
            }
        }
    }

    private void tryHarvest() {
        if (!uc.canMine()) {
            return;
        }

        if (manager.food.length != 0) {
            int maxAmount = 0;
            FoodInfo bestFood = manager.food[0];

            for (FoodInfo food : manager.food) {
                if (uc.canMine(food)) {
                    if (food.food > maxAmount) {
                        maxAmount = food.food;
                        bestFood = food;
                    }
                }
            }

            if (maxAmount != 0) {
                uc.mine(bestFood);
            }
        }
    }
}
