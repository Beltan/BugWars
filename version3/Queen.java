package version3;

import bugwars.*;

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
        if (!uc.canMove()) return;

        Location foodLocNotObs = manager.getIdleFoodLocationNotObs();
        Location foodLoc = manager.getIdleFoodLocation();
        if (manager.enemies.length != 0 && !manager.allObstructed()) {
            evalLocation();
        } else if (manager.bestFood != null && manager.myLocation != manager.bestFood && manager.myLocation.distanceSquared(manager.bestFood) > 2) {
            manager.path.moveToQueen(manager.bestFood);
        } else if ((foodLocNotObs.x != 0 || foodLocNotObs.y != 0) && manager.myLocation != foodLocNotObs && manager.myLocation.distanceSquared(foodLocNotObs) > 2) {
            manager.path.moveToQueen(foodLocNotObs);
        } else if ((foodLoc.x != 0 || foodLoc.y != 0) && manager.myLocation != foodLoc && manager.myLocation.distanceSquared(foodLoc) > 2) {
            manager.path.moveToQueen(foodLoc);
        } else {
            if (manager.getPassive() != 1 || manager.round > 1000 || manager.myLocation.distanceSquared(uc.getEnemyQueensLocation()[0]) > 200) {
                manager.path.moveToQueen(uc.getEnemyQueensLocation()[0]);
            }
        }
        manager.postMoveUpdate();
    }

    private void trySpawn() {
        Location idleFoodNotObs = manager.getIdleFoodLocationNotObs();
        Location idleFood = manager.getIdleFoodLocation();
        Direction target;

        if (manager.bestFood != null) {
            target = manager.bestFood.directionTo(manager.myLocation);
        } else if (idleFoodNotObs.x != 0 && idleFoodNotObs.y != 0) {
            target = idleFoodNotObs.directionTo(manager.myLocation);
        } else if (idleFood.x != 0 && idleFood.y != 0) {
            target = idleFood.directionTo(manager.myLocation);
        } else {
            target = uc.getEnemyQueensLocation()[0].directionTo(manager.myLocation);
        }

        if (manager.canSpawnAnt()) {

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

                if (uc.canSpawn(target, UnitType.BEETLE)) {
                    uc.spawn(target, UnitType.BEETLE);
                    manager.addCocoonList(manager.myLocation.add(target));
                }

                for (Direction dir : manager.dirs) {
                    if (uc.canSpawn(dir, UnitType.BEETLE)) {
                        uc.spawn(dir, UnitType.BEETLE);
                        manager.addCocoonList(manager.myLocation.add(dir));
                        break;
                    }
                }

            } else if (manager.canSpawnSpider()) {

                if (uc.canSpawn(target, UnitType.SPIDER)) {
                    uc.spawn(target, UnitType.SPIDER);
                    manager.addCocoonList(manager.myLocation.add(target));
                }

                for (Direction dir : manager.dirs) {
                    if (uc.canSpawn(dir, UnitType.SPIDER)) {
                        uc.spawn(dir, UnitType.SPIDER);
                        manager.addCocoonList(manager.myLocation.add(dir));
                        break;
                    }
                }
            } else if (manager.canSpawnBee()) {

                if (uc.canSpawn(target, UnitType.BEE)) {
                    uc.spawn(target, UnitType.BEE);
                    manager.addCocoonList(manager.myLocation.add(target));
                }

                for (Direction dir : manager.dirs) {
                    if (uc.canSpawn(dir, UnitType.BEE)) {
                        uc.spawn(dir, UnitType.BEE);
                        manager.addCocoonList(manager.myLocation.add(dir));
                        break;
                    }
                }
            }
        }
    }

    private void evalLocation() {
        Direction bestDirection = manager.dirs[8];
        int bestValue = -100000;
        int enemies = 0;
        int allies = 0;
        if (manager.units != null) {
            allies = manager.units.length;
        }

        for (UnitInfo enemy : manager.enemies) {
            UnitType enemyType = enemy.getType();
            if (enemyType != UnitType.QUEEN && enemyType != UnitType.ANT) {
                enemies++;
            }
        }

        for (int i = 0; i < manager.dirs.length; i++) {
            Location newLoc = manager.myLocation.add(manager.dirs[i]);
            if (uc.canMove(manager.dirs[i]) || manager.myLocation.isEqual(newLoc)) {
                int value = 0;
                for (int j = 0; j < manager.enemies.length; j++) {
                    Location target = manager.enemies[j].getLocation();
                    if (!manager.isObstructed(target)) {
                        int distance = newLoc.distanceSquared(target);
                        int health = uc.getInfo().getHealth();
                        if ((enemies <= allies) && ((health > 200 && enemies < 5) || (health > 150 && enemies < 4) || (health > 100 && enemies < 3) || (health > 50 && enemies < 2))) {
                            if (distance < 3) {
                                value -= 100;
                            }
                        } else {
                            value += distance;
                        }
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

        for (int i = 0; i < uc.getMyQueensLocation().length; i++) {
            if (uc.read(manager.INITIAL_QUEEN_INFO + i * 4) == 2) {
                continue;
            }
            uc.write(manager.INITIAL_QUEEN_INFO + 2 + i * 4, allies);
            uc.write(manager.INITIAL_QUEEN_INFO + 3 + i * 4, enemies);
            uc.write(manager.INITIAL_QUEEN_INFO + i * 4, 2);
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
