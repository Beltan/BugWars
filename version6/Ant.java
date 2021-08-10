package version6;

import bugwars.user.*;

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
        if (!uc.canMove()) return;

        int enemies = 0;
        int allies = 1;

        for (UnitInfo ally : manager.units) {
            UnitType allyType = ally.getType();
            if (allyType != UnitType.QUEEN && allyType != UnitType.ANT && !manager.isObstructed(ally.getLocation())) {
                allies++;
            }
        }
        for (UnitInfo enemy : manager.enemies) {
            UnitType enemyType = enemy.getType();
            if (enemyType != UnitType.QUEEN && enemyType != UnitType.ANT && !manager.isObstructed(enemy.getLocation())) {
                enemies++;
            }
        }

        Location foodLoc = manager.getIdleFoodLocation();
        Location foodLocNotObs = manager.getIdleFoodLocationNotObs();
        FoodInfo bestFood = null;
        int maxAmount = 0;

        for (FoodInfo food : manager.food) {
            if (food.food > maxAmount && !manager.isObstructed(food.location)) {
                maxAmount = food.food;
                bestFood = food;
            }
        }

        boolean moved;
        moved = manager.path.evalLocation(allies, enemies);

        if (!moved) {
            if (manager.food.length != 0 && bestFood != null && bestFood.food > 1 && !manager.myLocation.isEqual(bestFood.location)) {
                manager.path.evalFoodLocation(bestFood.location);
            } else if (foodLocNotObs.x != 0 || foodLocNotObs.y != 0) {
                manager.path.moveTo(foodLoc);
            } else if (foodLoc.x != 0 || foodLoc.y != 0) {
                manager.path.moveTo(foodLoc);
            }

            if (uc.canMove()) {
                Direction[] randomDirections = manager.shuffle(manager.dirs);
                for (Direction dir : randomDirections) {
                    if (uc.canMove(dir)) {
                        uc.move(dir);
                        break;
                    }
                }
            }
        }

        manager.postMoveUpdate();
    }

    private void tryHarvest() {
        if (!uc.canMine()) return;

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
}
