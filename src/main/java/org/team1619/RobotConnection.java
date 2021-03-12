package org.team1619;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public class RobotConnection {

    public static final int TEAM_NUMBER = 1619;
    public static final String TABLE_NAME = "vision-rpi";

    private final NetworkTableEntry pv;
    private final NetworkTableEntry px;
    private final NetworkTableEntry py;

    public RobotConnection() {
        NetworkTableInstance networkTableInstance = NetworkTableInstance.getDefault();

        NetworkTable table = networkTableInstance.getTable(TABLE_NAME);

        pv = table.getEntry("pv");
        px = table.getEntry("px");
        py = table.getEntry("py");

        pv.setNumber(0);
        px.setNumber(0);
        py.setNumber(0);

        networkTableInstance.startClientTeam(TEAM_NUMBER);
    }

    public void update(boolean valid, double x, double y) {
        pv.setNumber(valid ? 1 : 0);
        px.setNumber(x);
        py.setNumber(y);
    }
}
