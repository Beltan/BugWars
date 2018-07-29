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
                    manager.addCocoonList(manager.myLocation.add(dir));
                    break;
                }
            }
        }
    }

    private void tryHeal() {

    }
}
