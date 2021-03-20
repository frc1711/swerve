// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.team1711.swerve.commands;

import edu.wpi.first.wpilibj2.command.CommandBase;

import frc.team1711.swerve.subsystems.AutoSwerveDrive;
import frc.team1711.swerve.util.Vector;

/**
 * A command which drives a given {@link frc.team1711.swerve.subsystems.AutoSwerveDrive} in any
 * direction, without turning.
 * @author Gabriel Seaver
 */
public class AutonDrive extends CommandBase {
    
    private static final double correctionTurnScalar = 0.08;
    
    private final AutoSwerveDrive swerveDrive;
    
    private boolean finished;
    
    private final double
            initalGyroAngle,
            direction,
            distance,
            speed;
    
    /**
     * Strafes the swerve drive in a certain direction, at a certain speed, over a certain distance.
     * @param swerveDrive   The {@link AutoSwerveDrive} drive train
     * @param direction     The direction, in degrees, to travel in. Zero degrees corresponds with
     * directly forward, and an increase in {@code direction} corresponds with a direction further
     * clockwise from a top-down view. This value must be on the interval [0, 360).
     * @param distance      The distance to travel in the specified direction, in inches. This value
     * must be on the interval (0, infinity).
     * @param speed         The speed to travel at. This value must be on the interval (0, 1].
     */
    public AutonDrive (AutoSwerveDrive swerveDrive, double direction, double distance, double speed) {
        this.swerveDrive = swerveDrive;
        this.direction = direction;
        this.distance = distance;
        this.speed = speed;
        
        initalGyroAngle = swerveDrive.getGyroAngle();
        finished = false;
        
        addRequirements(swerveDrive);
    }
    
    @Override
    public void initialize () {
        swerveDrive.stop();
        swerveDrive.setDistanceReference();
    }
    
    @Override
    public void execute () {
        if (swerveDrive.getDistanceTraveled() < distance) {
            final Vector driveVector = new Vector(1, 0).toRotationDegrees(direction).scale(speed);
            swerveDrive.inputDrive(driveVector.getX(), driveVector.getY(), getCorrectionTurn());
        } else {
            finished = true;
        }
    }
    
    private double getCorrectionTurn () {
        double correctionTurn = initalGyroAngle - swerveDrive.getGyroAngle();
        while (correctionTurn >= 180) correctionTurn -= 360;
        while (correctionTurn < -180) correctionTurn += 360;
        return Math.max(Math.min(correctionTurn * correctionTurnScalar, 1), -1);
    }
    
    @Override
    public void end (boolean interrupted) {
        swerveDrive.stop();
    }
    
    @Override
    public boolean isFinished () {
        return finished;
    }
    
}