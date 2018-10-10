import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.eclipse.jetty.server.Server;

import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LANPaint extends Application {

    static boolean SERVER = true;
    static boolean TERMINATE = false;
    static int SCREEN = 1;
    static String ADDRESS = "localhost";

    static int WINDOW_WIDTH = 1920;
    static int WINDOW_HEIGHT = 1080;

    static int PIXEL_SIZE = 3;
    static boolean changeMade = false;

    static Color colour[] = new Color[216];
    static Stage stage;

    @Override
    public void start(Stage s) throws Exception {

        stage = s;

        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    colour[r + 6*g + 36*b] = Color.rgb(r*51, g*51, b*51);
                }
            }
        }

        Map.resetMap();

        Pane rootPane = new Pane();
        Scene scene = new Scene(rootPane);

        stage.setTitle("LANPaint");
        stage.setResizable(false);
        stage.setFullScreen(true);
        stage.setScene(scene);
        stage.setWidth(WINDOW_WIDTH);
        stage.setHeight(WINDOW_HEIGHT);
        stage.setOnCloseRequest((WindowEvent we) -> stage.close());
        stage.show();

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> Input.keysPressed.add(event.getCode()));
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> Input.keysPressed.remove(event.getCode()));

        Canvas canvas = new Canvas();

        canvas.setWidth(WINDOW_WIDTH);
        canvas.setHeight(WINDOW_HEIGHT);

        rootPane.getChildren().add(canvas);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        gc.setStroke(Color.WHITE);

        Input.prepareMouse(rootPane);

        if (SERVER) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    if (changeMade) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                        Date date = new Date();
                        String filename = "snapshots/" + dateFormat.format(date) + ".dat";
                        File.backup(filename);
                        changeMade = false;
                    }
                }
            }, 60 * 1000, 60 * 1000);
        }

        Frame.startTimer(gc);

        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(256),
                ae -> Network.getUpdate()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
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
                server.setHandler(new PaintServer());
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

