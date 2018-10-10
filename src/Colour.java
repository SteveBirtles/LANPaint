import javafx.scene.paint.Color;

class Colour {

    static Color colour[] = new Color[216];

    static void prepareColours() {

        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    colour[r + 6*g + 36*b] = Color.rgb(r*51, g*51, b*51);
                }
            }
        }

    }

}
