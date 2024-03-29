package version0;

import bugwars.user.*;

public class Queen {

    private MemoryManager manager;
    private UnitController uc;

    public Queen(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        tryHeal();
        trySpawn();
        tryMove();
        tryHeal();
    }

    private void tryMove() {
        Location foodLoc = manager.getIdleFoodLocation();
        if (manager.bestFood != null) {
            manager.path.moveTo(manager.bestFood);
        } else if (foodLoc.x != 0 || foodLoc.y != 0) {
            manager.path.moveTo(foodLoc);
        } else {
            manager.path.moveTo(uc.getEnemyQueensLocation()[0]);
        }
        manager.myLocation = uc.getLocation();
    }

    private void trySpawn() {
        if (manager.canSpawnAnt()) {
            Location idleFood = manager.getIdleFoodLocation();
            Direction target;
            if (manager.bestFood != null) {
                target = manager.bestFood.directionTo(manager.myLocation);
            } else if (idleFood.x != 0 && idleFood.y != 0) {
                target = idleFood.directionTo(manager.myLocation);
            } else {
                target = uc.getEnemyQueensLocation()[0].directionTo(manager.myLocation);
            }

            if (uc.canSpawn(target, UnitType.ANT)) {
                uc.spawn(target, UnitType.ANT);
                manager.addCocoonList(manager.myLocation.add(target));
            }

            for (Direction dir : manager.dirs) {
                if (uc.canSpawn(dir, UnitType.ANT)) {
                    uc.spawn(dir, UnitType.ANT);
                    manager.addCocoonList(manager.myLocation.add(dir));
                    break;
                }
            }
        } else if (manager.objective == UnitType.BEETLE){
            for (Direction dir : manager.dirs) {
                if (uc.canSpawn(dir, UnitType.BEETLE)) {
                    uc.spawn(dir, UnitType.BEETLE);
                    manager.addCocoonList(manager.myLocation.add(dir));
                    break;
                }
            }
        }
    }

    private void tryHeal() {

    }
}
