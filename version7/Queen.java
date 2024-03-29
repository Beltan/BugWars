package version7;

import bugwars.user.*;

public class Queen {

    private MemoryManager manager;
    private UnitController uc;
    private Direction[] dirs;

    public Queen(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
        dirs = manager.shuffle(manager.dirs);
    }

    public void play() {
        tryHeal();
        tryMove();
        trySpawn();
        tryHeal();
    }

    private void tryMove() {
        if (!uc.canMove()) return;

        int enemies = 0;
        int allies = 0;

        if (manager.units.length < 10) {
            for (UnitInfo ally : manager.units) {
                UnitType allyType = ally.getType();
                if (allyType != UnitType.QUEEN && allyType != UnitType.ANT && !manager.isObstructed(ally.getLocation())) {
                    allies++;
                }
            }
        } else {
            allies = manager.units.length;
        }
        for (UnitInfo enemy : manager.enemies) {
            UnitType enemyType = enemy.getType();
            if (enemyType != UnitType.QUEEN && enemyType != UnitType.ANT && !manager.isObstructed(enemy.getLocation())) {
                enemies++;
            }
        }

        Location foodLocNotObs = manager.getIdleFoodLocationNotObs();
        Location foodLoc = manager.getIdleFoodLocation();
        if (manager.enemies.length != 0 && !manager.allObstructed()) {
            manager.path.evalLocation(allies, enemies);
        } else if (manager.bestFood != null && manager.myLocation != manager.bestFood && manager.myLocation.distanceSquared(manager.bestFood) > 2) {
            manager.path.moveToQueen(manager.bestFood);
        } else if ((foodLocNotObs.x != 0 || foodLocNotObs.y != 0) && manager.myLocation != foodLocNotObs && manager.myLocation.distanceSquared(foodLocNotObs) > 2) {
            manager.path.moveToQueen(foodLocNotObs);
        } else if ((foodLoc.x != 0 || foodLoc.y != 0) && manager.myLocation != foodLoc && manager.myLocation.distanceSquared(foodLoc) > 2) {
            manager.path.moveToQueen(foodLoc);
        } else {
            manager.path.moveToQueen(uc.getEnemyQueensLocation()[0]);
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

            for (Direction dir : dirs) {
                if (uc.canSpawn(dir, UnitType.ANT)) {
                    uc.spawn(dir, UnitType.ANT);
                    manager.addCocoonList(manager.myLocation.add(dir));
                    break;
                }
            }

        } else if (manager.canSpawnBee()) {

            if (uc.canSpawn(target, UnitType.BEE)) {
                uc.spawn(target, UnitType.BEE);
                manager.addCocoonList(manager.myLocation.add(target));
            }

            for (Direction dir : dirs) {
                if (uc.canSpawn(dir, UnitType.BEE)) {
                    uc.spawn(dir, UnitType.BEE);
                    manager.addCocoonList(manager.myLocation.add(dir));
                    break;
                }
            }
        } else if (manager.canSpawnSpider()) {

            if (uc.canSpawn(target, UnitType.SPIDER)) {
                uc.spawn(target, UnitType.SPIDER);
                manager.addCocoonList(manager.myLocation.add(target));
            }

            for (Direction dir : dirs) {
                if (uc.canSpawn(dir, UnitType.SPIDER)) {
                    uc.spawn(dir, UnitType.SPIDER);
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

                for (Direction dir : dirs) {
                    if (uc.canSpawn(dir, UnitType.BEETLE)) {
                        uc.spawn(dir, UnitType.BEETLE);
                        manager.addCocoonList(manager.myLocation.add(dir));
                        break;
                    }
                }

            }
        }
    }

    private void tryHeal() {
        if (manager.units.length != 0) {
            int lowestHealth = 1000;
            UnitInfo bestTarget = null;
            UnitInfo ally;
            int index = 10;
            if (index > manager.units.length) index = manager.units.length;
            for (int i = 0; i < index; i++) {
                ally = manager.units[i];
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
}
