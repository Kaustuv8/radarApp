package UIElement;
import javax.swing.*;
import java.awt.*;

public class RadarLegendPanel extends JPanel {

    
    private final Color[] colors;
    private final String[] labels;

    public RadarLegendPanel() {
      
        this.colors = new Color[]{
            Color.RED,
            new Color(0, 56, 2),
        };
        this.labels = new String[]{
            "Blocked",
            "Visible"
        };
        setPreferredSize(new Dimension(400, 50)); // Wider and shorter for horizontal layout
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int panelWidth = getWidth();
        

        int boxWidth = panelWidth / colors.length;
        int boxHeight = 20;
        int topPadding = 5;

        for (int i = 0; i < colors.length; i++) {
            g.setColor(colors[i]);
            g.fillRect(i * boxWidth, topPadding, boxWidth, boxHeight);
            g.setColor(Color.BLACK);
            g.drawRect(i * boxWidth, topPadding, boxWidth, boxHeight);

            String label = labels[i];
            int labelWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(label, i * boxWidth + (boxWidth - labelWidth) / 2, topPadding + boxHeight + 15);
        }
    }
}
