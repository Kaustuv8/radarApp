package UIElement;

import classes.tileClass.Tile;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;

import org.geotools.map.GridCoverageLayer;
import org.geotools.swing.JMapPane;
import org.geotools.swing.tool.PanTool;
import org.geotools.swing.tool.ScrollWheelTool;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;
import org.geotools.styling.StyleBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.RasterSymbolizer;

import classes.TileManager;
import classes.elevationMapStyle;
import javax.media.jai.RasterFactory;
import javax.swing.JFrame;

public class ElevationMap extends JMapPane{
    private double lat;
    private double lon;
    private elevationMapStyle styleDetailProvider = new elevationMapStyle();
    public GridCoverage2D coverage;

    private Style giveStyle(double min, double max){
        StyleBuilder sb = new StyleBuilder();
        

        ColorMap colorMap = sb.createColorMap(
            styleDetailProvider.giveLabels(), 
            styleDetailProvider.giveValues(), 
            styleDetailProvider.giveColors(), 
            ColorMap.TYPE_RAMP
        );

        RasterSymbolizer sym = sb.createRasterSymbolizer();
        sym.setColorMap(colorMap);
        return sb.createStyle(sym);
    }

    public int[] giveElevationData(ArrayList<Tile> tiles){
        int ans[] = new int[2];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for(Tile tile : tiles){
            for (int val : tile.data) {
                if (val < min) min = val;
                if (val > max) max = val;
            }
        }
        if(min<0) min = 0;
        ans[0] = min;
        ans[1] = max;
        return ans;
    }

    public int[] giveElevationData(Tile tile){
        int ans[] = new int[2];
        
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int val : tile.data) {
            if (val < min) min = val;
            if (val > max) max = val;
        }
        ans[0] = min;
        ans[1] = max;
        return ans;
    }

    public ElevationMap(ArrayList<Tile> tiles) {
    
        // Build sorted sets for tile origins.
        // For rows, higher latitudes come first (top row) so we use reverse order.
            if (tiles == null || tiles.isEmpty()) {
            throw new IllegalArgumentException("Tile list is null or empty.");
        }

        TreeSet<Double> latSet = new TreeSet<>(Collections.reverseOrder());
        TreeSet<Double> lonSet = new TreeSet<>();

        for (Tile tile : tiles) {
            latSet.add(tile.originLat);
            lonSet.add(tile.originLon);
        }
        
        // Convert sets to lists for indexing.
        ArrayList<Double> latList = new ArrayList<>(latSet);
        ArrayList<Double> lonList = new ArrayList<>(lonSet);
        
        // Determine grid dimensions (number of rows and columns)
        int rows = latList.size();
        int cols = lonList.size();
        
        // Assume all tiles have the same dimensions.
        int tileWidth = tiles.get(0).width;
        int tileHeight = tiles.get(0).height;
        
        // Calculate overall raster dimensions based on grid size.
        int totalWidth = cols * tileWidth;
        int totalHeight = rows * tileHeight;
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, totalWidth, totalHeight, 1, null);
        
        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;
        
        // Fill the raster tile by tile based on its grid position.
        for (Tile tile : tiles) {
            // Get the column index via the tile's originLon.
            int col = lonList.indexOf(tile.originLon);
            int row = latList.indexOf(tile.originLat);
            if (col == -1 || row == -1) {
                System.out.println("Tile coordinate not found in index list: " + tile.originLat + ", " + tile.originLon);
                continue; // Skip tile
            }

            
            // Compute the pixel offset.
            int offsetX = col * tileWidth;
            int offsetY = row * tileHeight;

            // === 1. Check for valid data in the tile ===
            boolean hasValid = false;
            for (int val : tile.data) {
                if (val != -32767 && val != -9999 && val != 0) {
                    hasValid = true;
                break;
            }
        }

        if (!hasValid) {
            System.out.println("Skipping tile: " + tile.fileName + " (all values invalid)");
            continue;  // Skip tile entirely
        }

        // === 2. Tile has valid data, copy into raster ===
        for (int y = 0; y < tile.height; y++) {
            for (int x = 0; x < tile.width; x++) {
                int value = tile.data[y * tile.width + x];

                if (value == -32767 || value == -9999) {
                    value = 0;  
                }

                raster.setSample(offsetX + x, offsetY + y, 0, value);

                if (value < globalMin) globalMin = value;
                if (value > globalMax) globalMax = value;
                }
            }
        }

        if (globalMin == globalMax) {
            globalMax = globalMin + 1;
        }
        
        // Create the envelope based on the grid.
        // The top left coordinate is the origin of the top-left tile.
        double envelopeMinLon = lonList.get(0);  // first (smallest) longitude in ascending order
        double envelopeMaxLat = latList.get(0);    // first (largest) latitude in descending order
        
        // Since each tile is assumed to cover tileWidth * 0.001 and tileHeight * 0.001 degrees:
        double envelopeMaxLon = envelopeMinLon + cols * tileWidth * 0.001;
        double envelopeMinLat = envelopeMaxLat - rows * tileHeight * 0.001;
        
        ReferencedEnvelope envelope = new ReferencedEnvelope(
            envelopeMinLon, envelopeMaxLon, envelopeMinLat, envelopeMaxLat, DefaultGeographicCRS.WGS84
        );
        
        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D coverage = factory.create("Elevation", raster, envelope);
        this.coverage = coverage;
        MapContent map = new MapContent();
        map.setTitle("Elevation Heatmap");
        map.addLayer(new GridCoverageLayer(coverage, giveStyle(0, 9000)));
        
      
        //this.addMouseInteraction();
        this.setMapContent(map);
        this.setCursorTool(new ScrollWheelTool(this));
        this.setCursorTool(new PanTool());
}


    public ElevationMap(Tile tile, double latitude, double longitude, double heightRadar){
        this.lat = latitude;
        this.lon = longitude;
        if (tile == null) {
            System.err.println("Failed to load tile for lat=" + lat + ", lon=" + lon);
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int val : tile.data) {
            if (val < min) min = val;
            if (val > max) max = val;
        }
        if (min == max) max = min + 1;
        System.out.println("Elevation min: " + min + ", max: " + max);

        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, tile.width, tile.height, 1, null);

        // Use raw elevation values here (float version)
        for (int y = 0; y < tile.height; y++) {
            for (int x = 0; x < tile.width; x++) {
                float value = tile.data[y * tile.width + x];
                raster.setSample(x, y, 0, value);
            }
        }

        ReferencedEnvelope envelope = new ReferencedEnvelope(
                tile.originLon,
                tile.originLon + tile.width * 0.001,
                tile.originLat - tile.height * 0.001,
                tile.originLat,
                DefaultGeographicCRS.WGS84
        );
        envelope.setBounds(envelope);
        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D coverage = factory.create("Elevation", raster, envelope);

        MapContent map = new MapContent();
        map.setTitle("Elevation Heatmap");

        map.addLayer(new GridCoverageLayer(coverage, giveStyle(min, max)));
        
        //this.addMouseInteraction();
        this.setMapContent(map);
        this.setCursorTool(new ScrollWheelTool(this));
        this.setCursorTool(new PanTool());
        
    }

    

    public static void main(String[] args) {
        

        // Example coordinates and height
        double lat = 10;   // example latitude
        double lon = 75;   // example longitude

        TileManager manager = new TileManager();
        ArrayList<Tile> requiredTile = manager.getSurroundingTiles(lat, lon);
        JMapPane mapPane = new ElevationMap(requiredTile);
       

        JFrame frame = new JFrame("Radar LOS ElevationMap Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(mapPane);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
