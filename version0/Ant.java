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
        } else {
            Direction idleDirection = manager.myLocation.directionTo(manager.getIdleFoodLocation());
            if (uc.canMove(idleDirection)) {
                uc.move(idleDirection);
            } else {
                Direction randomDirections[] = manager.shuffle(manager.dirs);
                for (Direction dir : randomDirections) {
                    if (uc.canMove(dir)) {
                        uc.move(dir);
                        return;
                    }
                }
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
