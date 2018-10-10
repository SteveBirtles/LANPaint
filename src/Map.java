public class Map {

    static final int MAX_X = 640;
    static final int MAX_Y = 360;
    static int[][] clientMap = null;
    static int[][] mapBackup = null;
    static int[][] lastClientMap = null;

    static void updateMap(String pixels, int[][] map, long[][] timeMap, long time) {

        for (String chunk : pixels.split("x")) {
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

    static void resetMap() {

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

    }

}
