package version11;

import bugwars.user.*;

public class MapMemory {

    private MemoryManager manager;
    private UnitController uc;

    private final int SIZE = GameConstants.MAX_MAP_SIZE;
    private final int WORK_MARGIN = 1500;
    private final int ROCK_RECORD_PERIOD = 4;
    private final int ROCK_CHECKS = 4;
    private final int SEEN_ROCK = 1;
    private final int MIRRORED_ROCK = 2;
    private final int COORDINATE_SHIFT = 12;
    private final int COORDINATE_MASK = (1 << COORDINATE_SHIFT) - 1;
    private final int ROCK_LIST_CAPACITY = 3600;
    private final int REGION = 5;
    private final int REGIONS = SIZE / REGION;
    // Rounds a unit needs per tile, to trade staleness against travel
    private final int EXPLORE_DISTANCE_WEIGHT = 2;

    public final int SYMMETRY_MIRROR_X = 1;
    public final int SYMMETRY_MIRROR_Y = 2;
    public final int SYMMETRY_ROTATION = 4;

    public int SYMMETRY = 49;
    private int ROCK_LIST_SIZE = 67;
    private int ROCK_CHECK_INDEX = 68;
    public final int ROCK_BASE = 1000;
    private int ROCK_LIST_BASE = 36400;
    // Last round + 1 any unit stood in each region, 0 if never
    private int REGION_BASE = 8000;
    private int FOOD_TARGET_X = 290;
    private int FOOD_TARGET_Y = 291;
    private int EXPLORE_X = 8300;
    private int EXPLORE_Y = 8301;
    private int EXPLORE_PICK_ROUND = 8302;
    private int EXPLORE_DEADLINE = 8303;

    public FlowField enemyField;
    public FlowField homeField;
    public FlowField foodField;

    private Location lastRecordLocation = new Location(-1, -1);
    private final int MARK_PERIOD = 4;
    private int lastMarkX = -1;
    private int lastMarkY = -1;
    private int lastMarkRound = 0;
    private int lastRecordSymmetry = -1;

    public int domainLow;
    public int domainSize;

    public MapMemory(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
        enemyField = new FlowField(manager, this, 50, 10000, 30000, true, 292);
        homeField = new FlowField(manager, this, 70, 70000, 83000, false, 293);
        foodField = new FlowField(manager, this, 270, 57000, 23000, false, 294, FOOD_TARGET_X);
    }

    public int cell(int x, int y) {
        return (x % SIZE) * SIZE + (y % SIZE);
    }

    public int rockCount() {
        return uc.read(ROCK_LIST_SIZE);
    }

    public boolean isRock(int cell) {
        return uc.read(ROCK_BASE + cell) != 0;
    }

    public void recordRocks() {
        if ((manager.round + manager.myId) % ROCK_RECORD_PERIOD != 0) return;
        int symmetry = uc.read(SYMMETRY);
        if (manager.myLocation.isEqual(lastRecordLocation) && symmetry == lastRecordSymmetry) {
            verifyRocks();
            return;
        }
        lastRecordLocation = manager.myLocation;
        lastRecordSymmetry = symmetry;
        int xDoubleCenter = uc.read(manager.TEN_XCOORDINATE_CENTER) / 5;
        int yDoubleCenter = uc.read(manager.TEN_YCOORDINATE_CENTER) / 5;
        for (RockInfo rock : manager.rocks) {
            Location loc = rock.getLocation();
            recordRock(loc.x, loc.y, SEEN_ROCK);
            if (symmetry == SYMMETRY_MIRROR_X) {
                recordRock(xDoubleCenter - loc.x, loc.y, MIRRORED_ROCK);
            } else if (symmetry == SYMMETRY_MIRROR_Y) {
                recordRock(loc.x, yDoubleCenter - loc.y, MIRRORED_ROCK);
            } else if (symmetry == SYMMETRY_ROTATION) {
                recordRock(xDoubleCenter - loc.x, yDoubleCenter - loc.y, MIRRORED_ROCK);
            }
        }
        verifyRocks();
    }

    private void recordRock(int x, int y, int state) {
        if (x < 0 || y < 0) return;
        int slot = ROCK_BASE + cell(x, y);
        int current = uc.read(slot);
        if (current == 0) {
            int size = uc.read(ROCK_LIST_SIZE);
            if (size >= ROCK_LIST_CAPACITY) return;
            uc.write(slot, state);
            uc.write(ROCK_LIST_BASE + size, (x << COORDINATE_SHIFT) | y);
            uc.write(ROCK_LIST_SIZE, size + 1);
        } else if (current == MIRRORED_ROCK && state == SEEN_ROCK) {
            uc.write(slot, SEEN_ROCK);
        }
    }

    // Checks only a few list entries per call
    private void verifyRocks() {
        int size = uc.read(ROCK_LIST_SIZE);
        if (size == 0) return;
        int index = uc.read(ROCK_CHECK_INDEX);
        for (int i = 0; i < ROCK_CHECKS && size > 0; i++) {
            if (index >= size) index = 0;
            int packed = uc.read(ROCK_LIST_BASE + index);
            Location loc = new Location(packed >> COORDINATE_SHIFT, packed & COORDINATE_MASK);
            if (uc.canSenseLocation(loc) && !uc.hasObstacle(loc)) {
                uc.write(ROCK_BASE + cell(loc.x, loc.y), 0);
                size--;
                uc.write(ROCK_LIST_BASE + index, uc.read(ROCK_LIST_BASE + size));
            } else {
                index++;
            }
        }
        uc.write(ROCK_LIST_SIZE, size);
        uc.write(ROCK_CHECK_INDEX, index);
    }

    public void setFoodTarget(Location target) {
        if (uc.read(FOOD_TARGET_X) == target.x && uc.read(FOOD_TARGET_Y) == target.y) return;
        uc.write(FOOD_TARGET_X, target.x);
        uc.write(FOOD_TARGET_Y, target.y);
        foodField.invalidate();
    }

    public boolean isFoodTarget(Location target) {
        return uc.read(FOOD_TARGET_X) == target.x && uc.read(FOOD_TARGET_Y) == target.y;
    }

    private int region(Location loc) {
        return ((loc.x % SIZE) / REGION) * REGIONS + (loc.y % SIZE) / REGION;
    }

    public void markExplored(Location loc) {
        uc.write(REGION_BASE + region(loc), manager.round + 1);
    }

    public void markSeenRegions() {
        Location me = manager.myLocation;
        // Staleness is scored in rounds, refreshing a few rounds late changes nothing but costs every turn
        if (me.x == lastMarkX && me.y == lastMarkY && manager.round - lastMarkRound < MARK_PERIOD) return;
        lastMarkX = me.x;
        lastMarkY = me.y;
        lastMarkRound = manager.round;
        int sight = manager.myType.sightRangeSquared;
        int stamp = manager.round + 1;
        int cx = me.x - me.x % REGION + REGION / 2;
        int cy = me.y - me.y % REGION + REGION / 2;
        for (int dx = -REGION; dx <= REGION; dx += REGION) {
            int x = cx + dx;
            int ddx = (x - me.x) * (x - me.x);
            if (x < 0 || ddx > sight) continue;
            int column = REGION_BASE + ((x % SIZE) / REGION) * REGIONS;
            for (int dy = -REGION; dy <= REGION; dy += REGION) {
                int y = cy + dy;
                if (y >= 0 && ddx + (y - me.y) * (y - me.y) <= sight) uc.write(column + (y % SIZE) / REGION, stamp);
            }
        }
    }

    public int lastExplored(Location loc) {
        return uc.read(REGION_BASE + region(loc));
    }

    public Location exploreTarget() {
        if (uc.read(EXPLORE_PICK_ROUND) == 0) return null;
        return new Location(uc.read(EXPLORE_X), uc.read(EXPLORE_Y));
    }

    public int explorePickRound() {
        return uc.read(EXPLORE_PICK_ROUND);
    }

    public int exploreDeadline() {
        return uc.read(EXPLORE_DEADLINE);
    }

    public void setExploreTarget(Location target, int deadline) {
        uc.write(EXPLORE_X, target.x);
        uc.write(EXPLORE_Y, target.y);
        uc.write(EXPLORE_PICK_ROUND, manager.round + 1);
        uc.write(EXPLORE_DEADLINE, deadline);
    }

    // Center of the region that is stalest once travel time is added
    public Location stalestRegion() {
        axisDomain(manager.XLOWER_FINAL, manager.XHIGHER_FINAL, uc.read(manager.TEN_XCOORDINATE_CENTER) / 5, manager.XLOWER_BOUND, manager.XHIGHER_BOUND);
        int xLow = Math.max(domainLow, 0);
        int xHigh = domainLow + domainSize - 1;
        axisDomain(manager.YLOWER_FINAL, manager.YHIGHER_FINAL, uc.read(manager.TEN_YCOORDINATE_CENTER) / 5, manager.YLOWER_BOUND, manager.YHIGHER_BOUND);
        int yLow = Math.max(domainLow, 0);
        int yHigh = domainLow + domainSize - 1;
        Location me = manager.myLocation;

        Location best = null;
        int bestScore = manager.INF;
        for (int x = xLow - xLow % REGION; x <= xHigh; x += REGION) {
            int cx = Math.min(Math.max(x + REGION / 2, xLow), xHigh);
            int dx = Math.abs(cx - me.x);
            for (int y = yLow - yLow % REGION; y <= yHigh; y += REGION) {
                int cy = Math.min(Math.max(y + REGION / 2, yLow), yHigh);
                int dy = Math.abs(cy - me.y);
                int score = uc.read(REGION_BASE + ((cx % SIZE) / REGION) * REGIONS + (cy % SIZE) / REGION)
                        + EXPLORE_DISTANCE_WEIGHT * Math.max(dx, dy);
                if (score < bestScore) {
                    bestScore = score;
                    best = new Location(cx, cy);
                }
            }
        }
        return best;
    }

    public void work() {
        int limit = GameConstants.MAX_BYTECODES - WORK_MARGIN;
        if (uc.getEnergyUsed() > limit) return;
        if (enemyField.needsWork()) enemyField.work(limit);
        if (uc.getEnergyUsed() > limit) return;
        if (homeField.needsWork()) homeField.work(limit);
        if (uc.getEnergyUsed() > limit || uc.read(FOOD_TARGET_X) == 0) return;
        if (foodField.needsWork()) foodField.work(limit);
    }

    // Widest window that surely contains the map along one axis, never more than SIZE cells
    public void axisDomain(int lowFinalSlot, int highFinalSlot, int doubleCenter, int lowBoundSlot, int highBoundSlot) {
        int low = uc.read(lowFinalSlot);
        int high = uc.read(highFinalSlot);
        if (doubleCenter != 0) {
            if (low != 0 && high == 0) high = doubleCenter - low;
            else if (high != 0 && low == 0) low = doubleCenter - high;
        }
        if (low == 0 && high == 0) {
            int middle = doubleCenter != 0 ? doubleCenter / 2 : (uc.read(lowBoundSlot) + uc.read(highBoundSlot)) / 2;
            low = middle - SIZE / 2 + 1;
            high = low + SIZE - 1;
        } else if (high == 0) {
            high = low + SIZE - 1;
        } else if (low == 0) {
            low = high - SIZE + 1;
        }
        if (high - low + 1 > SIZE) high = low + SIZE - 1;
        domainLow = low;
        domainSize = high - low + 1;
    }
}
