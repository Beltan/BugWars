package version0;

import bugwars.*;

public class Beetle {

    private MemoryManager manager;
    private UnitController uc;

    public Beetle(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        tryAttack();
        tryMove();
        tryAttack();
    }

    private void tryMove() {
        if (manager.enemies.length == 0) {
            Location targetQueen = manager.closestEnemyQueen();
            manager.path.moveTo(targetQueen);
        } else {
            manager.path.moveTo(manager.enemies[0].getLocation());
        }
    }

    private void tryAttack() {
        if (!uc.canAttack() || manager.enemies.length == 0) {
            return;
        }

        int smallestHealth = 1000000;
        int health;
        UnitInfo lowestEnemy = manager.enemies[0];

        for (UnitInfo enemy : manager.enemies) {
            health = enemy.getHealth();
            if (uc.canAttack(enemy) && health < smallestHealth) {
                smallestHealth = health;
                lowestEnemy = enemy;
            }
        }

        if (smallestHealth != 1000000) {
            uc.attack(lowestEnemy);
        }
    }
}
