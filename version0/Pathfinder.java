package version0;

import bugwars.user.*;

public class Pathfinder {

    private MemoryManager manager;
    private UnitController uc;

    public Pathfinder(MemoryManager manager) {
        this.manager = manager;
        uc = manager.uc;
    }

    final int INF = 1000000;

    boolean rotateRight = true; // Should rotate right or left
    Location lastObstacleFound = null; // Latest obstacle found
    int minDistToEnemy = INF; // Minimum distance while going around an obstacle
    Location prevTarget = null; // Previous target
    int counter = 0;

    public void moveTo(Location target){
        if (!uc.canMove()) return;
        if (target == null) return;

        // Different target? ==> previous data does not help!
        if (prevTarget == null || !target.isEqual(prevTarget)) resetPathfinding();

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
                break;
            }
            Location newLoc = manager.myLocation.add(dir);
            if (uc.isOutOfMap(newLoc)) {
                rotateRight = !rotateRight;
            } else {
                // Update latest obstacle found and check counter for unit obstacles
                Location possibleObstacle = manager.myLocation.add(dir);
                if (uc.senseUnit(possibleObstacle) == null || counter > 1){
                    lastObstacleFound = manager.myLocation.add(dir);
                    if (rotateRight) dir = dir.rotateRight();
                    else dir = dir.rotateLeft();
                } else {
                    counter++;
                    if (counter > 10) {
                        resetPathfinding();
                    }
                }
            }
        }

        if (uc.canMove(dir)) uc.move(dir);
    }

    // Clear some of the previous data
    private void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
    }

}
