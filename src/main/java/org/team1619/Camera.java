package org.team1619;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

public class Camera {

    private final String id;
    private final VideoCapture capture;
    private final double angleOffset;

    private Mat image;
    private boolean imageUpdated;

    public Camera(String id, double angleOffset) {
        this.id = id;
        capture = new VideoCapture(id);
        this.angleOffset = angleOffset;
        capture.set(3, SettingsHandler.getCameraWidth());
        capture.set(4, SettingsHandler.getCameraHeight());
        // TODO should we remove this
        capture.set(5, 30);
        capture.set(6, VideoWriter.fourcc('M', 'J', 'P', 'G'));

        image = new Mat();
        imageUpdated = false;

//        new Thread(() -> {
//            var tempImage = new Mat();
//
//            while (true) {
//                capture.read(tempImage);
//
//                synchronized (this) {
//                    tempImage.copyTo(image);
//
//                    imageUpdated = true;
//                    this.notifyAll();
//                }
//            }
//        }).start();

        captureSync().release();
    }

    public Camera(String id) {
        this(id, 0.0);
    }

    public Camera(int id) {
        this.id = "";
        capture = new VideoCapture(id);
        this.angleOffset = 0.0;
        capture.set(3, SettingsHandler.getCameraWidth());
        capture.set(4, SettingsHandler.getCameraHeight());
        capture.set(5, 30);
        capture.set(6, VideoWriter.fourcc('M', 'J', 'P', 'G'));

        image = new Mat();
        imageUpdated = false;

//        new Thread(() -> {
//            var tempImage = new Mat();
//
//            while (true) {
//                capture.read(tempImage);
//
//                synchronized (this) {
//                    tempImage.copyTo(image);
//
//                    imageUpdated = true;
//                    this.notifyAll();
//                }
//            }
//        }).start();

        capture.set(21, 0.0);

//        while (capture.get(15) != -1) {
//            captureSync().release();
//
//            System.out.println(capture.get(15));
//            capture.set(21, 0.25);
//            capture.set(15, 0.01);
//        }
    }

    public Mat capture() {
        if(!imageUpdated) {
            synchronized (this) {
                if(!imageUpdated) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        imageUpdated = false;
        return image.clone();
    }

    public Mat captureSync() {
        var tempImage = new Mat();

        capture.read(tempImage);

        return tempImage;
    }

    public ProcessedImage captureProcessed() {
        var raw = capture();

        var processed = new ProcessedImage(raw, angleOffset, 0.0);

        raw.release();

        return processed;
    }

    public ProcessedImage captureProcessedSync() {
        var raw = captureSync();

        var processed = new ProcessedImage(raw, angleOffset, 0.0);

        raw.release();

        return processed;
    }

    public void setProperty(int property, double value) {
        capture.set(property, value);
    }

    public String getId() {
        return id;
    }
}
