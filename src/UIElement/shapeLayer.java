package UIElement;


import org.geotools.api.style.Style;

import org.geotools.map.MapContent;

import java.awt.geom.Point2D;

import org.geotools.map.FeatureLayer;

import org.geotools.map.Layer;
import org.geotools.swing.JMapPane;
import org.geotools.swing.event.MapMouseAdapter;
import org.geotools.swing.event.MapMouseEvent;



import java.io.File;

import java.util.HashMap;
import java.util.Map;

import org.geotools.styling.SLD;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import javax.swing.JFrame;

public class shapeLayer extends JMapPane{
    
    File shpFile = new File("data/borderShape/India_State_Boundary.shp");
    
    public double lat;
    public double lon;

    public void setValues(double a, double b){
        this.lat = a;
        this.lon = b;
    }

    public double[] getValues(){
        double[] ans = new double[2];
        ans[0] = lat;
        ans[1] = lon;
        return ans;
    }

   public void addMouseInteraction(){
    this.addMouseListener( new MapMouseAdapter(){
            @Override
            public void onMouseClicked(MapMouseEvent ev) {
                try {
                    CoordinateReferenceSystem worldCRS = mapContent.getCoordinateReferenceSystem();

                    // Fallback to first layer's CRS if mapContent CRS is null
                    if (worldCRS == null && !mapContent.layers().isEmpty()) {
                        Layer firstLayer = mapContent.layers().get(0);
                        worldCRS = firstLayer.getFeatureSource().getSchema().getCoordinateReferenceSystem();
                    }

                    if (worldCRS == null) {
                        throw new IllegalStateException("Could not determine the map CRS (worldCRS is null)");
                    }



                    // Define the WGS84 CRS (lat/lon)
                    CoordinateReferenceSystem latLonCRS = DefaultGeographicCRS.WGS84;

                    // Create the transformation from map CRS to lat/lon
                    MathTransform transform = CRS.findMathTransform(worldCRS, latLonCRS, true);

                    // Get world coordinates (usually in map units like meters)
                    Point2D point = ev.getWorldPos();  // x and y are in worldCRS

                    // Transform coordinates using double[] arrays
                    double[] srcCoords = new double[] { point.getX(), point.getY() };
                    double[] dstCoords = new double[2];

                    transform.transform(srcCoords, 0, dstCoords, 0, 1);

                    // Print Latitude and Longitude
                    System.out.printf("Latitude: %.6f, Longitude: %.6f%n", dstCoords[1], dstCoords[0]);
                    setValues(dstCoords[1], dstCoords[0]);

                } catch (Exception ex) {
                    ex.printStackTrace(); // handle appropriately
                }
            }
            @Override
            public void onMouseEntered(MapMouseEvent ev) {
                System.out.println("mouse entered map pane");
            }

            @Override
            public void onMouseExited(MapMouseEvent ev) {
                System.out.println("mouse left map pane");
            }
        }
        
        );
   }

    

    

    

    public shapeLayer() {
        super();
        try{
           

            // Create parameters map for DataStoreFinder
            Map<String, Object> params = new HashMap<>();
            params.put("url", shpFile.toURI().toURL());

            // Create DataStore
            DataStore dataStore = DataStoreFinder.getDataStore(params);
            if (dataStore == null) {
                System.err.println("Could not connect to data store");
                System.exit(1);
            }

            // Get the first feature type name (layer)
            String typeName = dataStore.getTypeNames()[0];

            // Get feature source
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

            // Create style for the features
            Style style = SLD.createSimpleStyle(featureSource.getSchema());

            // Create a map content and add our shapefile layer
            MapContent map = new MapContent();
            map.setTitle("Simple Shapefile Loader");
            Layer layer = new FeatureLayer(featureSource, style);
            map.addLayer(layer);
            this.setMapContent(map);
            this.addMouseInteraction();
            // Here you could add your code to display map, e.g. using JMapFrame or your GUI
            // For example, to quickly test displaying in a window:
            // org.geotools.swing.JMapFrame.showMap(map);

            System.out.println("Shapefile loaded successfully!");
        } catch(Exception e){
            System.err.println(e);
        }
        
        

    }


    

    public static void main(String[] args) {
        

        
        
        JMapPane mapPane = new shapeLayer();
       

        JFrame frame = new JFrame("Radar LOS ElevationMap Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(mapPane);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
