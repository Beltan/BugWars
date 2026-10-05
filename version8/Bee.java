package version8;

import bugwars.user.*;

public class Bee {

    private MemoryManager manager;
    private UnitController uc;

    public Bee(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        tryAttack();
        tryMove();
        tryAttack();
    }

    private void tryAttack() {
        if (!uc.canAttack() || (manager.enemies.length == 0 && manager.rocks.length == 0)) {
            return;
        }

        if (manager.enemies.length != 0) {
            int smallestHealth = manager.INF;
            int health;
            UnitInfo lowestEnemy = manager.enemies[0];

            for (UnitInfo enemy : manager.enemies) {
                health = enemy.getHealth();
                if (uc.canAttack(enemy) && health < smallestHealth) {
                    smallestHealth = health;
                    lowestEnemy = enemy;
                }
            }

            if (smallestHealth != manager.INF) {
                uc.attack(lowestEnemy);
            }
        } else {
            int smallestRock = manager.INF;
            int durability;
            RockInfo weakerRock = manager.rocks[0];

            for (RockInfo rock : manager.rocks) {
                durability = rock.getDurability();
                if (uc.canAttack(rock) && durability < smallestRock) {
                    smallestRock = durability;
                    weakerRock = rock;
                }
            }

            if (smallestRock != manager.INF) {
                uc.attack(weakerRock);
            }
        }
    }

    private void tryMove() {
        if (!uc.canMove()) return;

        Location targetQueen = manager.closestEnemyQueen();
        int enemies = manager.countVisibleSoldiers(manager.enemies, manager.INF);
        int allies = 1 + manager.countVisibleSoldiers(manager.units, 2 * enemies);

        if (manager.enemies.length != 0 && !manager.allObstructed()) {
            manager.path.evalLocation(allies, enemies);
        } else {
            manager.path.moveTo(targetQueen);
        }

        manager.postMoveUpdate();
    }
}
