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

        //At 120: 14 and 11.3
        //At 190.5: 8.9 and 7.2
//        Camera camera = new Camera(CameraDetector.getCameraFromId("USB 2.0 Camera: USB Camera (usb-0000:01:00.0-1.2)").getPaths().get(0), 0.0);
//        Camera camera = new Camera(CameraDetector.getCameraFromId("USB 2.0 Camera: HD USB Camera (usb-0000:01:00.0-1.2)").getPaths().get(0), 0.0);

        Camera camera = new Camera(0);

        var img = camera.captureSync();
        System.out.println(img.width() + " x " + img.height());
        img.release();

        List<Camera> cameras = List.of(camera);

//        cameras.forEach(c -> c.setProperty("exposure_auto", 1));
        cameras.forEach(c -> c.setProperty(44, 0));

        SettingsHandler.addUpdateListener((key) -> {
            switch (key) {
                case "Exposure": {
                    cameras.forEach(c -> c.setProperty(15, SettingsHandler.getDouble(key)));
                    break;
                }
                case "Brightness": {
                    cameras.forEach(c -> c.setProperty(10, SettingsHandler.getDouble(key)));
                    break;
                }
                case "Contrast": {
                    cameras.forEach(c -> c.setProperty(11, SettingsHandler.getDouble(key)));
                    break;
                }
                case "White Bal": {
                    cameras.forEach(c -> c.setProperty(45, SettingsHandler.getDouble(key)));
                    break;
                }
                case "Cam Hue": {
                    cameras.forEach(c -> c.setProperty(13, SettingsHandler.getDouble(key)));
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
        long processingTime = 0;
        int count = 0;

        var resized = new Mat();

        while (true) {
            long delta = System.currentTimeMillis() - startTime;
            if (delta > 5000) {

                System.out.println("Average: " + (delta / count) + " Average Processing: " + (processingTime / count) + " Loops: " + count);

                processingTime = 0;
                count = 0;
                startTime = System.currentTimeMillis();
            }
            count++;

//            var images = cameras.stream().sequential().map(Camera::captureProcessedSync).collect(Collectors.toList());

            var imgs = cameras.stream().sequential().map(Camera::captureSync).collect(Collectors.toList());

            var startProcessing = System.currentTimeMillis();

            var images = imgs.stream().map(i -> new ProcessedImage(i, 0, 28)).collect(Collectors.toList());

            String positionString = "";

            for(var i = 0; i < images.size(); i++) {
                var contourPosition = images.get(i).getContourPosition();
                positionString += "x" + i + ": " + round(contourPosition.getX()) + " y" + i + ": " + round(contourPosition.getY()) + "  ";
            }

            server.setValue("position", positionString);

            var contourPosition = images.get(0).getContourPosition();

            var robotPosition = new Vector(63 / Math.tan(Math.toRadians(-contourPosition.getY())), -contourPosition.getX());

            robotConnection.update(images.stream().allMatch(ProcessedImage::hasValidContour) && -100 < robotPosition.getX() && robotPosition.getX() < 600 && -400 < robotPosition.getY() && robotPosition.getY() < 400, robotPosition.getX(), -robotPosition.getY());

            server.setValue("robot-position", round(robotPosition.getX()), round(robotPosition.getY()));

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

                if(!combined.empty()) {
                    Imgproc.resize(combined, resized, new Size(combined.width() / scale, combined.height() / scale));

                    stream.setImage(resized);
                }

                finalImages.forEach(Mat::release);

                combined.release();

                resized.release();
            }

            images.forEach(ProcessedImage::release);

            processingTime += System.currentTimeMillis() - startProcessing;
        }
    }

    public static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
