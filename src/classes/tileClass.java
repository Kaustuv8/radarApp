package classes;

import java.io.Serializable;

public class tileClass {
    
    public static class Tile implements Serializable{
        private static final long serialVersionUID = 1L;
        public String fileName;
        public int[] data;
        public double originLat;
        public double originLon;
        public int width;
        public int height;
        public Tile(String fileName, int[] data, double originLat, double originLon, int width, int height) {
            this.fileName = fileName;
            this.data = data;
            this.originLat = originLat;
            this.originLon = originLon;
            this.width = width;
            this.height = height;
            }
    }
}
