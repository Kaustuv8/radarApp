package UIElement;
import classes.TileManager;
import classes.tileClass.Tile;
import java.util.concurrent.*;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.style.ExternalGraphic;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.styling.ColorMapEntryImpl;
import org.geotools.styling.ColorMapImpl;
import org.geotools.styling.StyleBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import RadarLossFunction.Losfn;

import javax.media.jai.RasterFactory;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.awt.Color;





public class coverageLayerProvider {



    
        

    public FeatureLayer addRadarMarker(GridCoverage2D coverage, double radarLat, double radarLon) {
    try {
        // Step 1: Get the map CRS from the coverage
        CoordinateReferenceSystem mapCRS = coverage.getCoordinateReferenceSystem2D();

        // Step 2: Create a point in WGS84
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        Coordinate coord = new Coordinate(radarLon, radarLat);
        Point point = geometryFactory.createPoint(coord);

        // Step 3: Transform from WGS84 to map CRS (same as coverage)
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
        if (!CRS.equalsIgnoreMetadata(mapCRS, wgs84)) {
            MathTransform transform = CRS.findMathTransform(wgs84, mapCRS, true);
            point = (Point) JTS.transform(point, transform);  // This gives point in map CRS
        }

        // Step 4: Define feature type using map CRS
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("RadarMarker");
        typeBuilder.setCRS(mapCRS);  // Same as radar coverage layer
        typeBuilder.add("location", Point.class);
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        // Step 5: Build feature
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.add(point);
        SimpleFeature feature = featureBuilder.buildFeature(null);
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        featureCollection.add(feature);

        // Step 6: Style the marker (e.g., red circle)
        StyleBuilder sb = new StyleBuilder();
        File iconFile = new File("data/radarIcon/marker.png");
        URL iconUrl = iconFile.toURI().toURL();

        ExternalGraphic icon = sb.createExternalGraphic(iconUrl.toExternalForm(), "image/png");

        
        
        Graphic graphic = sb.createGraphic(icon, null, null);
        graphic.setSize(sb.literalExpression(24)); // size in pixels
        PointSymbolizer symbolizer = sb.createPointSymbolizer(graphic);
        Style style = sb.createStyle(symbolizer);
        

        // Step 7: Create and return the feature layer
        FeatureLayer layer = new FeatureLayer(featureCollection, style);
        layer.setTitle("Radar Marker");
        return layer;

    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}

    public GridCoverageLayer giveCoverageLayer(
        WritableRaster elevationRaster,
        ReferencedEnvelope envelope,
        GridCoverage2D coverage,
        int totalHeight,
        int totalWidth,
        double radarLat,
        double radarLong,
        double radarHeight,
        double radius,
        double[] bandHeights,
        boolean curvatureCheck,
        double beamAngleMinDeg,
        double beamAngleMaxDeg,
        double azimuthMinDeg,
        double azimuthMaxDeg
    ) {
            try {
                GridGeometry2D gridGeometry = coverage.getGridGeometry();
                MathTransform gridToCRS = gridGeometry.getGridToCRS();
                MathTransform crsToGrid = gridToCRS.inverse();

                double[] gridCoordDouble = new double[2];
                crsToGrid.transform(new double[]{radarLong, radarLat}, 0, gridCoordDouble, 0, 1);

                int centerX = (int) Math.round(gridCoordDouble[0]);
                int centerY = (int) Math.round(gridCoordDouble[1]);
                double centerElevation = elevationRaster.getSample(centerX, centerY, 0);
                final double radarHeight_f = radarHeight + centerElevation;

                double deltaX = (envelope.getMaxX() - envelope.getMinX()) / (totalWidth - 1);
                double deltaY = (envelope.getMaxY() - envelope.getMinY()) / (totalHeight - 1);

                byte[] radarData = new byte[totalWidth * totalHeight];

                // --- Multithreading begins here ---
                int numThreads = Runtime.getRuntime().availableProcessors();
                ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                int rowsPerThread = totalHeight / numThreads;

                for (int i = 0; i < numThreads; i++) {
                    final int startY = i * rowsPerThread;
                    final int endY = (i == numThreads - 1) ? totalHeight : (i + 1) * rowsPerThread;

                    executor.submit(() -> {
                        for (int y = startY; y < endY; y++) {
                            for (int x = 0; x < totalWidth; x++) {
                                int dx = x - centerX;
                                int dy = y - centerY;

                                if (dx * dx + dy * dy <= radius * radius) {
                                    double targetLat = envelope.getMaxY() - y * deltaY;
                                    double targetLon = envelope.getMinX() + x * deltaX;

                                    int visibleBand = 0;
                                    for (int j = 0; j < bandHeights.length; j++) {
                                        boolean blocked = Losfn.isLOSBlocked(
                                            radarLat, radarLong, radarHeight_f,
                                            targetLat, targetLon, bandHeights[j],
                                            elevationRaster,
                                            envelope,
                                            curvatureCheck,
                                            beamAngleMinDeg,
                                            beamAngleMaxDeg,
                                            azimuthMinDeg,
                                            azimuthMaxDeg
                                        );
                                        if (!blocked) {
                                            visibleBand = j + 1;
                                            break;
                                        }
                                    }
                                    radarData[y * totalWidth + x] = (byte) visibleBand;
                                } else {
                                    radarData[y * totalWidth + x] = 0;
                                }
                            }
                        }
                    });
                }

                executor.shutdown();
                executor.awaitTermination(2, TimeUnit.MINUTES);  // wait for all threads

                // Create raster from byte data
                WritableRaster radarRaster = RasterFactory.createBandedRaster(
                    DataBuffer.TYPE_BYTE, totalWidth, totalHeight, 1, null
                );
                for (int y = 0; y < totalHeight; y++) {
                    for (int x = 0; x < totalWidth; x++) {
                        radarRaster.setSample(x, y, 0, radarData[y * totalWidth + x]);
                    }
                }

                GridCoverageFactory factory = new GridCoverageFactory();
                GridCoverage2D radarCoverage = factory.create("RadarCoverage", radarRaster, envelope);

                // --- Style section remains unchanged ---
                StyleBuilder sb = new StyleBuilder();
                RasterSymbolizer radarSym = sb.createRasterSymbolizer();
                ColorMapImpl colorMap = new ColorMapImpl();

                colorMap.addColorMapEntry(makeColorMapEntry(sb, 0.0, Color.BLACK, 0.0f, "No Coverage"));
                colorMap.addColorMapEntry(makeColorMapEntry(sb, 1.0, new Color(0, 255, 0), 0.6f, "Low Altitude"));
                colorMap.addColorMapEntry(makeColorMapEntry(sb, 2.0, Color.YELLOW, 0.6f, "Mid Altitude"));
                colorMap.addColorMapEntry(makeColorMapEntry(sb, 3.0, Color.ORANGE, 0.6f, "Medium High Altitude"));
                colorMap.addColorMapEntry(makeColorMapEntry(sb, 4.0, Color.RED, 0.6f, "High Altitude"));

                colorMap.setType(ColorMap.TYPE_VALUES);
                radarSym.setColorMap(colorMap);
                Style radarStyle = sb.createStyle(radarSym);

                return new GridCoverageLayer(radarCoverage, radarStyle);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private ColorMapEntryImpl makeColorMapEntry(StyleBuilder sb, double quantity, Color color, float opacity, String label) {
            ColorMapEntryImpl entry = new ColorMapEntryImpl();
            entry.setQuantity(sb.literalExpression(quantity));
            entry.setColor(sb.literalExpression(color));
            entry.setOpacity(sb.literalExpression(opacity));
            entry.setLabel(label);
            return entry;
        }
}

   
