package org.team1619;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.team1619.utilities.Point;
import org.team1619.utilities.Vector;
import org.team1619.web.HttpStreamServer;
import org.team1619.web.WebServer;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("Starting");

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Camera camera1 = new Camera(CameraDetector.getCameraFromId("USB 2.0 Camera: USB Camera (usb-0000:01:00.0-1.1)").getPaths().get(0), 0.3);
        Camera camera2 = new Camera(CameraDetector.getCameraFromId("USB 2.0 Camera: USB Camera (usb-0000:01:00.0-1.2)").getPaths().get(0), 1.5);
        Camera camera3 = new Camera(CameraDetector.getCameraFromId("USB 2.0 Camera: USB Camera (usb-0000:01:00.0-1.3)").getPaths().get(0), 3.1);
        Camera camera4 = new Camera(CameraDetector.getCameraFromId("USB 2.0 Camera: USB Camera (usb-0000:01:00.0-1.4)").getPaths().get(0), 1.0);

        List<Camera> cameras = List.of(camera1, camera3, camera4, camera2);

        cameras.forEach(c -> c.setProperty("exposure_auto", 1));
        cameras.forEach(c -> c.setProperty("white_balance_temperature_auto", 0));

        SettingsHandler.addUpdateListener((key) -> {
            switch (key) {
                case "Exposure": {
                    cameras.forEach(c -> c.setProperty("exposure_absolute", (int) SettingsHandler.getDouble(key)));
                    break;
                }
                case "Brightness": {
                    cameras.forEach(c -> c.setProperty("brightness", (int) SettingsHandler.getDouble(key)));
                    break;
                }
                case "Contrast": {
                    cameras.forEach(c -> c.setProperty("contrast", (int) SettingsHandler.getDouble(key)));
                    break;
                }
                case "White Bal": {
                    cameras.forEach(c -> c.setProperty("white_balance_temperature", (int) SettingsHandler.getDouble(key)));
                    break;
                }
                case "Cam Hue": {
                    cameras.forEach(c -> c.setProperty("hue", (int) SettingsHandler.getDouble(key)));
                    break;
                }
            }
        });

        var stream = new HttpStreamServer(5800);
        var server = new WebServer();

        var leftLocalization = new Localization(new Point(0, 30));
        var rightLocalization = new Localization(new Point(0, 24));

        var robotConnection = new RobotConnection();

        System.out.println("Running");

        long startTime = System.currentTimeMillis();
        int count = 0;

        var resized = new Mat();

        while (true) {
            long delta = System.currentTimeMillis() - startTime;
            if (delta > 5000) {
//                System.out.println("Loops: " + count + " Average loop time: " + (delta / count));

                count = 0;
                startTime = System.currentTimeMillis();
            }
            count++;

            var images = cameras.stream().sequential().map(Camera::captureProcessedSync).collect(Collectors.toList());

            String positionString = "";

            for(var i = 0; i < images.size(); i++) {
                var contourPosition = images.get(i).getContourPosition();
                positionString += "x" + i + ": " + round(contourPosition.getX()) + " y" + i + ": " + round(contourPosition.getY()) + "  ";
            }

            server.setValue("position", positionString);

            var leftRobotPosition = leftLocalization.update(images.get(0).getContourPosition().getX(), images.get(1).getContourPosition().getX());
            var rightRobotPosition = rightLocalization.update(images.get(3).getContourPosition().getX(), images.get(2).getContourPosition().getX());

            var robotPosition = new Vector(leftRobotPosition.add(rightRobotPosition)).scale(0.5);

            robotConnection.update(images.stream().allMatch(ProcessedImage::hasValidContour) && -100 < robotPosition.getX() && robotPosition.getX() < 600 && -400 < robotPosition.getY() && robotPosition.getY() < 400 && leftRobotPosition.distance(rightRobotPosition) < 20, robotPosition.getX(), -robotPosition.getY());

            server.setValue("robot-position", round(leftRobotPosition.getX()), round(leftRobotPosition.getY()), round(rightRobotPosition.getX()), round(rightRobotPosition.getY()), round(robotPosition.getX()), round(robotPosition.getY()));

            if(stream.connected()) {
                var finalImages = images.stream().parallel().map(ProcessedImage::getProcessedImage).collect(Collectors.toList());

                var combined = new Mat(finalImages.stream().parallel().mapToInt(Mat::rows).max().getAsInt(), finalImages.stream().mapToInt(Mat::cols).sum(), finalImages.get(0).type());

                int x = 0;
                for (var image : finalImages) {
                    var imageLocation = new Mat(combined, new Rect(x, 0, image.cols(), image.height()));
                    image.copyTo(imageLocation);
                    x += image.cols();
                }

                double scale = 1.0;

                Imgproc.resize(combined, resized, new Size(combined.width() / scale, combined.height() / scale));

                stream.setImage(resized);

                finalImages.forEach(Mat::release);

                combined.release();

                resized.release();
            }

            images.forEach(ProcessedImage::release);
        }
    }

    public static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
