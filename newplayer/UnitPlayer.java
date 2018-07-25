package newplayer;

import bugwars.*;

public class UnitPlayer {

    public void run(UnitController uc) {
	/*Insert here the code that should be executed only at the beginning of the unit's lifespan*/
        MemoryManager manager = new MemoryManager(uc);
        Queen queen = new Queen();
        Ant ant = new Ant();
        Bee bee = new Bee();
        Beetle beetle = new Beetle();
        Spider spider = new Spider();

        while (true){
			/*Insert here the code that should be executed every round*/

            manager.update();

            switch(manager.myType) {
                case QUEEN:
                    queen.play();
                    break;
                case ANT:
                    ant.play();
                    break;
                case BEETLE:
                    beetle.play();
                    break;
                case BEE:
                    bee.play();
                    break;
                case SPIDER:
                    spider.play();
                    break;
            }

            uc.yield(); //End of turn
        }
    }
}
