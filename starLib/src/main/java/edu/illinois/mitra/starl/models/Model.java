package edu.illinois.mitra.starl.models;

import android.content.Context;

import java.util.Random;

import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.modelinterfaces.ModelInterface;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.illinois.mitra.starl.objects.Point3i;
import edu.illinois.mitra.starl.objects.PositionList;

/**
 * The abstract base class of all kinds of robots, drones, etc. to be controlled by StarL.
 *
 *
 *
 * @see ModelRegistry
 * @see Model_Drone
 * @see Model_Ground
 */
public abstract class Model extends ItemPosition {

	private Random rand = new Random();

	protected double getRand(){
		return rand.nextDouble();
	}

	// Methods to be implemented in the subtype abstract classes
	public abstract Point3i predict(double[] noises, double timeSinceUpdate);
	public abstract void collision(Point3i collision_point);
	public abstract void updatePos(boolean followPredict);
	public abstract boolean inMotion();
	public abstract void updateSensor(ObstacleList obspoint_positions, PositionList<ItemPosition> sensepoint_positions);

	/**
	 * @return the minimum distance from the model an object can be without colliding
	 */
	public abstract int radius();

	/**
	 *
	 * @return
	 */
	public abstract String ip();

	/**
	 * Create the real interface for a particular model.
	 */
	public abstract ModelInterface createModelInterface(GlobalVarHolder gvh, Context context, String mac);

	public Model() {}

	public Model(String name, int x, int y){
		super(name, x, y);
	}

	public Model(String name, int x, int y, int z) {
		super(name, x, y, z);
	}

	public Model(ItemPosition t_pos) {
		super(t_pos);
	}

	/**
	 * Dynamically look up the name of the subclass of
	 * @return the polymorphic name of the class, without the package
	 */
	public final String getTypeName(){
	    return this.getClass().getSimpleName();
    }
}
