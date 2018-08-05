package version3;

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

        if (manager.getPassive() == 1 && manager.resources < 1000 && (manager.round < 1000 && manager.getEnemySpotted() == 0)) {
            if (manager.myLocation.distanceSquared(myQueen) > 36) {
                manager.path.moveTo(myQueen);
            }
        } else if (uc.getInfo().getHealth() * 2 < manager.unitHealth(manager.myType) && distance > 5 && (allies < enemies || manager.getTotalTroops() < 6)) {
            manager.path.moveTo(myQueen);
        } else if (manager.enemies.length != 0 && !manager.allObstructed()) {
            evalLocation();
        } else {
            manager.path.moveTo(targetQueen);
        }

        manager.myLocation = uc.getLocation();
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
        } else if (manager.rocks.length != 0) {
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

    private void evalLocation() {
        if (!uc.canMove()) return;

        Direction bestDirection = manager.dirs[8];
        int bestValue = -100000;

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

        for (int i = 0; i < manager.dirs.length; i++) {
            Location newLoc = manager.myLocation.add(manager.dirs[i]);
            if (uc.canMove(manager.dirs[i]) || manager.myLocation.isEqual(newLoc)) {
                int value = 0;
                int counter = 0;
                for (int j = 0; j < manager.enemies.length; j++) {
                    if (manager.enemies[j].getType() == UnitType.QUEEN && manager.enemies.length != 1) {
                        continue;
                    }
                    Location target = manager.enemies[j].getLocation();
                    int distance = newLoc.distanceSquared(target);
                    if (!manager.isObstructed(target) && allies >= enemies) {
                        if (distance < 3) {
                            value -= 200;
                        } else if (distance < 5) {
                            value -= 100;
                        } else if (distance == 5) {
                            if (counter == 0) {
                                value += 100;
                                counter++;
                            } else {
                                value -= 100;
                            }
                        } else {
                            value -= distance;
                        }
                    } else {
                        value += distance;
                    }
                }

                if (value >= bestValue) {
                    bestDirection = manager.dirs[i];
                    bestValue = value;
                }
            }
        }

        if (bestValue != -100000 && uc.canMove(bestDirection)) {
            uc.move(bestDirection);
        }
    }
}
