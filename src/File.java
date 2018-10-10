import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class File {


    static void backup(String filename) {
        try {
            FileOutputStream file = new FileOutputStream(filename);
            for (int i = 0; i < Map.MAX_X; i++) {
                for (int j = 0; j < Map.MAX_Y; j++) {
                    file.write((byte) (Map.clientMap[i][j]-128));
                }
            }
            file.close();
            System.out.println("Backup made: " + filename);
        } catch (IOException e) {
            System.out.println("Backup error - " + e.toString());
        }
    }

    static void restore() {
        try {

            byte[] buffer = new byte[1];

            FileInputStream file = new FileInputStream("save.dat");
            for (int i = 0; i < Map.MAX_X; i++) {
                for (int j = 0; j < Map.MAX_Y; j++) {
                    file.read(buffer);
                    Map.mapBackup[i][j] = buffer[0]+128;
                }
            }
            file.close();
            System.out.println("Restoration started: " + "save.dat");
        } catch (IOException e) {
            System.out.println("Restore error - " + e.toString());
        }
        Frame.RELOADER = 0;
    }

}
