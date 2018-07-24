package edu.illinois.mitra.starl.modelinterfaces;

/**
 * The base interface of any interface for communicating with real robots.
 */
public interface ModelInterface {
    /**
     * Disconnect from the robot, finalizing any ongoing instructions.
     */
    void disconnect();
}
