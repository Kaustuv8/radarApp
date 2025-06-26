package helperFunctions;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.io.FileWriter;
import java.io.IOException;

public class PolygonFormatExporter {

    public void exportPolygonAsCustomText(Polygon polygon, String outputPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Polygon(");

        Coordinate[] coords = polygon.getCoordinates();
        for (int i = 0; i < coords.length; i++) {
            Coordinate coord = coords[i];
            sb.append(coord.y).append(" ").append(coord.x);  // Lat Lon
            if (i < coords.length - 1) sb.append(", ");
        }

        sb.append(")");

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(sb.toString());
        }
    }
}
