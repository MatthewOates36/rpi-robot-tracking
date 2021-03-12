package org.team1619;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import java.io.IOException;

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
        capture.set(5, 30);
        capture.set(6, VideoWriter.fourcc('M', 'J', 'P', 'G'));

        image = new Mat();
        imageUpdated = false;

        new Thread(() -> {
            var tempImage = new Mat();

            while (true) {
                capture.read(tempImage);

                synchronized (this) {
                    tempImage.copyTo(image);

                    imageUpdated = true;
                    this.notifyAll();
                }
            }
        }).start();

        capture().release();
    }

    public Camera(String id) {
        this(id, 0.0);
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

    public ProcessedImage captureProcessed() {
        var raw = capture();

        var processed = new ProcessedImage(raw, angleOffset);

        raw.release();

        return processed;
    }

    public void setProperty(String property, int value) {
        setProperty(getId(), property, value);
    }

    public void setProperty(String id, String property, int value) {
        try {
            var process = Runtime.getRuntime().exec(String.format("v4l2-ctl -d %s --set-ctrl=%s=%s", id, property, value));
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getId() {
        return id;
    }
}
