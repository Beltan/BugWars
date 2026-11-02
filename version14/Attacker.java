package version14;

import bugwars.user.*;

public class Attacker {

    private MemoryManager manager;
    private UnitController uc;

    public Attacker(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    // Endgame queen first, then the weakest enemy in range, otherwise the breach rock or the weakest rock below maxRockDurability
    public void tryAttack(boolean breach, int maxRockDurability) {
        if (!uc.canAttack() || (manager.enemies.length == 0 && manager.rocks.length == 0)) return;

        UnitInfo queenTarget = manager.endgameQueenTarget();
        if (queenTarget != null) {
            uc.attack(queenTarget);
            return;
        }

        if (manager.enemies.length != 0) {
            attackWeakestEnemy();
            return;
        }
        if (breach && attackBreach()) return;
        attackWeakestRock(maxRockDurability);
    }

    private void attackWeakestEnemy() {
        int smallestHealth = manager.INF;
        UnitInfo lowestEnemy = null;
        int range = manager.myType.attackRangeSquared;
        for (UnitInfo enemy : manager.enemies) {
            // Enemies come nearest first, the rest are out of range
            if (manager.myLocation.distanceSquared(enemy.getLocation()) > range) break;
            int health = enemy.getHealth();
            if (health < smallestHealth && uc.canAttack(enemy)) {
                smallestHealth = health;
                lowestEnemy = enemy;
            }
        }
        if (lowestEnemy != null) {
            uc.attack(lowestEnemy);
            manager.tracker.onAttack(lowestEnemy);
        }
    }

    private boolean attackBreach() {
        Location breach = manager.map.breachTarget();
        if (breach == null || !uc.canSenseLocation(breach) || !uc.hasObstacle(breach)) return false;
        RockInfo breachRock = uc.senseObstacle(breach);
        if (breachRock == null || !uc.canAttack(breachRock)) return false;
        uc.attack(breachRock);
        return true;
    }

    private void attackWeakestRock(int maxRockDurability) {
        int smallestRock = maxRockDurability;
        RockInfo weakestRock = null;
        int range = manager.myType.attackRangeSquared;
        for (RockInfo rock : manager.rocks) {
            if (manager.myLocation.distanceSquared(rock.getLocation()) > range) break;
            int durability = rock.getDurability();
            if (durability < smallestRock && uc.canAttack(rock)) {
                smallestRock = durability;
                weakestRock = rock;
            }
        }
        if (weakestRock != null) uc.attack(weakestRock);
    }
}
