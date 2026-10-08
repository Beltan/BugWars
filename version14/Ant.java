package version14;

import bugwars.user.*;

public class Ant {

    private MemoryManager manager;
    private UnitController uc;
    // An ant mines up to ANT_MINING per round, a tile walked costs about a round of mining
    private final int FOOD_PER_TILE_WALKED = GameConstants.ANT_MINING;

    public Ant(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        manager.attacker.tryAttack(false, manager.INF);
        tryHarvest();
        tryMove();
        manager.attacker.tryAttack(false, manager.INF);
        tryHarvest();
    }

    private void tryMove() {
        if (!uc.canMove()) return;
        if (manager.makeRoom()) {
            manager.postMoveUpdate();
            return;
        }

        Location foodLoc = manager.getIdleFoodLocation();
        Location foodLocNotObs = manager.getIdleFoodLocationNotObs();
        FoodInfo bestFood = null;
        int bestScore = -manager.INF;
        Location me = manager.myLocation;
        for (FoodInfo food : manager.food) {
            int walk = Math.max(Math.abs(food.location.x - me.x), Math.abs(food.location.y - me.y));
            int score = food.food - FOOD_PER_TILE_WALKED * walk;
            if (score > bestScore && !manager.isObstructed(food.location)) {
                bestScore = score;
                bestFood = food;
            }
        }

        if (bestFood != null && manager.countVisibleSoldiers(manager.enemies, 1) != 0) {
            Pathfinder path = manager.path;
            if (me.distanceSquared(bestFood.location) <= GameConstants.ANT_MINING_RANGE_SQUARED && !path.threatenedNextTurn(me)) {
                manager.postMoveUpdate();
                return;
            }
            Direction step = me.directionTo(bestFood.location);
            if (uc.canMove(step) && !path.threatenedNextTurn(me.add(step))) {
                uc.move(step);
                manager.postMoveUpdate();
                return;
            }
        }

        if (!manager.path.evalLocation()) {
            if (bestFood != null && bestFood.food > 1 && !manager.myLocation.isEqual(bestFood.location)) {
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
}
