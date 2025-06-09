package classes;

import java.awt.Color;

public class elevationMapStyle {
    public final Color[] colors = new Color[] {
            new Color(0, 70, 200),     // Deep Blue - Sea Level
            new Color(160, 249, 62),  // Yellowish Green - Lowlands
            new Color(200, 255, 190),  // Light Green - Plains
            new Color(240, 230, 140),  // Khaki - Hills
            new Color(210, 105, 30),   // Chocolate - High Terrain
            new Color(255, 255, 255)   // White - Snow Peaks
        };

    public final double[] values = new double[] {
        0,
        200,
        500,
        1000,
        2500,
        9000
    };

    public final String[] labels = new String[] {
           "Sea Level", "Lowlands", "Hills", "High Hills", "Mountains", "Himalayas"
    };
    public Color[] giveColors(){
        return this.colors;
    }
    public double[] giveValues(){
        return this.values;
    }
    public String[] giveLabels(){
        return this.labels;
    }
}
