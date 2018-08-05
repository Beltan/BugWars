package version3;

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
        Location foodLocNotObs = manager.getIdleFoodLocationNotObs();
        if (manager.enemies.length != 0 && !manager.allObstructed()) {
            evalLocation();
        } else if (manager.food.length != 0) {
            FoodInfo bestFood = manager.food[0];

            for (FoodInfo food : manager.food) {
                if (food.food > maxAmount && !manager.isObstructed(food.location)) {
                    maxAmount = food.food;
                    bestFood = food;
                }
            }
            if (bestFood.food > 3) {
                if (!manager.myLocation.isEqual(bestFood.location)) {
                    manager.path.moveTo(bestFood.location);
                }
            } else if (foodLocNotObs.x != 0 || foodLocNotObs.y != 0) {
                manager.path.moveTo(foodLoc);
            } else if (foodLoc.x != 0 || foodLoc.y != 0) {
                manager.path.moveTo(foodLoc);
            }
        } else if (foodLocNotObs.x != 0 ||foodLocNotObs.y != 0){
            manager.path.moveTo(foodLoc);
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
        manager.myLocation = uc.getLocation();
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
        if (!uc.canAttack() || (manager.enemies.length == 0 && manager.rocks.length == 0)) {
            return;
        }

        if (manager.enemies.length != 0) {
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
            }
        } else if (manager.rocks.length != 0) {
            int smallestRock = 10000000;
            int durability;
            RockInfo weakerRock = manager.rocks[0];

            for (RockInfo rock : manager.rocks) {
                durability = rock.getDurability();
                if (uc.canAttack(rock) && durability < smallestRock) {
                    smallestRock = durability;
                    weakerRock = rock;
                }
            }

            if (smallestRock != 10000000) {
                uc.attack(weakerRock);
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
                    int distance = newLoc.distanceSquared(target);
                    if (!manager.isObstructed(target)) {
                        value += distance;
                    }
                }

                if (value >= bestValue) {
                    bestDirection = manager.dirs[i];
                    bestValue = value;
                }
            }
        }

        if (bestValue != -100000 && uc.canMove(bestDirection)) {
            uc.move(bestDirection);
        }
    }
}
