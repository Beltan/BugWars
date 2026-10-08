package version14;

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
    public Location bestFood;
    public int bestFoodHealth;
    public int foodCount;
    public int foodHealthSum;
    public int maxFoodSum;
    private int localAnts;
    private int localAntCocoons;
    public UnitType objective;
    private boolean edgesKnown = false;
    public RockInfo[] rocks;
    public Pathfinder path;
    public MapMemory map;
    public EnemyTracker tracker;
    public Attacker attacker;
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
    public final int ANT_SAFE_DISTANCE = 25;
    public final int SPIDER_MAX_ROCKS = 14;
    public final int FIRING_LINE_LENGTH = 4;
    public final int MIN_OPEN_LINES = 3;
    public final int PASSIVE_ROUNDS = 15;
    public final int SOLDIER_ROUND_MARGIN = 40;
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
    // More troops than this make contested food safe for ants
    public final int ESCORT_TROOPS = 5;
    // Below this many troops a queen near the enemy keeps building beetles
    public final int GUARD_TROOPS = 11;
    public final int ANT_FREE_TROOPS = 40;
    // While food is above 1 / FOOD_FULLNESS[i] of its initial amount, each ant claims ANT_FOOD_SHARE[i] food tiles
    public final double[] FOOD_FULLNESS = {1.5, 1.3, 1.15, 1.1};
    public final double[] ANT_FOOD_SHARE = {2.9, 2.4, 1.9, 1.5};
    public final int COCOON_ANT_WEIGHT = 1;
    // Far above any unit cost: the queen cannot spend, so more income only adds traffic
    public final int BANK_RESOURCES = 1000;
    // With a bank, units this close to a boxed queen step outwards so her ring can clear
    public final int EVACUATION_DISTANCE = 18;

    // 0 to 99 are general information and states
    public final int PREVIOUS_ROUND = 0;
    public final int MAP_SIZE_KNOWN = 3;
    public final int TEN_XCOORDINATE_CENTER = 4;
    public final int TEN_YCOORDINATE_CENTER = 5;
    public final int XHIGHER_BOUND = 6;
    public final int XLOWER_BOUND = 7;
    public final int YHIGHER_BOUND = 8;
    public final int YLOWER_BOUND = 9;
    public final int XHIGHER_FINAL = 10;
    public final int XLOWER_FINAL = 11;
    public final int YHIGHER_FINAL = 12;
    public final int YLOWER_FINAL = 13;
    public final int ANTS_PREVIOUS = 14;
    public final int ANTS_CURRENT = 15;
    public final int ANTS_COCOON = 16;
    public final int BEES_PREVIOUS = 17;
    public final int BEES_CURRENT = 18;
    public final int BEES_COCOON = 19;
    public final int BEETLES_PREVIOUS = 20;
    public final int BEETLES_CURRENT = 21;
    public final int BEETLES_COCOON = 22;
    public final int SPIDERS_PREVIOUS = 23;
    public final int SPIDERS_CURRENT = 24;
    public final int SPIDERS_COCOON = 25;
    public final int XIDLE_FOOD = 26;
    public final int YIDLE_FOOD = 27;
    public final int IDLE_FOOD_HEALTH = 28;
    public final int ENEMY_SEEN_LAST_ROUND = 30;
    public final int SPAWN_SOLDIERS_ROUND = 31;
    public final int XQUEEN_ALLOWED_SOLDIER = 32;
    public final int YQUEEN_ALLOWED_SOLDIER = 33;
    public final int XIDLE_FOOD_OBS = 34;
    public final int YIDLE_FOOD_OBS = 35;
    public final int IDLE_FOOD_HEALTH_OBS = 36;
    public final int PASSIVE = 39;
    public final int PASSIVE_COUNTER = 40;
    public final int ENEMY_QUEEN_COUNT = 43;
    public final int ENEMY_QUEENS_MOVED = 44;
    public final int XBEACON = 46;
    public final int YBEACON = 47;
    public final int BEACON_ROUND = 48;
    public final int XROOM = 37;
    public final int YROOM = 38;
    public final int ROOM_ROUND = 45;

    // 100 to 129 are cocoon IDs, 200 to 229 their hatching round and 230 to 259 their counter slot
    public final int INITIAL_COCOON_LIST = 100;
    public final int FINAL_COCOON_LIST = 129;
    public final int COCOON_HATCH_ROUND_OFFSET = 100;
    public final int COCOON_COUNTER_OFFSET = 130;

    // 160 to 169 are previous enemy Queen positions
    public final int PREVIOUS_ENEMY_QUEENS = 160;
    public final int OUR_STARTS = 300;
    public final int ENEMY_STARTS = 306;

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
        objective = UnitType.ANT;
        rocks = uc.senseObstacles();
        path = new Pathfinder(this);
        map = new MapMemory(this);
        tracker = new EnemyTracker(this);
        attacker = new Attacker(this);
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
        if (uc.read(MAP_SIZE_KNOWN) == 0) updateBounds();

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
        map.updateBreach();

        if (enemies.length != 0) {
            uc.write(ENEMY_SEEN_LAST_ROUND, 1);
        }

        if (myType == UnitType.QUEEN) updateObjective();
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
        int range = (int) Math.sqrt(myType.sightRangeSquared);
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

    // The explored bounds only matter until one axis has its center and an edge
    private void updateBounds() {
        int xCenter = uc.read(TEN_XCOORDINATE_CENTER);
        int yCenter = uc.read(TEN_YCOORDINATE_CENTER);
        if ((xCenter != 0 && (uc.read(XLOWER_FINAL) != 0 || uc.read(XHIGHER_FINAL) != 0))
                || (yCenter != 0 && (uc.read(YLOWER_FINAL) != 0 || uc.read(YHIGHER_FINAL) != 0))) {
            uc.write(MAP_SIZE_KNOWN, 1);
            return;
        }
        if (myLocation.x > uc.read(XHIGHER_BOUND)) uc.write(XHIGHER_BOUND, myLocation.x);
        if (myLocation.y > uc.read(YHIGHER_BOUND)) uc.write(YHIGHER_BOUND, myLocation.y);
        if (myLocation.x < uc.read(XLOWER_BOUND)) uc.write(XLOWER_BOUND, myLocation.x);
        if (myLocation.y < uc.read(YLOWER_BOUND)) uc.write(YLOWER_BOUND, myLocation.y);
    }

    public boolean isHurt() {
        return uc.getInfo().getHealth() * 2 < myType.maxHealth;
    }

    public Direction[] shuffle(Direction[] list) {
        Direction[] shuffledList = new Direction[MOVE_DIRECTIONS];
        for (Direction dir : list) {
            if (dir == Direction.ZERO) continue;
            int random = (int) (Math.random() * MOVE_DIRECTIONS);
            while (shuffledList[random] != null) random = (random + 1) % MOVE_DIRECTIONS;
            shuffledList[random] = dir;
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

    // Only enemy soldiers close to the queen or the food stop us from building ants
    private boolean safeForAnts() {
        for (UnitInfo enemy : enemies) {
            UnitType type = enemy.getType();
            if (type == UnitType.QUEEN || type == UnitType.ANT) continue;
            Location loc = enemy.getLocation();
            if (myLocation.distanceSquared(loc) > ANT_SAFE_DISTANCE && (bestFood == null || bestFood.distanceSquared(loc) > ANT_SAFE_DISTANCE)) continue;
            if (!isObstructed(loc)) return false;
        }
        return true;
    }

    private void countLocalAnts() {
        localAnts = 0;
        localAntCocoons = 0;
        for (UnitInfo unit : units) {
            if (unit.getType() == UnitType.ANT && !isObstructed(unit.getLocation())) {
                if (unit.isCocoon()) localAntCocoons++;
                else localAnts++;
            }
        }
    }

    public boolean canSpawnAnt() {
        if (bestFood == null || resources >= BANK_RESOURCES) return false;
        countLocalAnts();
        int troops = getTotalTroops();
        return localAnts + localAntCocoons < MAX_ANTS_PER_QUEEN
                && ((objective == UnitType.ANT && getSpawnSoldiersRound() > round) || troops > ANT_TROOP_RATIO * getAnts() || troops > ANT_FREE_TROOPS)
                && !antsBlockedByDanger() && safeForAnts() && foodHasRoom();
    }

    public boolean localFoodHasRoom() {
        countLocalAnts();
        return foodHasRoom();
    }

    // A single tile only takes another ant while at least half full; fuller food takes more ants per tile
    private boolean foodHasRoom() {
        if (foodCount == 1 && foodHealthSum * 2 < maxFoodSum) return false;
        for (int i = 0; i < FOOD_FULLNESS.length; i++) {
            if (foodHealthSum * FOOD_FULLNESS[i] > maxFoodSum && localAnts * ANT_FOOD_SHARE[i] + COCOON_ANT_WEIGHT * localAntCocoons < foodCount) return true;
        }
        return false;
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
                && getTotalTroops() <= ESCORT_TROOPS && isContested(bestFood);
    }

    public boolean canSpawnBeetle() {
        return ((((enemyBeetles + enemyBees > allyBeetles) && !allObstructed()) ||
                (getPassive() == 0 && (getBeetles() * 2 <= getSpiders()))) ||
                (myLocation.distanceSquared(closestEnemyQueen()) <= QUEEN_DANGER_DISTANCE && getTotalTroops() < GUARD_TROOPS));
    }

    // Many rocks in sight usually block a spider's shots, but in a maze the corridors still leave lines of fire.
    // Only worth it against enemy spiders: against pure beetles the dearer spider just slows the beetle race
    private boolean openFiringLines() {
        int open = 0;
        for (Direction dir : dirs) {
            if (dir == Direction.ZERO) continue;
            Location end = myLocation;
            for (int i = 0; i < FIRING_LINE_LENGTH; i++) end = end.add(dir);
            if (!uc.isOutOfMap(end) && !uc.hasObstacle(end) && !isObstructed(end)) open++;
            if (open >= MIN_OPEN_LINES) return true;
        }
        return false;
    }

    public boolean canSpawnSpider() {
        return (((enemies.length == 0 || allObstructed()) && (getPassive() == 1 || getBeetles() * 2 > getSpiders())) &&
                ((getBees() + 1) * 3 > getSpiders()) && (rocks.length < SPIDER_MAX_ROCKS || (tracker.count(UnitType.SPIDER) > 0 && openFiringLines())));
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
        // The line to a tile in sight stays in sight, so with no rock in sight nothing can block it
        if (rocks.length == 0 && myLocation.distanceSquared(target) <= myType.sightRangeSquared) {
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

    // Visible soldiers only, this unit included
    public boolean isOutnumbered() {
        int enemySoldiers = countVisibleSoldiers(enemies, INF);
        return 1 + countVisibleSoldiers(units, enemySoldiers) < enemySoldiers;
    }

    public boolean isThreatened(Location loc) {
        for (UnitInfo enemy : enemies) {
            if (path.isHarmless(enemy)) continue;
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
        int distance = myLocation.distanceSquared(queen);
        if (distance > ADJACENT_DISTANCE) {
            if (resources < BANK_RESOURCES || distance > EVACUATION_DISTANCE) return false;
            Direction best = null;
            int bestDistance = distance;
            for (Direction dir : dirs) {
                if (!uc.canMove(dir)) continue;
                int d = myLocation.add(dir).distanceSquared(queen);
                if (d > bestDistance) {
                    bestDistance = d;
                    best = dir;
                }
            }
            if (best == null) return false;
            uc.move(best);
            return true;
        }
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

    public int getAnts() {
        return uc.read(ANTS_PREVIOUS);
    }

    public int getBees() {
        return uc.read(BEES_PREVIOUS);
    }

    public int getBeetles() {
        return uc.read(BEETLES_PREVIOUS);
    }

    public int getSpiders() {
        return uc.read(SPIDERS_PREVIOUS);
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
}
