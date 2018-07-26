/**
 * Created by Mousa Almotairi on 4/28/2015.
 */

package edu.illinois.mitra.starl.gvh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;


public class RobotGroup {

    private int groupNum;
    private int rank;
    private int rf = 1000;   //default
    private double theta;
    public boolean setAfterBefore;
    public boolean isLast;
    private String afterBot;
    private String beforeBot;



    public RobotGroup(int id, Integer numGroups){
        groupNum = id % numGroups;
        setAfterBefore= true;
        rank = 0;
        isLast= false;

        double calcuateAngle = groupNum*(360/numGroups);
        if (numGroups == 2 && groupNum ==1){
            theta = Double.valueOf(90);
        }
        else {
            theta = calcuateAngle;
        }

        System.out.printf("Robot: %d Group: %d Theta: %f \n",id,groupNum,theta);
    }

    public int getGroupNum(){
        return groupNum;
    }

    public int getRF(){
        return rf;
    }
    public void setRF(int rf){
        this.rf = rf;
    }

    public void setBeforeBot(String beforeBot){
        this.beforeBot = beforeBot;
    }

    public void setAfterBot(String afterBot){
        this.afterBot = afterBot;
    }

    public String getBeforeBot(){
        return beforeBot;
    }

    public String getAfterBot(){
        return afterBot;
    }

    public void setRank(int rank){
        this.rank =rank;
    }
    public int getRank(){
        return rank;
    }

    public void setTheta(double theta){
        this.theta = theta;
    }

    public double getTheta(){
        return theta;
    }
}
