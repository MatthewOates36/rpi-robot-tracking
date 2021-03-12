package org.team1619.utilities;

import java.util.List;

/**
 * Vector is an add on to the Point class,
 * allowing it to preform vector operations
 *
 * @author Matthew Oates
 */

public class Vector extends Point {

    private double fMagnitude;
    private double fAngle;

    public Vector() {
        super();
        fMagnitude = 0;
        fAngle = 0;
    }

    public Vector(double magnitude, double angle) {
        this(new Point(magnitude * Math.cos(Math.toRadians(angle)), magnitude * Math.sin(Math.toRadians(angle))));
        fMagnitude = magnitude;
        fAngle = angle;
    }

    public Vector(List<Double> coordinates) {
        super(coordinates);
        fMagnitude = Math.sqrt(Math.pow(fX, 2) + Math.pow(fY, 2));
        fAngle = Math.toDegrees(Math.atan2(fY, fX));
    }

    public Vector(Point point) {
        super(point.getX(), point.getY());
        fMagnitude = Math.sqrt(Math.pow(fX, 2) + Math.pow(fY, 2));
        fAngle = Math.toDegrees(Math.atan2(fY, fX));
    }

    public Vector(double x1, double y1, double x2, double y2) {
        this(new Point(x2 - x1, y2 - y1));
        fMagnitude = Math.sqrt(Math.pow(fX, 2) + Math.pow(fY, 2));
        fAngle = Math.toDegrees(Math.atan2(fY, fX));
    }

    public Vector(Point point1, Point point2) {
        this(point1.getX(), point1.getY(), point2.getX(), point2.getY());
    }

    public double magnitude() {
        return fMagnitude;
    }

    public double angle() {
        return fAngle;
    }

    public Vector normalize() {
        return new Vector(1, angle());
    }

    public Vector scale(double scalar) {
        return new Vector(magnitude() * scalar, angle());
    }

    public Vector rotate(double degrees) {
        return new Vector(magnitude(), angle() + degrees);
    }

    public double dot(Vector vector) {
        return fX * vector.getX() + fY * vector.getY();
    }

    public String toString() {
        return "<" + fX + "," + fY + ">";
    }
}
