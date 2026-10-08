package version14;

import bugwars.user.*;

public class Bee {

    private MemoryManager manager;
    private UnitController uc;

    public Bee(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        manager.attacker.tryAttack(false, manager.INF);
        tryMove();
        manager.attacker.tryAttack(false, manager.INF);
    }

    private void tryMove() {
        if (!uc.canMove()) return;
        if (manager.makeRoom()) {
            manager.postMoveUpdate();
            return;
        }

        if (manager.enemies.length == 0 || manager.allObstructed() || !manager.path.evalLocation()) {
            manager.path.moveToObjectiveOrHome();
        }

        manager.postMoveUpdate();
    }
}
