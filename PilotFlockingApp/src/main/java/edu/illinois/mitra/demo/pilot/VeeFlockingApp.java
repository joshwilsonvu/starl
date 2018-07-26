package edu.illinois.mitra.demo.pilot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.PickedLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.gvh.RobotGroup;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public class VeeFlockingApp extends LogicThread {
    private int numGroups = 2;
    private int offsetDistance = 0;

    boolean initializeVee;
    private int leaderNum;
    private String leaderName;

    PositionList<ItemPosition> destinations = new PositionList();

    private enum STAGE {START, SYNC, MOVE, DONE}

    private STAGE stage = STAGE.START;
    private LeaderElection le;
    private Synchronizer sn;

    public VeeFlockingApp(GlobalVarHolder gvh) {
        super(gvh);
        initializeVee = true;
        //gvh.trace.traceStart();
        le = new PickedLeaderElection(gvh);
        sn = new BarrierSynchronizer(gvh, gvh.id.getParticipants().size()-1);

        MotionParameters.Builder settings = new MotionParameters.Builder();
        settings = settings.ENABLE_ARCING(true);
        settings = settings.STOP_AT_DESTINATION(true);
        settings = settings.COLAVOID_MODE(MotionParameters.COLAVOID_MODE_TYPE.USE_COLAVOID); // buggy, just goes back, deadlocks...
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);

        le.elect();
    }

    @Override
    public List<Object> callStarL() {
        String robotName = gvh.id.getName();
        Integer robotNum = gvh.id.getIdNumber();
        Integer count = 0;

        while (true) {
            switch (stage) {
                case START: {
                    if (le.getLeader() != null) {
                        if (initializeVee) {
                            System.out.printf("Robot %3d, leader is: " + le.getLeader() + "\n", robotNum);
                            leaderNum = le.getLeaderID();
                            leaderName = le.getLeader();
                        }
                        if(!gvh.id.getName().equals(leaderName)){
                            if(initializeVee){
                                if(gvh.id.getIdNumber() ==0){
                                    gvh.BotGroup = new RobotGroup(le.getLeaderID(), numGroups);
                                } else {
                                    gvh.BotGroup = new RobotGroup(gvh.id.getIdNumber(), numGroups);
                                }
                            }
                            sn.barrierSync("round" + count.toString());
                            stage = STAGE.SYNC;
                        } else{
                            stage = STAGE.MOVE;
                        }

                    }


                    break;
                }
                case SYNC: {
                    if (sn.barrierProceed("round" + count.toString())) {
                        getRankings(robotNum, leaderNum, robotName);
                        stage = STAGE.MOVE;
                    }
                    break;
                }
                case MOVE: {
                    if (!gvh.plat.moat.inMotion) {
                        ItemPosition dest;

                        if (initializeVee) {
                            dest = getInFormation(gvh.id.getName());
                            destinations.update(dest);
                            gvh.plat.moat.goTo(dest);
                        } else if(gvh.id.getName().equals(le.getLeader())) {
                            gvh.plat.moat.userControl(1000);
                            //gvh.plat.moat.goTo(new ItemPosition("test",(-2000),-1000,0));
                            //test -= 500;
                        } else {
                            dest = calculateMovement(robotName);
                            destinations.update(dest);
                            gvh.plat.moat.goTo(dest);

                        }
                        count += 1;
                    }

                    // wait here while robot is in motion
                    while (gvh.plat.moat.inMotion) {
                        gvh.sleep(100);
                    }

                    initializeVee = false;

                    stage = STAGE.START;
                    break;
                }

                case DONE: {
                    gvh.trace.traceEnd();
                    return Arrays.asList(results);
                }
            }
            gvh.sleep(100);

        }
    }

    public ItemPosition getInFormation(String name){
        ItemPosition dest;

        if (gvh.id.getName().equals(le.getLeader())) {
            dest = new ItemPosition(name, 0, 0, 0); // Let the leader in the center
            gvh.plat.moat.goTo(dest);
        } else {
            // All other bots move to their place according to their order in the group
            int oldX = gvh.BotGroup.getRank() * 1000;
            int oldY;

            if(gvh.BotGroup.getRank()%2 == 0){
                oldY = offsetDistance;
            } else{
                oldY = -offsetDistance;
            }

            System.out.println(gvh.BotGroup.getTheta() + " here " + oldY);

            double newXX = oldX * Math.cos(Math.toRadians(gvh.BotGroup.getTheta())) - oldY * Math.sin(Math.toRadians(gvh.BotGroup.getTheta()));
            double newYY = oldY * Math.cos(Math.toRadians(gvh.BotGroup.getTheta())) + oldX * Math.sin(Math.toRadians(gvh.BotGroup.getTheta()));
            int newX = (int) newXX;
            int newY = (int) newYY;



            dest = new ItemPosition(name, newX, newY, 0);
            gvh.plat.moat.goTo(dest);
            System.out.printf("%s rank: %d Going to X:%d Y:%d \n", gvh.id.getName(),gvh.BotGroup.getRank(), newX, newY);

        }
        return dest;
    }

    public ItemPosition calculateMovement(String robotName){
        /*
        * In order to do a translation to the origin, you need to know the position of the leader.
        * It can be directly ommunicated, but it may be possible to approximate the distance each robot is from the leader,
        * By using group rank and group theta, that can give an approximate distance needed for translation.
        *
        * First, must be flocking to ensure approximation is correct. If not flocking, then it should be able to get in flocking without translating.
        */
        int newX, newY, beforeX, beforeY, afterX, afterY;
        int X, Y,beforeXTran,beforeYTran,afterXTran,afterYTran;
        X = gvh.gps.getMyPosition().getX();
        Y = gvh.gps.getMyPosition().getY();

        ItemPosition leaderPosition = gvh.gps.getPosition(leaderName);
        ItemPosition BeforeBot = new ItemPosition("BeforeBot", 0, 0, 0);
        ItemPosition AfterBot = new ItemPosition("AfterBot", 0, 0, 0);

        if (!gvh.id.getName().equals(le.getLeader())) {
            BeforeBot = gvh.gps.getPosition(gvh.BotGroup.getBeforeBot());
            if (!gvh.BotGroup.isLast){
                AfterBot = gvh.gps.getPosition(gvh.BotGroup.getAfterBot());
            }

        }

        System.out.println(gvh.BotGroup.getBeforeBot() + " here " + gvh.BotGroup.getAfterBot());
        /*
        Method to find translation distance without knowing leader position by using current position and approximate distance to leader robot, based on rf and rank.
        Problem: Can find distance to lead drone, but can't find direction relative to origin, so not sure whether to add distance or subtract.
        Attempt: Use coordinates to find quadrant, but will only work if leader is always inside, not practical. Could use beforeBot position with angle
                to approximate direction, but not realistid
         */

        /*if(is_Flocking()){
            if(BeforeBot.getName().equals(leaderName)){
                 X = gvh.gps.getMyPosition().getX() - BeforeBot.getX();
                 Y = gvh.gps.getMyPosition().getY() - BeforeBot.getY();
                 beforeXTran = 0;
                 beforeYTran = 0;
                 afterXTran = AfterBot.getX()- BeforeBot.getX();
                 afterYTran = AfterBot.getY()- BeforeBot.getY();
            } else{
                X = X - (X-(int)(Math.cos(Math.toRadians(gvh.BotGroup.getTheta()))*(gvh.BotGroup.getRank()*gvh.BotGroup.getRF())));
                Y = Y - (Y-(int)(Math.sin(Math.toRadians(gvh.BotGroup.getTheta()))*(gvh.BotGroup.getRank()*gvh.BotGroup.getRF())));
            }
        }*/

        /*//Translate Coordinates
        newX = gvh.gps.getMyPosition().getX();// - BeforeBot.getX();
        newY = gvh.gps.getMyPosition().getY();// - BeforeBot.getY();
        beforeX = BeforeBot.getX();//0;
        beforeY = BeforeBot.getY();//0;
        afterX = AfterBot.getX();// - BeforeBot.getX();
        afterY = AfterBot.getY();//() - BeforeBot.getY();*/

        //Translate
        X = X - leaderPosition.getX();
        Y = Y - leaderPosition.getY();
        beforeXTran = BeforeBot.getX()- gvh.gps.getPosition(leaderName).getX();
        beforeYTran = BeforeBot.getY()- leaderPosition.getY();
        afterXTran = AfterBot.getX()- leaderPosition.getX();
        afterYTran = AfterBot.getY()- leaderPosition.getY();

        //Rotate
        newX = (int)rotate(X, Y,-1, "x");
        newY = (int)rotate(X, Y,-1, "y");
        beforeX = (int)rotate(beforeXTran, beforeYTran,-1, "x");
        beforeY = (int)rotate(beforeXTran, beforeYTran,-1, "y");
        afterX = (int)rotate(afterXTran, afterYTran,-1, "x");
        afterY = (int)rotate(afterXTran, afterYTran,-1, "y");

        //Remove Offset
        if(gvh.BotGroup.getRank()%2 == 0){
            newY -= offsetDistance;
        } else{
            newY += offsetDistance;
        }

        //Flocking
        if (!gvh.id.getName().equals(le.getLeader())) {
            //If Robot is the Rightmost (Last robot in the group)
            if (gvh.BotGroup.isLast) {
                newX = (beforeX + newX + gvh.BotGroup.getRF()) / 2;
                newY = (beforeY + newY) / 2;
            } else {
                newX = (beforeX + afterX) / 2;
                newY = (beforeY + afterY) / 2;
            }
        }
        System.out.printf("Flocking %s x %d y %d \n",gvh.id.getName(),newX,newY);

        if (is_Flocking()) {
            if (gvh.id.getName().equals(le.getLeader())) {
                gvh.BotGroup.setTheta(gvh.BotGroup.getTheta() + 90);
            } else {
                gvh.BotGroup.setTheta(gvh.BotGroup.getTheta() + 30);
                //gvh.BotGroup.setTheta() = (gvh.BotGroup.getTheta() - Math.atan(beforeBotPositions[1].getY() / beforeBotPositions[1].getX()));
                //gvh.BotGroup.setTheta() =gvh.BotGroup.getTheta() +30;
            }
        }

        //Add back offset
        if(gvh.BotGroup.getRank()%2 == 0){
            newY += offsetDistance;
        } else{
            newY -= offsetDistance;
        }

        //Rotate Back, have to use these doubles or method returns incorrect value.
        double testX = rotate(newX,newY,1,"x");
        double testY = rotate(newX,newY,1,"y");
        newX = (int)testX;
        newY = (int)testY;

        //Translate Back
        newX = newX + leaderPosition.getX();
        newY = newY + leaderPosition.getY();

        System.out.println(robotName + " has old coordination X " + gvh.gps.getMyPosition().getX() + " and Y " + gvh.gps.getMyPosition().getY() + " New X is " + newX + " and New Y is " + newY);
        return new ItemPosition(gvh.id.getName(), newX, newY);

    }

    public double rotate(int x, int y, int direction, String axis){
        if(axis.toLowerCase().equals("x")){
            return (x * Math.cos(Math.toRadians(direction * gvh.BotGroup.getTheta())) - y * Math.sin(Math.toRadians(direction * gvh.BotGroup.getTheta())));
        } else if(axis.toLowerCase().equals("y")) {
            return (y * Math.cos(Math.toRadians(direction * gvh.BotGroup.getTheta())) + x * Math.sin(Math.toRadians(direction * gvh.BotGroup.getTheta())));
        } else{
            return -1;
        }
    }

    public boolean is_Flocking() {
        boolean isFlocking = true;

        ItemPosition BeforeBot = new ItemPosition("BeforeBot", 0, 0, 0);
        ItemPosition AfterBot = new ItemPosition("AfterBot", 0, 0, 0);
        ItemPosition Bot = new ItemPosition("Bot", 0, 0, 0);

        int groupDis = 0;

        boolean once = true;
        for (int i = 0; i < gvh.id.getParticipants().size(); i++) {
            if (i != leaderNum) {
                groupDis = gvh.BotGroup.getRF();
                PositionList<ItemPosition> plAll = gvh.gps.get_robot_Positions();
                Bot = gvh.gps.getMyPosition();
                BeforeBot = gvh.gps.getPosition(gvh.BotGroup.getBeforeBot());

                if(!gvh.BotGroup.isLast){
                    AfterBot = gvh.gps.getPosition(gvh.BotGroup.getAfterBot());
                }

                // Distance between the bot and his before (left) neighbour
                double botDistance = Math.sqrt((Bot.getX() - BeforeBot.getX()) * (Bot.getX() - BeforeBot.getX()) + (Bot.getY() - BeforeBot.getY()) * (Bot.getY() - BeforeBot.getY()));
                if (botDistance < (groupDis - groupDis * 0.3) || botDistance > (groupDis + groupDis * 0.3)) {

                    System.out.println("It is false because before bot is out of the range, their distance between each other is " + String.valueOf(botDistance));
                    return false;
                }

                // Distance between the bot and his after (right) neighbour
                if (!gvh.BotGroup.isLast) {
                    double botDistanceAfter = Math.sqrt((Bot.getX() - AfterBot.getX()) * (Bot.getX() - AfterBot.getX()) + (Bot.getY() - AfterBot.getY()) * (Bot.getY() - AfterBot.getY()));

                    if (botDistanceAfter < (groupDis - groupDis * 0.3) || botDistanceAfter > (groupDis + groupDis * 0.3)) {
                        System.out.println("It is false because after bot is out of the range, their distance between each other is " + String.valueOf(botDistance));
                        return false;
                    }
                }
            }
        }

        return isFlocking;
    }

    public void getRankings(int robotNum, int leaderNum, String robotName) {
        // All below code in Elect state is to assign order-rank- for each robot in its group
        if (robotNum != leaderNum) {
            if (gvh.BotGroup.setAfterBefore) {
                ItemPosition myPosition = gvh.gps.getMyPosition();
                int mySummation = myPosition.getX() + myPosition.getY();
                int ranking = 1;
                PositionList<ItemPosition> plAll = gvh.gps.get_robot_Positions();
                for (ItemPosition rp : plAll.getList()) {
                    Integer rpNum = Integer.valueOf(rp.getName().substring(6));
                    if (rpNum != leaderNum) {
                        if (rpNum != robotNum) {
                            Integer rpGroup = Integer.valueOf(rp.getName().substring(6)) % numGroups;
                            if (gvh.BotGroup.getGroupNum() == rpGroup) {
                                int otherSummation = rp.getX() + rp.getY();

                                if (mySummation == otherSummation)
                                    System.out.println("############************** There is potential same locations ***************########### " + robotName + " and " + rp.getName());

                                if (mySummation >= otherSummation) {
                                    if (gvh.BotGroup.getBeforeBot() == null) {
                                        if (mySummation == otherSummation) {
                                            if (robotNum > Integer.valueOf(rp.getName().substring(6))) {
                                                gvh.BotGroup.setBeforeBot(rp.getName());
//here
                                            }
                                        } else {
                                            gvh.BotGroup.setBeforeBot(rp.getName());
                                        }

                                    } else {
                                        int xSub = 0;
                                        int ySub = 0;
                                        // int angleSub = 0;
                                        PositionList<ItemPosition> plAllSub = gvh.gps.get_robot_Positions();
                                        for (ItemPosition rpSub : plAllSub.getList()) {
                                            if (Integer.valueOf(rpSub.getName().substring(6)) == Integer.valueOf(gvh.BotGroup.getBeforeBot().substring(6))) {
                                                xSub = rpSub.getX();
                                                ySub = rpSub.getY();
                                                //angleSub = rpSub.angle;
                                            }

                                        }
                                        int beforeBotSummation = xSub + ySub;
                                        if (otherSummation > beforeBotSummation) {
                                            gvh.BotGroup.setBeforeBot(rp.getName());
                                        }
                                    }
                                    if (mySummation == otherSummation) {

                                        System.out.println("############************** There is potential same locations ***************########### " + robotName + " and " + rp.getName());
                                        if (robotNum < Integer.valueOf(rp.getName().substring(6))) {
                                            gvh.BotGroup.setAfterBot(rp.getName());
                                        } else {
                                            gvh.BotGroup.setBeforeBot(rp.getName());
                                            ranking++;
                                        }
                                    } else
                                        ranking++;
                                } else if (mySummation < otherSummation)
                                    if (gvh.BotGroup.getAfterBot() == null)
                                        gvh.BotGroup.setAfterBot(rp.getName());
                                    else {
                                        int xSub = 0;
                                        int ySub = 0;
                                        // int angleSub = 0;
                                        PositionList<ItemPosition> plAllSub = gvh.gps.get_robot_Positions();
                                        for (ItemPosition rpSub : plAllSub.getList()) {
                                            if (Integer.valueOf(rpSub.getName().substring(6)) == Integer.valueOf(gvh.BotGroup.getAfterBot().substring(6))) {
                                                xSub = rpSub.getX();
                                                ySub = rpSub.getY();
                                                //angleSub = rpSub.angle;
                                            }

                                        }
                                        int afterBotSummation = xSub + ySub;

                                        if (otherSummation == afterBotSummation)
                                            if (robotNum < Integer.valueOf(rp.getName().substring(6)))
                                                gvh.BotGroup.setAfterBot(rp.getName());

                                        if (otherSummation < afterBotSummation)
                                            gvh.BotGroup.setAfterBot(rp.getName());
                                    }
                                else if (Integer.valueOf(gvh.id.getName().substring(6)) > Integer.valueOf(rp.getName().substring(6))) {
                                    gvh.BotGroup.setBeforeBot(rp.getName());
                                    ranking++;
                                } else gvh.BotGroup.setAfterBot(rp.getName());
                            }

                        }
                    }
                }
                gvh.BotGroup.setRank(ranking);
                gvh.BotGroup.setAfterBefore = false;
                if (gvh.BotGroup.getBeforeBot() == null) {
                    plAll = gvh.gps.get_robot_Positions();
                    for (ItemPosition rp : plAll.getList())
                        if (Integer.valueOf(rp.getName().substring(6)) == leaderNum)
                            gvh.BotGroup.setBeforeBot(rp.getName());
                }
                if (gvh.BotGroup.getAfterBot() == null)
                    gvh.BotGroup.isLast = true;

                gvh.BotGroup.setAfterBefore = false;
            }
        }
    }


}
