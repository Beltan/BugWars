package version0;

import bugwars.*;

public class Queen {

    private MemoryManager manager;
    private UnitController uc;
    private Pathfinder path;

    public Queen(MemoryManager manager, Pathfinder path) {
        this.manager = manager;
        this.path = path;
        uc = manager.uc;
    }

    public void play() {
        tryHeal();
        trySpawn();
        tryMove();
        tryHeal();
        cocoonCount();
    }

    private void tryMove() {
        path.moveTo(manager.getIdleFoodLocation());
        manager.myLocation = uc.getLocation();
    }

    private void trySpawn() {
        if (manager.canSpawnAnt()) {
            for (Direction dir : manager.dirs) {
                if (uc.canSpawn(dir, UnitType.ANT)) {
                    uc.spawn(dir, UnitType.ANT);
                    break;
                }
            }
        }
    }

    private void tryHeal() {

    }

    private void cocoonCount() {
        for (UnitInfo unit : manager.units) {
            if (unit.isCocoon()) {
                UnitType cocoonType = unit.getType();
                if (cocoonType == UnitType.ANT) {
                    uc.write(manager.ANTS_COCOON, uc.read(manager.ANTS_COCOON) + 1);
                } else if (cocoonType == UnitType.BEE) {
                    uc.write(manager.BEES_COCOON, uc.read(manager.BEES_COCOON) + 1);
                } else if (cocoonType == UnitType.BEETLE) {
                    uc.write(manager.BEETLES_COCOON, uc.read(manager.BEETLES_COCOON) + 1);
                } else if (cocoonType == UnitType.SPIDER) {
                    uc.write(manager.SPIDERS_COCOON, uc.read(manager.SPIDERS_COCOON) + 1);
                }
            }
        }
    }

}
