package version0;

import bugwars.*;

public class MemoryManager {

    // Initialize parameters
    public UnitController uc;
    public boolean root;
    public int round;
    public Team opponent;
    public Team allies;
    public Direction[] dirs;
    public Location myLocation;
    public UnitType myType;

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

    public MemoryManager(UnitController uc) {
        this.uc = uc;
        round = uc.getRound();
        opponent = uc.getOpponent();
        allies = uc.getTeam();
        dirs = Direction.values();
        myLocation = uc.getLocation();
        myType = uc.getType();
    }

    public void update() {
        round = uc.getRound();
        myLocation = uc.getLocation();

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

        if (round == 0) {
            roundZeroInitialization();
            mapSizeUpdate();
        } else if (uc.read(FINAL_MAP_SIZE) == 0) {
            mapLimits();
            mapSizeUpdate();
        }
    }

    private void rootUpdate() {
        if (round == 0) {
            roundZeroRootInitialization();
        }

    }

    public void roundZeroRootInitialization() {
        Location myQueens[] = uc.getMyQueensLocation();
        Location enemyQueens[] = uc.getEnemyQueensLocation();
        int xLow = myQueens[0].x;
        int yLow = myQueens[0].y;
        int xHigh = myQueens[0].x;
        int yHigh = myQueens[0].y;

        // Checks what kind of simmetry does the map have
        if (myQueens[0].x == enemyQueens[0].x) {
            uc.write(TEN_YCOORDINATE_CENTER,  10 * (myQueens[0].y - (myQueens[0].y - enemyQueens[0].y) / 2));
        } else if (myQueens[0].y == enemyQueens[0].y) {
            uc.write(TEN_XCOORDINATE_CENTER,  10 * (myQueens[0].x - (myQueens[0].x - enemyQueens[0].x) / 2));
        } else {
            uc.write(TEN_XCOORDINATE_CENTER,  10 * (myQueens[0].x - (myQueens[0].x - enemyQueens[0].x) / 2));
            uc.write(TEN_YCOORDINATE_CENTER,  10 * (myQueens[0].y - (myQueens[0].y - enemyQueens[0].y) / 2));
        }

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
                uc.write(XLOWER_FINAL, newLoc1.x + 1);
                found2 = true;
            }
            Location newLoc3 = new Location(myLocation.x, myLocation.y + i);
            if (!found3 && uc.isOutOfMap(newLoc3)) {
                uc.write(YHIGHER_FINAL, newLoc1.y - 1);
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

        if (xCenter != 0 && (xLowFinal != 0 || xHighFinal != 0)) {
            uc.write(FINAL_MAP_SIZE, 2 * Math.abs((xCenter / 10) - Math.max(xLowFinal, xHighFinal)) + 1);
        } else if (yCenter != 0 && (yLowFinal != 0 || yHighFinal != 0)) {
            uc.write(FINAL_MAP_SIZE, 2 * Math.abs((yCenter / 10) - Math.max(yLowFinal, yHighFinal)) + 1);
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

}
