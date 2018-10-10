import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Network {

    static ArrayList<Pixel> newPixels = new ArrayList<>();
    private static int failCount = 0;

    static void getUpdate() {

        StringBuilder s = new StringBuilder();
        for (Pixel p : newPixels) {
            s.append("x").append(p.x).append("y").append(p.y).append("c").append(p.c);
        }
        newPixels.clear();
        Map.clientMap = Network.getUpdate(LANPaint.ADDRESS, Map.clientMap, s.toString());
    }

    private static int[][] getUpdate(String serverAddress, int[][] map, String changedPixels) {

        URL url;
        HttpURLConnection con;

        try {
            long lastTime = 0;
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
                        Map.updateMap(deltaData, map, null, serverTime);
                    }
                }

            }
        }

        catch(Exception ex) {
            ex.printStackTrace();
        }

        return map;
    }

}
