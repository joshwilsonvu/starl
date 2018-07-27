package edu.illinois.mitra.starl.demo.geo;

import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {

    public static void main(String[] args) {
        SimSettings.Builder settings = new SimSettings.Builder();
        settings.BOTS("Model_iRobot").COUNT = 1;
        settings.TIC_TIME_RATE(5);
        settings.WAYPOINT_FILE("geocasttestapp/waypoints/five.wpt");		//must specify relative path
        settings.DRAW_WAYPOINTS(false);
        settings.DRAW_WAYPOINT_NAMES(false);
        settings.DRAWER(new GeoDrawer());

        Simulation sim = new Simulation(GeoCastApp.class, settings.build());
        sim.start();
    }

}