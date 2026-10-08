package version13;

import bugwars.user.*;

public class Spider {

    private MemoryManager manager;
    private UnitController uc;

    private final int MAX_ROCK_DURABILITY = 200;

    public Spider(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        manager.attacker.tryAttack(false, MAX_ROCK_DURABILITY);
        tryMove();
        manager.attacker.tryAttack(false, MAX_ROCK_DURABILITY);
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
