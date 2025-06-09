package UIElement;
import classes.TileManager;
import classes.tileClass.Tile;

import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.styling.ColorMapEntryImpl;
import org.geotools.styling.ColorMapImpl;
import org.geotools.styling.StyleBuilder;

import RadarLossFunction.Losfn;

import javax.media.jai.RasterFactory;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.awt.Color;

public class coverageLayerProvider {

    public GridCoverageLayer giveCoverageLayer(
    WritableRaster elevationRaster,
    ReferencedEnvelope envelope,
    GridCoverage2D coverage,
    int totalHeight,
    int totalWidth,
    double radarLat,
    double radarLong,
    double radarHeight,
    double radius
) {
        try{
            GridGeometry2D gridGeometry = coverage.getGridGeometry();

            // Get the transform from grid to world coordinates
            MathTransform gridToCRS = gridGeometry.getGridToCRS();

            // Invert it to get world to grid transform
            MathTransform crsToGrid = gridToCRS.inverse();

            // Transform world position to grid position (double coords)
            double[] gridCoordDouble = new double[2];
            crsToGrid.transform(new double[] { radarLong, radarLat }, 0, gridCoordDouble, 0, 1);

            // Round to int grid coordinates
            int centerX = (int) Math.round(gridCoordDouble[0]);
            int centerY = (int) Math.round(gridCoordDouble[1]);
            //double centerElevation = elevationRaster.getSample(centerX, centerY, 0);
            //radarHeight+=centerElevation;
            double deltaX = (envelope.getMaxX() - envelope.getMinX()) / (totalWidth - 1);
            double deltaY = (envelope.getMaxY() - envelope.getMinY()) / (totalHeight - 1);
            float[] radarData = new float[totalWidth * totalHeight];
            
            for (int y = 0; y < totalHeight; y++) {
                for (int x = 0; x < totalWidth; x++) {
                    int dx = x - centerX;
                    int dy = y - centerY;

                    // Only within circle
                    if (dx * dx + dy * dy <= radius * radius) {
                        double targetLat = envelope.getMaxY() - y * deltaY;
                        double targetLon = envelope.getMinX() + x * deltaX;
                        int targetHeight = elevationRaster.getSample(x, y, 0);

                        boolean blocked = Losfn.isLOSBlocked(
                            radarLat, radarLong, radarHeight,
                            targetLat, targetLon, targetHeight,
                            elevationRaster, envelope
                        );

                        radarData[y * totalWidth + x] = blocked ? 0.5f : 1f; // 0.5 = blocked, 1.0 = visible
                    } else {
                        radarData[y * totalWidth + x] = 0f; // outside range
                    }
                }
            }

            // Create the radar raster
            WritableRaster radarRaster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_FLOAT, totalWidth, totalHeight, 1, null
            );
            for (int y = 0; y < totalHeight; y++) {
                for (int x = 0; x < totalWidth; x++) {
                    radarRaster.setSample(x, y, 0, radarData[y * totalWidth + x]);
                }
            }

            // Create the coverage
            GridCoverageFactory factory = new GridCoverageFactory();
            GridCoverage2D radarCoverage = factory.create("RadarCoverage", radarRaster, envelope);

            // Define the style
            StyleBuilder sb = new StyleBuilder();
            RasterSymbolizer radarSym = sb.createRasterSymbolizer();

            ColorMapEntry noCoverage = new ColorMapEntryImpl();
            noCoverage.setColor(sb.literalExpression(Color.BLACK));
            noCoverage.setQuantity(sb.literalExpression(0.0));
            noCoverage.setOpacity(sb.literalExpression(0.0f));
            noCoverage.setLabel("No Coverage");

            ColorMapEntry losBlocked = new ColorMapEntryImpl();
            losBlocked.setColor(sb.literalExpression(Color.RED)); // Red
            losBlocked.setQuantity(sb.literalExpression(0.5));
            losBlocked.setOpacity(sb.literalExpression(0.2f));
            losBlocked.setLabel("LOS Blocked");

            ColorMapEntry losVisible = new ColorMapEntryImpl();
            losVisible.setColor(sb.literalExpression(new Color(0, 56, 2))); // Green
            losVisible.setQuantity(sb.literalExpression(1.0));
            losVisible.setOpacity(sb.literalExpression(0.6f));
            losVisible.setLabel("LOS Clear");

            ColorMapImpl radarColorMap = new ColorMapImpl();
            radarColorMap.addColorMapEntry(noCoverage);
            radarColorMap.addColorMapEntry(losBlocked);
            radarColorMap.addColorMapEntry(losVisible);
            
            radarColorMap.setType(ColorMap.TYPE_VALUES);

            radarSym.setColorMap(radarColorMap);
            Style radarStyle = sb.createStyle(radarSym);
            System.out.println("");
            return new GridCoverageLayer(radarCoverage, radarStyle);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        coverageLayerProvider clp = new coverageLayerProvider();
        TileManager tM = new TileManager();
        ArrayList<Tile> reqTile = tM.getSurroundingTiles(10, 75);
        ElevationMap panel = new ElevationMap(reqTile);

        // Step 1: Get the MapContent
        MapContent content = panel.getMapContent();
        if (content == null) {
            throw new RuntimeException("MapContent is null");
        }

        // Step 2: Get the first layer (assuming it's the elevation layer)
        Layer layer = content.layers().get(0); // or iterate over all layers to find GridCoverageLayer
        if (!(layer instanceof GridCoverageLayer)) {
            throw new RuntimeException("Layer is not a GridCoverageLayer");
        }
        GridCoverageLayer gcLayer = (GridCoverageLayer) layer;

        // Step 3: Extract the GridCoverage2D
        GridCoverage2D coverage = (GridCoverage2D) gcLayer.getCoverage();

        // Step 4: Get the raster (elevation values)
        WritableRaster elevationRaster = (WritableRaster) coverage.getRenderedImage().getData();

        // Step 5: Get the geographic envelope
        ReferencedEnvelope envelope = new ReferencedEnvelope(coverage.getEnvelope2D());

        // Step 6: Get dimensions
        int width = elevationRaster.getWidth();
        int height = elevationRaster.getHeight();

        // Example output
        System.out.println("Width: " + width);
        System.out.println("Height: " + height);
        System.out.println("Envelope: " + envelope);
        clp.giveCoverageLayer(
            elevationRaster,
            envelope,
            coverage,
            height,
            width,
            (double)10,
            (double)75,
            (double)100,
            (double)1000
        );
    }
}
