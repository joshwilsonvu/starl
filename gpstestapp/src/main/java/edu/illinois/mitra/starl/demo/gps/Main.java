package edu.illinois.mitra.starl.demo.gps;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

    public static void main(String[] args) {
        SimSettings.Builder settings = new SimSettings.Builder();
        settings.BOTS("Model_iRobot").COUNT = 1;
        settings.TIC_TIME_RATE(5);
        settings.WAYPOINT_FILE("gpstestapp/waypoints/five.wpt");		//must specify relative path
        settings.DRAW_WAYPOINTS(false);
        settings.DRAW_WAYPOINT_NAMES(false);
        settings.DRAWER(new GpsDrawer());

        Simulation sim = new Simulation(GpsTestApp.class, settings.build());
        sim.start();
    }

}