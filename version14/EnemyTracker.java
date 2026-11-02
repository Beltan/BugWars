package version14;

import bugwars.user.*;

public class EnemyTracker {

    private MemoryManager manager;
    private UnitController uc;

    private final int MEMORY_ROUNDS = 100;
    private final int COUNT_PERIOD = 10;
    private final int UNIT_TYPES = 5;
    private final int TYPE_SHIFT = 16;
    private final int ROUND_MASK = (1 << TYPE_SHIFT) - 1;
    private final int COORDINATE_SHIFT = 12;
    private final int COORDINATE_MASK = (1 << COORDINATE_SHIFT) - 1;
    public final int DEFENSE_RADIUS = 64;
    private final int DEFENSE_ROUNDS = 3;

    private final int LIST_SIZE = 61;
    private final int COUNT_BASE = 62;
    private final int DEFENSE_ROUND = 81;
    private final int DEFENSE_LOCATION = 82;
    // A long list costs thousands, so a pass that runs short of energy goes on in the next root update
    private final int RECOUNT_INDEX = 83;
    private final int PARTIAL_COUNT_BASE = 84;
    private final int RECOUNT_ENERGY_LIMIT = GameConstants.MAX_BYTECODES - 11000;
    // The queen records after acting, the nearest enemies first
    private final int RECORD_ENERGY_LIMIT = GameConstants.MAX_BYTECODES - 2500;
    // Unit ids go from 0 to MAX_ID inclusive, so tables indexed by id need MAX_ID + 1 slots
    private final int SEEN_BASE = 40000;
    private final int LIST_BASE = SEEN_BASE + GameConstants.MAX_ID + 1;

    public EnemyTracker(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    private int pack(Location loc) {
        return (loc.x << COORDINATE_SHIFT) | loc.y;
    }

    private Location unpack(int packed) {
        return new Location(packed >> COORDINATE_SHIFT, packed & COORDINATE_MASK);
    }

    public void record() {
        if (manager.enemies.length == 0) return;
        int stamp = manager.round + 1;
        Location[] myQueens = uc.getMyQueensLocation();
        for (UnitInfo enemy : manager.enemies) {
            if (uc.getEnergyUsed() > RECORD_ENERGY_LIMIT) break;
            int id = enemy.getID();
            int seen = uc.read(SEEN_BASE + id);
            if ((seen & ROUND_MASK) == stamp) continue;
            UnitType type = enemy.getType();
            Location loc = enemy.getLocation();
            if (seen == 0) {
                int size = uc.read(LIST_SIZE);
                uc.write(LIST_BASE + size, id);
                uc.write(LIST_SIZE, size + 1);
            }
            uc.write(SEEN_BASE + id, ((type.ordinal() + 1) << TYPE_SHIFT) | stamp);

            if (type != UnitType.ANT && type != UnitType.QUEEN) {
                for (Location queen : myQueens) {
                    if (queen.distanceSquared(loc) <= DEFENSE_RADIUS) {
                        uc.write(DEFENSE_ROUND, stamp);
                        uc.write(DEFENSE_LOCATION, pack(loc));
                        break;
                    }
                }
            }
        }
    }

    public void onAttack(UnitInfo target) {
        if (target.getHealth() > manager.myType.getAttack()) return;
        int id = target.getID();
        uc.write(SEEN_BASE + id, uc.read(SEEN_BASE + id) & ~ROUND_MASK);
    }

    public void recount() {
        int next = uc.read(RECOUNT_INDEX);
        if (next == 0 && manager.round % COUNT_PERIOD != 0) return;
        int[] counts = new int[UNIT_TYPES];
        if (next != 0) {
            for (int i = 0; i < UNIT_TYPES; i++) counts[i] = uc.read(PARTIAL_COUNT_BASE + i);
        }
        int oldest = manager.round + 1 - MEMORY_ROUNDS;
        int size = uc.read(LIST_SIZE);
        int index = next == 0 ? 0 : next - 1;
        while (index < size) {
            if (uc.getEnergyUsed() > RECOUNT_ENERGY_LIMIT) {
                uc.write(LIST_SIZE, size);
                uc.write(RECOUNT_INDEX, index + 1);
                for (int i = 0; i < UNIT_TYPES; i++) uc.write(PARTIAL_COUNT_BASE + i, counts[i]);
                return;
            }
            int id = uc.read(LIST_BASE + index);
            int seen = uc.read(SEEN_BASE + id);
            int lastRound = seen & ROUND_MASK;
            if (lastRound != 0 && lastRound >= oldest) {
                counts[(seen >> TYPE_SHIFT) - 1]++;
                index++;
            } else {
                // Forgotten or killed: record() adds it back if seen again
                uc.write(SEEN_BASE + id, 0);
                size--;
                uc.write(LIST_BASE + index, uc.read(LIST_BASE + size));
            }
        }
        uc.write(LIST_SIZE, size);
        uc.write(RECOUNT_INDEX, 0);
        for (int i = 0; i < UNIT_TYPES; i++) {
            uc.write(COUNT_BASE + i, counts[i]);
        }
    }

    public int count(UnitType type) {
        return uc.read(COUNT_BASE + type.ordinal());
    }

    public Location defenseAlert() {
        int alertRound = uc.read(DEFENSE_ROUND) - 1;
        if (alertRound < 0 || manager.round - alertRound > DEFENSE_ROUNDS) return null;
        return unpack(uc.read(DEFENSE_LOCATION));
    }
}
