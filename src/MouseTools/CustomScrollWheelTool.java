package MouseTools;
import org.geotools.swing.tool.ScrollWheelTool;
import org.geotools.swing.JMapPane;
import java.awt.*;

public class CustomScrollWheelTool extends ScrollWheelTool {
    private final Cursor customCursor;

    public CustomScrollWheelTool(JMapPane mapPane, Cursor cursor) {
        super(mapPane);
        this.customCursor = cursor;
    }

    @Override
    public Cursor getCursor() {
        return customCursor;
    }
}
