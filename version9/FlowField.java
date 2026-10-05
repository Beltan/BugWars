package version9;

import bugwars.user.*;

// Weighted distance field to a set of queens, built incrementally with spare energy by every unit
public class FlowField {

    private MemoryManager manager;
    private UnitController uc;
    private MapMemory map;

    private final int SIZE = GameConstants.MAX_MAP_SIZE;
    private final int CELLS = SIZE * SIZE;
    private final int STRAIGHT_COST = 5;
    private final int DIAGONAL_COST = 7;
    private final int GENERATION_SHIFT = 16;
    private final int IN_QUEUE = 1 << 15;
    private final int DISTANCE_MASK = IN_QUEUE - 1;
    private final int REFRESH_ROUNDS = 50;

    // Shared memory, relative to stateBase
    private final int ACTIVE_BUFFER = 0;
    private final int ACTIVE_GENERATION = 1;
    private final int BUILD_BUFFER = 2;
    private final int BUILD_GENERATION = 3;
    private final int START_ROUND = 4;
    private final int QUEUE_HEAD = 5;
    private final int QUEUE_TAIL = 6;
    private final int DOMAIN_X = 7;
    private final int DOMAIN_Y = 8;
    private final int DOMAIN_WIDTH = 9;
    private final int DOMAIN_HEIGHT = 10;

    private int stateBase;
    private int bufferBase;
    private int queueBase;
    private boolean towardsEnemy;

    public FlowField(MemoryManager manager, MapMemory map, int stateBase, int bufferBase, int queueBase, boolean towardsEnemy) {
        this.manager = manager;
        this.map = map;
        uc = manager.uc;
        this.stateBase = stateBase;
        this.bufferBase = bufferBase;
        this.queueBase = queueBase;
        this.towardsEnemy = towardsEnemy;
    }

    private int stepCost(Direction dir) {
        return dir.dx != 0 && dir.dy != 0 ? DIAGONAL_COST : STRAIGHT_COST;
    }

    // Null if the field is not ready or no free step gets closer
    public Direction direction() {
        int active = uc.read(stateBase + ACTIVE_BUFFER);
        if (active == 0) return null;
        int base = bufferBase + (active - 1) * CELLS;
        int generation = uc.read(stateBase + ACTIVE_GENERATION);
        Location myLocation = manager.myLocation;

        int value = uc.read(base + map.cell(myLocation.x, myLocation.y));
        if ((value >> GENERATION_SHIFT) != generation) return null;
        int myDistance = value & DISTANCE_MASK;
        int bestTotal = manager.INF;
        Direction bestDirection = null;

        for (Direction dir : manager.dirs) {
            if (dir == Direction.ZERO || !uc.canMove(dir)) continue;
            Location next = myLocation.add(dir);
            value = uc.read(base + map.cell(next.x, next.y));
            if ((value >> GENERATION_SHIFT) != generation) continue;
            int distance = value & DISTANCE_MASK;
            if (distance >= myDistance) continue;
            int total = distance + stepCost(dir);
            if (total < bestTotal) {
                bestTotal = total;
                bestDirection = dir;
            }
        }
        return bestDirection;
    }

    public boolean needsWork() {
        return uc.read(stateBase + BUILD_BUFFER) != 0 || uc.read(stateBase + ACTIVE_BUFFER) == 0
                || manager.round - uc.read(stateBase + START_ROUND) >= REFRESH_ROUNDS;
    }

    public void work(int limit) {
        int building = uc.read(stateBase + BUILD_BUFFER);
        if (building == 0) {
            start();
            building = uc.read(stateBase + BUILD_BUFFER);
        }

        int base = bufferBase + (building - 1) * CELLS;
        int generation = uc.read(stateBase + BUILD_GENERATION);
        int stamp = generation << GENERATION_SHIFT;
        int xLow = uc.read(stateBase + DOMAIN_X);
        int yLow = uc.read(stateBase + DOMAIN_Y);
        int width = uc.read(stateBase + DOMAIN_WIDTH);
        int height = uc.read(stateBase + DOMAIN_HEIGHT);

        int head = uc.read(stateBase + QUEUE_HEAD);
        int tail = uc.read(stateBase + QUEUE_TAIL);
        while (head < tail && uc.getEnergyUsed() < limit) {
            int current = uc.read(queueBase + head % CELLS);
            head++;
            uc.write(stateBase + QUEUE_HEAD, head);
            int distance = uc.read(base + current) & DISTANCE_MASK;
            uc.write(base + current, stamp | distance);
            int cx = current / SIZE;
            int cy = current % SIZE;

            for (int dx = -1; dx <= 1; dx++) {
                int nx = (cx + dx + SIZE) % SIZE;
                if ((nx - xLow + SIZE) % SIZE >= width) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int ny = (cy + dy + SIZE) % SIZE;
                    if ((ny - yLow + SIZE) % SIZE >= height) continue;
                    int neighbor = nx * SIZE + ny;
                    if (map.isRock(neighbor)) continue;
                    int next = distance + (dx != 0 && dy != 0 ? DIAGONAL_COST : STRAIGHT_COST);
                    int value = uc.read(base + neighbor);
                    boolean fresh = (value >> GENERATION_SHIFT) == generation;
                    if (fresh && (value & DISTANCE_MASK) <= next) continue;
                    uc.write(base + neighbor, stamp | IN_QUEUE | next);
                    if (!fresh || (value & IN_QUEUE) == 0) {
                        uc.write(queueBase + tail % CELLS, neighbor);
                        tail++;
                    }
                }
            }
            uc.write(stateBase + QUEUE_TAIL, tail);
        }

        if (head >= tail) {
            uc.write(stateBase + ACTIVE_BUFFER, building);
            uc.write(stateBase + ACTIVE_GENERATION, generation);
            uc.write(stateBase + BUILD_BUFFER, 0);
        }
    }

    private void start() {
        int buffer = uc.read(stateBase + ACTIVE_BUFFER) == 1 ? 1 : 0;
        int generation = uc.read(stateBase + BUILD_GENERATION) + 1;
        int stamp = generation << GENERATION_SHIFT;
        int base = bufferBase + buffer * CELLS;

        map.axisDomain(manager.XLOWER_FINAL, manager.XHIGHER_FINAL, uc.read(manager.TEN_XCOORDINATE_CENTER) / 5, manager.XLOWER_BOUND, manager.XHIGHER_BOUND);
        uc.write(stateBase + DOMAIN_X, map.domainLow % SIZE);
        uc.write(stateBase + DOMAIN_WIDTH, map.domainSize);
        map.axisDomain(manager.YLOWER_FINAL, manager.YHIGHER_FINAL, uc.read(manager.TEN_YCOORDINATE_CENTER) / 5, manager.YLOWER_BOUND, manager.YHIGHER_BOUND);
        uc.write(stateBase + DOMAIN_Y, map.domainLow % SIZE);
        uc.write(stateBase + DOMAIN_HEIGHT, map.domainSize);

        int tail = 0;
        Location[] sources = towardsEnemy ? uc.getEnemyQueensLocation() : uc.getMyQueensLocation();
        for (Location queen : sources) {
            int queenCell = map.cell(queen.x, queen.y);
            uc.write(base + queenCell, stamp | IN_QUEUE);
            uc.write(queueBase + tail, queenCell);
            tail++;
        }

        uc.write(stateBase + BUILD_GENERATION, generation);
        uc.write(stateBase + BUILD_BUFFER, buffer + 1);
        uc.write(stateBase + START_ROUND, manager.round);
        uc.write(stateBase + QUEUE_HEAD, 0);
        uc.write(stateBase + QUEUE_TAIL, tail);
    }
}
