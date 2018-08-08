package version4;

import bugwars.*;

public class Pathfinder {

    private MemoryManager manager;
    private UnitController uc;

    public Pathfinder(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    final int INF = 1000000;

    boolean rotateRight = true; // Should rotate right or left
    Location lastObstacleFound = null; // Latest obstacle found
    int minDistToEnemy = INF; // Minimum distance while going around an obstacle
    Location prevTarget = null; // Previous target
    int counter = 0;

    public void moveTo(Location target){
        if (!uc.canMove()) return;
        if (target == null) return;

        if (prevTarget == null || target.distanceSquared(prevTarget) > 9 || manager.myLocation.isEqual(target) || (uc.canSenseLocation(target) && !manager.isObstructed(target))) {
            resetPathfinding();
        }

        // If minimum distance to the target, reset
        int d = manager.myLocation.distanceSquared(target);
        if (d <= minDistToEnemy) resetPathfinding();

        // Update data
        prevTarget = target;
        minDistToEnemy = Math.min(d, minDistToEnemy);

        // If there's an obstacle, try to go around it instead of going to the target directly
        Direction dir = manager.myLocation.directionTo(target);
        if (lastObstacleFound != null) dir = manager.myLocation.directionTo(lastObstacleFound);

        // This should not happen for a single unit, but whatever
        if (uc.canMove(dir)) resetPathfinding();

        // Rotate clockwise or counterclockwise. If try to go out of the map change the orientation
        for (int i = 0; i < 16; ++i){
            if (uc.canMove(dir)){
                uc.move(dir);
                counter = 0;
                break;
            }
            Location newLoc = manager.myLocation.add(dir);
            if (uc.isOutOfMap(newLoc)) {
                rotateRight = !rotateRight;
            } else {
                // Update latest obstacle found and check counter for unit obstacles
                Location possibleObstacle = manager.myLocation.add(dir);
                if (uc.senseUnit(possibleObstacle) == null || counter > 2){
                    lastObstacleFound = manager.myLocation.add(dir);
                    if (rotateRight) dir = dir.rotateRight();
                    else dir = dir.rotateLeft();
                } else {
                    counter++;
                }
                if (counter > 4) {
                    resetPathfinding();
                }
            }
        }

        if (uc.canMove(dir)) {
            uc.move(dir);
            counter = 0;
        }
    }

    boolean rotateRightQueen = true; // Should rotate right or left
    Location lastObstacleFoundQueen = null; // Latest obstacle found
    int minDistToEnemyQueen = INF; // Minimum distance while going around an obstacle
    Location prevTargetQueen = null; // Previous target
    int counterQueen = 0;

    public void moveToQueen(Location target){
        if (!uc.canMove()) return;
        if (target == null) return;

        if (prevTargetQueen == null || target.distanceSquared(prevTargetQueen) > 9 || manager.myLocation.isEqual(target) || (uc.canSenseLocation(target) && !manager.isObstructed(target))) {
            resetPathfindingQueen();
        }

        // If minimum distance to the target, reset
        int d = manager.myLocation.distanceSquared(target);
        if (d <= minDistToEnemyQueen) resetPathfindingQueen();

        // Update data
        prevTargetQueen = target;
        minDistToEnemyQueen = Math.min(d, minDistToEnemyQueen);

        // If there's an obstacle, try to go around it instead of going to the target directly
        Direction dir = manager.myLocation.directionTo(target);
        if (lastObstacleFoundQueen != null) dir = manager.myLocation.directionTo(lastObstacleFoundQueen);

        // This should not happen for a single unit, but whatever
        if (uc.canMove(dir)) resetPathfindingQueen();

        // Rotate clockwise or counterclockwise. If try to go out of the map change the orientation
        for (int i = 0; i < 16; ++i){
            if (uc.canMove(dir)){
                uc.move(dir);
                counterQueen = 0;
                break;
            }
            Location newLoc = manager.myLocation.add(dir);
            if (uc.isOutOfMap(newLoc)) {
                rotateRightQueen = !rotateRightQueen;
            } else {
                // Update latest obstacle found and check counter for unit obstacles
                Location possibleObstacle = manager.myLocation.add(dir);
                UnitInfo obstacle = uc.senseUnit(possibleObstacle);
                if (obstacle == null || obstacle.getType() == UnitType.QUEEN || counterQueen > 12){
                    lastObstacleFoundQueen = manager.myLocation.add(dir);
                    if (rotateRightQueen) dir = dir.rotateRight();
                    else dir = dir.rotateLeft();
                } else {
                    counterQueen++;
                    break;
                }
                if (counterQueen > 15) {
                    resetPathfindingQueen();
                }
            }
        }

        if (uc.canMove(dir)) {
            uc.move(dir);
            counterQueen = 0;
        }
    }

    // Clear some of the previous data
    private void resetPathfindingQueen(){
        lastObstacleFoundQueen = null;
        minDistToEnemyQueen = INF;
        counterQueen = 0;
    }

    // Clear some of the previous data
    private void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
        counter = 0;
    }

    public void evalFoodLocation(Location foodLoc) {
        int minDistance = 1000000;
        Direction bestDirection = manager.dirs[8];

        for (int i = 0; i < 9; i++) {
            int distance = manager.myLocation.add(manager.dirs[i]).distanceSquared(foodLoc);
            if (uc.canMove(manager.dirs[i]) && distance < minDistance) {
                minDistance = distance;
                bestDirection = manager.dirs[i];
            }
        }

        if (minDistance != 1000000) {
            uc.move(bestDirection);
        }
    }

    public void evalLocation(int allies, int enemies) {
        if (!uc.canMove()) return;

        MicroInfo[] microInfo = new MicroInfo[9];
        for (int i = 0; i < 9; i++) microInfo[i] = new MicroInfo(manager.myLocation.add(manager.dirs[i]), allies, enemies);

        for (UnitInfo enemy : manager.enemies) {
            for (int i = 0; i < 9; i++) {
                microInfo[i].update(enemy);
            }
        }

        int bestIndex = -1;

        for (int i = 8; i >= 0; i--) {
            if (!uc.canMove(manager.dirs[i])) continue;
            if (bestIndex < 0 || !microInfo[bestIndex].isBetter(microInfo[i])) bestIndex = i;
        }

        if (bestIndex != -1) {
            if (manager.enemies.length > 0) {
                uc.move(manager.dirs[bestIndex]);
            }
        }
    }

    class MicroInfo {
        int numEnemies;
        int numAnts;
        int numSpiders;
        int numBees;
        int numBeetles;
        int minDistToEnemy;
        int minDistToSoldier;
        int numAttacks;
        int allies;
        int enemies;
        boolean moveAndKill;
        Location loc;

        public MicroInfo(Location loc, int allies, int enemies) {
            this.loc = loc;
            this.allies = allies;
            this.enemies = enemies;
            numEnemies = 0;
            numAnts = 0;
            numSpiders = 0;
            numBees = 0;
            numBeetles = 0;
            numAttacks = 0;
            minDistToEnemy = 1000000;
            minDistToSoldier = 1000000;
            moveAndKill = false;
        }

        void update(UnitInfo unit) {
            UnitType type = unit.getType();
            if (!uc.isObstructed(loc, unit.getLocation())) {
                int distance = unit.getLocation().distanceSquared(loc);
                if (distance <= type.getAttackRangeSquared() && distance >= type.getMinAttackRangeSquared()) {
                    if (type != UnitType.ANT) numEnemies++;
                    if (type == UnitType.ANT) numAnts++;
                    if (type == UnitType.SPIDER) numSpiders++;
                    if (type == UnitType.BEE) numBees++;
                    if (type == UnitType.BEETLE) numBeetles++;
                }

                if (distance <= manager.myType.getAttackRangeSquared() && distance >= manager.myType.getMinAttackRangeSquared())
                    numAttacks++;
                if (distance < minDistToEnemy) minDistToEnemy = distance;
                if (distance < minDistToSoldier && type != UnitType.ANT && type != UnitType.QUEEN)
                    minDistToSoldier = distance;
            }
        }

        boolean canAttack() {
            return (manager.myType.getAttackRangeSquared() >= minDistToEnemy && manager.myType.getMinAttackRangeSquared() <= minDistToEnemy);
        }

        boolean isBetter(MicroInfo micro) {
            if (manager.myType == UnitType.ANT) return minDistToSoldier > micro.minDistToSoldier;
            if (manager.myType == UnitType.QUEEN) return minDistToEnemy > micro.minDistToEnemy;
            if (manager.myType != UnitType.SPIDER && allies >= enemies * 2) return minDistToEnemy <= micro.minDistToEnemy;
            if (numSpiders != 0 && numSpiders == numEnemies) return minDistToEnemy <= micro.minDistToEnemy;
            if (numEnemies < micro.numEnemies) return true;
            if (numEnemies > micro.numEnemies) return false;
            if (canAttack()) {
                if (!micro.canAttack()) return true;
                return minDistToEnemy >= micro.minDistToEnemy;
            }
            if (micro.canAttack()) return false;
            if (manager.myType == UnitType.SPIDER && manager.myType.getMinAttackRangeSquared() > minDistToEnemy) {
                return minDistToEnemy > micro.minDistToEnemy;
            }
            return minDistToEnemy <= micro.minDistToEnemy;
        }
    }

}
