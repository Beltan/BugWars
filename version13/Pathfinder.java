package version13;

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
    // Beetles commit when our fight value is 8 times theirs
    final int STRENGTH_NUM = 16;
    final int STRENGTH_DEN = 2;
    boolean stronger;
    int tradeThreats;
    final int EXPLORE_REACHED = 2;
    final int ATTACK_ARMY_SIZE = 8;
    final int STANDOFF_ROUNDS = 8;
    // Enemies that may hit a ready beetle on the tile it attacks from
    final int TRADE_THREATS = 3;
    final int QUEEN_SAFE_DISTANCE = BEETLE_THREAT_RANGE + 1;
    // Beyond this an ally stays out of heal range from every tile the queen can step to
    final int QUEEN_ALLY_RANGE = 25;
    // Diagonal moves cost about 1.41 times the movement delay of 2
    final int EXPLORE_ROUNDS_PER_TILE = 3;
    final int EXPLORE_MARGIN = 20;
    // A target that moved farther than this restarts the obstacle following
    final int TARGET_MOVED_DISTANCE = 9;
    // Two full turns of 45 degree rotations
    final int BUG_ROTATIONS = 16;
    // Rounds the queen waits behind a unit before walking around it
    final int QUEEN_WAIT_ROUNDS = 12;
    final int QUEEN_RESET_ROUNDS = 15;
    // With fewer troops than this a hurt soldier retreats even when not outnumbered
    final int RETREAT_TROOPS = 20;

    boolean rotateRight = Math.random() > 0.5;
    Location lastObstacleFound = null;
    int minDistToTarget = INF;
    Location prevTarget = null;
    int standoffRounds = 0;
    boolean readyToAttack;
    Location armyCenter;
    MicroInfo[] microPool;
    Location homeQueen;
    float myAttack;
    int myRange;
    int myMinRange;
    int lastStandoffRound = -1;

    public void moveToObjective() {
        if (!uc.canMove()) return;
        if (!manager.isEndgame() && manager.getTotalTroops() < ATTACK_ARMY_SIZE) {
            Location gather = manager.beacon();
            if (gather != null) {
                moveTo(gather);
                return;
            }
            Location breach = manager.map.breachTarget();
            if (breach != null && manager.myType == UnitType.BEETLE) {
                if (manager.myLocation.distanceSquared(breach) > manager.myType.attackRangeSquared) moveTo(breach);
                return;
            }
            Location target = exploreTarget();
            if (target != null) {
                moveTo(target);
                return;
            }
        }
        moveToEnemyQueen();
    }

    // One target shared by all soldiers so they explore together
    private Location exploreTarget() {
        MapMemory map = manager.map;
        Location target = map.exploreTarget();
        if (target != null) {
            int distance = manager.myLocation.distanceSquared(target);
            boolean reached = distance <= EXPLORE_REACHED
                    || (distance <= manager.myType.sightRangeSquared && (uc.isOutOfMap(target) || uc.hasObstacle(target)));
            if (reached || manager.round > map.exploreDeadline() || map.lastExplored(target) > map.explorePickRound()) {
                map.markExplored(target);
                target = null;
            }
        }
        if (target == null && uc.getEnergyUsed() < manager.ENERGY_SAFETY_LIMIT) {
            target = map.stalestRegion();
            if (target != null) {
                int tiles = Math.max(Math.abs(target.x - manager.myLocation.x), Math.abs(target.y - manager.myLocation.y));
                map.setExploreTarget(target, manager.round + EXPLORE_ROUNDS_PER_TILE * tiles + EXPLORE_MARGIN);
            }
        }
        return target;
    }

    // Hurt soldiers away from the queen go back to heal unless the fight around them is won
    public void moveToObjectiveOrHome() {
        if (manager.isHurt() && (manager.getTotalTroops() < RETREAT_TROOPS || manager.isOutnumbered())) {
            // Waits in heal range until healthy, off the queen's ring where she spawns
            int distance = manager.myLocation.distanceSquared(manager.closestAllyQueen());
            if (distance > GameConstants.QUEEN_HEALING_RANGE) moveToHome();
            else if (distance <= manager.ADJACENT_DISTANCE) moveToObjective();
        } else {
            moveToObjective();
        }
    }

    public void moveToEnemyQueen() {
        if (!uc.canMove()) return;
        Direction dir = manager.map.enemyField.direction();
        if (dir != null) {
            uc.move(dir);
            return;
        }
        moveTo(manager.closestEnemyQueen());
    }

    public void moveToHome() {
        if (!uc.canMove()) return;
        Direction dir = manager.map.homeField.direction();
        if (dir != null) {
            uc.move(dir);
            return;
        }
        moveTo(manager.closestAllyQueen());
    }

    public void moveTo(Location target){
        if (!uc.canMove()) return;
        if (target == null) return;

        if (prevTarget == null || target.distanceSquared(prevTarget) > TARGET_MOVED_DISTANCE || manager.myLocation.isEqual(target) || (uc.canSenseLocation(target) && !manager.isObstructed(target))) {
            resetPathfinding();
        }

        int d = manager.myLocation.distanceSquared(target);
        if (d <= minDistToTarget) resetPathfinding();

        prevTarget = target;
        minDistToTarget = Math.min(d, minDistToTarget);

        Direction dir = manager.myLocation.directionTo(target);
        if (lastObstacleFound != null) {
            dir = manager.myLocation.directionTo(lastObstacleFound);
            if (uc.canMove(dir)) {
                resetPathfinding();
                dir = manager.myLocation.directionTo(target);
            }
        }

        for (int i = 0; i < BUG_ROTATIONS; ++i){
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
    int minDistToTargetQueen = INF;
    Location prevTargetQueen = null;
    int counterQueen = 0;

    public void moveToQueen(Location target){
        if (!uc.canMove()) return;
        if (target == null) return;

        if (prevTargetQueen == null || target.distanceSquared(prevTargetQueen) > TARGET_MOVED_DISTANCE || manager.myLocation.isEqual(target) || (uc.canSenseLocation(target) && !manager.isObstructed(target))) {
            resetPathfindingQueen();
        }

        int d = manager.myLocation.distanceSquared(target);
        if (d <= minDistToTargetQueen) resetPathfindingQueen();

        prevTargetQueen = target;
        minDistToTargetQueen = Math.min(d, minDistToTargetQueen);

        Direction dir = manager.myLocation.directionTo(target);
        if (lastObstacleFoundQueen != null) {
            dir = manager.myLocation.directionTo(lastObstacleFoundQueen);
            if (uc.canMove(dir)) {
                resetPathfindingQueen();
                dir = manager.myLocation.directionTo(target);
            }
        }

        for (int i = 0; i < BUG_ROTATIONS; ++i){
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
                UnitInfo obstacle = uc.senseUnit(newLoc);
                if (obstacle == null || obstacle.getType() == UnitType.QUEEN || counterQueen > QUEEN_WAIT_ROUNDS){
                    lastObstacleFoundQueen = newLoc;
                    if (rotateRightQueen) dir = dir.rotateRight();
                    else dir = dir.rotateLeft();
                } else {
                    counterQueen++;
                    break;
                }
                if (counterQueen > QUEEN_RESET_ROUNDS) {
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
        minDistToTargetQueen = INF;
        counterQueen = 0;
    }

    private void resetPathfinding(){
        lastObstacleFound = null;
        minDistToTarget = INF;
    }

    private Location soldierCenter() {
        int x = 0, y = 0, n = 0;
        for (UnitInfo ally : manager.units) {
            UnitType type = ally.getType();
            if (type == UnitType.QUEEN || type == UnitType.ANT) continue;
            Location l = ally.getLocation();
            x += l.x;
            y += l.y;
            n++;
        }
        return n == 0 ? null : new Location(x / n, y / n);
    }

    private int threatRange(int attackRangeSquared, int movementRangeSquared) {
        double reach = Math.sqrt(attackRangeSquared) + Math.sqrt(movementRangeSquared);
        return (int) (reach * reach + 1e-9);
    }

    public boolean threatenedNextTurn(Location loc) {
        for (UnitInfo enemy : manager.enemies) {
            UnitType type = enemy.getType();
            int distance = loc.distanceSquared(enemy.getLocation());
            if (type == UnitType.BEETLE && distance <= BEETLE_THREAT_RANGE) return true;
            if (type == UnitType.BEE && distance <= BEE_THREAT_RANGE) return true;
            if (type == UnitType.SPIDER && distance <= SPIDER_THREAT_RANGE && distance > SPIDER_BLIND_SPOT) return true;
        }
        return false;
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

    // Fight value as total health times total damage per round (doubled to stay integer), visible fighters only
    private int strength(UnitInfo[] list) {
        int health = 0;
        int damage = 0;
        if (list == manager.units) {
            health = uc.getInfo().getHealth();
            damage = doubledDps(manager.myType);
        }
        for (UnitInfo unit : list) {
            UnitType type = unit.getType();
            if (type == UnitType.QUEEN || unit.isCocoon()) continue;
            // Fighters behind a rock corner join the fight a move later
            Location loc = unit.getLocation();
            if (manager.myLocation.distanceSquared(loc) > BEETLE_THREAT_RANGE && manager.isObstructed(loc)) continue;
            health += unit.getHealth();
            damage += doubledDps(type);
        }
        return health * damage;
    }

    private int doubledDps(UnitType type) {
        return (int) (2 * type.getAttack() / type.getAttackDelay());
    }

    public boolean evalLocation() {
        if (!uc.canMove()) return false;

        // Per turn constants for MicroInfo.update, which runs once per enemy and direction
        readyToAttack = uc.canAttack();
        tradeThreats = manager.isHurt() ? 1 : TRADE_THREATS;
        armyCenter = manager.myType == UnitType.QUEEN ? soldierCenter() : null;
        homeQueen = manager.closestAllyQueen();
        myAttack = manager.myType.getAttack();
        myRange = manager.myType.getAttackRangeSquared();
        myMinRange = manager.myType.getMinAttackRangeSquared();
        stronger = manager.myType == UnitType.BEETLE && strength(manager.units) * STRENGTH_DEN
                > strength(manager.enemies) * STRENGTH_NUM;

        int numDirs = manager.dirs.length;
        // Reused between turns, allocating nine objects per call costs energy in every fight
        if (microPool == null) microPool = new MicroInfo[numDirs];
        MicroInfo[] microInfo = microPool;
        boolean[] movable = new boolean[numDirs];
        Location target = lowestHealthEnemy();
        for (int i = 0; i < numDirs; i++) {
            if (microInfo[i] == null) microInfo[i] = new MicroInfo(manager.dirs[i]);
            else microInfo[i].reset(manager.dirs[i]);
            if (target != null) microInfo[i].distToTarget = microInfo[i].loc.distanceSquared(target);
            movable[i] = uc.canMove(manager.dirs[i]);
        }

        if (manager.myType == UnitType.QUEEN) {
            for (UnitInfo ally : manager.units) {
                // Skips the costly obstruction checks for allies she could not heal
                if (ally.getHealth() >= ally.getType().maxHealth) continue;
                if (manager.myLocation.distanceSquared(ally.getLocation()) > QUEEN_ALLY_RANGE) continue;
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
        if (isStandoff(microInfo, movable)) return false;

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

    // No enemy reachable this turn: hurt soldiers leave at once, healthy ones after a few rounds
    private boolean isStandoff(MicroInfo[] microInfo, boolean[] movable) {
        if (manager.myType == UnitType.QUEEN || manager.myType == UnitType.ANT) return false;
        if (manager.round != lastStandoffRound + 1) standoffRounds = 0;
        lastStandoffRound = manager.round;
        for (int i = 0; i < microInfo.length; i++) {
            if (movable[i] && (microInfo[i].hasTarget || microInfo[i].moveAndKill)) {
                standoffRounds = 0;
                return false;
            }
        }
        standoffRounds++;
        return manager.isHurt() || standoffRounds > STANDOFF_ROUNDS;
    }

    // Ties go to the cheaper move: staying, then straight, then diagonal
    private boolean isPreferred(MicroInfo micro, MicroInfo best) {
        boolean better = micro.isBetter(best);
        if (better == best.isBetter(micro)) return micro.dir.length() < best.dir.length();
        return better;
    }

    private Location lowestHealthEnemy() {
        UnitInfo lowest = null;
        for (UnitInfo enemy : manager.enemies) {
            if (lowest == null || enemy.getHealth() < lowest.getHealth()) lowest = enemy;
        }
        return lowest == null ? null : lowest.getLocation();
    }

    class MicroInfo {
        int numEnemies;
        int numAnts;
        int numSpiders;
        int softAttacks;
        int minDistToEnemy;
        int minDistToSoldier;
        int minDistToSpiderAnt;
        int minDistToBeetle;
        int minDistToWoundedAlly;
        int distToTarget = INF;
        boolean moveAndKill;
        boolean hasTarget;
        boolean obstructed;
        boolean diagonal;
        boolean diagonalDir;
        Direction dir;
        Location loc;

        public MicroInfo(Direction dir) {
            reset(dir);
        }

        void reset(Direction dir) {
            this.dir = dir;
            hasTarget = false;
            distToTarget = INF;
            loc = manager.myLocation.add(dir);
            numEnemies = 0;
            numAnts = 0;
            numSpiders = 0;
            softAttacks = 0;
            minDistToEnemy = INF;
            minDistToSoldier = INF;
            minDistToSpiderAnt = INF;
            minDistToBeetle = INF;
            minDistToWoundedAlly = INF;
            moveAndKill = false;
            diagonal = false;
            diagonalDir = dir.dx != 0 && dir.dy != 0;
            obstructed = true;
        }

        // Callers only pass wounded allies
        void updateAlly(UnitInfo unit) {
            Location unitLoc = unit.getLocation();
            int distance = loc.distanceSquared(unitLoc);
            if (distance >= minDistToWoundedAlly || uc.isObstructed(loc, unitLoc)) return;
            minDistToWoundedAlly = distance;
        }

        void update(UnitInfo unit) {
            UnitType type = unit.getType();
            Location unitLoc = unit.getLocation();
            if (!uc.isObstructed(loc, unitLoc)) {
                obstructed = false;
                if (diagonalDir) diagonal = true;
                int distance = unitLoc.distanceSquared(loc);
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
                    }
                    if (distance <= BEE_THREAT_RANGE) softAttacks++;
                    if (distance < minDistToSoldier) minDistToSoldier = distance;
                } else if (type == UnitType.BEETLE) {
                    if (distance <= GameConstants.BEETLE_ATTACK_RANGE_SQUARED) {
                        numEnemies++;
                    }
                    if (distance <= BEETLE_THREAT_RANGE) softAttacks++;
                    if (distance < minDistToBeetle) minDistToBeetle = distance;
                    if (distance < minDistToSoldier) minDistToSoldier = distance;
                }

                if (inAttackRange(distance)) {
                    hasTarget = true;
                    if (readyToAttack && unit.getHealth() <= myAttack) moveAndKill = true;
                }
                if (distance < minDistToEnemy) minDistToEnemy = distance;
            }
        }

        boolean canAttack() {
            return inAttackRange(minDistToEnemy);
        }

        boolean inAttackRange(int distance) {
            return myRange >= distance && myMinRange <= distance;
        }

        boolean isBetter(MicroInfo micro) {
            if (moveAndKill && !micro.moveAndKill) return true;
            if (!moveAndKill && micro.moveAndKill) return false;
            if (manager.myType == UnitType.ANT) {
                if (obstructed) return true;
                if (micro.obstructed) return false;
                // Run towards the queen, fleeing straight away corners the ant at the map edge
                if (softAttacks != micro.softAttacks) return softAttacks < micro.softAttacks;
                int home = loc.distanceSquared(homeQueen);
                int microHome = micro.loc.distanceSquared(homeQueen);
                if (home != microHome) return home < microHome;
                return minDistToSoldier > micro.minDistToSoldier;
            }
            if (manager.myType == UnitType.QUEEN) {
                if (manager.isEndgame()) return minDistToEnemy > micro.minDistToEnemy;
                if (minDistToWoundedAlly != INF) {
                    if (softAttacks <= 1 && micro.softAttacks <= 1) {
                        int healRange = GameConstants.QUEEN_HEALING_RANGE;
                        if (minDistToWoundedAlly <= healRange && micro.minDistToWoundedAlly > healRange) return true;
                        if (minDistToWoundedAlly > healRange && micro.minDistToWoundedAlly <= healRange) return false;
                        return minDistToWoundedAlly < micro.minDistToWoundedAlly;
                    }
                }
                if (minDistToBeetle != INF) {
                    // Beyond a beetle's reach extra distance buys nothing and drives the queen into corners
                    int safe = Math.min(minDistToBeetle, QUEEN_SAFE_DISTANCE);
                    int microSafe = Math.min(micro.minDistToBeetle, QUEEN_SAFE_DISTANCE);
                    if (safe != microSafe) return safe > microSafe;
                    if (safe < QUEEN_SAFE_DISTANCE) return minDistToBeetle > micro.minDistToBeetle;
                }
                if (armyCenter != null) {
                    int army = loc.distanceSquared(armyCenter);
                    int microArmy = micro.loc.distanceSquared(armyCenter);
                    if (army != microArmy) return army < microArmy;
                }
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
            if (stronger) {
                boolean inRange = inAttackRange(distToTarget);
                if (inRange != micro.inAttackRange(micro.distToTarget)) return inRange;
                return distToTarget <= micro.distToTarget;
            }
            if (manager.myType != UnitType.SPIDER && numSpiders != 0 && numSpiders == numEnemies) return minDistToEnemy <= micro.minDistToEnemy;
            if (manager.myType == UnitType.BEETLE && !readyToAttack) {
                int threats = numEnemies + numAnts;
                int microThreats = micro.numEnemies + micro.numAnts;
                if (threats != microThreats) return threats < microThreats;
            }
            // A ready beetle trades: being able to hit beats dodging up to three enemies
            if (manager.myType == UnitType.BEETLE && readyToAttack && canAttack() != micro.canAttack()
                    && Math.max(softAttacks, micro.softAttacks) <= tradeThreats) return canAttack();
            if (softAttacks < micro.softAttacks) return true;
            if (softAttacks > micro.softAttacks) return false;

            if (manager.myType == UnitType.SPIDER) {
                // Keep some enemy in range, even while reloading, and never close into the blind spot
                if (hasTarget != micro.hasTarget) return hasTarget;
                if (hasTarget) {
                    if (minDistToEnemy == micro.minDistToEnemy || (softAttacks == 0 && micro.softAttacks == 0)) return !diagonal;
                    return minDistToEnemy > micro.minDistToEnemy;
                }
                int minRange = GameConstants.MIN_SPIDER_ATTACK_RANGE_SQUARED;
                boolean blind = minDistToEnemy < minRange;
                if (blind != (micro.minDistToEnemy < minRange)) return !blind;
                if (blind) return minDistToEnemy >= micro.minDistToEnemy;
                return minDistToEnemy <= micro.minDistToEnemy;
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
