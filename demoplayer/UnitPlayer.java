package demoplayer;

import bugwars.*;

public class UnitPlayer {

    public void run(UnitController uc) {
	/*Insert here the code that should be executed only at the beginning of the unit's lifespan*/

	    /*enemy team*/
	    Team opponent = uc.getOpponent();

        while (true){
			/*Insert here the code that should be executed every round*/

			/*Generate a random number from 0 to 7, both included*/
			int randomNumber = (int)(Math.random()*8);

			/*Get corresponding direction*/
			Direction dir = Direction.values()[randomNumber];

			/*move in direction dir if possible*/
			if (uc.canMove(dir)) uc.move(dir);

			/*If this unit is a queen, try spawning a beetle at direction dir*/
			if (uc.getType() == UnitType.QUEEN) {
                if (uc.canSpawn(dir, UnitType.BEETLE)) uc.spawn(dir, UnitType.BEETLE);
            }

			/*Else, go through all visible units and attack the first one you see*/
			else {
                UnitInfo[] visibleEnemies = uc.senseUnits(opponent);
                for (int i = 0; i < visibleEnemies.length; ++i) {
                    if (uc.canAttack(visibleEnemies[i])) uc.attack(visibleEnemies[i]);
                }
            }

            uc.yield(); //End of turn
        }

    }
}
