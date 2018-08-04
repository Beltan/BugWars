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
        tryAttack();
        tryHarvest();
        tryMove();
        tryAttack();
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
        manager.enemies = uc.senseUnits(manager.opponent);
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

    private void tryAttack() {
        if (!uc.canAttack() || manager.enemies.length == 0) {
            return;
        }

        int smallestHealth = 1000000;
        int health;
        UnitInfo lowestEnemy = manager.enemies[0];

        for (UnitInfo enemy : manager.enemies) {
            health = enemy.getHealth();
            if (uc.canAttack(enemy) && health < smallestHealth) {
                smallestHealth = health;
                lowestEnemy = enemy;
            }
        }

        if (smallestHealth != 1000000) {
            uc.attack(lowestEnemy);
        } else if (manager.allObstructed()) {
            for (RockInfo rock : manager.rocks) {
                if (uc.canAttack(rock)) {
                    uc.attack(rock);
                    break;
                }
            }
        }
    }
}
