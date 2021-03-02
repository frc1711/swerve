// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package swerve;

import edu.wpi.first.wpilibj.drive.RobotDriveBase;

/**
 * Utilizes {@link SwerveWheel} subsystems to create a singular, easy-to-use swerve drive.
 * @author Gabriel Seaver
 */
public class SwerveDrive extends RobotDriveBase {
    
    private final SwerveWheel
            flWheel,
            frWheel,
            rlWheel,
            rrWheel;
    
    private double
            turnSpeed,
            directMoveSpeed;
    
    private final double widthToHeightRatio;
    
    /**
     * Creates a new {@code SwerveDrive} given {@link SwerveWheel} wheels and several speed constants.
     * <b>Note: {@link #SwerveDrive(SwerveWheel, SwerveWheel, SwerveWheel, SwerveWheel, double)}
     * should be used instead if the wheelbase and track are not equal.</b>
     * @param _flWheel              The front left {@code SwerveWheel}
     * @param _frWheel              The front right {@code SwerveWheel}
     * @param _rlWheel              The rear left {@code SwerveWheel}
     * @param _rrWheel              The rear right {@code SwerveWheel}
     */
    public SwerveDrive (
        SwerveWheel _flWheel,
        SwerveWheel _frWheel,
        SwerveWheel _rlWheel,
        SwerveWheel _rrWheel) {
        
        this(_flWheel, _frWheel, _rlWheel, _rrWheel, 1);
    }
    
    /**
     * Creates a new {@code SwerveDrive} given {@link SwerveWheel} wheels and several speed constants.
     * @param _flWheel              The front left {@code SwerveWheel}
     * @param _frWheel              The front right {@code SwerveWheel}
     * @param _rlWheel              The rear left {@code SwerveWheel}
     * @param _rrWheel              The rear right {@code SwerveWheel}
     * @param _widthToHeightRatio   The ratio from the track to the wheelbase (the distance between the centers
     * of the front or back wheels divided by the distance between the centers of the left or right wheels).
     * {@link #SwerveDrive(SwerveWheel, SwerveWheel, SwerveWheel, SwerveWheel)} is recommended if this ratio is 1:1.
     */
    public SwerveDrive (
        SwerveWheel _flWheel,
        SwerveWheel _frWheel,
        SwerveWheel _rlWheel,
        SwerveWheel _rrWheel,
        double _widthToHeightRatio) {
        
        flWheel = _flWheel;
        frWheel = _frWheel;
        rlWheel = _rlWheel;
        rrWheel = _rrWheel;
        
        directMoveSpeed = 0.5 * m_maxOutput;
        turnSpeed = 0.5 * m_maxOutput;
        
        widthToHeightRatio = _widthToHeightRatio;
    }
    
    /**
     * Drives the {@code SwerveDrive} given direct movement inputs and rotational inputs,
     * all on the interval [-1, 1], where +y is forwards and +x is to the right.
     * @param directMoveX   The strafing speed in the x direction
     * @param directMoveY   The strafing speed in the y direction
     * @param rotate        The rotational speed, where a positive value rotates clockwise
     * and a negative value rotates counterclockwise
     */
    public void drive (double directMoveX, double directMoveY, double rotate) {
        
        // Deadbands
        directMoveX = accountForDeadband(directMoveX);
        directMoveY = accountForDeadband(directMoveY);
        rotate = accountForDeadband(rotate);
        
        // Calculating vectors
        final Vector baseVector = new Vector(directMoveX * directMoveSpeed, directMoveY * directMoveSpeed);
        // Rotate vector FR is the rotation vector that will be added to the FR wheel
        final Vector rotateVectorFR = new Vector(rotate * widthToHeightRatio * turnSpeed, -rotate * turnSpeed);
        
        /*
        Clockwise rotation vector additions.
        (top-down view of robot with --+ representing vector arrows for clockwise turning)
        See https://www.desmos.com/calculator/3rogeuv7u2
        |
        |        +
        |       /     \
        |   FL   |---| +   FR
        |        |   |
        |  RL  + |---|   RR
        |       \     /
        |            +
        */
        
        final Vector frVector = baseVector.add(rotateVectorFR);
        final Vector rrVector = baseVector.add(rotateVectorFR.reflectAcrossY());
        final Vector rlVector = baseVector.add(rotateVectorFR.scale(-1));
        final Vector flVector = baseVector.add(rotateVectorFR.reflectAcrossX());
        
        // Set wheel speeds
        double
                flSpeed = flVector.getMagnitude(),
                frSpeed = frVector.getMagnitude(),
                rlSpeed = rlVector.getMagnitude(),
                rrSpeed = rrVector.getMagnitude();
        
        
        // Because wheel speeds must be in correct proportions in order for swerve
        // to function correctly, we check if the maximum speed is within
        // the proper bounds and if it isn't then divide all by the maximum speed,
        // then scale to fit the upper limit again.
        final double maxSpeed = Math.max(Math.max(flSpeed, frSpeed), Math.max(rlSpeed, rrSpeed));
        
        if (maxSpeed > m_maxOutput) {
            flSpeed /= maxSpeed;
            frSpeed /= maxSpeed;
            rlSpeed /= maxSpeed;
            rrSpeed /= maxSpeed;
            
            flSpeed *= m_maxOutput;
            frSpeed *= m_maxOutput;
            rlSpeed *= m_maxOutput;
            rrSpeed *= m_maxOutput;
        }
        
        // Sets the final wheel speeds and rotations
        flWheel.setSpeedAndRotation(flVector.getRotationDegrees(), flSpeed);
        frWheel.setSpeedAndRotation(frVector.getRotationDegrees(), frSpeed);
        rlWheel.setSpeedAndRotation(rlVector.getRotationDegrees(), rlSpeed);
        rrWheel.setSpeedAndRotation(rrVector.getRotationDegrees(), rrSpeed);
    }
    
    /**
     * Sets a distance reference on the encoders, such that the output of
     * {@link #getDistanceTraveled()} will be based on the distance from this reference.
     */
    public void setDistanceReference () {
        flWheel.resetDirectionalEncoder();
        frWheel.resetDirectionalEncoder();
        rlWheel.resetDirectionalEncoder();
        rrWheel.resetDirectionalEncoder();
    }
    
    /**
     * Gets the distance traveled, in inches, since the last distance reference was set.
     * This value is determined by the average distance traveled for each {@link SwerveWheel},
     * so the return value of this method is <b>only going to be accurate if all
     * {@code SwerveWheel} wheels have the same rotation</b> (or an equivalent angle).
     * @return The number of inches traveled
     * @see #setDistanceReference()
     */
    public double getDistanceTraveled () {
        return (Math.abs(flWheel.getDirectionalDifference()) +
                Math.abs(frWheel.getDirectionalDifference()) +
                Math.abs(rlWheel.getDirectionalDifference()) +
                Math.abs(rrWheel.getDirectionalDifference())) / 4;
    }
    
    /**
     * Stops all modules immediately.
     */
    @Override
    public void stopMotor () {
        flWheel.stop();
        frWheel.stop();
        rlWheel.stop();
        rrWheel.stop();
    }
    
    @Override
    public String getDescription () {
        return "swerve drive";
    }
    
    @Override
    public void setMaxOutput(double maxOutput) {
        m_maxOutput = maxOutput;
        directMoveSpeed = 0.5 * m_maxOutput;
        turnSpeed = 0.5 * m_maxOutput;
    }
    
    private double accountForDeadband (double value) {
        if (Math.abs(value) < m_deadband) return 0;
        // Puts value in [m_deadband, 1] or [-1, -m_deadband] into range [0, 1] or [-1, 0]
        return (value + (value > 0 ? -m_deadband : m_deadband)) / (1 - m_deadband);
    }
    
}