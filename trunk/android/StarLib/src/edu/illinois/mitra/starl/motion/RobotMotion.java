package edu.illinois.mitra.starl.motion;

import edu.illinois.mitra.starl.interfaces.Cancellable;
import edu.illinois.mitra.starl.objects.ItemPosition;

/**
 * Abstract class describing methods which all robot motion controllers should implement
 * @author Adam Zimmerman
 *
 */
public abstract class RobotMotion extends Thread implements Cancellable {
	
	public boolean inMotion = false;
	
	public RobotMotion() {}
	
	public RobotMotion(String name) {
		super("RobotMotion-"+name);
	}
	
	/**
	 * Go to a destination using the default motion parameters
	 * @param dest the robot's destination
	 */
	public abstract void goTo(ItemPosition dest);
	
	/**
	 * Go to a destination using specific motion parameters
	 * @param dest the robot's destination
	 * @param param the motion parameters to use during motion
	 */
	public abstract void goTo(ItemPosition dest, MotionParameters param);
	
	/**
	 * Turn to face a destination using the default motion parameters
	 * @param dest the destination to face
	 */
	public abstract void turnTo(ItemPosition dest);
	
	/**
	 * Turn to face a destination using specific motion parameters
	 * @param dest the robot's destination
	 * @param param the motion parameters to use during motion
	 */
	public abstract void turnTo(ItemPosition dest, MotionParameters param);

	/**
	 * Enable robot motion
	 */
	public abstract void motion_resume();
	
	/**
	 * Stop the robot and disable motion until motion_resume is called. This cancels the current motion. 
	 */
	public abstract void motion_stop();
	
	/**
	 * Set the default motion parameters to use
	 * @param param the parameters to use by default
	 */
	public abstract void setParameters(MotionParameters param);
}