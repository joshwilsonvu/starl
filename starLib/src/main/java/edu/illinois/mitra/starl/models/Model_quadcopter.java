package edu.illinois.mitra.starl.models;

import edu.illinois.mitra.starl.exceptions.ItemFormattingException;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.illinois.mitra.starl.objects.Point3d;
import edu.illinois.mitra.starl.objects.PositionList;
/**
 * This class represents a simple model of the quadcopter
 * @author Yixiao Lin
 * @version 1.0
 */

public class Model_quadcopter extends Model_Drone {
	// for default values, see initial_helper()
	// mass in kilograms

	// angular speed
	//	public double v_pitch;
	//	public double v_roll;

	//	private double a_yaw;
	//	private double a_pitch;
	//	private double a_roll;

	private int x_p = 0;
	private int y_p = 0;
	private int z_p = 0;

	private double yaw_p = 0.0;
	private double pitch_p = 0.0;
	private double roll_p = 0.0;

	private double v_yaw_p = 0.0;
	//	private double v_pitch_p;
	//	private double v_roll_p;

	private double v_x_p = 0.0;
	private double v_y_p = 0.0;
	private double v_z_p = 0.0;

	// platform specific control parameters: see page 78 of http://www.msh-tools.com/ardrone/ARDrone_Developer_Guide.pdf
	public double windx = 0;   // millimeter/s
	public double windy = 0;
	public double windt = 0;
	public double windxNoise;
	public double windyNoise;

	/**
	 * Construct an Model_quadcopter from a received GPS broadcast message
	 *
	 * @param received GPS broadcast received
	 * @throws ItemFormattingException
	 */

	public Model_quadcopter(String received) throws ItemFormattingException{
		initial_helper();
		String[] parts = received.replace(",", "").split("\\|");
		if(parts.length == 9) {
			this.name = parts[1];
			this.setX(Integer.parseInt(parts[2]));
			this.setY(Integer.parseInt(parts[3]));
			this.setZ(Integer.parseInt(parts[4]));
			this.yaw = Integer.parseInt(parts[5]);
			this.pitch = Integer.parseInt(parts[6]);
			this.roll = Integer.parseInt(parts[7]);
		} else {
			throw new ItemFormattingException("Should be length 9, is length " + parts.length);
		}
	}

	public Model_quadcopter(String name, int x, int y) {
		super(name,x,y);
		initial_helper();
	}

	public Model_quadcopter(String name, int x, int y, int z) {
		super(name, x, y, z);
		initial_helper();
	}

	public Model_quadcopter(String name, int x, int y, int z, double yaw, double pitch, double roll, int radius) {
		super(name, x, y, z);
		initial_helper();
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
	}

	public Model_quadcopter(String name, int x, int y, int z, int yaw) {
		super(name, x, y, z);
		initial_helper();
		this.yaw = yaw;
	}


	public Model_quadcopter(ItemPosition t_pos) {
		super(t_pos.name, t_pos.getX(), t_pos.getY(), t_pos.getZ());
		initial_helper();
	}

	@Override
	public String toString() {
		return name + ": " + getX() + ", " + getY() + ", " + getZ() + "; yaw, pitch, roll, gaz: " + yaw + ", " + pitch + ", " + roll + " ," + gaz;
	}

	/**
	 *
	 * @return true if one robot is facing another robot/point
	 */
	/*
	public boolean isFacing(Point3d other) {
		if(other == null) {
			return false;
		}
		if(other.getX == this.getX && other.getY == this.getY){
			return true;
		}
    	double angleT = Math.toDegrees(Math.atan2((other.getY - this.getY) , (other.getX - this.getX)));
    	if(angleT  == 90){
    		if(this.getY < other.getY)
    			angleT = angleT + 90;
    		double temp = this.angle % 360;
    		if(temp > 0)
    			return true;
    		else
    			return false;
    	}
		if(angleT < 0)
		{
			angleT += 360;
		}
		double angleT1, angleT2, angleself;
		angleT1 = (angleT - 90) % 360;
		if(angleT1 < 0)
		{
			angleT1 += 360;
		}
		angleT2 = (angleT + 90) % 360;
		if(angleT2 < 0)
		{
			angleT2 += 360;
		}
		angleself = this.angle % 360;
		if(angleself < 0)
		{
			angleself += 360;
		}
		if(angleT2 <= 180)
		{
			if((angleself < angleT1) && (angleself > angleT2))
				return false;
			else
				return true;
		}
		else
		{
			if(angleself > angleT2 || angleself < angleT1)
				return false;
			else
				return true;

		}
	}
	 */

	/**
	 *
	 * @return Number of degrees this position must rotate to face position other
	 */
	/*
	public <T extends Point3d> int angleTo(T other) {
		if(other == null) {
			return 0;
		}

		int delta_x = other.getX - this.getX;
		int delta_y = other.getY - this.getY;
		double angle = this.angle;
		int otherAngle = (int) Math.toDegrees(Math.atan2(delta_y,delta_x));
		if(angle > 180) {
			angle -= 360;
		}
		int retAngle = Common.min_magitude((int)(otherAngle - angle),(int)(angle - otherAngle));
		retAngle = retAngle%360;
		if(retAngle > 180) {
			retAngle = retAngle-360;
		}
		if(retAngle <= -180) {
			retAngle = retAngle+360;
		}
		if(retAngle > 180 || retAngle< -180){
			System.out.println(retAngle);
		}
		return  Math.round(retAngle);
	}

	public void set(int getX, int getY, int angle) {
		this.getX = getX;
		this.getY = getY;
		this.angle = angle;
	}

	public void set(Model_Quadcopter other) {
		this.getX = other.getX;
		this.getY = other.getY;
		this.angle = other.angle;
	}
	 */


	private void initial_helper(){
		yaw = 0.0;
		pitch = 0.0;
		roll = 0.0;
		v_x = 0.0;
		v_y = 0.0;
		v_z = 0.0;
		v_yaw = 0.0;
		gaz = 0.0;
		//		a_yaw = 0;
		//		a_pitch = 0;
		//		a_roll = 0;
	}

	@Override
	public int radius() { return 340; }

	@Override
	public double height() { return 50; }

	@Override
	public double mass() { return .5; }	// default mass is 500 grams

	@Override
	public double max_gaz() { return 1000; }

	@Override
	public double max_pitch_roll() { return 20; }

	@Override
	public double max_yaw_speed() { return 200; }

	@Override
	public Point3d predict(double[] noises, double timeSinceUpdate) {
		if(noises.length != 3){
			System.out.println("Incorrect number of noises parameters passed in, please pass in getX, getY, getZ, yaw, pitch, roll noises");
			return new Point3d(getX(), getY(), getZ());
		}
		v_yaw += (v_yawR - v_yaw)*timeSinceUpdate;
		pitch += (pitchR - pitch)*timeSinceUpdate;
		roll += (rollR-roll)*timeSinceUpdate;
		gaz += (gazR-gaz)*timeSinceUpdate;

		double xNoise = (getRand()*2*noises[0]) - noises[0];
		double yNoise = (getRand()*2*noises[0]) - noises[0];
		double zNoise = (getRand()*2*noises[0]) - noises[0];
		double yawNoise = (getRand()*2*noises[1]) - noises[1];

		windt += timeSinceUpdate;
		windxNoise =  xNoise + windx*Math.sin(windt);
		windyNoise =  yNoise + windy*Math.sin(windt);


		//	double yawNoise = (getRand()*2*noises[3]) - noises[3];
		//double pitchNoise = (getRand()*2*noises[4]) - noises[4];
		//double rollNoise = (getRand()*2*noises[5]) - noises[5];

		//TODO: correct the model

		// speed is in millimeter/second
		// mass in kilograms
		// each pixel is 1 millimeter
		// timeSinceUpdate is in second
		int dX = (int) (xNoise + v_x*timeSinceUpdate + windxNoise);
		int dY= (int) (yNoise +  v_y*timeSinceUpdate + windyNoise);
		int dZ= (int) (zNoise +  gaz*timeSinceUpdate);

		x_p = getX() +dX;
		y_p = getY() +dY;
		z_p = getZ() +dZ;

		double thrust;
		if((mass() * Math.cos(Math.toRadians(roll)) * Math.cos(Math.toRadians(pitch))) != 0){
			thrust = ((gaz+1000) / (mass() * Math.cos(Math.toRadians(roll))) / (Math.cos(Math.toRadians(pitch))));
		}
		else{
			thrust = 1000;
		}

		//double thrust = Math.abs((gaz) * (mass * Math.cos(Math.toRadians(roll)) * Math.cos(Math.toRadians(pitch))));
		//double thrust = 100;
		double dv_x = - ((thrust)  * (Math.sin(Math.toRadians(roll)) * Math.sin(Math.toRadians(yaw)) + Math.cos(Math.toRadians(roll)) * Math.sin(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw))))/ (mass()) ;
		double dv_y = ((thrust)  * (Math.sin(Math.toRadians(roll)) * Math.cos(Math.toRadians(yaw)) - Math.cos(Math.toRadians(roll)) * Math.sin(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw))))/ (mass()) ;


		v_x_p = v_x + dv_x * timeSinceUpdate;
		v_y_p = v_y + dv_y * timeSinceUpdate;
		v_z_p = gaz;

		double dYaw = (v_yaw*timeSinceUpdate);
		yaw_p = (yaw + dYaw) %360;

		return new Point3d(x_p, y_p, z_p);
	}

	@Override
	public void collision(Point3d collision_point) {
		// No collision point, set both sensor to false
		if(collision_point == null){
			return;
		}
		else{
			gaz = -1000;
		}
	}

	@Override
	public void updatePos(boolean followPredict) {
		if(followPredict){
			setX(x_p);
			setY(y_p);
			setZ(z_p);

			yaw = yaw_p;
			//		pitch = pitch_p;
			//		roll = roll_p;
			v_yaw = v_yaw_p;
			//		v_pitch = v_pitch_p;
			//		v_roll = v_roll_p;

			v_x = v_x_p;
			v_y = v_y_p;
			v_z = v_z_p;
		}
		else{
			setZ(z_p);
			v_z = v_z_p;
			if(getZ() < 20){
				roll = 0;
				pitch = 0;
			}
		}
		if(getZ() < 0){
			setZ(0);
			v_z = 0;
		}
	}

	@Override
	public boolean inMotion() {
		return (v_x != 0 || v_y != 0 || v_z != 0 || v_yaw != 0);
	}

	@Override
	public void updateSensor(ObstacleList obspoint_positions,
							 PositionList<ItemPosition> sensepoint_positions) {
		return;
		// no sensor model yet
	}
}
