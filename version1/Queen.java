package version1;

import bugwars.user.*;

public class Queen {

    private MemoryManager manager;
    private UnitController uc;

    public Queen(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        tryHeal();
        trySpawn();
        tryMove();
        tryHeal();
    }

    private void tryMove() {
        Location foodLoc = manager.getIdleFoodLocation();
        if (manager.isExtreme(manager.myLocation)) {
            if (manager.myLocation.isEqual(foodLoc)) {
                manager.path.moveToQueen(uc.getEnemyQueensLocation()[0]);
            } else if (manager.bestFood != null) {
                manager.path.moveToQueen(manager.bestFood);
            } else if (foodLoc.x != 0 || foodLoc.y != 0) {
                manager.path.moveToQueen(foodLoc);
            } else {
                evalLocation();
            }
        } else if (manager.enemies.length != 0 && !manager.allObstructed()) {
            evalLocation();
        } else if (manager.bestFood != null) {
            manager.path.moveToQueen(manager.bestFood);
        } else if (foodLoc.x != 0 || foodLoc.y != 0) {
            manager.path.moveToQueen(foodLoc);
        } else {
            manager.path.moveToQueen(uc.getEnemyQueensLocation()[0]);
        }
        manager.myLocation = uc.getLocation();
        manager.enemies = uc.senseUnits(manager.opponent);
    }

    private void trySpawn() {
        if (manager.canSpawnAnt()) {
            Location idleFood = manager.getIdleFoodLocation();
            Direction target;
            if (manager.bestFood != null) {
                target = manager.bestFood.directionTo(manager.myLocation);
            } else if (idleFood.x != 0 && idleFood.y != 0) {
                target = idleFood.directionTo(manager.myLocation);
            } else {
                target = uc.getEnemyQueensLocation()[0].directionTo(manager.myLocation);
            }

            if (uc.canSpawn(target, UnitType.ANT)) {
                uc.spawn(target, UnitType.ANT);
                manager.addCocoonList(manager.myLocation.add(target));
            }

            for (Direction dir : manager.dirs) {
                if (uc.canSpawn(dir, UnitType.ANT)) {
                    uc.spawn(dir, UnitType.ANT);
                    manager.addCocoonList(manager.myLocation.add(dir));
                    break;
                }
            }
        } else if (manager.objective == UnitType.BEETLE){
            if (manager.canSpawnBeetle()) {
                for (Direction dir : manager.dirs) {
                    if (uc.canSpawn(dir, UnitType.BEETLE)) {
                        uc.spawn(dir, UnitType.BEETLE);
                        manager.addCocoonList(manager.myLocation.add(dir));
                        break;
                    }
                }
            } else if (manager.canSpawnSpider()) {
                for (Direction dir : manager.dirs) {
                    if (uc.canSpawn(dir, UnitType.SPIDER)) {
                        uc.spawn(dir, UnitType.SPIDER);
                        manager.addCocoonList(manager.myLocation.add(dir));
                        break;
                    }
                }
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
                if (manager.isExtreme(newLoc)) {
                    value -= 100;
                }
                for (int j = 0; j < manager.enemies.length; j++) {
                    Location target = manager.enemies[j].getLocation();
                    if (!manager.isObstructed(target)) {
                        int distance = newLoc.distanceSquared(target);
                        if (distance < 6) {
                            value += distance * distance;
                        }
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

    private void tryHeal() {
        int lowestHealth = 1000;
        UnitInfo bestTarget = null;
        for (UnitInfo ally : manager.units) {
            int health = ally.getHealth();
            int maxHealth = manager.unitHealth(ally.getType());
            if (uc.canHeal(ally) && health < lowestHealth && maxHealth != health) {
                lowestHealth = health;
                bestTarget = ally;
            }
        }
        if (bestTarget != null) {
            uc.heal(bestTarget);
        }
    }
}
