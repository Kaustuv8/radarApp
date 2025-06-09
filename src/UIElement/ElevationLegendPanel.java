package UIElement;
import javax.swing.*;
import java.awt.*;

public class ElevationLegendPanel extends JPanel {

    private final double[] values;
    private final Color[] colors;
    private final String[] labels;

    public ElevationLegendPanel(double[] values, Color[] colors, String[] labels) {
        this.values = values;
        this.colors = colors;
        this.labels = labels;
        setPreferredSize(new Dimension(200, 200));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int boxHeight = getHeight() / colors.length;
        int boxWidth = 30;
        int padding = 10;

        for (int i = 0; i < colors.length; i++) {
            g.setColor(colors[i]);
            g.fillRect(padding, i * boxHeight, boxWidth, boxHeight);

            g.setColor(Color.BLACK);
            g.drawRect(padding, i * boxHeight, boxWidth, boxHeight);

            String label = labels[i] + " (" + (int)values[i] + " m)";
            g.drawString(label, padding + boxWidth + 10, i * boxHeight + boxHeight / 2 + 5);
        }
    }
}
