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
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LANPaint extends Application {

    public static final boolean FULLSCREEN = true;

    public static boolean SERVER = true;
    public static int SCREEN = 1; // DON'T CHANGE THIS
    public static String ADDRESS = "localhost";

    public static int WINDOW_WIDTH = 1920;
    public static int WINDOW_HEIGHT = 1080;

    public static int PIXEL_SIZE = 3;

    public static final int MAX_X = 640;
    public static final int MAX_Y = 360;

    public static int RELOADER = -1;
    public static boolean changeMade = false;

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

            long time = 0;
            String pixels = null;

            response.setContentType("text/html; charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            if (request.getRequestURI().equals("/favicon.ico")) return;

            String method = request.getMethod().toUpperCase();

            String queryString = request.getQueryString();
            if (queryString == null) queryString = "";


            for (String q : queryString.split("&")) {
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
                String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy"));
                String ip = request.getRemoteAddr();
                System.out.println("[" + timeStamp + "] Updated received from " + ip + " (" + pixels + ")");
                updateMap(pixels, serverPixelMap, serverTimeMap, serverTime);
                changeMade = true;
            }

            response.getWriter().println("time=" + serverTime + "<br/>pixels=" + mapDelta(time));

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
                } else {
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

    public static int failCount = 0;

    public static HashSet<KeyCode> keysPressed = new HashSet<>();

    public static int[][] clientMap = null;
    public static int[][] lastClientMap = null;
    public static int[][] mapBackup = null;
    public static Color colour[] = new Color[216];
    public static int selectedRed = 5;
    public static int selectedGreen = 0;
    public static int selectedBlue = 0;
    public static int selectedColour = 0;
    public static int lastSelectedColour = 0;

    public static long lastTime = 0;

    private void backup(String filename) {
        try {
            FileOutputStream file = new FileOutputStream(filename);
            for (int i = 0; i < MAX_X; i++) {
                for (int j = 0; j < MAX_Y; j++) {
                    file.write(clientMap[i][j]);
                }
            }
            file.close();
            System.out.println("Backup made: " + filename);
        } catch (IOException e) {
            System.out.println("Backup error - " + e.toString());
        }
    }

    private void restore(String filename) {
        try {
            FileInputStream file = new FileInputStream(filename);
            for (int i = 0; i < MAX_X; i++) {
                for (int j = 0; j < MAX_Y; j++) {
                    mapBackup[i][j] = file.read();
                }
            }
            file.close();
            System.out.println("Restoration started: " + filename);
        } catch (IOException e) {
            System.out.println("Restore error - " + e.toString());
        }
        RELOADER = 0;
    }

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
        mapBackup = new int[MAX_X][MAX_Y];
        for (int x = 0; x < MAX_X; x++) {
            for (int y = 0; y < MAX_Y; y++) {
                clientMap[x][y] = 0;
                lastClientMap[x][y] = 0;
                mapBackup[x][y] = 0;
            }
        }

        Pane rootPane = new Pane();
        Scene scene = new Scene(rootPane);

        stage.setTitle("LANPaint");
        stage.setResizable(false);
        stage.setFullScreen(FULLSCREEN);
        stage.setScene(scene);
        stage.setWidth(WINDOW_WIDTH);
        stage.setHeight(WINDOW_HEIGHT + (FULLSCREEN ? 0 : 28));
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

        rootPane.setOnMouseClicked(event -> {

            int x;
            int y;

            if (SERVER) {
                x = (int) event.getSceneX() / PIXEL_SIZE;
                y = (int) event.getSceneY() / PIXEL_SIZE;
            } else {
                int screenX = (SCREEN-1) % 5;
                int screenY = (SCREEN-1) / 5;

                int xx = (int) (event.getSceneX()-128) / PIXEL_SIZE;
                int yy = (int) (event.getSceneY()-152) / PIXEL_SIZE;

                if (xx < 0 || xx > 127) {
                    x = -1;
                } else {
                    x = xx + screenX*128;
                }

                if (yy < 0 || yy > 89) {
                    y = -1;
                } else {
                    y = yy + screenY*90;
                }

            }


            if (!SERVER) {
                if (x >= 0 && y >= 0 && x < MAX_X && y < MAX_Y) {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (clientMap[x][y] != selectedColour) {
                            clientMap[x][y] = 215;
                            newPixels.add(new Pixel(x, y, selectedColour));
                        }
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        int picked = clientMap[x][y];
                        selectedRed = picked % 6;
                        selectedGreen = Math.floorDiv(picked - selectedRed, 6) % 6;
                        selectedBlue = Math.floorDiv(picked - selectedRed - selectedGreen * 6, 36);
                    }
                }
            }

        });

        if (SERVER) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    if (changeMade) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                        Date date = new Date();
                        String filename = "snapshots/" + dateFormat.format(date) + ".dat";
                        backup(filename);
                        changeMade = false;
                    }
                }
            }, 60 * 1000, 60 * 1000);
        }

        new AnimationTimer() {
            @Override
            public void handle(long now) {

                if (SERVER && RELOADER == -1) {
                    restore("save.dat");
                }

                for (KeyCode k : keysPressed) {

                    if (k == KeyCode.ESCAPE) {
                        if (SERVER) backup("save.dat");
                        stage.close();
                    }

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

                if (SERVER && RELOADER >= 0 && RELOADER < MAX_X * MAX_Y) {

                    Random rnd = new Random(System.currentTimeMillis());
                    outer:
                    for (int i = 0; i < 32; i++) {
                        while (true) {
                            RELOADER++;
                            if (RELOADER >= MAX_X * MAX_Y) {
                                break outer;
                            }
                            int x = RELOADER % MAX_X;
                            int y = RELOADER / MAX_X;
                            if (mapBackup[x][y] != clientMap[x][y]) {
                                newPixels.add(new Pixel(x, y, mapBackup[x][y]));
                                break;
                            }
                        }
                    }
                }

                selectedColour = selectedRed + selectedGreen*6 + selectedBlue*36;

                if (!SERVER) {
                    gc.strokeRect(127, 151, WINDOW_WIDTH-254, WINDOW_HEIGHT-302);
                    gc.setFill(colour[selectedColour]);
                    gc.fillRect(0, 0, WINDOW_WIDTH, 20);
                    gc.fillRect(0, WINDOW_HEIGHT - 20, WINDOW_WIDTH, 20);
                    gc.fillRect(0, 0, 20, WINDOW_HEIGHT);
                    gc.fillRect(WINDOW_WIDTH - 20, 0, 20, WINDOW_HEIGHT);
                    lastSelectedColour = selectedColour;
                }

                int startX = 0;
                int startY = 0;
                int endX = MAX_X;
                int endY = MAX_Y;

                int screenX = (SCREEN-1) % 5;
                int screenY = (SCREEN-1) / 5;

                if (!SERVER) {
                    startX = -16;
                    startY = -19;
                    endX = 128+16;
                    endY = 90+19;
                }

                if (clientMap != null) {
                    for (int x = startX; x < endX; x++) {
                        for (int y = startY; y < endY; y++) {

                            int xx = x + screenX*128;
                            int yy = y + screenY*90;

                            if (xx < 0 || yy < 0 || xx >= MAX_X || yy >= MAX_Y) { continue; }

                            int value = clientMap[xx][yy];

                            if (value < 0 || value > 215) continue;
                            if (value == lastClientMap[xx][yy]) continue;

                            gc.setFill(colour[value]);

                            if (SERVER) {
                                gc.fillRect(x * PIXEL_SIZE, y * PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE);
                            } else {
                               gc.fillRect(x * PIXEL_SIZE + 128, y * PIXEL_SIZE + 152 , PIXEL_SIZE, PIXEL_SIZE);
                            }


                            lastClientMap[xx][yy] = value;

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
        clientMap = getUpdate(ADDRESS, clientMap, s.toString());
    }

    public static int[][] getUpdate(String serverAddress, int[][] map, String changedPixels) {

        URL url;
        HttpURLConnection con;

        try {
            url = new URL("http://" + serverAddress + ":8888"
                    + "?time=" + (map == null ? -1 : lastTime)
                    + (changedPixels.length() > 0 ? "&pixels=" + changedPixels: ""));
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int responseCode = 0;
            try {
                responseCode = con.getResponseCode();
            } catch (ConnectException ce) {
                failCount++;
                System.out.println("Unable to connect to Server... [" + failCount + "/10]");
                if (failCount >= 10) System.exit(-10);
            }

            if (responseCode == 200) {
                failCount = 0;
                InputStream inputStream = con.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder input = new StringBuilder();
                while (br.ready()) {
                    input.append(br.readLine());
                }

                if (input.toString().contains("<br/>pixels=")) {
                    String[] splitInput = input.toString().split("<br/>pixels=");

                    if (splitInput.length == 2) {
                        long serverTime = Long.parseLong(splitInput[0].replace("time=", ""));
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

        if (args.length == 0 || args.length > 2) {
            System.out.println("Incorrect command line arguments.");
            System.exit(1);
        } else if (args.length == 2) {
            SERVER = false;
            ADDRESS = args[0];
            if (args[1].toLowerCase().equals("auto")) {
                InetAddress inetAddress = InetAddress.getLocalHost();
                String hostname = inetAddress.getHostName();
                String suffix = hostname.substring(hostname.length()-2, hostname.length());
                SCREEN = Integer.parseInt(suffix);
                if (SCREEN < 1 || SCREEN > 20) {
                    System.out.println(hostname + ": Invalid screen assigned, exiting...");
                    System.exit(2);
                } else {
                    System.out.println(hostname + ": Automatically assigned to screen " + SCREEN);
                }
            } else {
                SCREEN = Integer.parseInt(args[1]);
            }
            WINDOW_WIDTH = 1280;
            WINDOW_HEIGHT = 1024;
            PIXEL_SIZE = 8;
        } else {
            if (args[0].toLowerCase().equals("server")) {
                Server server = new Server(8888);
                server.setHandler(new LANPaintServer());
                server.start();
                System.out.println("Server online!");
            } else {
                ADDRESS = args[0];
            }
        }

        launch(args);
        System.exit(0);

    }

}

