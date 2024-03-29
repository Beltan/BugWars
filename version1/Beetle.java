package version1;

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
        Location myQueen = manager.closestAllyQueen();
        if (uc.getInfo().getHealth() * 2 < manager.unitHealth(manager.myType)) {
            if (manager.myLocation.distanceSquared(myQueen) > 5) {
                manager.path.moveTo(myQueen);
            }
        } else if (uc.getInfo().getHealth() * 3 < manager.unitHealth(manager.myType) * 2 && manager.myLocation.distanceSquared(myQueen) < 6) {
            if (!manager.isObstructed(myQueen)) {
                evalLocation();
            } else {
                manager.path.moveTo(myQueen);
            }
        } else {
            if (manager.enemies.length == 0 || manager.allObstructed()) {
                Location targetQueen = manager.closestEnemyQueen();
                manager.path.moveTo(targetQueen);
            } else {
                evalLocation();
            }
        }
        manager.enemies = uc.senseUnits(manager.opponent);
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
        } else if (manager.allObstructed()) {
            for (RockInfo rock : manager.rocks) {
                if (uc.canAttack(rock)) {
                    uc.attack(rock);
                    break;
                }
            }
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
                        if (distance < 3) {
                            value -= 100;
                        } else if (distance < 6) {
                            value += 100;
                        } else {
                            value -= distance;
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
