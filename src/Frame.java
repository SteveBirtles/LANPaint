import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;

class Frame {

    static int RELOADER = -1;

    static void startTimer(GraphicsContext gc) {

        new AnimationTimer() {
            @Override
            public void handle(long now) {

                if (!PaintClient.TERMINATE) {

                    if (PaintClient.SERVER && RELOADER == -1) {
                        File.restore();
                    }

                    Input.processKeys();

                    if (PaintClient.SERVER && RELOADER >= 0 && RELOADER < Map.MAX_X * Map.MAX_Y) {

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

                    if (!PaintClient.SERVER) {
                        gc.strokeRect(127, 151, PaintClient.WINDOW_WIDTH - 254, PaintClient.WINDOW_HEIGHT - 302);
                        gc.setFill(Colour.colour[Input.selectedColour]);
                        gc.fillRect(0, 0, PaintClient.WINDOW_WIDTH, 20);
                        gc.fillRect(0, PaintClient.WINDOW_HEIGHT - 20, PaintClient.WINDOW_WIDTH, 20);
                        gc.fillRect(0, 0, 20, PaintClient.WINDOW_HEIGHT);
                        gc.fillRect(PaintClient.WINDOW_WIDTH - 20, 0, 20, PaintClient.WINDOW_HEIGHT);
                    }

                    int startX = 0;
                    int startY = 0;
                    int endX = Map.MAX_X;
                    int endY = Map.MAX_Y;

                    int screenX = (PaintClient.SCREEN - 1) % 5;
                    int screenY = (PaintClient.SCREEN - 1) / 5;

                    if (!PaintClient.SERVER) {
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

                                gc.setFill(Colour.colour[value]);

                                if (PaintClient.SERVER) {
                                    gc.fillRect(x * PaintClient.PIXEL_SIZE, y * PaintClient.PIXEL_SIZE, PaintClient.PIXEL_SIZE, PaintClient.PIXEL_SIZE);
                                } else {
                                    gc.fillRect(x * PaintClient.PIXEL_SIZE + 128, y * PaintClient.PIXEL_SIZE + 152, PaintClient.PIXEL_SIZE, PaintClient.PIXEL_SIZE);
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
