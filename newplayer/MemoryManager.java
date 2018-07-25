package newplayer;

import bugwars.*;

public class MemoryManager {

    public UnitController uc;

    public boolean root;

    public int PREVIOUS_ROUND = 0;
    public int CURRENT_ROUND = 1;

    public int round;
    public Team opponent;
    public Team allies;
    public Direction[] dirs;
    public Location myLocation;
    public UnitType myType;

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

    }

}
