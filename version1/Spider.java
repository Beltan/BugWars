package version1;

import bugwars.*;

public class Spider {

    private MemoryManager manager;
    private UnitController uc;

    public Spider(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    public void play() {
        tryAttack();
        tryMove();
        tryAttack();
    }

    private void tryMove() {
        if (manager.enemies.length == 0 || manager.allObstructed()) {
            Location targetQueen = manager.closestEnemyQueen();
            manager.path.moveTo(targetQueen);
        } else {
            evalLocation();
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

    private void evalLocation() {
        if (!uc.canMove()) return;

        Direction bestDirection = manager.dirs[8];
        int bestValue = -100000;

        for (int i = 0; i < manager.dirs.length; i++) {
            Location newLoc = manager.myLocation.add(manager.dirs[i]);
            if (uc.canMove(manager.dirs[i]) || manager.myLocation.isEqual(newLoc)) {
                int value = 0;
                for (int j = 0; j < manager.enemies.length; j++) {
                    Location target = manager.enemies[j].getLocation();
                    if (!manager.isObstructed(target)) {
                        int distance = newLoc.distanceSquared(target);
                        if (distance == 17) {
                            value += 100;
                        } else if (distance > 17) {
                            value -= 10;
                        } else if (distance < 9) {
                            value -= 1000;
                        } else if (distance < 14) {
                            value -= 50;
                        } else {
                            value += distance;
                        }
                    }
                }

                if (value >= bestValue) {
                    bestDirection = manager.dirs[i];
                    bestValue = value;
                }
            }
        }

        if (bestValue != -100000) {
            uc.move(bestDirection);
        }
    }
}