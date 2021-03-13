package org.team1619;

import org.team1619.utilities.Point;
import org.team1619.utilities.Vector;

public class Localization {

    private static final double LEFT_ANGLE_OFFSET = 90.0;
    private static final double RIGHT_ANGLE_OFFSET = 90.0;
    private static final double LIMELIGHT_DISTANCE = 54.0;
    private final Point offset;


    public Localization(Point offset) {
        this.offset = offset;
    }

    public Localization() {
        this(new Point());
    }

    public Vector update(double leftAngle, double rightAngle) {

        double transformedLeftLimelightAngleRadians = Math.toRadians(LEFT_ANGLE_OFFSET + leftAngle);
        double transformedRightLimelightAngleRadians = Math.toRadians(RIGHT_ANGLE_OFFSET - rightAngle);

        double thirdTriangleAngleRadians = Math.PI - transformedLeftLimelightAngleRadians - transformedRightLimelightAngleRadians;

        double triangleSideLength = LIMELIGHT_DISTANCE * Math.sin(transformedRightLimelightAngleRadians) / Math.sin(thirdTriangleAngleRadians);

        double x = triangleSideLength * Math.sin(transformedLeftLimelightAngleRadians);
        double y = triangleSideLength * Math.cos(transformedLeftLimelightAngleRadians);

        return new Vector(new Point(x, y).subtract(offset));
    }
}
