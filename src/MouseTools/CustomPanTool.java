package MouseTools;
import org.geotools.swing.tool.PanTool;
import org.geotools.swing.JMapPane;
import java.awt.*;

public class CustomPanTool extends PanTool{
    private final Cursor customCursor;

    public CustomPanTool(JMapPane mapPane, Cursor cursor) {
        
        this.customCursor = cursor;
    }

    @Override
    public Cursor getCursor() {
        return customCursor;
    }
}
 
    

