package RadarLossFunction;

import java.awt.image.Raster;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class Losfn {

    public static boolean isLOSBlocked(
        double lat1, double lon1, double height1,
        double lat2, double lon2, double height2,
        Raster elevationRaster,
        ReferencedEnvelope envelope
    ) {
        int samplePoints = 500;
        int hopSize = 50;

        int width = elevationRaster.getWidth();
        int height = elevationRaster.getHeight();

        double envelopeMinLat = envelope.getMinY();  // min latitude (bottom)
        double envelopeMaxLat = envelope.getMaxY();  // max latitude (top)
        double envelopeMinLon = envelope.getMinX();  // min longitude (left)
        double envelopeMaxLon = envelope.getMaxX();  // max longitude (right)

        double pixelSizeLon = (envelopeMaxLon - envelopeMinLon) / width;
        double pixelSizeLat = (envelopeMaxLat - envelopeMinLat) / height;

        for (int i = 1; i < samplePoints; i += hopSize) {
            double fraction = i / (double) samplePoints;

            double lat = lat1 + fraction * (lat2 - lat1);
            double lon = lon1 + fraction * (lon2 - lon1);
            double lineHeight = height1 + fraction * (height2 - height1);

            // Convert lat/lon to raster pixel coordinates
            int x = (int) ((lon - envelopeMinLon) / pixelSizeLon);
            int y = (int) ((envelopeMaxLat - lat) / pixelSizeLat); // Y is top-down

            if (x < 0 || x >= width || y < 0 || y >= height) {
                continue; // outside bounds
            }

            int terrainElevation = elevationRaster.getSample(x, y, 0);

            if (terrainElevation > lineHeight) {
                return true; // LOS is blocked
            }
        }

        return false; // No obstruction
    }
}
