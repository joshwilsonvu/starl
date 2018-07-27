package edu.illinois.mitra.starl.demo.gps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.RobotMotion;
import edu.illinois.mitra.starl.objects.ItemPosition;

public class GpsTestApp extends LogicThread {

    private enum STAGE { START, MOVE, DONE }
    private STAGE stage = STAGE.START;

    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
    ItemPosition currentDestination;

    private RobotMotion moat;

    private int n_waypoints;
    private int cur_waypoint = 0;

    public GpsTestApp(GlobalVarHolder gvh) {
        super(gvh);
        //gvh.trace.traceStart(SimSettings.TRACE_CLOCK_DRIFT_MAX, SimSettings.TRACE_CLOCK_SKEW_MAX);
        moat = gvh.plat.moat;
        n_waypoints = gvh.gps.getWaypointPositions().getNumPositions()-1;
        if(n_waypoints == -1) System.out.println("The GpsTestApp requires N waypoints named DEST0 -> DESTN");

        for(ItemPosition i : gvh.gps.getWaypointPositions())
            destinations.put(i.getName(), i);
    }

    @Override
    public List<Object> callStarL() {
        while(true) {
            switch (stage) {
                case START:
                    gvh.trace.traceSync("LAUNCH",gvh.time());
                    stage = STAGE.MOVE;
                    moat.goTo(gvh.gps.getWaypointPosition("DEST"+cur_waypoint));
                    break;

                case MOVE:
                    if(!moat.inMotion) {
                        if(cur_waypoint < n_waypoints) {
                            cur_waypoint ++;
                            moat.goTo(gvh.gps.getWaypointPosition("DEST"+cur_waypoint));
                        } else {
                            stage = STAGE.DONE;
                        }
                    }
                    break;

                case DONE:
                    System.out.println("Done");
                    gvh.trace.traceEnd();
                    return returnResults();
            }

            gvh.sleep(100);
        }
    }
}