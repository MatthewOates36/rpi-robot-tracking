package org.team1619.web;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

public class HttpStreamServer {

    private static final String BOUNDARY = "stream";

    private final int port;
    private final ServerSocket serverSocket;
    private final Set<Socket> sockets;
    private final Object matLock;

    private Mat imageMat;
    private long lastUpdateTime;

    public HttpStreamServer(int port) throws IOException {
        this.port = port;
        this.imageMat = new Mat();
        serverSocket = new ServerSocket(port);
        sockets = new HashSet<>();
        new Thread(() -> {
            while (true) {
                try {
                    startStreamingServer(serverSocket.accept());
                } catch (SocketTimeoutException e) {

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        matLock = new Object();
        lastUpdateTime = 0;
    }

    public static BufferedImage mat2bufferedImage(Mat image) throws IOException {
        MatOfByte bytemat = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, bytemat);
        byte[] bytes = bytemat.toArray();
        bytemat.release();
        InputStream in = new ByteArrayInputStream(bytes);
        BufferedImage img = ImageIO.read(in);
        return img;
    }

    public void startStreamingServer(Socket socket) throws IOException {
        writeHeader(socket.getOutputStream(), BOUNDARY);
        socket.setSoTimeout(200);
        synchronized (sockets) {
            sockets.add(socket);
        }
    }

    private void writeHeader(OutputStream stream, String boundary) throws IOException {
        stream.write(("HTTP/1.0 200 OK\r\n" +
                "Connection: close\r\n" +
                "Max-Age: 0\r\n" +
                "Expires: 0\r\n" +
                "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                "Pragma: no-cache\r\n" +
                "Content-Type: multipart/x-mixed-replace; " +
                "boundary=" + boundary + "\r\n" +
                "\r\n" +
                "--" + boundary + "\r\n").getBytes());
    }

    public void setImage(Mat img) {
        if(System.currentTimeMillis() - lastUpdateTime < 300) {
            return;
        }
        lastUpdateTime = System.currentTimeMillis();

        var image = img.clone();
        new Thread(() -> {
            if(sockets.isEmpty()) {
                image.release();
                return;
            }

            try {
                pushImage(image);
                image.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void pushImage(Mat frame) throws IOException {
        BufferedImage img;
        if (null == frame || frame.empty()) {
            return;
        }

        img = mat2bufferedImage(frame);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        var removeSockets = new HashSet<Socket>();
        synchronized (sockets) {
            for (var socket : sockets) {
                try {
                    var outputStream = socket.getOutputStream();
                    outputStream.write(("Content-type: image/jpeg\r\n" +
                            "Content-Length: " + imageBytes.length + "\r\n" +
                            "\r\n").getBytes());
                    outputStream.write(imageBytes);
                    outputStream.write(("\r\n--" + BOUNDARY + "\r\n").getBytes());
                } catch (Exception ex) {
                    removeSockets.add(socket);
                }
            }
        }
        sockets.addAll(removeSockets);
    }

    public void stopStreamingServer() throws IOException {
        sockets.forEach(socket -> {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverSocket.close();
    }

    public boolean connected() {
        return !sockets.isEmpty();
    }
}