package helperFunctions;
import java.awt.image.WritableRaster;
import org.geotools.process.raster.ContourProcess;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;

import org.locationtech.jts.geom.Polygon;
import java.util.*;
public class PolygonList{


    public List<Polygon> extractDetailedGreenContours(WritableRaster raster, ReferencedEnvelope envelope) {
        //for (int y = 0; y < raster.getHeight(); y++) {
        //    for (int x = 0; x < raster.getWidth(); x++) {
        //        double val = raster.getSampleDouble(x, y, 0);
        //        System.out.print(val == 1.0 ? "1 " : "· ");
        //    }
        //    System.out.println();
        //}

        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D coverage = factory.create("radar", raster, envelope);
       

        

        
        ContourProcess contour = new ContourProcess();
        double[] levels = new double[]{1.0};

        SimpleFeatureCollection contourFeatures = contour.execute(
            coverage,
            0,            // band index (usually 0)
            levels,       // exact contour values
            null,         // ignore interval since levels provided
            Boolean.FALSE, // don't simplify
            Boolean.FALSE, // don't smooth
            null,         // no region of interest (whole coverage)
            null          // no ProgressListener
        );
        System.out.println("Contour feature count: " + contourFeatures.size());
        try (SimpleFeatureIterator it = contourFeatures.features()) {
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                System.out.println("Geometry type: " + feature.getDefaultGeometry().getClass().getSimpleName());
            }
        }
        // Collect polygons
        List<Polygon> polygons = new ArrayList<>();

        try (SimpleFeatureIterator it = contourFeatures.features()) {
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                Object geom = feature.getDefaultGeometry();

                if (geom instanceof org.locationtech.jts.geom.LineString line) {
                    if (line.isClosed()) {
                        Polygon poly = line.getFactory().createPolygon(line.getCoordinates());
                        polygons.add(poly);
                    } else {
                        System.out.println("Skipping open contour: not a closed ring.");
                    }
                }
            }
        }

        return polygons;
    }

}