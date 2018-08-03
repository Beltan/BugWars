package version1;

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
        if (manager.enemies.length != 0 && !manager.allObstructed()) {
            evalLocation();
        } else if (manager.food.length != 0) {
            FoodInfo bestFood = manager.food[0];

            for (FoodInfo food : manager.food) {
                if (food.food > maxAmount) {
                    maxAmount = food.food;
                    bestFood = food;
                }
            }

            if (!manager.myLocation.isEqual(bestFood.location)) {
                manager.path.moveTo(bestFood.location);
            }
        } else if (foodLoc.x != 0 ||foodLoc.y != 0){
            manager.path.moveTo(foodLoc);
        } else {
            Direction randomDirections[] = manager.shuffle(manager.dirs);
            for (Direction dir : randomDirections) {
                if (uc.canMove(dir)) {
                    uc.move(dir);
                    break;
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

    private void evalLocation() {
        if (!uc.canMove()) return;

        Direction bestDirection = manager.dirs[8];
        int bestValue = -100000;

        for (int i = 0; i < manager.dirs.length; i++) {
            Location newLoc = manager.myLocation.add(manager.dirs[i]);
            if (uc.canMove(manager.dirs[i]) || manager.myLocation.isEqual(newLoc)) {
                int value = 0;
                for (int j = 0; j < manager.enemies.length; j++) {
                    Location target = manager.enemies[j].getLocation();
                    if (!manager.isObstructed(target)) {
                        int distance = newLoc.distanceSquared(target);
                        value += distance * distance;
                    }
                }

                if (value >= bestValue) {
                    bestDirection = manager.dirs[i];
                    bestValue = value;
                }
            }
        }

        if (bestValue != -100000) {
            uc.move(bestDirection);
        }
    }
}
