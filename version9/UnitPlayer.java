package version9;

import bugwars.user.UnitController;
import bugwars.user.UnitType;

public class UnitPlayer {

    public void run(UnitController uc) {

        MemoryManager manager = new MemoryManager(uc);

        Queen queen = new Queen(manager);
        Ant ant = new Ant(manager);
        Bee bee = new Bee(manager);
        Beetle beetle = new Beetle(manager);
        Spider spider = new Spider(manager);

        manager.removeCocoonList(uc.getInfo().getID());

        while (true){

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

            manager.map.work();
            uc.yield();
        }
    }
}
