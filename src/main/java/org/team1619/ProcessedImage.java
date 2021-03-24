package org.team1619;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.team1619.utilities.Point;
import org.team1619.utilities.Vector;

import java.util.ArrayList;

public class ProcessedImage {

    private Mat image;
    private Rect boundingRectangle;
    private boolean hasValidContour;
    private Vector contourPosition;

    public ProcessedImage(Mat img, double angleOffset) {
        image = img.clone();

        boundingRectangle = null;
        hasValidContour = false;
        contourPosition = new Vector();

        if (image.empty()) {
            return;
        }

        var hsv = new Mat();
        var threshold = new Mat();
        var hierarchy = new Mat();

        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);

        Core.inRange(
                hsv,
                new Scalar(SettingsHandler.getDouble("Hue:low"), SettingsHandler.getDouble("Saturation:low"), SettingsHandler.getDouble("Value:low")),
                new Scalar(SettingsHandler.getDouble("Hue:high"), SettingsHandler.getDouble("Saturation:high"), SettingsHandler.getDouble("Value:high")),
                threshold);

        var contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        boundingRectangle = filterContours(contours, false);

        if ("Threshold".equals(SettingsHandler.getString("threshold"))) {
            Imgproc.cvtColor(threshold, image, Imgproc.COLOR_GRAY2BGR);
        }

        if ("Show".equals(SettingsHandler.getString("contours"))) {
            Imgproc.drawContours(image, contours, -1, new Scalar(255, 0, 0), 1);

            if (null != boundingRectangle) {
                Imgproc.rectangle(image, boundingRectangle.tl(), boundingRectangle.br(), new Scalar(0, 255, 0), 2);
            }
        }

        contours.stream().parallel().forEach(Mat::release);

        if (null != boundingRectangle) {
            var tl = boundingRectangle.tl();
            var br = boundingRectangle.br();

            contourPosition = new Vector(new Point(-xAngleFromImagePosition(((tl.x + br.x) / 2.0) - (SettingsHandler.getCameraWidth() / 2.0)) - angleOffset, -yAngleFromImagePosition(((tl.y + br.y) / 2.0) - (SettingsHandler.getCameraHeight() / 2.0))));
//            contourPosition = new Vector(new Point(-xAngleFromImagePosition(((tl.x + br.x) / 2.0) - (SettingsHandler.getCameraWidth() / 2.0)) - angleOffset, -(((tl.y + br.y) / 2.0) - (SettingsHandler.getCameraHeight() / 2.0))));

            hasValidContour = true;
        }

        hsv.release();
        threshold.release();
        hierarchy.release();
    }

    public ProcessedImage(Mat img) {
        this(img, 0.0);
    }

    private static double xAngleFromImagePosition(double position) {
        return 139.73888 / SettingsHandler.getCameraWidth() * position;
    }

    private static double yAngleFromImagePosition(double position) {
        return 104.0778188 / SettingsHandler.getCameraHeight() * position;
    }

    private static Rect filterContours(ArrayList<MatOfPoint> contours, boolean release) {
        Rect rectangle = null;

        String filterMode = SettingsHandler.getString("filter");

        for (var contour : contours) {
            var rect = Imgproc.boundingRect(contour);

            if (validContour(rect) && (null == rectangle || compareContours(rectangle, rect, filterMode))) {
                rectangle = rect;
            }

            if (release) {
                contour.release();
            }
        }

        return rectangle;
    }

    private static boolean validContour(Rect contour) {
        var size = (contour.width * contour.height) / (double) (SettingsHandler.getCameraWidth() * SettingsHandler.getCameraHeight());
        var ratio = contour.width / (double) contour.height;
        var x = contour.x * 100 / (double) (SettingsHandler.getCameraWidth());
        var y = contour.y * 100 / (double) (SettingsHandler.getCameraHeight());

        return (SettingsHandler.getDouble("Size:low") / 1000.0) <= size && size <= (SettingsHandler.getDouble("Size:high") / 1000.0) &&
                SettingsHandler.getDouble("W/H Ratio:low") <= ratio && ratio <= SettingsHandler.getDouble("W/H Ratio:high") &&
                SettingsHandler.getDouble("X Range:low") <= x && x <= SettingsHandler.getDouble("X Range:high") &&
                SettingsHandler.getDouble("Y Range:low") <= y && y <= SettingsHandler.getDouble("Y Range:high");
    }

    private static boolean compareContours(Rect contour1, Rect contour2, String compareMode) {
        double x1 = (contour1.tl().x + contour1.br().x) / 2.0;
        double y1 = (contour1.tl().y + contour1.br().y) / 2.0;

        double x2 = (contour2.tl().x + contour2.br().x) / 2.0;
        double y2 = (contour2.tl().y + contour2.br().y) / 2.0;

        switch (compareMode) {
            case "largest":
                return contour1.area() < contour2.area();
            case "smallest":
                return contour1.area() > contour2.area();
            case "lowest":
                return y1 < y2;
            case "highest":
                return y1 > y2;
            case "leftmost":
                return x1 > x2;
            case "rightmost":
                return x1 < x2;
            case "centermost":
                return Math.sqrt(x1 * x1 + y1 * y1) > Math.sqrt(x2 * x2 + y2 * y2);
            default:
                throw new IllegalStateException();
        }
    }

    public Mat getProcessedImage() {
        return image.clone();
    }

    public Rect getBoundingRectangle() {
        return boundingRectangle.clone();
    }

    public Vector getContourPosition() {
        return contourPosition;
    }

    public void release() {
        image.release();
    }

    public boolean hasValidContour() {
        return hasValidContour;
    }
}
