package edu.illinois.mitra.demo.flocking;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.illinois.mitra.starl.functions.PickedLeaderElection;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;

import edu.illinois.mitra.starl.gvh.RobotGroup;

import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.MotionParameters;

import edu.illinois.mitra.starl.objects.Common;

import edu.illinois.mitra.starl.objects.ItemPosition;

import edu.illinois.mitra.starl.functions.BarrierSynchronizer;
import edu.illinois.mitra.starl.functions.RandomLeaderElection;
import edu.illinois.mitra.starl.interfaces.LeaderElection;
import edu.illinois.mitra.starl.interfaces.Synchronizer;
import edu.illinois.mitra.starl.objects.PositionList;

/*
 * Created by Mousa Almotairi on 4/28/2015.
 *
 * Flocking app maintains vee formation as it rotates around middle, doesn't have translations.
 * Relies on central bot list in common class to determine if flocking.
 */


public class FlockingApp extends LogicThread {

    private static final boolean RANDOM_DESTINATION = false;
    String wpn = "wp";
    boolean initializeVee;
    private int n_waypoints;
    private int cur_waypoint = 0;
    private int leaderNum;

    PositionList<ItemPosition> destinations = new PositionList();

    private enum STAGE {START, SYNC, ELECT, MOVE, DONE}

    private STAGE stage = STAGE.START;

    private LeaderElection le;
    private Synchronizer sn;


    public FlockingApp(GlobalVarHolder gvh) {
        super(gvh);
        initializeVee = true;
        //gvh.trace.traceStart();

        le = new PickedLeaderElection(gvh);

        gvh.BotGroup = new RobotGroup(gvh.id.getIdNumber(), Common.numOFgroups);
        sn = new BarrierSynchronizer(gvh, gvh.id.getParticipants().size());

        MotionParameters.Builder settings = new MotionParameters.Builder();
        settings = settings.ENABLE_ARCING(true);
        settings = settings.STOP_AT_DESTINATION(true);
        settings = settings.COLAVOID_MODE(MotionParameters.COLAVOID_MODE_TYPE.USE_COLAVOID); // buggy, just goes back, deadlocks...
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);

        //n_waypoints = gvh.gps.getWaypointPositions().getNumPositions();
        n_waypoints = Integer.MAX_VALUE;
        String n = wpn + gvh.id.getName() + cur_waypoint;
        destinations.update(new ItemPosition(n, 2000, 2000, 0));

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
                    sn.barrierSync("round" + count.toString());
                    stage = STAGE.SYNC;
                    System.out.printf("robot %3d, round " + count.toString() + "\n", robotNum);

                    break;
                }
                case SYNC: {
                    if (sn.barrierProceed("round" + count.toString())) {
                        stage = STAGE.ELECT;


                    }
                    break;
                }
                case ELECT: {
                    if (le.getLeader() != null) {
                        System.out.printf("robot %3d, leader is: " + le.getLeader() + "\n", robotNum);
                        stage = STAGE.MOVE;
                        if(initializeVee) {
                            leaderNum = le.getLeaderID();
                            getRankings(robotNum, leaderNum, robotName);
                            // For Testing purpose
                            for (int i = 0; i < Common.numOFbots; i++) {
                                System.out.println("bot" + i + " and his before bot is " + Common.bots_neighbour[i][0] + " and his after bot is " + Common.bots_neighbour[i][1] + " and group distance is " + Common.bots_neighbour[i][2]);
                            }
                        }


                    }
                    break;
                }
                case MOVE: {
                    if (!gvh.plat.moat.inMotion) {
                        //if(cur_waypoint < n_waypoints) {
                        //System.out.println(robotName + ": I've stopped moving!");
                        String n = wpn + gvh.id.getName() + cur_waypoint;
                        System.out.println(robotName + ": New destination is (" + destinations.getPosition(n).getX() + ", " + destinations.getPosition(n).getY() + ")!");

                        ItemPosition dest;
                        if (initializeVee) {
                            // Let the leader in the center
                            if (gvh.id.getName().equals(le.getLeader())) {
                                dest = new ItemPosition(n, 0, 0, 0);
                                gvh.plat.moat.goTo(dest);
                                System.out.println("Leader " + gvh.id.getName() + " going to " + dest);

                            } else {

                                // All other bot_info move to their place according to their order in the group
                                System.out.println(gvh.id.getName() + " rank " + gvh.BotGroup.getRank());
                                int oldX = gvh.BotGroup.getRank() * 1000;
                                int oldY = 0;

                                System.out.println(gvh.id.getName() + " " + oldX + " " + oldY);

                                double newXX = oldX * Math.cos(Math.toRadians(gvh.BotGroup.getTheta())) - oldY * Math.sin(Math.toRadians(gvh.BotGroup.getTheta()));
                                double newYY = oldY * Math.cos(Math.toRadians(gvh.BotGroup.getTheta())) + oldX * Math.sin(Math.toRadians(gvh.BotGroup.getTheta()));

                                int newX = (int) newXX;
                                int newY = (int) newYY;

                                dest = new ItemPosition(n, newX, newY, (int)gvh.BotGroup.getTheta());
                                gvh.plat.moat.goTo(dest);
                                System.out.printf("%s Going to X:%d Y:%d \n", gvh.id.getName(), newX, newY);


                            }
                            destinations.update(dest);


                            initializeVee = false;
                        } else {

                            //*********************** START: Rotation **********************
                            int newX = 0;
                            int newY = 0;
                            int beforeX = 0;
                            int beforeY = 0;
                            int afterX = 0;
                            int afterY = 0;
                            ItemPosition BeforeBot = new ItemPosition("BeforeBot", 0, 0, 0);
                            ItemPosition AfterBot = new ItemPosition("AfterBot", 0, 0, 0);


                            if (!gvh.id.getName().equals(le.getLeader())) {
                                PositionList<ItemPosition> plAll = gvh.gps.get_robot_Positions();
                                for (ItemPosition rp : plAll.getList()) {
                                    if (rp.getName().equals(gvh.BotGroup.getBeforeBot()))
                                        BeforeBot = rp;
                                    if (!gvh.BotGroup.isLast) {
                                        if (rp.getName().equals(gvh.BotGroup.getAfterBot()))
                                            AfterBot = rp;
                                    }

                                }
                            }


                              //****************** Rotation for the robot********************
                            double newXX = rotate(gvh.gps.getMyPosition().getX(), gvh.gps.getMyPosition().getY(),-1, "x");
                            double newYY = rotate(gvh.gps.getMyPosition().getX(), gvh.gps.getMyPosition().getY(),-1, "y");
                            newX = (int) newXX;
                            newY = (int) newYY;

                            System.out.printf("Second %s X %d Y %d XX %f \n",gvh.id.getName(), newX,newY, newXX);


                            //******************** Rotation for robot before the Robot (Left Robot)**************
                            double beforeXX = BeforeBot.getX() * Math.cos(Math.toRadians(-gvh.BotGroup.getTheta())) - BeforeBot.getY() * Math.sin(Math.toRadians(-gvh.BotGroup.getTheta()));
                            double beforeYY = BeforeBot.getY() * Math.cos(Math.toRadians(-gvh.BotGroup.getTheta())) + BeforeBot.getX() * Math.sin(Math.toRadians(-gvh.BotGroup.getTheta()));

                            beforeX = (int) beforeXX;
                            beforeY = (int) beforeYY;

                            //******************** Rotation for robot after the Robot (right Robot)**************
                            double afterXX = AfterBot.getX() * Math.cos(Math.toRadians(-gvh.BotGroup.getTheta())) - AfterBot.getY() * Math.sin(Math.toRadians(-gvh.BotGroup.getTheta()));
                            double afterYY = AfterBot.getY() * Math.cos(Math.toRadians(-gvh.BotGroup.getTheta())) + AfterBot.getX() * Math.sin(Math.toRadians(-gvh.BotGroup.getTheta()));


                            afterX = (int) afterXX;
                            afterY = (int) afterYY;


                            //*********************** END: Rotation   **********************

                            //*********************** START: Forming the flock **********************
                            //*********************** Leader doesn't need any change
                            if (!gvh.id.getName().equals(le.getLeader())) {


                                //*********************** If Robot is the Rightmost (Last robot in the group)
                                if (gvh.BotGroup.isLast) {

                                    System.out.printf("name1 %s newX: %d newY %d before %s X: %d theta %f \n",gvh.id.getName(),newX,newY,gvh.BotGroup.getBeforeBot(),beforeX, gvh.BotGroup.getTheta());

                                    newX = (beforeX + newX + 1000) / 2;
                                    newY = (beforeY + newY) / 2;

                                    System.out.printf("name2 %s newX: %d newY %d beforeX %d \n",gvh.id.getName(),newX,newY,beforeX);

                                } else {

                                    // ******************** If it is interior
                                    System.out.printf("good1 %s newX: %d newY %d before %s X: %d theta %f \n",gvh.id.getName(),newX,newY,gvh.BotGroup.getBeforeBot(),beforeX, gvh.BotGroup.getTheta());


                                    newX = (beforeX + afterX) / 2;
                                    newY = (beforeY + afterY) / 2;

                                }
                            }

                            //*********************** END: Forming the flock   **********************


                            //*********************** START: Rotation Back**********************

                            //*********************** Leader doesn't need any change
                            // if (!gvh.id.getName().equals(le.getLeader())) {

                            //Once flocking, rotate by theta degrees
                            if (is_Flocking()) {
                                gvh.BotGroup.setTheta(gvh.BotGroup.getTheta() + 20);

                                newX = newX + 100;
                                newY = newY + 150;
                                //gvh.BotGroup.rf *= 1.25;

                                System.out.println("Robot number is " + robotNum);
                                if (!Common.bots_neighbour[robotNum][2].equals("none")) {
                                    Common.bots_neighbour[robotNum][2] = String.valueOf(gvh.BotGroup.getRF());
                                }


                            }

                            //  System.out.println("Back Angle: " + robotName + " its new X coordinate is " + gvh.BotGroup.getTheta());


                            newXX = newX * Math.cos(Math.toRadians(gvh.BotGroup.getTheta())) - newY * Math.sin(Math.toRadians(gvh.BotGroup.getTheta()));
                            newYY = newY * Math.cos(Math.toRadians(gvh.BotGroup.getTheta())) + newX * Math.sin(Math.toRadians(gvh.BotGroup.getTheta()));

                            newX = (int) newXX;
                            newY = (int) newYY;

                            //    System.out.println("Back Rotation: " + robotName + " its new X coordinate is " + newX + " and its new Y coordinate is " + newY + " and its order in groups is " + gvh.BotGroup.getRank());
                            // }

                            //*********************** END: Rotation   **********************


                            System.out.println(robotName + " has old coordination X " + gvh.gps.getMyPosition().getX() + " and Y " + gvh.gps.getMyPosition().getY() + " New X is " + newX + " and New Y is " + newY);
                            dest = new ItemPosition(n, newX, newY,(int) gvh.BotGroup.getTheta());
                            destinations.update(dest);

                            gvh.plat.moat.goTo(dest);
                        }


                        count += 1;

                    }

                    // wait here while robot is in motion
                    while (gvh.plat.moat.inMotion) {
                        gvh.sleep(100);
                    }

                    stage = STAGE.START; // repeat

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

    public double rotate(int x, int y, int direction, String axis){
        if(axis.toLowerCase().equals("x")){
            return (x * Math.cos(Math.toRadians(direction * gvh.BotGroup.getTheta())) - y * Math.sin(Math.toRadians(direction * gvh.BotGroup.getTheta())));
        } else if(axis.toLowerCase().equals("y")) {
            return (y * Math.cos(Math.toRadians(direction * gvh.BotGroup.getTheta())) + x * Math.sin(Math.toRadians(direction * gvh.BotGroup.getTheta())));
        } else{
            return -1;
        }
    }




    private static final Random rand = new Random();

    @SuppressWarnings("unchecked")
    private <X, T> T getRandomElement(Map<X, T> map) {
        if (RANDOM_DESTINATION)
            return (T) map.values().toArray()[rand.nextInt(map.size())];
        else
            return (T) map.values().toArray()[0];
    }

    public boolean is_Flocking() {
        boolean isFlocking = true;

        ItemPosition BeforeBot = new ItemPosition("BeforeBot", 0, 0, 0);
        ItemPosition AfterBot = new ItemPosition("AfterBot", 0, 0, 0);
        ItemPosition Bot = new ItemPosition("Bot", 0, 0, 0);

        int groupDis = 0;

        Integer leadNum = Integer.valueOf(le.getLeader().substring(6));

        boolean once = true;
        for (int i = 0; i < Common.numOFbots; i++) {
            if (!Common.bots_neighbour[i][2].equals("none")) {
                groupDis = gvh.BotGroup.getRF();

                if (once) {
                    System.out.println("Reference distance between each group is " + groupDis);
                    once = false;
                }

            }
            if (i != leadNum) {
                PositionList<ItemPosition> plAll = gvh.gps.get_robot_Positions();
                for (ItemPosition rp : plAll.getList()) {
                    //TODO: will have to change string for each robot.
                    if (rp.getName().equals("irobot" + i))
                        Bot = rp;
                    if (rp.getName().equals(Common.bots_neighbour[i][0]))
                        BeforeBot = rp;
                    if (!Common.bots_neighbour[i][1].equals("none")) {
                        if (rp.getName().equals(Common.bots_neighbour[i][1]))
                            AfterBot = rp;
                    }

                }
                System.out.println(Bot.name + " " + BeforeBot.name);
                System.out.println(Bot.getX() + " X " + BeforeBot.getX() + " " + Bot.getY() + " Y " + BeforeBot.getY());

                // Distance between the bot and his before (left) neighbour
                double botDistance = Math.sqrt((Bot.getX() - BeforeBot.getX()) * (Bot.getX() - BeforeBot.getX()) + (Bot.getY() - BeforeBot.getY()) * (Bot.getY() - BeforeBot.getY()));
                System.out.println(botDistance + " Before " + (groupDis - groupDis * 0.3) + " " + (groupDis + groupDis * 0.3));
                if (botDistance < (groupDis - groupDis * 0.3) || botDistance > (groupDis + groupDis * 0.3)) {

                    System.out.println("It is false because before bot is out of the range, their distance between each other is " + String.valueOf(botDistance));
                    return false;
                }

                // Distance between the bot and his after (right) neighbour
                if (!Common.bots_neighbour[i][1].equals("none")) {
                    double botDistanceAfter = Math.sqrt((Bot.getX() - AfterBot.getX()) * (Bot.getX() - AfterBot.getX()) + (Bot.getY() - AfterBot.getY()) * (Bot.getY() - AfterBot.getY()));

                    System.out.println(botDistanceAfter + " After " + (groupDis - groupDis * 0.3) + " " + (groupDis + groupDis * 0.3));
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
                            Integer rpGroup = Integer.valueOf(rp.getName().substring(6)) % Common.numOFgroups;
                            if (gvh.BotGroup.getGroupNum() == rpGroup) {
                                int otherSummation = rp.getX() + rp.getY();
                                // if (mySummation == otherSummation){

                                //}
                                if (mySummation == otherSummation)
                                    System.out.println("############************** There is potential same locations ***************########### " + robotName + " and " + rp.getName());

                                if (mySummation >= otherSummation) {
                                    if (gvh.BotGroup.getBeforeBot() == null) {
                                        if (mySummation == otherSummation) {
                                            if (robotNum > Integer.valueOf(rp.getName().substring(6)))
                                                gvh.BotGroup.setBeforeBot(rp.getName());
                                        } else
                                            gvh.BotGroup.setBeforeBot(rp.getName());

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
                                        if (otherSummation > beforeBotSummation)
                                            gvh.BotGroup.setBeforeBot(rp.getName());
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

                Common.bots_neighbour[robotNum][0] = gvh.BotGroup.getBeforeBot();
                if (!gvh.BotGroup.isLast)
                    Common.bots_neighbour[robotNum][1] = gvh.BotGroup.getAfterBot();
                else Common.bots_neighbour[robotNum][1] = "none";

                Common.bots_neighbour[robotNum][2] = Integer.toString(gvh.BotGroup.getRF());
                gvh.BotGroup.setAfterBefore = false;
            }

        } else {
            Common.bots_neighbour[robotNum][0] = "none";
            Common.bots_neighbour[robotNum][1] = "none";
            Common.bots_neighbour[robotNum][2] = "none";
        }
    }

}
