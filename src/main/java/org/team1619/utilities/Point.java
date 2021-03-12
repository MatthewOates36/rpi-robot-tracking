package org.team1619.utilities;

import java.util.List;

/**
 * Point is a simple class which stores an x and y value for a point,
 * and can do simple operations on the point
 *
 * @author Matthew Oates
 */

public class Point {

    protected final double fX;
    protected final double fY;

    public Point() {
        fX = 0;
        fY = 0;
    }

    public Point(double x, double y) {
        fX = x;
        fY = y;
    }

    public Point(List<Double> coordinates) {
        if(coordinates.size() < 1) {
            fX = 0;
            fY = 0;
        } else if(coordinates.size() < 2) {
            fX = coordinates.get(0);
            fY = 0;
        } else {
            fX = coordinates.get(0);
            fY = coordinates.get(1);
        }
    }

    public double getX() {
        return fX;
    }

    public double getY() {
        return fY;
    }

    public Point add(Point point) {
        return new Point(fX + point.getX(), fY + point.getY());
    }

    public Point subtract(Point point) {
        return new Point(fX - point.getX(), fY - point.getY());
    }

    public double distance(Point point) {
        return Math.sqrt(Math.pow(point.fX - fX, 2) + Math.pow(point.fY - fY, 2));
    }

    public String toString() {
        return "(" + String.format("%.4f", fX) + "," + String.format("%.4f", fY) + ")";
    }
}
