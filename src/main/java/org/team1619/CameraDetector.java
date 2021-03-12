package org.team1619;

import java.io.IOException;
import java.util.*;

public class CameraDetector {

    public static Set<DetectedCamera> getDetectedCameras() {
        var detectedCameras = new HashSet<DetectedCamera>();

        try {
            var process = Runtime.getRuntime().exec("v4l2-ctl --list-devices");
            process.waitFor();

            var lines = new String(process.getInputStream().readAllBytes()).split("\n");
            for(var l = 0; l < lines.length; l++) {
                var line = lines[l].trim();

                if(!line.isEmpty()) {
                    l++;

                    var camera = new DetectedCamera(line.substring(0, line.length() - 1));

                    String innerLine;
                    while (l < lines.length && !(innerLine = lines[l].trim()).isEmpty()) {
                        camera.addPath(innerLine);
                        l++;
                    }

                    detectedCameras.add(camera);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return detectedCameras;
    }

    public static DetectedCamera getCameraFromId(String id) {
        var cameras = getDetectedCameras();

        var cameraMap = new HashMap<String, DetectedCamera>();

        cameras.stream().forEach(camera -> cameraMap.put(camera.getIdentifier(), camera));

        return cameraMap.get(id);
    }

    public static class DetectedCamera {

        private final String id;
        private final List<String> paths;

        public DetectedCamera(String id) {
            this.id = id;
            this.paths = new ArrayList<>();
        }

        public String getIdentifier() {
            return id;
        }

        public List<String> getPaths() {
            return new ArrayList<>(paths);
        }

        private void addPath(String path) {
            paths.add(path);
        }

        @Override
        public String toString() {
            return getIdentifier() + ": " + paths;
        }
    }
}
