import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class PaintServer extends AbstractHandler {

    private int[][] serverPixelMap = null;
    private long[][] serverTimeMap = null;
    static boolean changeMade = false;

    PaintServer() {

        serverPixelMap = new int[Map.MAX_X][Map.MAX_Y];
        serverTimeMap = new long[Map.MAX_X][Map.MAX_Y];
        for (int x = 0; x < Map.MAX_X; x++) {
            for (int y = 0; y < Map.MAX_Y; y++) {
                serverPixelMap[x][y] = 0;
                serverTimeMap[x][y] = 0;
            }
        }
    }

    private String mapDelta(long time) {
        StringBuilder s = new StringBuilder();
        for (int x = 0; x < Map.MAX_X; x++) {
            for (int y = 0; y < Map.MAX_Y; y++) {
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
            Map.updateMap(pixels, serverPixelMap, serverTimeMap, serverTime);
            PaintServer.changeMade = true;
        }

        response.getWriter().println("time=" + serverTime + "<br/>pixels=" + mapDelta(time));

        baseRequest.setHandled(true);

    }

    static void startSnapshots() {

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

}
