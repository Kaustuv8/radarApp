package helperFunctions;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
 import org.geotools.geometry.jts.ReferencedEnvelope;
import java.awt.image.WritableRaster;
import java.util.*;
public class rasterPolygon {
    
    public List<Polygon> rasterMaskToPolygons(WritableRaster raster, ReferencedEnvelope envelope) {
        GeometryFactory gf = new GeometryFactory();
        List<Polygon> pixelPolygons = new ArrayList<>();
    
        int width = raster.getWidth();
        int height = raster.getHeight();
    
        double cellWidth = envelope.getWidth() / width;
        double cellHeight = envelope.getHeight() / height;
    
        double originX = envelope.getMinX();
        double originY = envelope.getMinY();
    
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = raster.getSampleDouble(x, y, 0);
                if (value == 1.0) {
                    double minX = originX + x * cellWidth;
                    double maxX = minX + cellWidth;
                    double minY = originY + (height - y - 1) * cellHeight;
                    double maxY = minY + cellHeight;
    
                    Coordinate[] square = new Coordinate[]{
                        new Coordinate(minX, minY),
                        new Coordinate(minX, maxY),
                        new Coordinate(maxX, maxY),
                        new Coordinate(maxX, minY),
                        new Coordinate(minX, minY)
                    };
    
                    Polygon cell = gf.createPolygon(square);
                    pixelPolygons.add(cell);
                }
            }
        }
    
        if (pixelPolygons.isEmpty()) return List.of();
    
        Geometry union = CascadedPolygonUnion.union(pixelPolygons);
        if (union instanceof Polygon poly) return List.of(poly);
        else if (union instanceof MultiPolygon mp) {
            List<Polygon> result = new ArrayList<>();
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                result.add((Polygon) mp.getGeometryN(i));
            }
            return result;
        }
    
        return List.of(); // fallback
    }
   

}
