import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;

class Frame {

    static int RELOADER = -1;

    static void startTimer(GraphicsContext gc) {

        new AnimationTimer() {
            @Override
            public void handle(long now) {

                if (!LANPaint.TERMINATE) {

                    if (LANPaint.SERVER && RELOADER == -1) {
                        File.restore();
                    }

                    Input.processKeys();

                    if (LANPaint.SERVER && RELOADER >= 0 && RELOADER < Map.MAX_X * Map.MAX_Y) {

                        outer:
                        for (int i = 0; i < 32; i++) {
                            while (true) {
                                RELOADER++;
                                if (RELOADER >= Map.MAX_X * Map.MAX_Y) {
                                    break outer;
                                }
                                int x = RELOADER % Map.MAX_X;
                                int y = RELOADER / Map.MAX_X;
                                if (Map.mapBackup[x][y] != Map.clientMap[x][y]) {
                                    Network.newPixels.add(new Pixel(x, y, Map.mapBackup[x][y]));
                                    break;
                                }
                            }
                        }
                    }

                    Input.selectedColour = Input.selectedRed + Input.selectedGreen * 6 + Input.selectedBlue * 36;

                    if (!LANPaint.SERVER) {
                        gc.strokeRect(127, 151, LANPaint.WINDOW_WIDTH - 254, LANPaint.WINDOW_HEIGHT - 302);
                        gc.setFill(LANPaint.colour[Input.selectedColour]);
                        gc.fillRect(0, 0, LANPaint.WINDOW_WIDTH, 20);
                        gc.fillRect(0, LANPaint.WINDOW_HEIGHT - 20, LANPaint.WINDOW_WIDTH, 20);
                        gc.fillRect(0, 0, 20, LANPaint.WINDOW_HEIGHT);
                        gc.fillRect(LANPaint.WINDOW_WIDTH - 20, 0, 20, LANPaint.WINDOW_HEIGHT);
                    }

                    int startX = 0;
                    int startY = 0;
                    int endX = Map.MAX_X;
                    int endY = Map.MAX_Y;

                    int screenX = (LANPaint.SCREEN - 1) % 5;
                    int screenY = (LANPaint.SCREEN - 1) / 5;

                    if (!LANPaint.SERVER) {
                        startX = -16;
                        startY = -19;
                        endX = 128 + 16;
                        endY = 90 + 19;
                    }

                    if (Map.clientMap != null) {
                        for (int x = startX; x < endX; x++) {
                            for (int y = startY; y < endY; y++) {

                                int xx = x + screenX * 128;
                                int yy = y + screenY * 90;

                                if (xx < 0 || yy < 0 || xx >= Map.MAX_X || yy >= Map.MAX_Y) {
                                    continue;
                                }

                                int value = Map.clientMap[xx][yy];

                                if (value < 0 || value > 215) continue;
                                if (value == Map.lastClientMap[xx][yy]) continue;

                                gc.setFill(LANPaint.colour[value]);

                                if (LANPaint.SERVER) {
                                    gc.fillRect(x * LANPaint.PIXEL_SIZE, y * LANPaint.PIXEL_SIZE, LANPaint.PIXEL_SIZE, LANPaint.PIXEL_SIZE);
                                } else {
                                    gc.fillRect(x * LANPaint.PIXEL_SIZE + 128, y * LANPaint.PIXEL_SIZE + 152, LANPaint.PIXEL_SIZE, LANPaint.PIXEL_SIZE);
                                }


                                Map.lastClientMap[xx][yy] = value;

                            }
                        }
                    }

                }
            }
        }.start();

    }

}
