package version1;

import bugwars.*;

public class MemoryManager {

    public UnitController uc;
    public boolean root;
    public int round;
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
    public UnitType objective;
    public RockInfo[] rocks;
    public Pathfinder path;

    // Shared memory
    // 0 to 99 are general information and states
    public int PREVIOUS_ROUND = 0;
    public int CURRENT_ROUND = 1;
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
    public int XQUEEN_ALLOWED = 32;
    public int YQUEEN_ALLOWED = 33;

    // 100 to 129 are cocoon IDs
    public int INITIAL_COCOON_LIST = 100;
    public int FINAL_COCOON_LIST = 129;

    public MemoryManager(UnitController uc) {
        this.uc = uc;
        round = uc.getRound();
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
    }

    public void update() {
        round = uc.getRound();
        myLocation = uc.getLocation();
        units = uc.senseUnits(allies);
        enemies = uc.senseUnits(opponent);
        food = uc.senseFood();
        rocks = uc.senseObstacles();

        // Root check
        uc.write(CURRENT_ROUND, round);
        int previousRound = uc.read(PREVIOUS_ROUND);

        if ((round != 0 && previousRound != round) || (round == 0 && previousRound == 0)) {
            uc.write(PREVIOUS_ROUND, round);
            root = true;
        } else {
            root = false;
        }

        if (root) {
            rootUpdate();
        }

        // Map update
        if (round == 0 && uc.read(FINAL_MAP_SIZE) == 0) {
            roundZeroInitialization();
            mapSizeUpdate();
        } else if (uc.read(FINAL_MAP_SIZE) == 0) {
            mapLimits();
            mapSizeUpdate();
        }

        // Unit counter update
        if(uc.getType() == UnitType.ANT) {
            uc.write(ANTS_CURRENT, uc.read(ANTS_CURRENT) + 1);
        } else if(uc.getType() == UnitType.BEE) {
            uc.write(BEES_CURRENT, uc.read(BEES_CURRENT) + 1);
        } else if(uc.getType() == UnitType.BEETLE) {
            uc.write(BEETLES_CURRENT, uc.read(BEETLES_CURRENT) + 1);
        } else if(uc.getType() == UnitType.SPIDER) {
            uc.write(SPIDERS_CURRENT, uc.read(SPIDERS_CURRENT) + 1);
        }

        // Idle food update
        int idleFoodHealth = getIdleFoodHealth();
        Location idleFoodLocation = getIdleFoodLocation();
        int xLoc = idleFoodLocation.x;
        int yLoc = idleFoodLocation.y;

        if (myLocation.isEqual(idleFoodLocation) || (uc.canSenseLocation(idleFoodLocation) && uc.senseUnit(idleFoodLocation) != null)) {
            idleFoodHealth = 0;
        } else {
            for (FoodInfo foodUnit : food) {
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

        // Enemy spotted check
        if (enemies.length != 0) {
            uc.write(ENEMY_SPOTTED, 1);
        }

        // Enemy seen last round
        if (enemies.length != 0) {
            uc.write(ENEMY_SEEN_LAST_ROUND, 1);
        }

        // Update objective
        if (myType == UnitType.QUEEN) {
            updateObjective();
        }
    }

    private void rootUpdate() {
        if (round == 0) {
            roundZeroRootInitialization();
        }

        // Updates ants
        uc.write(ANTS_PREVIOUS, uc.read(ANTS_CURRENT) + uc.read(ANTS_COCOON));
        uc.write(ANTS_CURRENT, 0);

        // Updates bees
        uc.write(BEES_PREVIOUS, uc.read(BEES_CURRENT) + uc.read(BEES_COCOON));
        uc.write(BEES_CURRENT, 0);

        // Updates beetles
        uc.write(BEETLES_PREVIOUS, uc.read(BEETLES_CURRENT) + uc.read(BEETLES_COCOON));
        uc.write(BEETLES_CURRENT, 0);

        // Updates spiders
        uc.write(SPIDERS_PREVIOUS, uc.read(SPIDERS_CURRENT) + uc.read(SPIDERS_COCOON));
        uc.write(SPIDERS_CURRENT, 0);

        // Updates soldier round
        soldierRoundSpawn();

        // Updates enemy seen last round
        uc.write(ENEMY_SEEN_LAST_ROUND, 0);
    }

    public void roundZeroRootInitialization() {
        Location myQueens[] = uc.getMyQueensLocation();
        Location enemyQueens[] = uc.getEnemyQueensLocation();
        Location myQueen = myQueens[0];
        Location enemyQueen = enemyQueens[0];
        int xLow = myQueens[0].x;
        int yLow = myQueens[0].y;
        int xHigh = myQueens[0].x;
        int yHigh = myQueens[0].y;

        for (Location queen : myQueens) {
            if (xHigh < queen.x) {
                xHigh = queen.x;
            } else if (xLow > queen.x) {
                xLow = queen.x;
            }
            if (yHigh < queen.y) {
                yHigh = queen.y;
            } else if (yLow > queen.y) {
                yLow = queen.y;
            }
        }

        for (Location queen : enemyQueens) {
            if (xHigh < queen.x) {
                xHigh = queen.x;
            } else if (xLow > queen.x) {
                xLow = queen.x;
            }
            if (yHigh < queen.y) {
                yHigh = queen.y;
            } else if (yLow > queen.y) {
                yLow = queen.y;
            }
        }

        uc.write(XHIGHER_BOUND, xHigh);
        uc.write(XLOWER_BOUND, xLow);
        uc.write(YHIGHER_BOUND, yHigh);
        uc.write(YLOWER_BOUND, yLow);

        // Checks what kind of symmetry does the map have
        for (Location queen : myQueens) {
            if (myQueen.x > queen.x) {
                myQueen = queen;
            } else if (myQueen.x == queen.x && myQueen.y > queen.y) {
                myQueen = queen;
            }
        }

        for (Location queen : enemyQueens) {
            if (enemyQueen.x < queen.x) {
                enemyQueen = queen;
            } else if (enemyQueen.x == queen.x && enemyQueen.y < queen.y) {
                enemyQueen = queen;
            }
        }

        double coordinate;
        if (myQueen.x == enemyQueen.x) {
            coordinate = 10 * (myQueen.y - (myQueen.y - enemyQueen.y) / 2.0);
            uc.write(TEN_YCOORDINATE_CENTER,  (int) coordinate);
        } else if (myQueen.y == enemyQueen.y) {
            coordinate = 10 * (myQueen.x - (myQueen.x - enemyQueen.x) / 2.0);
            uc.write(TEN_XCOORDINATE_CENTER,  (int) coordinate);
        } else {
            coordinate = 10 * (myQueen.x - (myQueen.x - enemyQueen.x) / 2.0);
            uc.write(TEN_XCOORDINATE_CENTER,  (int) coordinate);
            coordinate = 10 * (myQueen.y - (myQueen.y - enemyQueen.y) / 2.0);
            uc.write(TEN_YCOORDINATE_CENTER,  (int) coordinate);
        }
    }

    // Checks if map limit is seen
    public void roundZeroInitialization() {
        int range = unitRange();
        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;
        boolean found4 = false;

        for (int i = 1; i <= range; i++) {
            Location newLoc1 = new Location(myLocation.x + i, myLocation.y);
            if (!found1 && uc.isOutOfMap(newLoc1)) {
                uc.write(XHIGHER_FINAL, newLoc1.x - 1);
                found1 = true;
            }
            Location newLoc2 = new Location(myLocation.x - i, myLocation.y);
            if (!found2 && uc.isOutOfMap(newLoc2)) {
                uc.write(XLOWER_FINAL, newLoc2.x + 1);
                found2 = true;
            }
            Location newLoc3 = new Location(myLocation.x, myLocation.y + i);
            if (!found3 && uc.isOutOfMap(newLoc3)) {
                uc.write(YHIGHER_FINAL, newLoc3.y - 1);
                found3 = true;
            }
            Location newLoc4 = new Location(myLocation.x, myLocation.y - i);
            if (!found4 && uc.isOutOfMap(newLoc4)) {
                uc.write(YLOWER_FINAL, newLoc4.y + 1);
                found4 = true;
            }
        }
    }

    // Updates the known size of the map
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
                uc.write(XHIGHER_BOUND, myLocation.x);
            }
            if (myLocation.y > yHigh) {
                uc.write(YHIGHER_BOUND, myLocation.y);
            }
            if (myLocation.x < xLow) {
                uc.write(XLOWER_BOUND, myLocation.x);
            }
            if (myLocation.x < yLow) {
                uc.write(YLOWER_BOUND, myLocation.y);
            }

            uc.write(CURRENT_MAP_SIZE, Math.max(xHigh - xLow, yHigh - yLow));
        }
    }

    // Updates the known map limits
    public void mapLimits() {
        int range = unitRange();
        Location newLoc1 = new Location(myLocation.x + range, myLocation.y);
        if (uc.read(XHIGHER_BOUND) != 0 && uc.isOutOfMap(newLoc1)) {
            uc.write(XHIGHER_FINAL, newLoc1.x);
        }
        Location newLoc2 = new Location(myLocation.x - range, myLocation.y);
        if (uc.read(XLOWER_BOUND) != 0 && uc.isOutOfMap(newLoc2)) {
            uc.write(XLOWER_FINAL, newLoc1.x);
        }
        Location newLoc3 = new Location(myLocation.x, myLocation.y + range);
        if (uc.read(YHIGHER_BOUND) != 0 && uc.isOutOfMap(newLoc3)) {
            uc.write(YHIGHER_FINAL, newLoc1.y);
        }
        Location newLoc4 = new Location(myLocation.x, myLocation.y - range);
        if (uc.read(YLOWER_BOUND) != 0 && uc.isOutOfMap(newLoc4)) {
            uc.write(YLOWER_FINAL, newLoc1.y);
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

    // Bad but good enough random directions
    public Direction[] shuffle(Direction list[]) {
        Direction shuffledList[] = new Direction[8];
        int random;

        for (int i = 0; i < 8; i++) {
            random = (int )(Math.random() * 8);
            if (shuffledList[random] == null) {
                shuffledList[random] = list[i];
            } else {
                for (int j = 0; j < 8; j++) {
                    if (shuffledList[(random + j) % 8] == null) {
                        shuffledList[(random + j) % 8] = list[i];
                        break;
                    }
                }
            }
        }

        return shuffledList;
    }

    // Ant spawn conditions
    public boolean canSpawnAnt() {
        bestFood = null;
        bestFoodHealth = 0;
        int foodHealth = 0;
        int maxFood = 0;
        int foodCount = 0;

        for (FoodInfo foodUnit : food) {
            if (!isObstructed(foodUnit.location)) {
                foodHealth += foodUnit.food;
                maxFood += foodUnit.initialFood;
                foodCount++;
                if (foodUnit.food == foodUnit.initialFood && bestFoodHealth < foodUnit.food) {
                    bestFood = foodUnit.location;
                    bestFoodHealth = foodUnit.food;
                }
            }
        }

        int antCount = 0;
        int cocoonAnts = 0;
        for (UnitInfo unit : units) {
            if (unit.getType() == UnitType.ANT) {
                if (unit.isCocoon()) {
                    cocoonAnts++;
                    continue;
                }
                antCount++;
            }
        }

        return (((objective == UnitType.ANT && getSpawnSoldiersRound() > round) || getTotalTroops() > getAnts() + 10) && (foodCount != 1 || maxFood == foodHealth) &&
                ((foodHealth * 1.5 > maxFood && antCount * 2.9 + 4 * cocoonAnts < foodCount) ||
                (foodHealth * 1.3 > maxFood && antCount * 2.4 + 4 * cocoonAnts < foodCount) ||
                (foodHealth * 1.15 > maxFood && antCount * 1.9 + 4 * cocoonAnts < foodCount) ||
                (foodHealth * 1.1 > maxFood && antCount * 1.5 + 4 * cocoonAnts < foodCount)));
    }

    // Beetle spawn conditions


    public boolean canSpawnBeetle() {
        return (2 * getSpiders() + 1 >= getBeetles() || (enemies.length != 0 && !allObstructed()));
    }

    public boolean canSpawnSpider() {
        return 2 * getSpiders() + 1 < getBeetles();
    }

    // Add a new cocoon
    public void addCocoonList(Location targetLocation) {
        for (int i = INITIAL_COCOON_LIST; i <= FINAL_COCOON_LIST; i++) {
            if (uc.read(i) == 0) {
                UnitInfo targetCocoon = uc.senseUnit(targetLocation);
                uc.write(i, targetCocoon.getID());
                UnitType cocoonType = targetCocoon.getType();
                if (cocoonType == UnitType.ANT) {
                    uc.write(ANTS_COCOON, uc.read(ANTS_COCOON) + 1);
                } else if (cocoonType == UnitType.BEE) {
                    uc.write(BEES_COCOON, uc.read(BEES_COCOON) + 1);
                } else if (cocoonType == UnitType.BEETLE) {
                    uc.write(BEETLES_COCOON, uc.read(BEETLES_COCOON) + 1);
                } else if (cocoonType == UnitType.SPIDER) {
                    uc.write(SPIDERS_COCOON, uc.read(SPIDERS_COCOON) + 1);
                }
                return;
            }
        }
    }

    // Remove an existing cocoon
    public void removeCocoonList(int ID) {
        if (myType == UnitType.QUEEN) {
            return;
        }
        for (int i = INITIAL_COCOON_LIST; i <= FINAL_COCOON_LIST; i++) {
            if (uc.read(i) == ID) {
                uc.write(i, 0);
                UnitType cocoonType = myType;
                if (cocoonType == UnitType.ANT) {
                    uc.write(ANTS_COCOON, uc.read(ANTS_COCOON) - 1);
                } else if (cocoonType == UnitType.BEE) {
                    uc.write(BEES_COCOON, uc.read(BEES_COCOON) - 1);
                } else if (cocoonType == UnitType.BEETLE) {
                    uc.write(BEETLES_COCOON, uc.read(BEETLES_COCOON) - 1);
                } else if (cocoonType == UnitType.SPIDER) {
                    uc.write(SPIDERS_COCOON, uc.read(SPIDERS_COCOON) - 1);
                }
                break;
            }
        }
    }

    // Sets the minimum round before a soldier can be spawn
    public void soldierRoundSpawn() {
        if (getEnemySeenLastRound() == 1) {
            uc.write(SPAWN_SOLDIERS_ROUND, round);
            return;
        }

        Location myQueens[] = uc.getMyQueensLocation();
        Location enemyQueens[] = uc.getEnemyQueensLocation();
        Location allowedToSpawn = myQueens[0];
        int distance;
        int smallestDistance = 1000000;
        for (Location queen : myQueens) {
            for (Location enemyQueen : enemyQueens) {
                distance = queen.distanceSquared(enemyQueen);
                if (distance < smallestDistance) {
                    smallestDistance = distance;
                    allowedToSpawn = queen;
                }
            }
        }
        uc.write(XQUEEN_ALLOWED, allowedToSpawn.x);
        uc.write(YQUEEN_ALLOWED, allowedToSpawn.y);
        uc.write(SPAWN_SOLDIERS_ROUND, (int) Math.sqrt(smallestDistance) + 10);
    }

    public Location closestEnemyQueen() {
        Location enemyQueens[] = uc.getEnemyQueensLocation();
        if (enemyQueens.length == 0) {
            return null;
        }
        int smallestDistance = 1000000;
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

    public Location closestAllyQueen() {
        Location allyQueens[] = uc.getMyQueensLocation();
        if (allyQueens.length == 0) {
            return null;
        }
        int smallestDistance = 1000000;
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

    // Updates current objective
    public void updateObjective() {
        if (myLocation.isEqual(getAllowed()) && round >= getSpawnSoldiersRound()) {
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

    // Getters
    public int getFinalMapSize() {
        return uc.read(FINAL_MAP_SIZE);
    }

    public int getCurrentMapSize() {
        return uc.read(CURRENT_MAP_SIZE);
    }

    public int getQueens() {
        return uc.getMyQueensLocation().length;
    }

    public int getEnemyQuenns() {
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

    public int getEnemySpotted() {
        return uc.read(ENEMY_SPOTTED);
    }

    public int getEnemySeenLastRound() {
        return uc.read(ENEMY_SEEN_LAST_ROUND);
    }

    public Location getAllowed() {
        return new Location(uc.read(XQUEEN_ALLOWED), uc.read(YQUEEN_ALLOWED));
    }

    public int getSpawnSoldiersRound() {
        return uc.read(SPAWN_SOLDIERS_ROUND);
    }
}
