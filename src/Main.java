import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

import classes.TileManager;
import classes.elevationMapStyle;
import UIElement.ElevationLegendPanel;
import UIElement.RadarLegendPanel;

import org.geotools.swing.event.MapMouseAdapter;
import org.geotools.swing.event.MapMouseEvent;

import classes.tileClass.Tile;
import UIElement.ElevationMap;
import UIElement.coverageLayerProvider;
import UIElement.shapeLayer;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.apache.sis.geometry.DirectPosition2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;

import org.geotools.swing.JMapPane;

import MouseTools.CustomPanTool;
import MouseTools.CustomScrollWheelTool;

public class Main extends JFrame implements ActionListener{
    
    private JTextField latField;
    private JTextField lonField;
    private JTextField heightField;
    private JLabel radiusLabel;
    private JButton b1;
    private JTextField radiusField;
    private JLabel reportLabel;
    private JToolBar toolBar;
    private Container c;
    private ElevationMap map;
    private coverageLayerProvider covLayer = new coverageLayerProvider();
    private boolean radarPressed = false;
    private boolean zoomPressed = false;
    private boolean panPressed = false;
    private double selLat = 91;// Selected Latitude : (-90 <= currLate <= +90)
    private double selLon = 181;// Selected Longitude : (-180 <= currLon <= +180)
    private TileManager manager = new TileManager();
    private elevationMapStyle styleDetailProvider = new elevationMapStyle();
    private Image cursorImage = Toolkit.getDefaultToolkit().getImage("data/cursorImage/Radar.jpg");
    private Point hotspot = new Point(0, 0);  // top-left corner of the image
    private Cursor radarCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, hotspot, "MyCursor");
    private Cursor currentCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private boolean showRadarPanel = false;
    private JLabel heightLabel = new JLabel("Height");
    private shapeLayer shapeMap = new shapeLayer();
    private JLabel elevationLabel = new JLabel("Elevation : null");

    public GridCoverageLayer addCoverageToMap(
        ElevationMap panel, 
        double radarLat,
        double radarLon,
        double radarHeight,
        double radarRadius
    ){
    
        
        
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
        System.out.println("Map CRS: " + map.getMapContent().getCoordinateReferenceSystem());
        System.out.println("Coverage CRS: " + coverage.getCoordinateReferenceSystem2D());
        return covLayer.giveCoverageLayer(
            
            elevationRaster,
            envelope,
            coverage,
            height,
            width,
            radarLat,
            radarLon,
            radarHeight,
            radarRadius
        );
        

        
    }
    
    public void toggleRadarUI(){
        heightLabel.setVisible(showRadarPanel);
        heightField.setVisible(showRadarPanel);
        radiusField.setVisible(showRadarPanel);
        radiusLabel.setVisible(showRadarPanel);
        elevationLabel.setVisible(showRadarPanel);
    }
    
    public void removeLegendsMapsAndToolBarUI(){
        for(Component comp : c.getComponents()){
            if (comp instanceof JMapPane
                    || comp instanceof ElevationLegendPanel
                    || comp instanceof RadarLegendPanel
                    || comp instanceof JToolBar) {
                        c.remove(comp);
                }
            }
    }
    

    
    private JToolBar setToolBar(JMapPane map){
        toolBar = new JToolBar();
        toolBar.setOrientation(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        ButtonGroup cursorToolGrp = new ButtonGroup();

        JButton zoomInBtn = new JButton("Zoom");
        zoomInBtn.addActionListener(e -> {
            zoomPressed = !zoomPressed;
            panPressed = false;
            radarPressed = false;
            currentCursor = Cursor.getPredefinedCursor(zoomPressed ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR );
            setCursor(currentCursor);
            map.setCursorTool(new CustomScrollWheelTool(map, currentCursor));
        });
        toolBar.add(zoomInBtn);
        cursorToolGrp.add(zoomInBtn);

        JButton panButton = new JButton("Pan");
        panButton.addActionListener(e -> {
            panPressed = !panPressed;
            zoomPressed = false;
            radarPressed = false;
            currentCursor = Cursor.getPredefinedCursor(panPressed ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR );
            setCursor(currentCursor);
            map.setCursorTool(new CustomPanTool(map, currentCursor));
        });
        toolBar.add(panButton);
        cursorToolGrp.add(panButton);

        JButton radarButton = new JButton(radarPressed ? "Exit Radar" : "Place Radar");
        radarButton.addActionListener(e -> {
            radarPressed = !radarPressed;
            zoomPressed = false;
            panPressed = false;
            currentCursor = radarPressed ? radarCursor :Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
            setCursor(currentCursor);
            radarButton.setText(radarPressed ? "Exit Radar" : "Place Radar");
            map.setCursorTool(new CustomPanTool(map, currentCursor));
            //map.setCursorTool(currentCursor);
        });
        toolBar.add(radarButton);
        cursorToolGrp.add(radarButton);
        JButton clearRadarButton = new JButton("Clear Radar");
        clearRadarButton.addActionListener(e-> {
            MapContent mapCon = map.getMapContent();
            int idx=0;
            for(Layer l : mapCon.layers()){
                if(idx==0){
                    idx++;
                    continue;
                }
                if(l instanceof GridCoverageLayer){
                    mapCon.removeLayer(l);
                }
            }
        });
        toolBar.add(clearRadarButton);
        JButton mapToggle = new JButton("Return");
        mapToggle.addActionListener(e -> {
            c.remove(map);
            map.getMapContent().dispose();
            showRadarPanel = false;
            toggleRadarUI();
            removeLegendsMapsAndToolBarUI();
            setCursor(Cursor.getDefaultCursor());
            c.add(shapeMap, BorderLayout.CENTER);
            setShapeMapMouseAction();
        });
        toolBar.add(mapToggle);
        cursorToolGrp.add(clearRadarButton);
        return toolBar;
    }

    Main(){
        c = this.getContentPane();
        c.setLayout(new BorderLayout());
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,10, 25));
        
        JLabel latLabel = new JLabel("Latitude");
        latLabel.setAlignmentX(CENTER_ALIGNMENT);
        JLabel lonLabel = new JLabel("Longitude");
        lonLabel.setAlignmentX(CENTER_ALIGNMENT);
        heightLabel.setAlignmentX(CENTER_ALIGNMENT);
        latField = new JTextField(10);
        
        lonField = new JTextField(10);
       
        heightField = new JTextField(10);
        radiusLabel = new  JLabel("Enter Radius");
        radiusLabel.setAlignmentX(CENTER_ALIGNMENT);
        radiusField = new JTextField(10);
        radiusField.setAlignmentX(CENTER_ALIGNMENT);
        reportLabel = new JLabel();
        b1 = new JButton("Show Map");
        b1.addActionListener(this);
        inputPanel.add(latLabel);
        inputPanel.add(latField);
        
        inputPanel.add(lonLabel);
        inputPanel.add(lonField);
     
        inputPanel.add(heightLabel);
        inputPanel.add(heightField);
        
        inputPanel.add(radiusLabel);
        inputPanel.add(radiusField);
        inputPanel.add(b1);
        inputPanel.add(reportLabel);
        inputPanel.setVisible(true);
        inputPanel.setPreferredSize(new Dimension(150,300));
        inputPanel.add(elevationLabel);
        toggleRadarUI();
        setShapeMapMouseAction();
        c.add(shapeMap, BorderLayout.CENTER);
        

        c.add(inputPanel, BorderLayout.WEST);
        
    }


    public void setShapeMapMouseAction(){
        shapeMap.addMouseListener(new MapMouseAdapter() {
                @Override 
                public void onMouseClicked(MapMouseEvent e){
                    double[] data = shapeMap.getValues();
                    selLat = Math.floor(data[0]);
                    selLon = Math.floor(data[1]);
                    latField.setText(Double.toString(selLat));
                    lonField.setText(Double.toString(selLon));
                    selLat = Math.floor(selLat);
                    selLon = Math.floor(selLon);

                }

            }
        );
    }

    public void actionPerformed(ActionEvent e){
        if(e.getSource() == b1){
            reportLabel.setText("");
            c.remove(shapeMap);
            // Show loading screen
            JRootPane root = SwingUtilities.getRootPane(c);
            JPanel loadingPanel = new JPanel(new BorderLayout());
            loadingPanel.setBackground(new Color(0, 0, 0, 150));
            JLabel loadingLabel = new JLabel("Loading map...", SwingConstants.CENTER);
            loadingLabel.setForeground(Color.WHITE);
            loadingPanel.add(loadingLabel, BorderLayout.CENTER);
            root.setGlassPane(loadingPanel);
            loadingPanel.setVisible(true);
            
            new SwingWorker<ElevationMap, Void>() {
                ArrayList<Tile> tiles;
                double lat, lon;

                @Override
                protected ElevationMap doInBackground() throws Exception {
                        lat = Double.parseDouble(latField.getText());
                        lon = Double.parseDouble(lonField.getText());
                        tiles = manager.getSurroundingTiles(lat, lon);
                        return new ElevationMap(tiles);
                    }

                @Override
                protected void done() {
                    try {
                        //map.getMapContent().dispose();
                        map = get(); // Get the ElevationMap built in background
                        showRadarPanel = true;
                        toggleRadarUI();
                        if (map != null) {
                            // Remove old map components
                            for (Component comp : c.getComponents()) {
                                if (comp instanceof JMapPane
                                        || comp instanceof ElevationLegendPanel
                                        || comp instanceof RadarLegendPanel
                                        || comp instanceof JToolBar) {
                                    c.remove(comp);
                                }
                            }

                            map.addMouseListener(new MapMouseAdapter() {
                                @Override
                                public void onMouseClicked(MapMouseEvent e) {
                                    try {
                                        selLat = e.getWorldPos().y;
                                        selLon = e.getWorldPos().x;
                                        System.out.println("Latitude : " + selLat + " Longitude : " + selLon);
                                         // Get elevation using evaluate
                                        double[] value = new double[1];
                                        DirectPosition2D pos = new DirectPosition2D(selLon, selLat);
                                        map.coverage.evaluate(pos, value);
                                        double elevation = value[0];

                                        elevationLabel.setText("Elevation: " + elevation + " m");
                                        
                                        if (!radarPressed) return;
                                        double height = Double.parseDouble(heightField.getText()) + elevation;
                                        double radius = Double.parseDouble(radiusField.getText());
                                        JRootPane root = SwingUtilities.getRootPane(c);
                                        //****************** */
                                    
                                        JPanel loadingPanel = new JPanel(new BorderLayout());
                                        loadingPanel.setOpaque(true);
                                        loadingPanel.setBackground(new Color(0, 0, 0, 150)); // semi-transparent black
                                        JLabel loadingLabel = new JLabel("Loading radar coverage...", SwingConstants.CENTER);
                                        loadingLabel.setForeground(Color.WHITE);
                                        loadingPanel.add(loadingLabel, BorderLayout.CENTER);
                                        root.setGlassPane(loadingPanel);
                                        loadingPanel.setVisible(true);
                                        //*****************  */
                                        new SwingWorker<GridCoverageLayer, Void>() {
                                            protected GridCoverageLayer doInBackground(){
                                                return addCoverageToMap(map, selLat, selLon, height, radius);
                                            }
                                            protected void done(){
                                                try{
                                                    map.getMapContent().addLayer(get());
                                                } catch(Exception e){
                                                    reportLabel.setText(e.getMessage());

                                                    e.printStackTrace();
                                                }
                                                finally{
                                                    loadingPanel.setVisible(false);
                                                }
                                            }
                                        }.execute();
                                    } catch (Exception err) {
                                        reportLabel.setText(err.getMessage());
                                    }
                                }
                            });
                            // Add new map + UI
                            c.add(map, BorderLayout.CENTER);
                            Color[] colors = styleDetailProvider.giveColors();
                            double[] values = styleDetailProvider.giveValues();
                            String[] labels = styleDetailProvider.giveLabels();
                            ElevationLegendPanel legend = new ElevationLegendPanel(values, colors, labels);
                            legend.setBorder(new EmptyBorder(0, 100, 0, 10));
                            c.add(legend, BorderLayout.EAST);
                            c.add(new RadarLegendPanel(), BorderLayout.SOUTH);
                            c.add(setToolBar(map), BorderLayout.NORTH);
                            c.revalidate();
                            c.repaint();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        reportLabel.setText("Error loading map: " + ex.getMessage());
                    } finally {
                        loadingPanel.setVisible(false); // Hide loading screen
                    }
                }
            }.execute();
        
        }
        
    }
    public static void main(String[] args) {
        Main curr = new Main();
        curr.setSize(800,600);
        curr.setVisible(true);
    }
}
