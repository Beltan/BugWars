package version14;

import bugwars.user.*;

public class Beetle {

    private MemoryManager manager;
    private UnitController uc;

    public Beetle(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        manager.attacker.tryAttack(true, manager.INF);
        tryMove();
        manager.attacker.tryAttack(true, manager.INF);
    }

    private void tryMove() {
        if (!uc.canMove()) return;
        if (manager.makeRoom()) {
            manager.postMoveUpdate();
            return;
        }

        if (!manager.path.evalLocation()) manager.path.moveToObjectiveOrHome();

        manager.postMoveUpdate();
    }
}
