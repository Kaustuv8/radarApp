package helperFunctions;
import java.awt.image.WritableRaster;
import java.util.*;
import org.apache.sis.geometry.DirectPosition2D;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.locationtech.jts.geom.*;

public class RadarBoundaryExtractor {

    public Polygon extractGreenRegionBoundaryWKT(
        WritableRaster radarCoverage,
        CoordinateReferenceSystem crs,
        AffineTransform2D gridToCRS
    ) throws Exception {

        int width = radarCoverage.getWidth();
        int height = radarCoverage.getHeight();

        GeometryFactory geometryFactory = new GeometryFactory();
        List<Coordinate> borderPoints = new ArrayList<>();

        // March around edges where green (1) pixels meet non-green
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int val = radarCoverage.getSample(x, y, 0);
                if (val == 1) {
                    // Check 4-neighbors for border
                    boolean isBorder = false;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            if (radarCoverage.getSample(x + dx, y + dy, 0) != 1) {
                                isBorder = true;
                            }
                        }
                    }
                    if (isBorder) {
                        borderPoints.add(new Coordinate(x, y));
                    }
                }
            }
        }

        if (borderPoints.isEmpty()) {
            System.out.println("⚠️⚠️⚠️⚠️⚠️⚠️⚠️ Polygon Empty ⚠️⚠️⚠️⚠️⚠️⚠️⚠️");
            return null;
        }

        // Optional: sort points using convex hull (approximate shape)
        Coordinate[] coordsArray = borderPoints.toArray(new Coordinate[0]);
        Geometry borderGeom = geometryFactory.createMultiPointFromCoords(coordsArray).convexHull();

        // Convert to geographic coordinates
        Coordinate[] geoCoords = Arrays.stream(borderGeom.getCoordinates())
            .map(pixelCoord -> {
                try {
                    DirectPosition2D gridPt = new DirectPosition2D(pixelCoord.x, pixelCoord.y);
                    DirectPosition2D geoPt = new DirectPosition2D();
                    gridToCRS.transform(gridPt, geoPt);
                    return new Coordinate(geoPt.x, geoPt.y);  // lon, lat order
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toArray(Coordinate[]::new);

        // Close the polygon
        if (!geoCoords[0].equals2D(geoCoords[geoCoords.length - 1])) {
            geoCoords = Arrays.copyOf(geoCoords, geoCoords.length + 1);
            geoCoords[geoCoords.length - 1] = geoCoords[0];
        }

        Polygon polygon = geometryFactory.createPolygon(geoCoords);
        return polygon; // Return WKT
    }
}
