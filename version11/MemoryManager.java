package version11;

import bugwars.user.*;

public class MemoryManager {

    public UnitController uc;
    public boolean root;
    public int round;
    public int resources;
    public Team opponent;
    public Team allies;
    public Direction[] dirs;
    public Location myLocation;
    public UnitType myType;
    public UnitInfo[] units;
    public UnitInfo[] enemies;
    public FoodInfo[] food;
    public UnitInfo[] cocoon;
    public Location bestFood;
    public int bestFoodHealth;
    public int foodCount;
    public int foodHealthSum;
    public int maxFoodSum;
    public UnitType objective;
    private boolean edgesKnown = false;
    public RockInfo[] rocks;
    public Pathfinder path;
    public MapMemory map;
    public EnemyTracker tracker;
    public int myId;
    public int enemyBeetles;
    public int enemySpiders;
    public int enemyBees;
    public int allyBeetles;
    public int allySpiders;
    public int allyBees;

    // Instance fields, static variables are not allowed
    public final int INF = 1000000;
    public final int MOVE_DIRECTIONS = 8;
    public final int ADJACENT_DISTANCE = 2;
    public final int ENERGY_SAFETY_LIMIT = GameConstants.MAX_BYTECODES - 4000;
    public final int QUEEN_DANGER_DISTANCE = 200;
    public final int PASSIVE_ROUNDS = 15;
    public final int SOLDIER_ROUND_MARGIN = 30;
    public final int COCOON_EXPIRY_MARGIN = 2;
    public final int MAX_ANTS_PER_QUEEN = 20;
    public final int WALK_DETOUR = 2;
    public final int WALK_SLACK = 2;
    public final int MAX_START_QUEENS = 3;
    // Contested when the enemy start is at most about 20% farther: squared distances compared as 100 : 144
    public final int CONTESTED_SCALE = 100;
    public final int CONTESTED_MARGIN = 144;
    public final int SOLDIER_FOOD_REPORT_PERIOD = 3;
    // After the opening, ants are allowed while soldiers outnumber ants by this ratio
    public final double ANT_TROOP_RATIO = 1.0;
    public final int ENDGAME_ROUNDS = 150;

    // 0 to 99 are general information and states
    public int PREVIOUS_ROUND = 0;
    public int CURRENT_MAP_SIZE = 2;
    public int FINAL_MAP_SIZE = 3;
    public int TEN_XCOORDINATE_CENTER = 4;
    public int TEN_YCOORDINATE_CENTER = 5;
    public int XHIGHER_BOUND = 6;
    public int XLOWER_BOUND = 7;
    public int YHIGHER_BOUND = 8;
    public int YLOWER_BOUND = 9;
    public int XHIGHER_FINAL = 10;
    public int XLOWER_FINAL = 11;
    public int YHIGHER_FINAL = 12;
    public int YLOWER_FINAL = 13;
    public int ANTS_PREVIOUS = 14;
    public int ANTS_CURRENT = 15;
    public int ANTS_COCOON = 16;
    public int BEES_PREVIOUS = 17;
    public int BEES_CURRENT = 18;
    public int BEES_COCOON = 19;
    public int BEETLES_PREVIOUS = 20;
    public int BEETLES_CURRENT = 21;
    public int BEETLES_COCOON = 22;
    public int SPIDERS_PREVIOUS = 23;
    public int SPIDERS_CURRENT = 24;
    public int SPIDERS_COCOON = 25;
    public int XIDLE_FOOD = 26;
    public int YIDLE_FOOD = 27;
    public int IDLE_FOOD_HEALTH = 28;
    public int ENEMY_SPOTTED = 29;
    public int ENEMY_SEEN_LAST_ROUND = 30;
    public int SPAWN_SOLDIERS_ROUND = 31;
    public int XQUEEN_ALLOWED_SOLDIER = 32;
    public int YQUEEN_ALLOWED_SOLDIER = 33;
    public int XIDLE_FOOD_OBS = 34;
    public int YIDLE_FOOD_OBS = 35;
    public int IDLE_FOOD_HEALTH_OBS = 36;
    public int PASSIVE = 39;
    public int PASSIVE_COUNTER = 40;
    public int QUEEN_SEES_ENEMY = 41;
    public int QUEEN_SEES_ENEMY_CURRENT = 42;
    public int ENEMY_QUEEN_COUNT = 43;
    public int ENEMY_QUEENS_MOVED = 44;
    public int XBEACON = 46;
    public int YBEACON = 47;
    public int BEACON_ROUND = 48;
    public int XROOM = 37;
    public int YROOM = 38;
    public int ROOM_ROUND = 45;

    // 100 to 129 are cocoon IDs, 200 to 229 their hatching round and 230 to 259 their counter slot
    public int INITIAL_COCOON_LIST = 100;
    public int FINAL_COCOON_LIST = 129;
    public int COCOON_HATCH_ROUND_OFFSET = 100;
    public int COCOON_COUNTER_OFFSET = 130;

    // 160 to 169 are previous enemy Queen positions
    public int PREVIOUS_ENEMY_QUEENS = 160;
    public int OUR_STARTS = 300;
    public int ENEMY_STARTS = 306;

    public MemoryManager(UnitController uc) {
        this.uc = uc;
        round = uc.getRound();
        resources = uc.getResources();
        opponent = uc.getOpponent();
        allies = uc.getTeam();
        dirs = Direction.values();
        myLocation = uc.getLocation();
        myType = uc.getType();
        units = uc.senseUnits(allies);
        enemies = uc.senseUnits(opponent);
        food = uc.senseFood();
        bestFood = null;
        bestFoodHealth = 0;
        cocoon = new UnitInfo[10];
        objective = UnitType.ANT;
        rocks = uc.senseObstacles();
        path = new Pathfinder(this);
        map = new MapMemory(this);
        tracker = new EnemyTracker(this);
        myId = uc.getInfo().getID();
    }

    public void update() {
        round = uc.getRound();
        resources = uc.getResources();
        myLocation = uc.getLocation();
        units = uc.senseUnits(allies);
        enemies = uc.senseUnits(opponent);
        food = uc.senseFood();
        rocks = uc.senseObstacles();

        // Only the queen's spawn rules read these counts
        if (myType == UnitType.QUEEN) countVisibleTypes();

        tracker.record();
        map.recordRocks();

        // The first unit to act each round does the once-per-round shared work
        int roundStamp = round + 1;
        if (uc.read(PREVIOUS_ROUND) != roundStamp) {
            uc.write(PREVIOUS_ROUND, roundStamp);
            root = true;
        } else {
            root = false;
        }

        if (root) {
            rootUpdate();
        }

        mapLimits();
        if (uc.read(FINAL_MAP_SIZE) == 0) {
            mapSizeUpdate();
        }

        if (myType == UnitType.ANT) {
            uc.write(ANTS_CURRENT, uc.read(ANTS_CURRENT) + 1);
        } else if (myType == UnitType.BEE) {
            uc.write(BEES_CURRENT, uc.read(BEES_CURRENT) + 1);
        } else if (myType == UnitType.BEETLE) {
            uc.write(BEETLES_CURRENT, uc.read(BEETLES_CURRENT) + 1);
        } else if (myType == UnitType.SPIDER) {
            uc.write(SPIDERS_CURRENT, uc.read(SPIDERS_CURRENT) + 1);
        }

        // Soldiers report food every few rounds only, the shared idle food rarely needs a faster refresh
        if (myType == UnitType.QUEEN || myType == UnitType.ANT || (enemies.length == 0 && (round + myId) % SOLDIER_FOOD_REPORT_PERIOD == 0)) {
            int idleFoodHealth = getIdleFoodHealth();
            Location idleFoodLocation = getIdleFoodLocation();
            int xLoc = idleFoodLocation.x;
            int yLoc = idleFoodLocation.y;

            if (isIdleFoodTaken(idleFoodLocation)) {
                idleFoodHealth = 0;
            } else {
                for (FoodInfo foodUnit : food) {
                    if (uc.getEnergyUsed() > ENERGY_SAFETY_LIMIT) break;
                    if (foodUnit.food == foodUnit.initialFood && idleFoodHealth < foodUnit.food) {
                        idleFoodHealth = foodUnit.food;
                        xLoc = foodUnit.location.x;
                        yLoc = foodUnit.location.y;
                    }
                }
            }

            if (idleFoodHealth == 0) {
                uc.write(IDLE_FOOD_HEALTH, 0);
                uc.write(XIDLE_FOOD, 0);
                uc.write(YIDLE_FOOD, 0);
            } else if (idleFoodHealth != getIdleFoodHealth()) {
                uc.write(IDLE_FOOD_HEALTH, idleFoodHealth);
                uc.write(XIDLE_FOOD, xLoc);
                uc.write(YIDLE_FOOD, yLoc);
            }

            int idleFoodHealthNotObs = getIdleFoodHealthNotObs();
            Location idleFoodLocationNotObs = getIdleFoodLocationNotObs();
            int xLocNotObs = idleFoodLocationNotObs.x;
            int yLocNotObs = idleFoodLocationNotObs.y;

            if (isIdleFoodTaken(idleFoodLocationNotObs)) {
                idleFoodHealthNotObs = 0;
            } else {
                for (FoodInfo foodUnit : food) {
                    if (uc.getEnergyUsed() > ENERGY_SAFETY_LIMIT) break;
                    if (foodUnit.food == foodUnit.initialFood && idleFoodHealthNotObs < foodUnit.food && !isObstructed(foodUnit.location)) {
                        idleFoodHealthNotObs = foodUnit.food;
                        xLocNotObs = foodUnit.location.x;
                        yLocNotObs = foodUnit.location.y;
                    }
                }
            }

            if (idleFoodHealthNotObs == 0) {
                uc.write(IDLE_FOOD_HEALTH_OBS, 0);
                uc.write(XIDLE_FOOD_OBS, 0);
                uc.write(YIDLE_FOOD_OBS, 0);
            } else if (idleFoodHealthNotObs != getIdleFoodHealthNotObs()) {
                uc.write(IDLE_FOOD_HEALTH_OBS, idleFoodHealthNotObs);
                uc.write(XIDLE_FOOD_OBS, xLocNotObs);
                uc.write(YIDLE_FOOD_OBS, yLocNotObs);
            }
        }

        Location sighting = null;
        for (UnitInfo enemy : enemies) {
            if (!isObstructed(enemy.getLocation())) {
                sighting = enemy.getLocation();
                break;
            }
        }
        if (sighting != null) {
            uc.write(ENEMY_SPOTTED, 1);
            if (myType == UnitType.QUEEN || uc.read(BEACON_ROUND) != round + 1) {
                uc.write(XBEACON, sighting.x);
                uc.write(YBEACON, sighting.y);
                uc.write(BEACON_ROUND, round + 1);
            }
        } else {
            Location gather = beacon();
            if (gather != null && uc.canSenseLocation(gather)) uc.write(BEACON_ROUND, 0);
        }
        map.markSeenRegions();

        if (enemies.length != 0) {
            uc.write(ENEMY_SEEN_LAST_ROUND, 1);
        }

        if (myType == UnitType.QUEEN) {
            if (enemies.length != 0 && !allObstructed()) {
                uc.write(QUEEN_SEES_ENEMY_CURRENT, 1);
            }
            updateObjective();
        }
    }

    private boolean isIdleFoodTaken(Location foodLocation) {
        if (myLocation.isEqual(foodLocation)) return true;
        if (!isSet(foodLocation) || !uc.canSenseLocation(foodLocation)) return false;
        if (uc.senseUnit(foodLocation) != null) return true;
        FoodInfo foodInfo = uc.senseFoodAtLocation(foodLocation);
        return foodInfo == null || foodInfo.food < foodInfo.initialFood;
    }

    private void rootUpdate() {
        if (round == 0) {
            roundZeroRootInitialization();
        }

        expireCocoons();

        tracker.recount();

        uc.write(ANTS_PREVIOUS, uc.read(ANTS_CURRENT) + uc.read(ANTS_COCOON));
        uc.write(ANTS_CURRENT, 0);

        uc.write(BEES_PREVIOUS, uc.read(BEES_CURRENT) + uc.read(BEES_COCOON));
        uc.write(BEES_CURRENT, 0);

        uc.write(BEETLES_PREVIOUS, uc.read(BEETLES_CURRENT) + uc.read(BEETLES_COCOON));
        uc.write(BEETLES_CURRENT, 0);

        uc.write(SPIDERS_PREVIOUS, uc.read(SPIDERS_CURRENT) + uc.read(SPIDERS_COCOON));
        uc.write(SPIDERS_CURRENT, 0);

        soldierRoundSpawn();

        uc.write(ENEMY_SEEN_LAST_ROUND, 0);

        uc.write(QUEEN_SEES_ENEMY, uc.read(QUEEN_SEES_ENEMY_CURRENT));
        uc.write(QUEEN_SEES_ENEMY_CURRENT, 0);

        Location[] enemyQueens = uc.getEnemyQueensLocation();
        boolean moved = false;
        if (round != 0 && uc.read(ENEMY_QUEEN_COUNT) == enemyQueens.length) {
            for (int i = 0; i < enemyQueens.length; i++) {
                if (!getPreviousEnemyQueenLocation(i).isEqual(enemyQueens[i])) {
                    moved = true;
                    break;
                }
            }
        }
        uc.write(ENEMY_QUEEN_COUNT, enemyQueens.length);
        for (int i = 0; i < enemyQueens.length; i++) {
            uc.write(PREVIOUS_ENEMY_QUEENS + i * 2, enemyQueens[i].x);
            uc.write(PREVIOUS_ENEMY_QUEENS + 1 + i * 2, enemyQueens[i].y);
        }

        if (moved) {
            uc.write(ENEMY_QUEENS_MOVED, 1);
            uc.write(PASSIVE_COUNTER, 0);
        } else {
            uc.write(PASSIVE_COUNTER, getPassiveCounter() + 1);
        }

        if (uc.read(ENEMY_QUEENS_MOVED) == 0 || getPassiveCounter() > PASSIVE_ROUNDS) {
            uc.write(PASSIVE, 1);
        } else {
            uc.write(PASSIVE, 0);
        }
    }

    public void roundZeroRootInitialization() {
        Location[] myQueens = uc.getMyQueensLocation();
        Location[] enemyQueens = uc.getEnemyQueensLocation();
        for (int i = 0; i < myQueens.length && i < MAX_START_QUEENS; i++) {
            uc.write(OUR_STARTS + 2 * i, myQueens[i].x);
            uc.write(OUR_STARTS + 2 * i + 1, myQueens[i].y);
        }
        for (int i = 0; i < enemyQueens.length && i < MAX_START_QUEENS; i++) {
            uc.write(ENEMY_STARTS + 2 * i, enemyQueens[i].x);
            uc.write(ENEMY_STARTS + 2 * i + 1, enemyQueens[i].y);
        }

        int xLow = myQueens[0].x;
        int yLow = myQueens[0].y;
        int xHigh = myQueens[0].x;
        int yHigh = myQueens[0].y;

        for (Location queen : myQueens) {
            if (xHigh < queen.x) xHigh = queen.x;
            if (xLow > queen.x) xLow = queen.x;
            if (yHigh < queen.y) yHigh = queen.y;
            if (yLow > queen.y) yLow = queen.y;
        }

        for (Location queen : enemyQueens) {
            if (xHigh < queen.x) xHigh = queen.x;
            if (xLow > queen.x) xLow = queen.x;
            if (yHigh < queen.y) yHigh = queen.y;
            if (yLow > queen.y) yLow = queen.y;
        }

        uc.write(XHIGHER_BOUND, xHigh);
        uc.write(XLOWER_BOUND, xLow);
        uc.write(YHIGHER_BOUND, yHigh);
        uc.write(YLOWER_BOUND, yLow);

        // Which symmetries map our starting queens onto theirs
        int xDoubleCenter = xLow + xHigh;
        int yDoubleCenter = yLow + yHigh;
        boolean mirrorX = true;
        boolean mirrorY = true;
        boolean rotation = true;

        for (Location queen : myQueens) {
            if (!containsLocation(enemyQueens, xDoubleCenter - queen.x, queen.y)) mirrorX = false;
            if (!containsLocation(enemyQueens, queen.x, yDoubleCenter - queen.y)) mirrorY = false;
            if (!containsLocation(enemyQueens, xDoubleCenter - queen.x, yDoubleCenter - queen.y)) rotation = false;
        }

        int symmetry = 0;
        if (mirrorX) symmetry += map.SYMMETRY_MIRROR_X;
        if (mirrorY) symmetry += map.SYMMETRY_MIRROR_Y;
        if (rotation) symmetry += map.SYMMETRY_ROTATION;
        uc.write(map.SYMMETRY, symmetry);

        // An axis center is only known when every symmetry that fits mirrors that axis
        if ((mirrorX || rotation) && !mirrorY) {
            uc.write(TEN_XCOORDINATE_CENTER, 5 * xDoubleCenter);
        }
        if ((mirrorY || rotation) && !mirrorX) {
            uc.write(TEN_YCOORDINATE_CENTER, 5 * yDoubleCenter);
        }
    }

    private boolean containsLocation(Location[] locations, int x, int y) {
        for (Location location : locations) {
            if (location.x == x && location.y == y) return true;
        }
        return false;
    }

    private void countVisibleTypes() {
        enemyBeetles = 0;
        enemySpiders = 0;
        enemyBees = 0;
        for (UnitInfo enemy: enemies) {
            UnitType type = enemy.getType();
            if (type == UnitType.BEETLE) enemyBeetles++;
            if (type == UnitType.SPIDER) enemySpiders++;
            if (type == UnitType.BEE) enemyBees++;
        }

        allyBeetles = 0;
        allySpiders = 0;
        allyBees = 0;
        for (UnitInfo ally: units) {
            UnitType type = ally.getType();
            if (type == UnitType.BEETLE) allyBeetles++;
            if (type == UnitType.SPIDER) allySpiders++;
            if (type == UnitType.BEE) allyBees++;
        }
    }

    public void mapLimits() {
        if (edgesKnown) return;
        edgesKnown = uc.read(XHIGHER_FINAL) != 0 && uc.read(XLOWER_FINAL) != 0 && uc.read(YHIGHER_FINAL) != 0 && uc.read(YLOWER_FINAL) != 0;
        findMapEdge(1, 0, XHIGHER_FINAL, XLOWER_FINAL, TEN_XCOORDINATE_CENTER);
        findMapEdge(-1, 0, XLOWER_FINAL, XHIGHER_FINAL, TEN_XCOORDINATE_CENTER);
        findMapEdge(0, 1, YHIGHER_FINAL, YLOWER_FINAL, TEN_YCOORDINATE_CENTER);
        findMapEdge(0, -1, YLOWER_FINAL, YHIGHER_FINAL, TEN_YCOORDINATE_CENTER);
    }

    // The opposite edge is the mirror image when the axis is symmetric
    private void findMapEdge(int dx, int dy, int slot, int oppositeSlot, int centerSlot) {
        if (uc.read(slot) != 0) return;
        int range = unitRange();
        if (!uc.isOutOfMap(new Location(myLocation.x + dx * range, myLocation.y + dy * range))) return;

        for (int i = 1; i <= range; i++) {
            Location newLoc = new Location(myLocation.x + dx * i, myLocation.y + dy * i);
            if (uc.isOutOfMap(newLoc)) {
                int edge = dx != 0 ? newLoc.x - dx : newLoc.y - dy;
                uc.write(slot, edge);
                int doubleCenter = uc.read(centerSlot) / 5;
                if (doubleCenter != 0 && uc.read(oppositeSlot) == 0) uc.write(oppositeSlot, doubleCenter - edge);
                return;
            }
        }
    }

    public void mapSizeUpdate() {
        int xCenter = uc.read(TEN_XCOORDINATE_CENTER);
        int yCenter = uc.read(TEN_YCOORDINATE_CENTER);
        int xLowFinal = uc.read(XLOWER_FINAL);
        int yLowFinal = uc.read(YLOWER_FINAL);
        int xHighFinal = uc.read(XHIGHER_FINAL);
        int yHighFinal = uc.read(YHIGHER_FINAL);

        double coordinate;
        if (xCenter != 0 && (xLowFinal != 0 || xHighFinal != 0)) {
            coordinate = 2 * Math.abs((xCenter / 10.0) - Math.max(xLowFinal, xHighFinal)) + 1;
            uc.write(FINAL_MAP_SIZE, (int) coordinate);
        } else if (yCenter != 0 && (yLowFinal != 0 || yHighFinal != 0)) {
            coordinate = 2 * Math.abs((yCenter / 10.0) - Math.max(yLowFinal, yHighFinal)) + 1;
            uc.write(FINAL_MAP_SIZE, (int) coordinate);
        } else {
            int xLow = uc.read(XLOWER_BOUND);
            int yLow = uc.read(YLOWER_BOUND);
            int xHigh = uc.read(XHIGHER_BOUND);
            int yHigh = uc.read(YHIGHER_BOUND);

            if (myLocation.x > xHigh) {
                xHigh = myLocation.x;
                uc.write(XHIGHER_BOUND, xHigh);
            }
            if (myLocation.y > yHigh) {
                yHigh = myLocation.y;
                uc.write(YHIGHER_BOUND, yHigh);
            }
            if (myLocation.x < xLow) {
                xLow = myLocation.x;
                uc.write(XLOWER_BOUND, xLow);
            }
            if (myLocation.y < yLow) {
                yLow = myLocation.y;
                uc.write(YLOWER_BOUND, yLow);
            }

            uc.write(CURRENT_MAP_SIZE, Math.max(xHigh - xLow, yHigh - yLow));
        }
    }

    private int unitRange() {
        int range = 0;

        if (myType == UnitType.QUEEN) {
            range = (int) Math.sqrt(GameConstants.QUEEN_SIGHT_RANGE_SQUARED);
        } else if (myType == UnitType.ANT) {
            range = (int) Math.sqrt(GameConstants.ANT_SIGHT_RANGE_SQUARED);
        } else if (myType == UnitType.BEE) {
            range = (int) Math.sqrt(GameConstants.BEE_SIGHT_RANGE_SQUARED);
        } else if (myType == UnitType.BEETLE) {
            range = (int) Math.sqrt(GameConstants.BEETLE_SIGHT_RANGE_SQUARED);
        } else if (myType == UnitType.SPIDER) {
            range = (int) Math.sqrt(GameConstants.SPIDER_SIGHT_RANGE_SQUARED);
        }

        return range;
    }

    public int unitHealth(UnitType ally) {
        int health = 0;

        if (ally == UnitType.QUEEN) {
            health = GameConstants.QUEEN_MAX_HEALTH;
        } else if (ally == UnitType.ANT) {
            health = GameConstants.ANT_MAX_HEALTH;
        } else if (ally == UnitType.BEE) {
            health = GameConstants.BEE_MAX_HEALTH;
        } else if (ally == UnitType.BEETLE) {
            health = GameConstants.BEETLE_MAX_HEALTH;
        } else if (ally == UnitType.SPIDER) {
            health = GameConstants.SPIDER_MAX_HEALTH;
        }

        return health;
    }

    public Direction[] shuffle(Direction[] list) {
        Direction[] shuffledList = new Direction[MOVE_DIRECTIONS];
        int random;

        for (int i = 0; i < MOVE_DIRECTIONS; i++) {
            random = (int )(Math.random() * MOVE_DIRECTIONS);
            if (shuffledList[random] == null) {
                shuffledList[random] = list[i];
            } else {
                for (int j = 0; j < MOVE_DIRECTIONS; j++) {
                    if (shuffledList[(random + j) % MOVE_DIRECTIONS] == null) {
                        shuffledList[(random + j) % MOVE_DIRECTIONS] = list[i];
                        break;
                    }
                }
            }
        }

        return shuffledList;
    }

    // Food behind a thin wall: the walk around it is short compared with the straight line
    private boolean isShortWalk(Location loc) {
        int path = map.homeField.distance(loc);
        if (path < 0) return false;
        int straight = Math.max(Math.abs(loc.x - myLocation.x), Math.abs(loc.y - myLocation.y));
        return path <= map.homeField.stepCost() * (WALK_DETOUR * straight + WALK_SLACK);
    }

    public void scanFood() {
        bestFood = null;
        bestFoodHealth = 0;
        foodHealthSum = 0;
        maxFoodSum = 0;
        foodCount = 0;

        for (FoodInfo foodUnit : food) {
            if (!myLocation.isEqual(foodUnit.location)) {
                if (uc.getEnergyUsed() > ENERGY_SAFETY_LIMIT) break;
                if (!isObstructed(foodUnit.location) || isShortWalk(foodUnit.location)) {
                    foodHealthSum += foodUnit.food;
                    maxFoodSum += foodUnit.initialFood;
                    foodCount++;
                    // Contested food is never full again once enemy ants mine it, half full is still worth an ant
                    if (foodUnit.food * 2 >= foodUnit.initialFood && bestFoodHealth < foodUnit.food) {
                        bestFood = foodUnit.location;
                        bestFoodHealth = foodUnit.food;
                    }
                }
            }
        }
    }

    public boolean canSpawnAnt() {
        if (bestFood == null) return false;

        int antCount = 0;
        int cocoonAnts = 0;
        for (UnitInfo unit : units) {
            if (unit.getType() == UnitType.ANT && !isObstructed(unit.getLocation())) {
                if (unit.isCocoon()) {
                    cocoonAnts++;
                    continue;
                }
                antCount++;
            }
        }

        return (antCount + cocoonAnts < MAX_ANTS_PER_QUEEN && (((objective == UnitType.ANT && getSpawnSoldiersRound() > round) ||
                ((getTotalTroops() > ANT_TROOP_RATIO * getAnts() || getTotalTroops() > 40) && (enemies.length == 0 || allObstructed()))) &&
                (myLocation.distanceSquared(closestEnemyQueen()) > QUEEN_DANGER_DISTANCE || getTotalTroops() > 5 || !isContested(bestFood)) &&
                (foodCount != 1 || foodHealthSum * 2 >= maxFoodSum) && (enemies.length == 0 || allObstructed()) &&
                ((foodHealthSum * 1.5 > maxFoodSum && antCount * 2.9 + 4 * cocoonAnts < foodCount) ||
                (foodHealthSum * 1.3 > maxFoodSum && antCount * 2.4 + 4 * cocoonAnts < foodCount) ||
                (foodHealthSum * 1.15 > maxFoodSum && antCount * 1.9 + 4 * cocoonAnts < foodCount) ||
                (foodHealthSum * 1.1 > maxFoodSum && antCount * 1.5 + 4 * cocoonAnts < foodCount))));
    }

    // The food part of canSpawnAnt
    public boolean localFoodHasRoom() {
        int antCount = 0;
        int cocoonAnts = 0;
        for (UnitInfo unit : units) {
            if (unit.getType() == UnitType.ANT && !isObstructed(unit.getLocation())) {
                if (unit.isCocoon()) cocoonAnts++;
                else antCount++;
            }
        }
        return (foodCount != 1 || maxFoodSum == foodHealthSum) &&
                ((foodHealthSum * 1.5 > maxFoodSum && antCount * 2.9 + 4 * cocoonAnts < foodCount) ||
                (foodHealthSum * 1.3 > maxFoodSum && antCount * 2.4 + 4 * cocoonAnts < foodCount) ||
                (foodHealthSum * 1.15 > maxFoodSum && antCount * 1.9 + 4 * cocoonAnts < foodCount) ||
                (foodHealthSum * 1.1 > maxFoodSum && antCount * 1.5 + 4 * cocoonAnts < foodCount));
    }

    // Food not clearly on our side of the map: an unescorted ant there is likely lost
    public boolean isContested(Location foodLocation) {
        int ours = closestStart(foodLocation, OUR_STARTS);
        int theirs = closestStart(foodLocation, ENEMY_STARTS);
        return theirs * CONTESTED_SCALE <= ours * CONTESTED_MARGIN;
    }

    private int closestStart(Location loc, int base) {
        int best = INF;
        for (int i = 0; i < MAX_START_QUEENS; i++) {
            int x = uc.read(base + 2 * i);
            if (x == 0) break;
            int d = loc.distanceSquared(new Location(x, uc.read(base + 2 * i + 1)));
            if (d < best) best = d;
        }
        return best;
    }

    // The danger rule keeps ants away from this food: whoever got here second should look elsewhere
    public boolean antsBlockedByDanger() {
        return bestFood != null && myLocation.distanceSquared(closestEnemyQueen()) <= QUEEN_DANGER_DISTANCE
                && getTotalTroops() <= 5 && isContested(bestFood);
    }

    public boolean canSpawnBeetle() {
        return ((((enemyBeetles + enemyBees > allyBeetles) && !allObstructed()) ||
                (getPassive() == 0 && (getBeetles() * 2 <= getSpiders()))) ||
                (myLocation.distanceSquared(closestEnemyQueen()) <= QUEEN_DANGER_DISTANCE && getTotalTroops() < 11));
    }

    public boolean canSpawnSpider() {
        return (((enemies.length == 0 || allObstructed()) && (getPassive() == 1 || getBeetles() * 2 > getSpiders())) &&
                ((getBees() + 1) * 3 > getSpiders()) && rocks.length < 14);
    }

    public boolean canSpawnBee() {
        return ((enemySpiders > allyBees && !allObstructed()) || ((getBees() + 1) * 3 <= getSpiders()));
    }

    private int cocoonCounter(UnitType type) {
        if (type == UnitType.ANT) return ANTS_COCOON;
        if (type == UnitType.BEE) return BEES_COCOON;
        if (type == UnitType.BEETLE) return BEETLES_COCOON;
        if (type == UnitType.SPIDER) return SPIDERS_COCOON;
        return -1;
    }

    public void addCocoonList(Location targetLocation) {
        UnitInfo targetCocoon = uc.senseUnit(targetLocation);
        if (targetCocoon == null) return;
        int counter = cocoonCounter(targetCocoon.getType());
        if (counter < 0) return;

        for (int i = INITIAL_COCOON_LIST; i <= FINAL_COCOON_LIST; i++) {
            if (uc.read(i) == 0) {
                uc.write(i, targetCocoon.getID());
                uc.write(i + COCOON_HATCH_ROUND_OFFSET, round + GameConstants.COCOON_TURNS);
                uc.write(i + COCOON_COUNTER_OFFSET, counter);
                uc.write(counter, uc.read(counter) + 1);
                return;
            }
        }
    }

    public void removeCocoonList(int ID) {
        if (myType == UnitType.QUEEN) {
            return;
        }
        for (int i = INITIAL_COCOON_LIST; i <= FINAL_COCOON_LIST; i++) {
            if (uc.read(i) == ID) {
                clearCocoonSlot(i);
                break;
            }
        }
    }

    // Removes cocoons killed before hatching
    private void expireCocoons() {
        for (int i = INITIAL_COCOON_LIST; i <= FINAL_COCOON_LIST; i++) {
            if (uc.read(i) != 0 && round > uc.read(i + COCOON_HATCH_ROUND_OFFSET) + COCOON_EXPIRY_MARGIN) {
                clearCocoonSlot(i);
            }
        }
    }

    private void clearCocoonSlot(int i) {
        int counter = uc.read(i + COCOON_COUNTER_OFFSET);
        uc.write(counter, uc.read(counter) - 1);
        uc.write(i, 0);
        uc.write(i + COCOON_HATCH_ROUND_OFFSET, 0);
        uc.write(i + COCOON_COUNTER_OFFSET, 0);
    }

    // Sets the minimum round before a soldier can be spawned
    public void soldierRoundSpawn() {
        if (getEnemySeenLastRound() == 1) {
            uc.write(SPAWN_SOLDIERS_ROUND, round - 1);
            return;
        }

        Location[] myQueens = uc.getMyQueensLocation();
        Location[] enemyQueens = uc.getEnemyQueensLocation();
        Location allowedToSpawnSoldiers = myQueens[0];

        int distance;
        int smallestDistance = INF;
        for (Location queen : myQueens) {
            for (Location enemyQueen : enemyQueens) {
                distance = queen.distanceSquared(enemyQueen);
                if (distance < smallestDistance) {
                    smallestDistance = distance;
                    allowedToSpawnSoldiers = queen;
                }
            }
        }

        uc.write(XQUEEN_ALLOWED_SOLDIER, allowedToSpawnSoldiers.x);
        uc.write(YQUEEN_ALLOWED_SOLDIER, allowedToSpawnSoldiers.y);
        int minRound = getSpawnSoldiersRound();
        int currentRound = (int) Math.sqrt(smallestDistance) + SOLDIER_ROUND_MARGIN;
        if (currentRound < minRound || minRound == 0) {
            uc.write(SPAWN_SOLDIERS_ROUND, currentRound);
        }
    }

    public Location closestEnemyQueen() {
        Location[] enemyQueens = uc.getEnemyQueensLocation();
        if (enemyQueens.length == 0) {
            return null;
        }
        int smallestDistance = INF;
        int distance;
        Location closest = enemyQueens[0];
        for (Location enemyQueen : enemyQueens) {
            distance = myLocation.distanceSquared(enemyQueen);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                closest = enemyQueen;
            }
        }
        return closest;
    }

    // Latest unobstructed enemy sighting, cleared once seen empty
    public Location beacon() {
        if (uc.read(BEACON_ROUND) == 0) return null;
        return new Location(uc.read(XBEACON), uc.read(YBEACON));
    }

    public Location closestAllyQueen() {
        Location[] allyQueens = uc.getMyQueensLocation();
        int smallestDistance = INF;
        int distance;
        Location closest = allyQueens[0];
        for (Location allyQueen : allyQueens) {
            distance = myLocation.distanceSquared(allyQueen);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                closest = allyQueen;
            }
        }
        return closest;
    }

    public boolean isObstructed(Location target) {
        if (myLocation.isEqual(target)) {
            return false;
        }
        Direction dir = myLocation.directionTo(target);
        Location origin = myLocation.add(dir);
        if (uc.hasObstacle(origin)) {
            return true;
        }
        if (uc.isObstructed(origin, target)) {
            return true;
        }
        return false;
    }

    // Stops counting at limit, callers only compare against it
    public int countVisibleSoldiers(UnitInfo[] list, int limit) {
        int count = 0;
        for (UnitInfo unit : list) {
            if (count >= limit) break;
            UnitType type = unit.getType();
            if (type != UnitType.QUEEN && type != UnitType.ANT && !isObstructed(unit.getLocation())) {
                count++;
            }
        }
        return count;
    }

    public boolean isThreatened(Location loc) {
        for (UnitInfo enemy : enemies) {
            if (loc.distanceSquared(enemy.getLocation()) <= enemy.getType().getAttackRangeSquared()) return true;
        }
        return false;
    }

    // Queen health decides most games that reach the round limit
    public boolean isEndgame() {
        return round >= GameConstants.MAX_TURNS - ENDGAME_ROUNDS;
    }

    public UnitInfo endgameQueenTarget() {
        if (!isEndgame()) return null;
        for (UnitInfo enemy : enemies) {
            if (enemy.getType() == UnitType.QUEEN && uc.canAttack(enemy)) return enemy;
        }
        return null;
    }

    public boolean allObstructed() {
        boolean obstructed = true;
        for (UnitInfo enemy : enemies) {
            if (!isObstructed(enemy.getLocation())) {
                obstructed = false;
                break;
            }
        }
        return obstructed;
    }

    public void updateObjective() {
        if (round < getSpawnSoldiersRound()) return;
        // Other queens follow at their own distance based round instead of staying on ants for the whole game
        int ownRound = (int) Math.sqrt(myLocation.distanceSquared(closestEnemyQueen())) + SOLDIER_ROUND_MARGIN;
        if (myLocation.isEqual(getAllowedSoldier()) || round >= ownRound) {
            objective = UnitType.BEETLE;
        }
    }

    public boolean isExtreme(Location target) {
        for (Direction dir : dirs) {
            if (uc.isOutOfMap(target.add(dir))) {
                return true;
            }
        }
        return false;
    }

    // (0, 0) means no location, maps have positive offsets
    public boolean isSet(Location loc) {
        return loc.x != 0 || loc.y != 0;
    }

    // Queen only: our own units on every tile around her that is not rock or edge leave no room to spawn or move
    public void checkRoom() {
        if (resources < GameConstants.ANT_COST) return;
        for (Direction dir : dirs) {
            if (dir == Direction.ZERO) continue;
            Location loc = myLocation.add(dir);
            if (uc.isOutOfMap(loc) || uc.hasObstacle(loc)) continue;
            UnitInfo unit = uc.senseUnit(loc);
            // Enemies on the ring mean a fight, where our units must not step away
            if (unit == null || unit.getTeam() != allies) return;
        }
        uc.write(XROOM, myLocation.x);
        uc.write(YROOM, myLocation.y);
        uc.write(ROOM_ROUND, round);
    }

    public boolean makeRoom() {
        int roomRound = uc.read(ROOM_ROUND);
        if (roomRound == 0 || roomRound < round - 1) return false;
        Location queen = new Location(uc.read(XROOM), uc.read(YROOM));
        if (myLocation.distanceSquared(queen) > ADJACENT_DISTANCE) return false;
        for (Direction dir : dirs) {
            if (uc.canMove(dir) && myLocation.add(dir).distanceSquared(queen) > ADJACENT_DISTANCE) {
                uc.move(dir);
                return true;
            }
        }
        return false;
    }

    public void postMoveUpdate() {
        Location newLocation = uc.getLocation();
        if (newLocation.isEqual(myLocation)) return;
        myLocation = newLocation;
        enemies = uc.senseUnits(opponent);
        units = uc.senseUnits(allies);
        rocks = uc.senseObstacles();
        food = uc.senseFood();
    }

    public int getFinalMapSize() {
        return uc.read(FINAL_MAP_SIZE);
    }

    public int getCurrentMapSize() {
        return uc.read(CURRENT_MAP_SIZE);
    }

    public int getQueens() {
        return uc.getMyQueensLocation().length;
    }

    public int getEnemyQueens() {
        return uc.getEnemyQueensLocation().length;
    }

    public int getAnts() {
        return uc.read(ANTS_PREVIOUS);
    }

    public int getAntsCocoon() {
        return uc.read(ANTS_COCOON);
    }

    public int getBees() {
        return uc.read(BEES_PREVIOUS);
    }

    public int getBeesCocoon() {
        return uc.read(BEES_COCOON);
    }

    public int getBeetles() {
        return uc.read(BEETLES_PREVIOUS);
    }

    public int getBeetlesCocoon() {
        return uc.read(BEETLES_COCOON);
    }

    public int getSpiders() {
        return uc.read(SPIDERS_PREVIOUS);
    }

    public int getSpidersCocoon() {
        return uc.read(SPIDERS_COCOON);
    }

    public int getTotalTroops() {
        return uc.read(BEES_PREVIOUS) + uc.read(BEETLES_PREVIOUS) + uc.read(SPIDERS_PREVIOUS);
    }

    public Location getIdleFoodLocation() {
        return new Location(uc.read(XIDLE_FOOD), uc.read(YIDLE_FOOD));
    }

    public int getIdleFoodHealth() {
        return uc.read(IDLE_FOOD_HEALTH);
    }

    public Location getIdleFoodLocationNotObs() {
        return new Location(uc.read(XIDLE_FOOD_OBS), uc.read(YIDLE_FOOD_OBS));
    }

    public int getIdleFoodHealthNotObs() {
        return uc.read(IDLE_FOOD_HEALTH_OBS);
    }

    public int getEnemySpotted() {
        return uc.read(ENEMY_SPOTTED);
    }

    public int getEnemySeenLastRound() {
        return uc.read(ENEMY_SEEN_LAST_ROUND);
    }

    public Location getAllowedSoldier() {
        return new Location(uc.read(XQUEEN_ALLOWED_SOLDIER), uc.read(YQUEEN_ALLOWED_SOLDIER));
    }

    public int getSpawnSoldiersRound() {
        return uc.read(SPAWN_SOLDIERS_ROUND);
    }

    public Location getPreviousEnemyQueenLocation(int index) {
        return new Location(uc.read(PREVIOUS_ENEMY_QUEENS + index * 2), uc.read(PREVIOUS_ENEMY_QUEENS + 1 + index * 2));
    }

    public int getPassive() {
        return uc.read(PASSIVE);
    }

    public int getPassiveCounter() {
        return uc.read(PASSIVE_COUNTER);
    }

    public int getQueenSeesEnemy() {
        return uc.read(QUEEN_SEES_ENEMY);
    }
}
