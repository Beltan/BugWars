package version0;

import bugwars.user.*;

public class UnitPlayer {

    public void run(UnitController uc) {

        // Insert here the code that should be executed only at the beginning of the unit's lifespan

        MemoryManager manager = new MemoryManager(uc);

        Queen queen = new Queen(manager);
        Ant ant = new Ant(manager);
        Bee bee = new Bee(manager);
        Beetle beetle = new Beetle(manager);
        Spider spider = new Spider(manager);

        manager.removeCocoonList(uc.getInfo().getID());

        while (true){

			// Insert here the code that should be executed every round

            manager.update();

            if (manager.myType == UnitType.QUEEN) {
                queen.play();
            } else if (manager.myType == UnitType.ANT) {
                ant.play();
            } else if (manager.myType == UnitType.BEE) {
                bee.play();
            } else if (manager.myType == UnitType.BEETLE) {
                beetle.play();
            } else if (manager.myType == UnitType.SPIDER) {
                spider.play();
            }

            uc.yield(); //End of turn
        }
    }
}
