package version11;

import bugwars.user.*;

public class Queen {

    private MemoryManager manager;
    private UnitController uc;
    private Direction[] dirs;
    private boolean scanned;
    private int unreachableNext = 0;

    private final int MAX_FILTERED_UNITS = 10;
    private final int SURPLUS_RESOURCES = 400;
    private final int UNREACHABLE_MEMORY = 4;
    private Location[] unreachable = new Location[UNREACHABLE_MEMORY];

    public Queen(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
        dirs = manager.shuffle(manager.dirs);
    }

    public void play() {
        scanned = false;
        tryHeal();
        tryMove();
        trySpawn();
        tryHeal();
        manager.checkRoom();
    }

    private void tryMove() {
        if (!uc.canMove()) return;
        manager.scanFood();
        scanned = true;
        Location start = manager.myLocation;

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
        boolean leaveForFood = manager.foodCount == 0 || !manager.localFoodHasRoom() || manager.antsBlockedByDanger();
        if (manager.enemies.length != 0 && !manager.allObstructed()) {
            manager.path.evalLocation(allies, enemies);
        } else if (manager.bestFood != null && isFar(manager.bestFood) && !leaveForFood) {
            manager.path.moveToQueen(manager.bestFood);
        } else if (leaveForFood && manager.isSet(foodLocNotObs) && isFar(foodLocNotObs) && goToFood(foodLocNotObs)) {
        } else if (leaveForFood && manager.isSet(foodLoc) && isFar(foodLoc) && goToFood(foodLoc)) {
        } else if (manager.bestFood != null && isFar(manager.bestFood)) {
            manager.path.moveToQueen(manager.bestFood);
        } else {
            // Straight-line pathing walks into dead ends, the field knows the way round
            Direction dir = manager.map.enemyField.downhill(manager.myLocation);
            if (dir == null) manager.path.moveToQueen(manager.closestEnemyQueen());
            else if (uc.canMove(dir)) uc.move(dir);
        }
        manager.postMoveUpdate();
        if (!manager.myLocation.isEqual(start)) manager.scanFood();
    }

    // False when the finished food field shows the target walled off
    private boolean goToFood(Location target) {
        if (manager.isContested(target) && manager.antsBlockedByDanger()) return false;
        for (Location ignored : unreachable) {
            if (ignored != null && ignored.isEqual(target)) return false;
        }
        MapMemory map = manager.map;
        map.setFoodTarget(target);
        if (map.foodField.ready()) {
            if (!map.foodField.reachable(manager.myLocation)) {
                unreachable[unreachableNext] = target;
                unreachableNext = (unreachableNext + 1) % unreachable.length;
                return false;
            }
            Direction dir = map.foodField.direction();
            if (dir != null) {
                uc.move(dir);
                return true;
            }
        }
        manager.path.moveToQueen(target);
        return true;
    }

    private boolean isFar(Location target) {
        return manager.myLocation.distanceSquared(target) > manager.ADJACENT_DISTANCE;
    }

    private void trySpawn() {
        // The food scan only matters for ants; below their cost no unit is affordable anyway
        if (!scanned) {
            if (manager.resources >= UnitType.ANT.getCost()) manager.scanFood();
            else manager.bestFood = null;
        }
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

        // Along the path, so soldiers are not born in a dead end behind the queen
        Direction soldierTarget = manager.map.enemyField.downhill(manager.myLocation);
        if (soldierTarget == null) soldierTarget = manager.myLocation.directionTo(manager.closestEnemyQueen());
        Location alert = manager.tracker.defenseAlert();
        boolean underAttack = (manager.enemies.length != 0 && !manager.allObstructed())
                || (alert != null && manager.myLocation.distanceSquared(alert) <= manager.tracker.DEFENSE_RADIUS);

        if (manager.canSpawnAnt()) {
            spawn(UnitType.ANT, target);

        } else if (manager.canSpawnBee()) {
            spawn(UnitType.BEE, soldierTarget);
        } else if (manager.canSpawnSpider()) {
            spawn(UnitType.SPIDER, soldierTarget);
        } else if (manager.objective == UnitType.BEETLE && manager.canSpawnBeetle()) {
            spawn(UnitType.BEETLE, soldierTarget);
        } else if (underAttack || manager.resources >= SURPLUS_RESOURCES) {
            spawn(manager.tracker.count(UnitType.SPIDER) > manager.getBees() ? UnitType.BEE : UnitType.BEETLE, soldierTarget);
        }
    }

    // Safe tiles first, soldier cocoons still absorb damage when none is safe
    private void spawn(UnitType type, Direction target) {
        if (spawnAt(type, target, true)) return;
        for (Direction dir : dirs) {
            if (spawnAt(type, dir, true)) return;
        }
        if (type == UnitType.ANT) return;
        if (spawnAt(type, target, false)) return;
        for (Direction dir : dirs) {
            if (spawnAt(type, dir, false)) return;
        }
    }

    private boolean spawnAt(UnitType type, Direction dir, boolean safeOnly) {
        if (!uc.canSpawn(dir, type)) return false;
        Location loc = manager.myLocation.add(dir);
        if (safeOnly && manager.isThreatened(loc)) return false;
        uc.spawn(dir, type);
        manager.addCocoonList(loc);
        return true;
    }

    // Soldiers before ants, then lowest health
    private void tryHeal() {
        if (!uc.canHeal()) return;
        UnitInfo bestTarget = null;
        int bestScore = manager.INF;
        for (UnitInfo ally : uc.senseUnits(GameConstants.QUEEN_HEALING_RANGE, manager.allies)) {
            int health = ally.getHealth();
            if (health == manager.unitHealth(ally.getType())) continue;
            int score = ally.getType() == UnitType.ANT ? health + GameConstants.QUEEN_MAX_HEALTH : health;
            if (score < bestScore && uc.canHeal(ally)) {
                bestScore = score;
                bestTarget = ally;
            }
        }
        if (bestTarget != null) {
            uc.heal(bestTarget);
        }
    }
}
