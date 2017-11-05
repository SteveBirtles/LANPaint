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
import javafx.scene.input.MouseButton;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

public class LanPaint extends Application {

    public static boolean server = true;
    public static boolean client = false;

    public static boolean fullscreen = true;
    public static String serverAddress = server ? "localhost" : "192.168.1.2";

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
            StringBuilder s = new StringBuilder();
            for (int x = 0; x < MAX_X; x++) {
                for (int y = 0; y < MAX_Y; y++) {
                    if (serverTimeMap[x][y] > time) {
                        s.append("x" + x + "y" + y + "c" + serverPixelMap[x][y]);
                    }
                }
            }
            return s.toString();
        }

        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {

            long serverTime = System.currentTimeMillis() >> 8;

            long time = -1;
            String pixels = null;

            response.setContentType("text/html; charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            if (request.getRequestURI().equals("/favicon.ico")) return;

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
                    updateMap(pixels, serverPixelMap, serverTimeMap, serverTime);
                }

                response.getWriter().println(serverTime + ":" + mapDelta(time));

            }

            baseRequest.setHandled(true);

        }

    }

    public static void updateMap(String pixels, int[][] map, long[][] timeMap, long time) {

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

    public static int[][] clientMap = null;
    public static int[][] lastClientMap = null;
    public static Color colour[] = new Color[216];
    public static int selectedRed = 5;
    public static int selectedGreen = 0;
    public static int selectedBlue = 0;
    public static int selectedColour = 0;
    public static int lastSelectedColour = 0;

    public static long lastTime = 0;

    @Override
    public void start(Stage stage) throws Exception {

        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    colour[r + 6*g + 36*b] = Color.rgb(r*51, g*51, b*51);
                }
            }
        }

        clientMap = new int[MAX_X][MAX_Y];
        lastClientMap = new int[MAX_X][MAX_Y];
        for (int x = 0; x < MAX_X; x++) {
            for (int y = 0; y < MAX_Y; y++) {
                clientMap[x][y] = 0;
                lastClientMap[x][y] = 0;
            }
        }

        Pane rootPane = new Pane();
        Scene scene = new Scene(rootPane);

        stage.setTitle("LANPaint" + (server ? " [SERVER]" : ""));
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
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        gc.setStroke(Color.WHITE);
        gc.strokeRect(39, 39, MAX_X*PIXEL_SIZE+2, MAX_Y*PIXEL_SIZE+2);

        rootPane.setOnMouseClicked(event -> {

            int x = (int) (event.getSceneX() - 40) / PIXEL_SIZE;
            int y = (int) (event.getSceneY() - 40) / PIXEL_SIZE;
            if (x >= 0 && y >= 0 && x < MAX_X && y < MAX_Y) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    if (clientMap[x][y] != selectedColour) {
                        clientMap[x][y] = 215;
                        newPixels.add(new Pixel(x, y, selectedColour));
                    }
                }
                else if (event.getButton() == MouseButton.SECONDARY) {
                    int picked = clientMap[x][y];
                    selectedRed = picked % 6;
                    selectedGreen = Math.floorDiv(picked - selectedRed, 6) % 6;
                    selectedBlue = Math.floorDiv(picked - selectedRed - selectedGreen * 6, 36);
                }
            }

        });

        new AnimationTimer() {
            @Override
            public void handle(long now) {

                for (KeyCode k : keysPressed) {

                    if (k == KeyCode.ESCAPE) stage.close();

                    if (k == KeyCode.DIGIT1) { selectedRed = 5; selectedGreen = 0; selectedBlue = 0; }
                    if (k == KeyCode.DIGIT2) { selectedRed = 5; selectedGreen = 2; selectedBlue = 0; }
                    if (k == KeyCode.DIGIT3) { selectedRed = 5; selectedGreen = 4; selectedBlue = 0; }
                    if (k == KeyCode.DIGIT4) { selectedRed = 0; selectedGreen = 5; selectedBlue = 0; }
                    if (k == KeyCode.DIGIT5) { selectedRed = 0; selectedGreen = 3; selectedBlue = 3; }
                    if (k == KeyCode.DIGIT6) { selectedRed = 0; selectedGreen = 0; selectedBlue = 5; }
                    if (k == KeyCode.DIGIT7) { selectedRed = 2; selectedGreen = 0; selectedBlue = 5; }
                    if (k == KeyCode.DIGIT8) { selectedRed = 4; selectedGreen = 0; selectedBlue = 5; }
                    if (k == KeyCode.DIGIT9) { selectedRed = 5; selectedGreen = 5; selectedBlue = 5; }
                    if (k == KeyCode.DIGIT0) { selectedRed = 0; selectedGreen = 0; selectedBlue = 0; }

                    if (k == KeyCode.Q) selectedRed = 0;
                    if (k == KeyCode.W) selectedRed = 1;
                    if (k == KeyCode.E) selectedRed = 2;
                    if (k == KeyCode.R) selectedRed = 3;
                    if (k == KeyCode.T) selectedRed = 4;
                    if (k == KeyCode.Y) selectedRed = 5;

                    if (k == KeyCode.A) selectedGreen = 0;
                    if (k == KeyCode.S) selectedGreen = 1;
                    if (k == KeyCode.D) selectedGreen = 2;
                    if (k == KeyCode.F) selectedGreen = 3;
                    if (k == KeyCode.G) selectedGreen = 4;
                    if (k == KeyCode.H) selectedGreen = 5;

                    if (k == KeyCode.Z) selectedBlue = 0;
                    if (k == KeyCode.X) selectedBlue = 1;
                    if (k == KeyCode.C) selectedBlue = 2;
                    if (k == KeyCode.V) selectedBlue = 3;
                    if (k == KeyCode.B) selectedBlue = 4;
                    if (k == KeyCode.N) selectedBlue = 5;

                }

                selectedColour = selectedRed + selectedGreen*6 + selectedBlue*36;

                if (lastSelectedColour != selectedColour) {
                    gc.setFill(colour[selectedColour]);
                    gc.fillRect(0, 0, WINDOW_WIDTH, 20);
                    gc.fillRect(0, WINDOW_HEIGHT - 20, WINDOW_WIDTH, 20);
                    gc.fillRect(0, 0, 20, WINDOW_HEIGHT);
                    gc.fillRect(WINDOW_WIDTH - 20, 0, 20, WINDOW_HEIGHT);
                    lastSelectedColour = selectedColour;
                }

                if (clientMap != null) {
                    for (int x = 0; x < MAX_X; x++) {
                        for (int y = 0; y < MAX_Y; y++) {

                            int value = clientMap[x][y];

                            if (value < 0 || value > 215) continue;
                            if (value == lastClientMap[x][y]) continue;

                            gc.setFill(colour[value]);
                            gc.fillRect(40 + x*PIXEL_SIZE, 40 + y*PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE);

                            lastClientMap[x][y] = value;

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

                if (input.toString().contains(":")) {
                    String[] splitInput = input.toString().split(":");

                    if (splitInput.length == 2) {
                        long serverTime = Long.parseLong(splitInput[0]);
                        String deltaData = splitInput[1];
                        updateMap(deltaData, map, null, serverTime);
                    }
                }

            }
        }

        catch(Exception ex) {
            ex.printStackTrace();
        }

        return map;
    }

    public static void main(String[] args) throws Exception {

        if (LanPaint.server) {
            Server server = new Server(8081);
            server.setHandler(new LANPaintServer());
            server.start();
            System.out.println("Server online!");
        }

        if (LanPaint.client) {
            launch(args);
            System.exit(0);
        }
    }

}

