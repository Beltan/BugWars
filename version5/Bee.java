package version5;

import bugwars.*;

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

    private void tryMove() {
        if (!uc.canMove()) return;

        Location myQueen = manager.closestAllyQueen();
        Location targetQueen = manager.closestEnemyQueen();
        int distance = manager.myLocation.distanceSquared(myQueen);
        int enemies = 0;
        int allies = 1;

        for (UnitInfo ally : manager.units) {
            UnitType allyType = ally.getType();
            if (allyType != UnitType.QUEEN && allyType != UnitType.ANT && !manager.isObstructed(ally.getLocation())) {
                allies++;
            }
        }
        for (UnitInfo enemy : manager.enemies) {
            UnitType enemyType = enemy.getType();
            if (enemyType != UnitType.QUEEN && enemyType != UnitType.ANT && !manager.isObstructed(enemy.getLocation())) {
                enemies++;
            }
        }

        boolean moved = false;
        Direction myQueenDirection = manager.myLocation.directionTo(myQueen);
        Direction closestEnemy;
        Direction one;
        Direction two;
        Direction three;

        if (manager.enemies.length != 0) {
            closestEnemy = manager.myLocation.directionTo(manager.enemies[0].getLocation());
            one = closestEnemy.opposite();
            two = one.rotateLeft();
            three = one.rotateRight();
            if (myQueenDirection.isEqual(one) || myQueenDirection.isEqual(two) || myQueenDirection.isEqual(three)) {
                if (uc.getInfo().getHealth() * 2 < manager.unitHealth(manager.myType) && distance > 5 && (allies < enemies || manager.getTotalTroops() < 20)) {
                    manager.path.moveTo(myQueen);
                    moved = true;
                }
            }
        }

        if (manager.enemies.length != 0 && !moved) {
            moved = manager.path.evalLocation(allies, enemies);
        }
        if (!moved && uc.getInfo().getHealth() * 2 < manager.unitHealth(manager.myType) && distance > 5 && (allies < enemies || manager.getTotalTroops() < 20)) {
            manager.path.moveTo(myQueen);
        } else if (!moved) {
            manager.path.moveTo(targetQueen);
        }

        manager.postMoveUpdate();
    }

    private void tryAttack() {
        if (!uc.canAttack() || (manager.enemies.length == 0 && manager.rocks.length == 0)) {
            return;
        }

        if (manager.enemies.length != 0) {
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
        } else {
            int smallestRock = 10000000;
            int durability;
            RockInfo weakerRock = manager.rocks[0];

            for (RockInfo rock : manager.rocks) {
                durability = rock.getDurability();
                if (uc.canAttack(rock) && durability < smallestRock) {
                    smallestRock = durability;
                    weakerRock = rock;
                }
            }

            if (smallestRock != 10000000) {
                uc.attack(weakerRock);
            }
        }
    }
}