package myhabittracker;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Utility class to generate checkbox icons programmatically.
 */
public class CheckboxIconGenerator {
    
    private static final int ICON_SIZE = 24;
    private static final int PADDING = 3;
    private static final Color CHECKBOX_COLOR = new Color(145, 204, 161); // #91cca1
    private static final Color BORDER_COLOR = new Color(100, 100, 100);
    private static final float STROKE_WIDTH = 2.5f;
    
    /**
     * Generates an empty checkbox icon.
     * @return ImageIcon representing an empty checkbox
     */
    public static ImageIcon createEmptyCheckbox() {
        BufferedImage img = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        // Enable anti-aliasing for smoother lines
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw border
        g2d.setColor(BORDER_COLOR);
        g2d.setStroke(new BasicStroke(STROKE_WIDTH));
        g2d.drawRect(PADDING, PADDING, ICON_SIZE - 2 * PADDING - 1, ICON_SIZE - 2 * PADDING - 1);
        
        g2d.dispose();
        return new ImageIcon(img);
    }
    
    /**
     * Generates a filled checkbox icon (checked state).
     * @return ImageIcon representing a filled checkbox
     */
    public static ImageIcon createFilledCheckbox() {
        BufferedImage img = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        // Enable anti-aliasing
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill the checkbox
        g2d.setColor(CHECKBOX_COLOR);
        g2d.fillRect(PADDING, PADDING, ICON_SIZE - 2 * PADDING, ICON_SIZE - 2 * PADDING);
        
        // Draw border
        g2d.setColor(BORDER_COLOR);
        g2d.setStroke(new BasicStroke(STROKE_WIDTH));
        g2d.drawRect(PADDING, PADDING, ICON_SIZE - 2 * PADDING - 1, ICON_SIZE - 2 * PADDING - 1);
        
        g2d.dispose();
        return new ImageIcon(img);
    }
}
