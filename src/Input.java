import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.HashSet;

class Input {

    static int selectedRed = 5;
    static int selectedGreen = 0;
    static int selectedBlue = 0;
    static int selectedColour = 0;

    static HashSet<KeyCode> keysPressed = new HashSet<>();
    static Stage stage;

    static void prepareMouse(Pane rootPane) {

        rootPane.setOnMouseClicked(event -> {

            int x;
            int y;

            if (PaintClient.SERVER) {
                x = (int) event.getSceneX() / PaintClient.PIXEL_SIZE;
                y = (int) event.getSceneY() / PaintClient.PIXEL_SIZE;
            } else {
                int screenX = (PaintClient.SCREEN-1) % 5;
                int screenY = (PaintClient.SCREEN-1) / 5;

                int xx = (int) (event.getSceneX()-128) / PaintClient.PIXEL_SIZE;
                int yy = (int) (event.getSceneY()-152) / PaintClient.PIXEL_SIZE;

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

            if (!PaintClient.SERVER) {
                if (x >= 0 && y >= 0 && x < Map.MAX_X && y < Map.MAX_Y) {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (Map.clientMap[x][y] != selectedColour) {
                            Map.clientMap[x][y] = 215;
                            Network.newPixels.add(new Pixel(x, y, selectedColour));
                        }
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        int picked = Map.clientMap[x][y];
                        Input.selectedRed = picked % 6;
                        Input.selectedGreen = Math.floorDiv(picked - Input.selectedRed, 6) % 6;
                        Input.selectedBlue = Math.floorDiv(picked - Input.selectedRed - Input.selectedGreen * 6, 36);
                    }
                }
            }

        });

    }

    static void processKeys() {

        for (KeyCode k : keysPressed) {

            if (k == KeyCode.ESCAPE) {
                if (PaintClient.SERVER) {
                    File.backup("save.dat");
                    PaintClient.TERMINATE = true;
                }
                stage.close();
            }

            if (k == KeyCode.DIGIT1) {
                selectedRed = 5;
                selectedGreen = 0;
                selectedBlue = 0;
            }
            if (k == KeyCode.DIGIT2) {
                selectedRed = 5;
                selectedGreen = 2;
                selectedBlue = 0;
            }
            if (k == KeyCode.DIGIT3) {
                selectedRed = 5;
                selectedGreen = 4;
                selectedBlue = 0;
            }
            if (k == KeyCode.DIGIT4) {
                selectedRed = 0;
                selectedGreen = 5;
                selectedBlue = 0;
            }
            if (k == KeyCode.DIGIT5) {
                selectedRed = 0;
                selectedGreen = 3;
                selectedBlue = 3;
            }
            if (k == KeyCode.DIGIT6) {
                selectedRed = 0;
                selectedGreen = 0;
                selectedBlue = 5;
            }
            if (k == KeyCode.DIGIT7) {
                selectedRed = 2;
                selectedGreen = 0;
                selectedBlue = 5;
            }
            if (k == KeyCode.DIGIT8) {
                selectedRed = 4;
                selectedGreen = 0;
                selectedBlue = 5;
            }
            if (k == KeyCode.DIGIT9) {
                selectedRed = 5;
                selectedGreen = 5;
                selectedBlue = 5;
            }
            if (k == KeyCode.DIGIT0) {
                selectedRed = 0;
                selectedGreen = 0;
                selectedBlue = 0;
            }

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

    }


}
