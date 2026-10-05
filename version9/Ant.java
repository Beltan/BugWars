package version9;

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

        int enemies = manager.countVisibleSoldiers(manager.enemies, manager.INF);
        int allies = 1 + manager.countVisibleSoldiers(manager.units, 2 * enemies);

        Location foodLoc = manager.getIdleFoodLocation();
        Location foodLocNotObs = manager.getIdleFoodLocationNotObs();
        FoodInfo bestFood = null;
        int maxAmount = 0;

        for (FoodInfo food : manager.food) {
            if ((food.food > maxAmount + 25 || maxAmount == 0) && !manager.isObstructed(food.location)) {
                maxAmount = food.food;
                bestFood = food;
            }
        }

        boolean moved;
        moved = manager.path.evalLocation(allies, enemies);

        if (!moved) {
            if (manager.food.length != 0 && bestFood != null && bestFood.food > 1 && !manager.myLocation.isEqual(bestFood.location)) {
                manager.path.evalFoodLocation(bestFood.location);
            } else if (manager.isSet(foodLocNotObs)) {
                manager.path.moveTo(foodLocNotObs);
            } else if (manager.isSet(foodLoc)) {
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
                if (food.food > maxAmount && uc.canMine(food)) {
                    maxAmount = food.food;
                    bestFood = food;
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

        UnitInfo queenTarget = manager.endgameQueenTarget();
        if (queenTarget != null) {
            uc.attack(queenTarget);
            return;
        }

        if (manager.enemies.length != 0) {
            int smallestHealth = manager.INF;
            int health;
            UnitInfo lowestEnemy = manager.enemies[0];

            for (UnitInfo enemy : manager.enemies) {
                health = enemy.getHealth();
                if (uc.canAttack(enemy) && health < smallestHealth) {
                    smallestHealth = health;
                    lowestEnemy = enemy;
                }
            }

            if (smallestHealth != manager.INF) {
                uc.attack(lowestEnemy);
                manager.tracker.onAttack(lowestEnemy);
            }
        } else if (manager.rocks.length != 0) {
            int smallestRock = manager.INF;
            int durability;
            RockInfo weakerRock = manager.rocks[0];

            for (RockInfo rock : manager.rocks) {
                durability = rock.getDurability();
                if (uc.canAttack(rock) && durability < smallestRock) {
                    smallestRock = durability;
                    weakerRock = rock;
                }
            }

            if (smallestRock != manager.INF) {
                uc.attack(weakerRock);
            }
        }
    }
}
