package version8;

import bugwars.user.*;

public class Queen {

    private MemoryManager manager;
    private UnitController uc;
    private Direction[] dirs;

    private final int MAX_FILTERED_UNITS = 10;
    private final int MAX_HEAL_CANDIDATES = 10;
    private final int SURPLUS_RESOURCES = 400;

    public Queen(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
        dirs = manager.shuffle(manager.dirs);
    }

    public void play() {
        manager.scanFood();
        tryHeal();
        tryMove();
        trySpawn();
        tryHeal();
    }

    private void tryMove() {
        if (!uc.canMove()) return;

        int enemies = 0;
        int allies = 0;

        if (manager.units.length < MAX_FILTERED_UNITS) {
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
        } else if (manager.bestFood != null && isFar(manager.bestFood)) {
            manager.path.moveToQueen(manager.bestFood);
        } else if (manager.isSet(foodLocNotObs) && isFar(foodLocNotObs)) {
            manager.path.moveToQueen(foodLocNotObs);
        } else if (manager.isSet(foodLoc) && isFar(foodLoc)) {
            manager.path.moveToQueen(foodLoc);
        } else {
            manager.path.moveToQueen(manager.closestEnemyQueen());
        }
        manager.postMoveUpdate();
        manager.scanFood();
    }

    private boolean isFar(Location target) {
        return manager.myLocation.distanceSquared(target) > manager.ADJACENT_DISTANCE;
    }

    private void trySpawn() {
        Location idleFoodNotObs = manager.getIdleFoodLocationNotObs();
        Location idleFood = manager.getIdleFoodLocation();
        Direction target;

        if (manager.bestFood != null) {
            target = manager.myLocation.directionTo(manager.bestFood);
        } else if (manager.isSet(idleFoodNotObs)) {
            target = manager.myLocation.directionTo(idleFoodNotObs);
        } else if (manager.isSet(idleFood)) {
            target = manager.myLocation.directionTo(idleFood);
        } else {
            target = manager.myLocation.directionTo(manager.closestEnemyQueen());
        }

        if (manager.canSpawnAnt()) {
            spawn(UnitType.ANT, target);
        } else if (manager.canSpawnBee()) {
            spawn(UnitType.BEE, target);
        } else if (manager.canSpawnSpider()) {
            spawn(UnitType.SPIDER, target);
        } else if (manager.objective == UnitType.BEETLE && manager.canSpawnBeetle()) {
            spawn(UnitType.BEETLE, target);
        } else if (manager.resources >= SURPLUS_RESOURCES) {
            spawn(manager.enemySpiders > manager.allyBees ? UnitType.BEE : UnitType.BEETLE, target);
        }
    }

    private void spawn(UnitType type, Direction target) {
        if (uc.canSpawn(target, type)) {
            uc.spawn(target, type);
            manager.addCocoonList(manager.myLocation.add(target));
            return;
        }

        for (Direction dir : dirs) {
            if (uc.canSpawn(dir, type)) {
                uc.spawn(dir, type);
                manager.addCocoonList(manager.myLocation.add(dir));
                return;
            }
        }
    }

    private void tryHeal() {
        if (manager.units.length != 0) {
            int lowestHealth = manager.INF;
            UnitInfo bestTarget = null;
            UnitInfo ally;
            int index = Math.min(MAX_HEAL_CANDIDATES, manager.units.length);
            for (int i = 0; i < index; i++) {
                ally = manager.units[i];
                int health = ally.getHealth();
                int maxHealth = manager.unitHealth(ally.getType());
                if (health < lowestHealth && maxHealth != health && uc.canHeal(ally)) {
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
