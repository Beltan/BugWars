package version12;

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

    private int LIST_SIZE = 61;
    private int COUNT_BASE = 62;
    private int DEFENSE_ROUND = 81;
    private int DEFENSE_LOCATION = 82;
    // Unit ids go from 0 to MAX_ID inclusive, so tables indexed by id need MAX_ID + 1 slots
    private int SEEN_BASE = 40000;
    private int LIST_BASE = SEEN_BASE + GameConstants.MAX_ID + 1;
    private int LOCATION_BASE = GameConstants.TEAM_ARRAY_SIZE - GameConstants.MAX_ID - 1;

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
            uc.write(LOCATION_BASE + id, pack(loc));

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
        if (manager.round % COUNT_PERIOD != 0) return;
        int[] counts = new int[UNIT_TYPES];
        int oldest = manager.round + 1 - MEMORY_ROUNDS;
        int size = uc.read(LIST_SIZE);
        int index = 0;
        while (index < size) {
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
        for (int i = 0; i < UNIT_TYPES; i++) {
            uc.write(COUNT_BASE + i, counts[i]);
        }
    }

    public int count(UnitType type) {
        return uc.read(COUNT_BASE + type.ordinal());
    }

    public Location lastLocation(int id) {
        return unpack(uc.read(LOCATION_BASE + id));
    }

    public Location defenseAlert() {
        int alertRound = uc.read(DEFENSE_ROUND) - 1;
        if (alertRound < 0 || manager.round - alertRound > DEFENSE_ROUNDS) return null;
        return unpack(uc.read(DEFENSE_LOCATION));
    }
}
