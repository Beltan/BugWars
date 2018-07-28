package version0;

import bugwars.*;

public class Queen {

    private MemoryManager manager;
    private UnitController uc;

    public Queen(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        tryHeal();
        tryMove();
        trySpawn();
        tryHeal();
        cocoonCount();
    }

    private void tryMove() {
        Direction idleDirection = manager.myLocation.directionTo(manager.getIdleFoodLocation());
        if (uc.canMove(idleDirection)) {
            uc.move(idleDirection);
            return;
        }

        Direction randomDirections[] = manager.shuffle(manager.dirs);
        for (Direction dir : randomDirections) {
            if (uc.canMove(dir)) {
                uc.move(dir);
                return;
            }
        }
    }

    private void trySpawn() {
        if (manager.canSpawnAnt()) {
            for (Direction dir : manager.dirs) {
                if (uc.canSpawn(dir, UnitType.ANT)) {
                    uc.spawn(dir, UnitType.ANT);
                }
            }
        }
    }

    private void tryHeal() {

    }

    private void cocoonCount() {
        for (UnitInfo unit : manager.units) {
            if (unit.isCocoon()) {
                UnitType cocconType = unit.getType();
                if (cocconType == UnitType.ANT) {
                    uc.write(manager.ANTS_COCOON, uc.read(manager.ANTS_COCOON) + 1);
                } else if (cocconType == UnitType.BEE) {
                    uc.write(manager.BEES_COCOON, uc.read(manager.BEES_COCOON) + 1);
                } else if (cocconType == UnitType.BEETLE) {
                    uc.write(manager.BEETLES_COCOON, uc.read(manager.BEETLES_COCOON) + 1);
                } else if (cocconType == UnitType.SPIDER) {
                    uc.write(manager.SPIDERS_COCOON, uc.read(manager.SPIDERS_COCOON) + 1);
                }
            }
        }
    }

}
