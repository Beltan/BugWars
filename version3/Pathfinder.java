package version3;

import bugwars.*;

public class Pathfinder {

    private MemoryManager manager;
    private UnitController uc;

    public Pathfinder(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    final int INF = 1000000;

    boolean rotateRight = false; // Should rotate right or left
    Location lastObstacleFound = null; // Latest obstacle found
    int minDistToEnemy = INF; // Minimum distance while going around an obstacle
    Location prevTarget = null; // Previous target
    int counter = 0;

    public void moveTo(Location target){
        if (!uc.canMove()) return;
        if (target == null) return;

        if (prevTarget == null || target.distanceSquared(prevTarget) > 9 || manager.myLocation.isEqual(target) || (uc.canSenseLocation(target) && !manager.isObstructed(target))) {
            resetPathfinding();
        }

        // If minimum distance to the target, reset
        int d = manager.myLocation.distanceSquared(target);
        if (d <= minDistToEnemy) resetPathfinding();

        // Update data
        prevTarget = target;
        minDistToEnemy = Math.min(d, minDistToEnemy);

        // If there's an obstacle, try to go around it instead of going to the target directly
        Direction dir = manager.myLocation.directionTo(target);
        if (lastObstacleFound != null) dir = manager.myLocation.directionTo(lastObstacleFound);

        // This should not happen for a single unit, but whatever
        if (uc.canMove(dir)) resetPathfinding();

        // Rotate clockwise or counterclockwise. If try to go out of the map change the orientation
        for (int i = 0; i < 16; ++i){
            if (uc.canMove(dir)){
                uc.move(dir);
                counter = 0;
                break;
            }
            Location newLoc = manager.myLocation.add(dir);
            if (uc.isOutOfMap(newLoc)) {
                rotateRight = !rotateRight;
            } else {
                // Update latest obstacle found and check counter for unit obstacles
                Location possibleObstacle = manager.myLocation.add(dir);
                if (uc.senseUnit(possibleObstacle) == null || counter > 2){
                    lastObstacleFound = manager.myLocation.add(dir);
                    if (rotateRight) dir = dir.rotateRight();
                    else dir = dir.rotateLeft();
                } else {
                    counter++;
                }
                if (counter > 4) {
                    resetPathfinding();
                }
            }
        }

        if (uc.canMove(dir)) {
            uc.move(dir);
            counter = 0;
        }
    }

    boolean rotateRightQueen = false; // Should rotate right or left
    Location lastObstacleFoundQueen = null; // Latest obstacle found
    int minDistToEnemyQueen = INF; // Minimum distance while going around an obstacle
    Location prevTargetQueen = null; // Previous target
    int counterQueen = 0;

    public void moveToQueen(Location target){
        if (!uc.canMove()) return;
        if (target == null) return;

        if (prevTargetQueen == null || target.distanceSquared(prevTargetQueen) > 9 || manager.myLocation.isEqual(target) || (uc.canSenseLocation(target) && !manager.isObstructed(target))) {
            resetPathfindingQueen();
        }

        // If minimum distance to the target, reset
        int d = manager.myLocation.distanceSquared(target);
        if (d <= minDistToEnemyQueen) resetPathfindingQueen();

        // Update data
        prevTargetQueen = target;
        minDistToEnemyQueen = Math.min(d, minDistToEnemyQueen);

        // If there's an obstacle, try to go around it instead of going to the target directly
        Direction dir = manager.myLocation.directionTo(target);
        if (lastObstacleFoundQueen != null) dir = manager.myLocation.directionTo(lastObstacleFoundQueen);

        // This should not happen for a single unit, but whatever
        if (uc.canMove(dir)) resetPathfindingQueen();

        // Rotate clockwise or counterclockwise. If try to go out of the map change the orientation
        for (int i = 0; i < 16; ++i){
            if (uc.canMove(dir)){
                uc.move(dir);
                counterQueen = 0;
                break;
            }
            Location newLoc = manager.myLocation.add(dir);
            if (uc.isOutOfMap(newLoc)) {
                rotateRightQueen = !rotateRightQueen;
            } else {
                // Update latest obstacle found and check counter for unit obstacles
                Location possibleObstacle = manager.myLocation.add(dir);
                if (uc.senseUnit(possibleObstacle) == null || counterQueen > 12){
                    lastObstacleFoundQueen = manager.myLocation.add(dir);
                    if (rotateRightQueen) dir = dir.rotateRight();
                    else dir = dir.rotateLeft();
                } else {
                    counterQueen++;
                    break;
                }
                if (counterQueen > 15) {
                    resetPathfindingQueen();
                }
            }
        }

        if (uc.canMove(dir)) {
            uc.move(dir);
            counterQueen = 0;
        }
    }

    // Clear some of the previous data
    private void resetPathfindingQueen(){
        lastObstacleFoundQueen = null;
        minDistToEnemyQueen = INF;
        counterQueen = 0;
    }

    // Clear some of the previous data
    private void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
        counter = 0;
    }

}
