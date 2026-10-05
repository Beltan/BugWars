package version9;

import bugwars.user.*;

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
        if (!uc.canMove()) return;

        Location myQueen = manager.closestAllyQueen();
        int distance = manager.myLocation.distanceSquared(myQueen);
        int enemies = manager.countVisibleSoldiers(manager.enemies, manager.INF);
        int allies = 1 + manager.countVisibleSoldiers(manager.units, 2 * enemies);

        boolean moved;
        moved = manager.path.evalLocation(allies, enemies);

        if (!moved) {
            if (uc.getInfo().getHealth() * 2 < manager.unitHealth(manager.myType) && distance > GameConstants.QUEEN_HEALING_RANGE && (allies < enemies || manager.getTotalTroops() < 20)) {
                manager.path.moveToHome();
            } else {
                manager.path.moveToEnemyQueen();
            }
        }

        manager.postMoveUpdate();
    }

    private void tryAttack() {
        if (!uc.canAttack() || (manager.enemies.length == 0 && manager.rocks.length == 0)) {
            return;
        }

        UnitInfo queenTarget = manager.endgameQueenTarget();
        if (queenTarget != null) {
            uc.attack(queenTarget);
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
                manager.tracker.onAttack(lowestEnemy);
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
}
