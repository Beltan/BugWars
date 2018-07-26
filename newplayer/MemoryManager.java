package newplayer;

import bugwars.*;

public class MemoryManager {

    // Initialize parameters
    public UnitController uc;
    public boolean root;
    public int round;
    public Team opponent;
    public Team allies;
    public Direction[] dirs;
    public Location myLocation;
    public UnitType myType;

    // Shared memory
    // 0 to 99 are general information and states
    public int PREVIOUS_ROUND = 0;
    public int CURRENT_ROUND = 1;
    public int CURRENT_MAP_SIZE = 2;
    public int FINAL_MAP_SIZE = 3;
    public int TEN_XCOORDINATE_CENTER = 4;
    public int TEN_YCOORDINATE_CENTER = 5;
    public int XHIGHER_BOUND = 6;
    public int XLOWER_BOUND = 7;
    public int YHIGHER_BOUND = 8;
    public int YLOWER_BOUND = 9;

    public MemoryManager(UnitController uc) {
        this.uc = uc;
        round = uc.getRound();
        opponent = uc.getOpponent();
        allies = uc.getTeam();
        dirs = Direction.values();
        myLocation = uc.getLocation();
        myType = uc.getType();
    }

    public void update() {
        round = uc.getRound();
        myLocation = uc.getLocation();

        // Root check
        uc.write(CURRENT_ROUND, round);
        int previousRound = uc.read(PREVIOUS_ROUND);

        if ((round != 0 && previousRound != round) || (round == 0 && previousRound == 0)) {
            uc.write(PREVIOUS_ROUND, round);
            root = true;
        } else {
            root = false;
        }

        if (root) {
            rootUpdate();
        }
    }

    private void rootUpdate() {
        if (round == 0) {
            roundZeroInitialization();
        }

    }

    public void roundZeroInitialization() {

    }

    public void mapSizeUpdate() {
        int range = unitRange();
    }

    private int unitRange() {
        int range = 0;
        switch (myType) {
            case ANT:
                range = (int) Math.sqrt(GameConstants.ANT_SIGHT_RANGE_SQUARED);
                break;
            case BEE:
                range = (int) Math.sqrt(GameConstants.BEE_SIGHT_RANGE_SQUARED);
                break;
            case BEETLE:
                range = (int) Math.sqrt(GameConstants.BEETLE_SIGHT_RANGE_SQUARED);
                break;
            case QUEEN:
                range = (int) Math.sqrt(GameConstants.QUEEN_SIGHT_RANGE_SQUARED);
                break;
            case SPIDER:
                range = (int) Math.sqrt(GameConstants.SPIDER_SIGHT_RANGE_SQUARED);
                break;
        }
        return range;
    }

}
