package classes;
import java.io.*;
import java.util.*;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

import classes.tileClass.Tile;

public class TileManager {
   

    // Cache loaded tiles to avoid repeated disk reads (optional)
    private final Map<String, Tile> tileCache = new HashMap<>();
    
    

    /**
     * Get tile containing the given lat, lon.
     * @param lat Latitude in decimal degrees
     * @param lon Longitude in decimal degrees
     * @return Tile object or null if not found
     */


    public static ReferencedEnvelope getTileEnvelope(tileClass.Tile tile) {
        double cellSize = 1.0 / 3600.0; // for both lat and lon

        double minX = tile.originLon;
        double maxX = tile.originLon + tile.width * cellSize;

        double maxY = tile.originLat;
        double minY = tile.originLat - tile.height * cellSize;

        try{
            CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
            return new ReferencedEnvelope(minX, maxX, minY, maxY, crs);
        } catch (Exception e){
            System.err.println(e);
        }
        return null;
    }


    public static Tile TileFromFile(File F){
        // Register GDAL drivers
        gdal.AllRegister();
        gdal.PushErrorHandler("CPLQuietErrorHandler");  
        Dataset dataset = gdal.Open(F.getAbsolutePath(), gdalconstConstants.GA_ReadOnly);
        if(dataset == null){
            System.err.println("Failed to open " + F.getAbsolutePath());
            return null;
        }
        double[] geoTransform = dataset.GetGeoTransform();
        // Get raster band (assuming single band for DT2)
        Band band = dataset.GetRasterBand(1);
        int width = band.getXSize();
        int height = band.getYSize();
        

        // Read raster data
        int[] rasterData = new int[width * height];
        band.ReadRaster(0, 0, width, height, rasterData);
        double originLon = geoTransform[0];
        double originLat = geoTransform[3];
        //Create Tile
        Tile toBeReturned =  new Tile(F.getName(), rasterData, originLat, originLon, width, height);
        // Close dataset
        dataset.delete();
        //Return the tile
        return toBeReturned;
    }

    public ArrayList<Tile> getSurroundingTiles(double lat, double lon) {
        ArrayList<Tile> tiles = new ArrayList<>();

        int centerLat = (int) Math.floor(lat);
        int centerLon = (int) Math.floor(lon);

        // Loop over -1, 0, +1 offset in lat and lon to get 3x3 grid
        for (int dLat = -1; dLat <= 1; dLat++) {
            for (int dLon = -1; dLon <= 1; dLon++) {
                int neighborLat = centerLat + dLat;
                int neighborLon = centerLon + dLon;
                Tile neighborTile = getTile(neighborLat+1, neighborLon+1); // getTile works with center of tile
                if (neighborTile != null) {
                    tiles.add(neighborTile);
                }
                else{
                    break;
                }
            }
        }

        return tiles;
    }




    public Tile getTile(double lat, double lon) {
        // Determine tile origin coordinates (integer degree)
        int tileLat = (int) Math.floor(lat);
        int tileLon = (int) Math.floor(lon);

        // Build filename based on tile origin
        String filename = buildTileFileName(tileLat, tileLon);
        filename = "data\\terrainData\\".concat(filename);
        // Check cache first
        if (tileCache.containsKey(filename)) {
            return tileCache.get(filename);
        }

        // Load tile from disk
        Tile tile = TileFromFile(new File(filename));
        if (tile != null) {
            tileCache.put(filename, tile);
        }
        return tile;
    }

    /**
     * Construct filename from tile origin.
     * Example: lat=9, lon=76 => "n09_e076_1arc_v3.pkl"
     */
    private String buildTileFileName(int lat, int lon) {
        // Latitude prefix
        String latPrefix = (lat >= 0) ? "n" : "s";
        int latAbs = Math.abs(lat);

        // Longitude prefix
        String lonPrefix = (lon >= 0) ? "e" : "w";
        int lonAbs = Math.abs(lon);

        // Format with leading zeros to 2 or 3 digits as needed
        String latStr = String.format("%s%02d", latPrefix, latAbs);
        String lonStr = String.format("%s%03d", lonPrefix, lonAbs);

        // Adjust filename pattern if different
        String filename = latStr + "_" + lonStr + "_1arc_v3.dt2";

        return filename;
    }

    
    


    public int getElevation(double lat, double lon) {
        Tile tile = getTile(lat, lon);
        if (tile == null) {
            return Integer.MIN_VALUE; // or some nodata value
        }

        // Calculate pixel indices inside tile
        // Tile origin (tile.originLat, tile.originLon) is bottom-left or top-left? 
        // Assuming originLat/originLon are bottom-left corner of tile
        double latDiff = lat - tile.originLat; // degrees difference in latitude
        double lonDiff = lon - tile.originLon; // degrees difference in longitude

        // Each tile covers 1 degree with 3601 pixels
        double pixelSize = 1.0 / (tile.width - 1); // degrees per pixel

        int row = (int) ((1.0 - latDiff) / pixelSize); // Flip latitude: top-left origin
        int col = (int) (lonDiff / pixelSize);

        // Check bounds
        if (row < 0 || row >= tile.height || col < 0 || col >= tile.width) {
            return Integer.MIN_VALUE; // outside tile
        }

        // Index in data array (row-major order)
        int idx = row * tile.width + col;

        return tile.data[idx];
    }

    



    // Test example
    public static void main(String[] args) {
        
        TileManager manager = new TileManager();

        double lat = 10;
        double lon = 75;

        List<Tile> tiles = manager.getSurroundingTiles(lat, lon);
        if(tiles==null || tiles.size() == 0) System.out.println("Empty List");
        System.out.println("Loaded " + tiles.size() + " surrounding tiles:");
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        int tileWidth = tiles.get(0).width;
        int tileHeight = tiles.get(0).height;

        for (Tile tile : tiles) {
            double tileLat = tile.originLat;
            double tileLon = tile.originLon;

            minLat = Math.min(minLat, tileLat - tileHeight * 0.001); // assuming 0.001 deg/pixel
            maxLat = Math.max(maxLat, tileLat);
            minLon = Math.min(minLon, tileLon);
            maxLon = Math.max(maxLon, tileLon + tileWidth * 0.001);
            System.out.println("Loaded Tile : " + tile.originLat + ", " + tile.originLon);
        
        }
        System.out.println("Max Latitude : " + maxLat + ", Min Latitude : " + maxLat);
        System.out.println("Max Longitude : " + maxLon + "Min Longitude : " + minLon );
    }
}
