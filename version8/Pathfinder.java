package version8;

import bugwars.user.*;

public class Pathfinder {

    private MemoryManager manager;
    private UnitController uc;

    public Pathfinder(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    final int INF = 1000000;
    final int SPIDER_THREAT_RANGE = threatRange(GameConstants.SPIDER_ATTACK_RANGE_SQUARED, GameConstants.SPIDER_MOVEMENT_RANGE_SQUARED);
    final int BEE_THREAT_RANGE = threatRange(GameConstants.BEE_ATTACK_RANGE_SQUARED, GameConstants.BEE_MOVEMENT_RANGE_SQUARED);
    final int BEETLE_THREAT_RANGE = threatRange(GameConstants.BEETLE_ATTACK_RANGE_SQUARED, GameConstants.BEETLE_MOVEMENT_RANGE_SQUARED);
    final int SPIDER_BLIND_SPOT = 4;

    boolean rotateRight = Math.random() > 0.5;
    Location lastObstacleFound = null;
    int minDistToEnemy = INF;
    Location prevTarget = null;

    public void moveTo(Location target){
        if (!uc.canMove()) return;
        if (target == null) return;

        if (prevTarget == null || target.distanceSquared(prevTarget) > 9 || manager.myLocation.isEqual(target) || (uc.canSenseLocation(target) && !manager.isObstructed(target))) {
            resetPathfinding();
        }

        int d = manager.myLocation.distanceSquared(target);
        if (d <= minDistToEnemy) resetPathfinding();

        prevTarget = target;
        minDistToEnemy = Math.min(d, minDistToEnemy);

        Direction dir = manager.myLocation.directionTo(target);
        if (lastObstacleFound != null) {
            dir = manager.myLocation.directionTo(lastObstacleFound);
            if (uc.canMove(dir)) {
                resetPathfinding();
                dir = manager.myLocation.directionTo(target);
            }
        }

        for (int i = 0; i < 16; ++i){
            if (uc.canMove(dir)){
                uc.move(dir);
                break;
            }
            Location newLoc = manager.myLocation.add(dir);
            if (uc.isOutOfMap(newLoc)) {
                rotateRight = !rotateRight;
            } else {
                // Units are treated as obstacles too
                lastObstacleFound = newLoc;
            }
            if (rotateRight) dir = dir.rotateRight();
            else dir = dir.rotateLeft();
        }

        if (uc.canMove(dir)) {
            uc.move(dir);
        }
    }

    boolean rotateRightQueen = true;
    Location lastObstacleFoundQueen = null;
    int minDistToEnemyQueen = INF;
    Location prevTargetQueen = null;
    int counterQueen = 0;

    public void moveToQueen(Location target){
        if (!uc.canMove()) return;
        if (target == null) return;

        if (prevTargetQueen == null || target.distanceSquared(prevTargetQueen) > 9 || manager.myLocation.isEqual(target) || (uc.canSenseLocation(target) && !manager.isObstructed(target))) {
            resetPathfindingQueen();
        }

        int d = manager.myLocation.distanceSquared(target);
        if (d <= minDistToEnemyQueen) resetPathfindingQueen();

        prevTargetQueen = target;
        minDistToEnemyQueen = Math.min(d, minDistToEnemyQueen);

        Direction dir = manager.myLocation.directionTo(target);
        if (lastObstacleFoundQueen != null) {
            dir = manager.myLocation.directionTo(lastObstacleFoundQueen);
            if (uc.canMove(dir)) {
                resetPathfindingQueen();
                dir = manager.myLocation.directionTo(target);
            }
        }

        for (int i = 0; i < 16; ++i){
            if (uc.canMove(dir)){
                uc.move(dir);
                counterQueen = 0;
                break;
            }
            Location newLoc = manager.myLocation.add(dir);
            if (uc.isOutOfMap(newLoc)) {
                rotateRightQueen = !rotateRightQueen;
                if (rotateRightQueen) dir = dir.rotateRight();
                else dir = dir.rotateLeft();
            } else {
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

    private void resetPathfindingQueen(){
        lastObstacleFoundQueen = null;
        minDistToEnemyQueen = INF;
        counterQueen = 0;
    }

    private void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
    }

    private int threatRange(int attackRangeSquared, int movementRangeSquared) {
        double reach = Math.sqrt(attackRangeSquared) + Math.sqrt(movementRangeSquared);
        return (int) (reach * reach + 1e-9);
    }

    public void evalFoodLocation(Location foodLoc) {
        int minDistance = INF;
        Direction bestDirection = Direction.ZERO;

        for (Direction dir : manager.dirs) {
            if (!uc.canMove(dir)) continue;
            int distance = manager.myLocation.add(dir).distanceSquared(foodLoc);
            if (distance < minDistance || (distance == minDistance && dir.length() < bestDirection.length())) {
                minDistance = distance;
                bestDirection = dir;
            }
        }

        if (minDistance != INF) {
            uc.move(bestDirection);
        }
    }

    public boolean evalLocation(int allies, int enemies) {
        if (!uc.canMove()) return false;

        int numDirs = manager.dirs.length;
        MicroInfo[] microInfo = new MicroInfo[numDirs];
        boolean[] movable = new boolean[numDirs];
        for (int i = 0; i < numDirs; i++) {
            microInfo[i] = new MicroInfo(manager.dirs[i], allies, enemies);
            movable[i] = uc.canMove(manager.dirs[i]);
        }

        if (manager.myType == UnitType.QUEEN) {
            for (UnitInfo ally : manager.units) {
                for (int i = 0; i < numDirs; i++) {
                    if (movable[i]) microInfo[i].updateAlly(ally);
                }
            }
        }

        for (UnitInfo enemy : manager.enemies) {
            for (int i = 0; i < numDirs; i++) {
                if (movable[i]) microInfo[i].update(enemy);
            }
        }

        boolean obstructedEnemies = true;
        for (int i = 0; i < numDirs; i++) {
            if (!microInfo[i].obstructed) {
                obstructedEnemies = false;
                break;
            }
        }

        if (obstructedEnemies) return false;

        int bestIndex = -1;

        for (int i = numDirs - 1; i >= 0; i--) {
            if (!movable[i]) continue;
            if (bestIndex < 0 || isPreferred(microInfo[i], microInfo[bestIndex])) bestIndex = i;
        }

        if (bestIndex != -1) {
            if (manager.dirs[bestIndex] != Direction.ZERO) {
                uc.move(manager.dirs[bestIndex]);
            }
            return true;
        }
        return false;
    }

    // Ties go to the cheaper move: staying, then straight, then diagonal
    private boolean isPreferred(MicroInfo micro, MicroInfo best) {
        boolean better = micro.isBetter(best);
        if (better == best.isBetter(micro)) return micro.dir.length() < best.dir.length();
        return better;
    }

    class MicroInfo {
        int numEnemies;
        int numAnts;
        int numSpiders;
        int numBees;
        int numBeetles;
        int softAttacks;
        int minDistToEnemy;
        int minDistToSoldier;
        int minDistToSpiderAnt;
        int minDistToBeetle;
        int minDistToWoundedAlly;
        int allies;
        int enemies;
        boolean moveAndKill;
        boolean obstructed;
        boolean diagonal;
        Direction dir;
        Location loc;

        public MicroInfo(Direction dir, int allies, int enemies) {
            this.dir = dir;
            this.allies = allies;
            this.enemies = enemies;
            loc = manager.myLocation.add(dir);
            numEnemies = 0;
            numAnts = 0;
            numSpiders = 0;
            numBees = 0;
            numBeetles = 0;
            softAttacks = 0;
            minDistToEnemy = INF;
            minDistToSoldier = INF;
            minDistToSpiderAnt = INF;
            minDistToBeetle = INF;
            minDistToWoundedAlly = INF;
            moveAndKill = false;
            diagonal = false;
            obstructed = true;
        }

        void updateAlly(UnitInfo unit) {
            Location unitLoc = unit.getLocation();
            if (uc.isObstructed(loc, unitLoc)) return;
            int hp = unit.getHealth();
            if (hp < manager.unitHealth(unit.getType())) {
                int distance = loc.distanceSquared(unitLoc);
                if (distance < minDistToWoundedAlly) minDistToWoundedAlly = distance;
            }
        }

        void update(UnitInfo unit) {
            UnitType type = unit.getType();
            boolean currentObstructed = uc.isObstructed(loc, unit.getLocation());
            if (!currentObstructed) {
                obstructed = false;
                if (dir == Direction.NORTHEAST || dir == Direction.NORTHWEST || dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST) diagonal = true;
                int distance = unit.getLocation().distanceSquared(loc);
                if (type == UnitType.ANT) {
                    if (distance <= GameConstants.ANT_ATTACK_RANGE_SQUARED) {
                        numAnts++;
                    }
                    if (distance < minDistToSpiderAnt) minDistToSpiderAnt = distance;
                }
                else if (type == UnitType.SPIDER) {
                    if (distance <= GameConstants.SPIDER_ATTACK_RANGE_SQUARED && distance >= GameConstants.MIN_SPIDER_ATTACK_RANGE_SQUARED) {
                        numSpiders++;
                        numEnemies++;
                    }
                    if (manager.myType != UnitType.BEE) {
                        if (distance <= SPIDER_THREAT_RANGE && distance > SPIDER_BLIND_SPOT) softAttacks++;
                    }
                    if (distance < minDistToSpiderAnt) minDistToSpiderAnt = distance;
                    if (distance < minDistToSoldier) minDistToSoldier = distance;
                } else if (type == UnitType.BEE) {
                    if (distance <= GameConstants.BEE_ATTACK_RANGE_SQUARED) {
                        numEnemies++;
                        numBees++;
                    }
                    if (distance <= BEE_THREAT_RANGE) softAttacks++;
                    if (distance < minDistToSoldier) minDistToSoldier = distance;
                } else if (type == UnitType.BEETLE) {
                    if (distance <= GameConstants.BEETLE_ATTACK_RANGE_SQUARED) {
                        numEnemies++;
                        numBeetles++;
                    }
                    if (distance <= BEETLE_THREAT_RANGE) softAttacks++;
                    if (distance < minDistToBeetle) minDistToBeetle = distance;
                    if (distance < minDistToSoldier) minDistToSoldier = distance;
                }

                if (uc.canAttack() && inAttackRange(distance) && unit.getHealth() <= manager.myType.getAttack()) moveAndKill = true;
                if (distance < minDistToEnemy) minDistToEnemy = distance;
            }
        }

        boolean canAttack() {
            return inAttackRange(minDistToEnemy);
        }

        boolean inAttackRange(int distance) {
            return manager.myType.getAttackRangeSquared() >= distance && manager.myType.getMinAttackRangeSquared() <= distance;
        }

        boolean isBetter(MicroInfo micro) {
            if (moveAndKill && !micro.moveAndKill) return true;
            if (!moveAndKill && micro.moveAndKill) return false;
            if (manager.myType == UnitType.ANT) {
                if (obstructed) return true;
                if (micro.obstructed) return false;
                return minDistToSoldier > micro.minDistToSoldier;
            }
            if (manager.myType == UnitType.QUEEN) {
                if (minDistToWoundedAlly != INF) {
                    if (softAttacks <= 1 && micro.softAttacks <= 1) {
                        int healRange = GameConstants.QUEEN_HEALING_RANGE;
                        if (minDistToWoundedAlly <= healRange && micro.minDistToWoundedAlly > healRange) return true;
                        if (minDistToWoundedAlly > healRange && micro.minDistToWoundedAlly <= healRange) return false;
                        return minDistToWoundedAlly < micro.minDistToWoundedAlly;
                    }
                }
                if (minDistToBeetle != INF) return minDistToBeetle > micro.minDistToBeetle;
                return minDistToEnemy > micro.minDistToEnemy;
            }
            if (manager.myType == UnitType.BEE) {
                if (minDistToSpiderAnt != INF) {
                    if (minDistToSpiderAnt <= UnitType.BEE.attackRangeSquared && micro.minDistToSpiderAnt > UnitType.BEE.attackRangeSquared) return true;
                    if (minDistToSpiderAnt > UnitType.BEE.attackRangeSquared && micro.minDistToSpiderAnt <= UnitType.BEE.attackRangeSquared) return false;
                    if (minDistToSpiderAnt <= UnitType.BEE.attackRangeSquared && micro.minDistToSpiderAnt <= UnitType.BEE.attackRangeSquared) {
                        if (softAttacks == micro.softAttacks) return minDistToSpiderAnt <= micro.minDistToSpiderAnt;
                        return softAttacks < micro.softAttacks;
                    }
                    return minDistToSpiderAnt <= micro.minDistToSpiderAnt;
                }

                if (minDistToBeetle != INF) return minDistToBeetle > micro.minDistToBeetle;

                return minDistToEnemy <= micro.minDistToEnemy;
            }
            if (manager.myType != UnitType.SPIDER && allies >= enemies * 2) return minDistToEnemy <= micro.minDistToEnemy;
            if (manager.myType != UnitType.SPIDER && numSpiders != 0 && numSpiders == numEnemies) return minDistToEnemy <= micro.minDistToEnemy;
            if (softAttacks < micro.softAttacks) return true;
            if (softAttacks > micro.softAttacks) return false;

            if (manager.myType == UnitType.SPIDER) {
                if (uc.canAttack()) {
                    if (canAttack()) {
                        if (!micro.canAttack()) return true;
                        if (minDistToEnemy == micro.minDistToEnemy) {
                            return !diagonal;
                        }
                        if (softAttacks == 0 && micro.softAttacks == 0) return !diagonal;
                        return minDistToEnemy > micro.minDistToEnemy;
                    }
                    if (micro.canAttack()) return false;
                }
                if (softAttacks == 0 && micro.softAttacks == 0) return minDistToEnemy <= micro.minDistToEnemy;
                if (softAttacks != 0 && micro.softAttacks != 0) return minDistToEnemy >= micro.minDistToEnemy;
            } else {
                if (canAttack()) {
                    if (!micro.canAttack()) return true;
                    if (minDistToEnemy == micro.minDistToEnemy) {
                        return !diagonal;
                    }
                    return minDistToEnemy > micro.minDistToEnemy;
                }
                if (micro.canAttack()) return false;
            }

            return minDistToEnemy <= micro.minDistToEnemy;
        }
    }

}
