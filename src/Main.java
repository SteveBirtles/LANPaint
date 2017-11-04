import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Main extends Application {

    public static String serverAddress = "localhost";

    public static final int MAX_X = 150;
    public static final int MAX_Y = 118;

    public static class LANPaintServer extends AbstractHandler {

        private int[][] serverPixelMap = null;
        private long[][] serverTimeMap = null;

        public LANPaintServer() {
            serverPixelMap = new int[MAX_X][MAX_Y];
            serverTimeMap = new long[MAX_X][MAX_Y];
            for (int x = 0; x < MAX_X; x++) {
                for (int y = 0; y < MAX_Y; y++) {
                    serverPixelMap[x][y] = 0;
                    serverTimeMap[x][y] = 0;
                }
            }

        }

        private String mapDelta(long time) {
            int dCount = 0;
            StringBuilder s = new StringBuilder();
            for (int x = 0; x < MAX_X; x++) {
                for (int y = 0; y < MAX_Y; y++) {
                    if (serverTimeMap[x][y] > time) {
                        s.append("x" + x + "y" + y + "c" + serverPixelMap[x][y]);
                        dCount++;
                    }
                }
            }
            //System.out.println("Found " + dCount + " differences at time " + time);
            return s.toString();
        }

        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {

            String[] ip = request.getRemoteAddr().split("\\.");

            long time = -1;
            String pixels = null;

            response.setContentType("text/html; charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            if (request.getRequestURI().equals("/favicon.ico")) return; // SKIP FAVICON REQUESTS;

            String method = request.getMethod().toUpperCase();

            if (request.getQueryString() != null) {

                for (String q : request.getQueryString().split("&")) {
                    if (q.contains("=")) {

                        String variable = q.split("=")[0];
                        String value = q.split("=")[1];

                        if (method.equals("GET")) {
                            if (variable.equals("time")) time = Long.parseLong(value);
                            if (variable.equals("pixels")) pixels = value;
                        }
                    }
                }

                if (pixels != null) {
                    updateMap(pixels, serverPixelMap, serverTimeMap);
                }

                response.getWriter().println(mapDelta(time));

            }

            baseRequest.setHandled(true);

        }

    }

    public static long updateMap(String pixels, int[][] map, long[][] timeMap) {

        long time = System.currentTimeMillis() >> 8;

        for (String chunk : pixels.toString().split("x")) {
            if (!chunk.contains("y") || !chunk.contains("c")) continue;
            String[] ySplit = chunk.split("y");
            String[] cSplit = ySplit[1].split("c");
            int x = Integer.parseInt(ySplit[0]);
            int y = Integer.parseInt(cSplit[0]);
            int c = Integer.parseInt(cSplit[1]);

            if (x >= 0 && y >= 0 && x < MAX_X && y < MAX_Y) {
                if (timeMap == null) {
                    map[x][y] = c;
                }
                else {
                    if (timeMap[x][y] < time) {
                        map[x][y] = c;
                        timeMap[x][y] = time;
                    }
                }
            }
        }

        return time;

    }

    public class Pixel {
        public int x;
        public int y;
        public int c;
        public Pixel(int x, int y, int c) {
            this.x = x;
            this.y = y;
            this.c = c;
        }
    }

    public static ArrayList<Pixel> newPixels = new ArrayList<>();

    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 1024;
    public static final int PIXEL_SIZE = 8;
    public static int failCount = 0;

    public static HashSet<KeyCode> keysPressed = new HashSet<>();
    public static boolean fullscreen = true;

    public static int[][] clientMap = null;
    public static Color colour[] = new Color[16];
    public static int selectedColour = 0;

    public static long lastTime = 0;

    @Override
    public void start(Stage stage) throws Exception {

        colour[0] = Color.BLACK;
        colour[1] = Color.DARKGRAY;
        colour[2] = Color.LIGHTGRAY;
        colour[3] = Color.WHITE;

        colour[4] = Color.BROWN;
        colour[5] = Color.ORANGE;
        colour[6] = Color.YELLOW;
        colour[7] = Color.LIMEGREEN;

        colour[8] = Color.DARKGREEN;
        colour[9] = Color.CYAN;
        colour[10] = Color.LIGHTBLUE;
        colour[11] = Color.BLUE;

        colour[12] = Color.PURPLE;
        colour[13] = Color.MAGENTA;
        colour[14] = Color.RED;
        colour[15] = Color.PINK;

        clientMap = new int[MAX_X][MAX_Y];
        for (int x = 0; x < MAX_X; x++) {
            for (int y = 0; y < MAX_Y; y++) {
                clientMap[x][y] = 0;
            }
        }

        Pane rootPane = new Pane();
        Scene scene = new Scene(rootPane);

        stage.setTitle("LANPaint");
        stage.setResizable(false);
        stage.setFullScreen(fullscreen);
        stage.setScene(scene);
        stage.setWidth(WINDOW_WIDTH);
        stage.setHeight(WINDOW_HEIGHT);
        stage.setOnCloseRequest((WindowEvent we) -> stage.close());
        stage.show();

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> keysPressed.add(event.getCode()));
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> keysPressed.remove(event.getCode()));

        Canvas canvas = new Canvas();

        canvas.setWidth(WINDOW_WIDTH);
        canvas.setHeight(WINDOW_HEIGHT);

        rootPane.getChildren().add(canvas);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        new AnimationTimer() {
            @Override
            public void handle(long now) {

                for (KeyCode k : keysPressed) {

                    if (k == KeyCode.ESCAPE) stage.close();

                    if (k == KeyCode.Q) selectedColour = 0;
                    if (k == KeyCode.W) selectedColour = 1;
                    if (k == KeyCode.E) selectedColour = 2;
                    if (k == KeyCode.R) selectedColour = 3;
                    if (k == KeyCode.T) selectedColour = 4;
                    if (k == KeyCode.Y) selectedColour = 5;
                    if (k == KeyCode.U) selectedColour = 6;
                    if (k == KeyCode.I) selectedColour = 7;

                    if (k == KeyCode.A) selectedColour = 8;
                    if (k == KeyCode.S) selectedColour = 9;
                    if (k == KeyCode.D) selectedColour = 10;
                    if (k == KeyCode.F) selectedColour = 11;
                    if (k == KeyCode.G) selectedColour = 12;
                    if (k == KeyCode.H) selectedColour = 13;
                    if (k == KeyCode.J) selectedColour = 14;
                    if (k == KeyCode.K) selectedColour = 15;

                    if (k == KeyCode.SPACE) {
                        Point point = MouseInfo.getPointerInfo().getLocation();
                        int x = (int) (point.getX() - 40) / PIXEL_SIZE;
                        int y = (int) (point.getY() - 40) / PIXEL_SIZE;
                        if (x >= 0 && y >= 0 && x < MAX_X && y < MAX_Y) {
                            if (clientMap[x][y] != selectedColour) {
                                clientMap[x][y] = selectedColour;
                                newPixels.add(new Pixel(x, y, selectedColour));
                            }
                        }
                    }


                }

                gc.setFill(colour[selectedColour]);
                gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

                if (clientMap != null) {
                    for (int x = 0; x < MAX_X; x++) {
                        for (int y = 0; y < MAX_Y; y++) {

                            int value = clientMap[x][y];
                            if (value < 0 || value > 15) continue;

                            gc.setFill(colour[value]);
                            gc.fillRect(40 + x*PIXEL_SIZE, 40 + y*PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE);

                        }
                    }
                }

            }
        }.start();

        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(256),
                ae -> getUpdate()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private static void getUpdate() {
        StringBuilder s = new StringBuilder();
        for (Pixel p : newPixels) {
            s.append("x" + p.x + "y" + p.y + "c" + p.c);
        }
        newPixels.clear();
        clientMap = getUpdate(serverAddress, clientMap, s.toString());
    }

    public static int[][] getUpdate(String serverAddress, int[][] map, String changedPixels) {

        URL url;
        HttpURLConnection con;

        try {
            url = new URL("http://" + serverAddress + ":8081"
                    + "?time=" + (map == null ? -1 : lastTime)
                    + (changedPixels.length() > 0 ? "&pixels=" + changedPixels: ""));
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = 0;
            try {
                responseCode = con.getResponseCode();
            } catch (ConnectException ce) {
                System.out.println("Unable to connect to server...");
                failCount++;
                if (failCount > 10) System.exit(-10);
            }

            if (responseCode == 200) {
                failCount = 0;
                InputStream inputStream = con.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder input = new StringBuilder();
                while (br.ready()) {
                    input.append(br.readLine());
                }

                lastTime = updateMap(input.toString(), map, null);

            }
        }

        catch(Exception ex) {
            ex.printStackTrace();
        }

        return map;
    }

    public static void main(String[] args) throws Exception {

        Server server = new Server(8081);
        server.setHandler(new LANPaintServer());
        server.start();

        launch(args);

        server.stop();
        System.exit(0);
    }

}

