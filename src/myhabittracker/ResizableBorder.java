package myhabittracker;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Adds resize functionality to undecorated JFrames by detecting
 * mouse position at frame edges and allowing drag-to-resize.
 * 
 * This class properly handles component hierarchies by adding listeners
 * to all components in the frame to capture edge events.
 * 
 * Usage:
 * <pre>
 * JFrame frame = new JFrame();
 * frame.setUndecorated(true);
 * ResizableBorder.install(frame);
 * </pre>
 * 
 * @author asus
 */
public class ResizableBorder extends MouseAdapter {
    
    private final JFrame frame;
    private final int borderWidth;
    
    private Point mouseDownCoords;
    private Point frameLocationOnScreen;
    private Rectangle originalBounds;
    private int resizeDirection;
    
    // Resize direction constants
    private static final int NONE = 0;
    private static final int NORTH = 1;
    private static final int SOUTH = 2;
    private static final int WEST = 4;
    private static final int EAST = 8;
    private static final int NORTHWEST = NORTH | WEST;
    private static final int NORTHEAST = NORTH | EAST;
    private static final int SOUTHWEST = SOUTH | WEST;
    private static final int SOUTHEAST = SOUTH | EAST;
    
    /**
     * Creates a resizable border with default width (5px)
     * 
     * @param frame The JFrame to make resizable
     */
    public ResizableBorder(JFrame frame) {
        this(frame, 5);
    }
    
    /**
     * Creates a resizable border with specified width
     * 
     * @param frame The JFrame to make resizable
     * @param borderWidth The width of the resize area in pixels
     */
    public ResizableBorder(JFrame frame, int borderWidth) {
        this.frame = frame;
        this.borderWidth = borderWidth;
    }
    
    /**
     * Installs resize functionality on a frame
     * 
     * @param frame The frame to make resizable
     * @return The ResizableBorder instance
     */
    public static ResizableBorder install(JFrame frame) {
        return install(frame, 5);
    }
    
    /**
     * Installs resize functionality on a frame with specified border width
     * 
     * @param frame The frame to make resizable
     * @param borderWidth The width of the resize area in pixels
     * @return The ResizableBorder instance
     */
    public static ResizableBorder install(JFrame frame, int borderWidth) {
        ResizableBorder border = new ResizableBorder(frame, borderWidth);
        
        // Add listeners to the frame
        frame.addMouseListener(border);
        frame.addMouseMotionListener(border);
        
        // Add listeners to all components recursively
        addListenersRecursively(frame.getContentPane(), border);
        
        return border;
    }
    
    /**
     * Recursively adds mouse listeners to all components in the container
     */
    private static void addListenersRecursively(Container container, ResizableBorder border) {
        container.addMouseListener(border);
        container.addMouseMotionListener(border);
        
        for (Component comp : container.getComponents()) {
            comp.addMouseListener(border);
            comp.addMouseMotionListener(border);
            
            if (comp instanceof Container) {
                addListenersRecursively((Container) comp, border);
            }
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        // Convert mouse coordinates to frame coordinates
        Point pointOnFrame = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), frame);
        
        mouseDownCoords = pointOnFrame;
        frameLocationOnScreen = frame.getLocationOnScreen();
        originalBounds = frame.getBounds();
        resizeDirection = getResizeDirection(pointOnFrame);
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        resizeDirection = NONE;
        updateCursorForComponent(e.getComponent(), e.getPoint());
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        updateCursorForComponent(e.getComponent(), e.getPoint());
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (resizeDirection == NONE) {
            return;
        }
        
        // Convert current mouse position to frame coordinates
        Point currentMouseOnFrame = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), frame);
        
        int deltaX = currentMouseOnFrame.x - mouseDownCoords.x;
        int deltaY = currentMouseOnFrame.y - mouseDownCoords.y;
        
        int newX = originalBounds.x;
        int newY = originalBounds.y;
        int newWidth = originalBounds.width;
        int newHeight = originalBounds.height;
        
        // Apply minimum size constraints
        int minWidth = Math.max(frame.getMinimumSize().width, 400);
        int minHeight = Math.max(frame.getMinimumSize().height, 300);
        
        // Handle horizontal resize
        if ((resizeDirection & WEST) != 0) {
            newX = originalBounds.x + deltaX;
            newWidth = originalBounds.width - deltaX;
            if (newWidth < minWidth) {
                newX = originalBounds.x + originalBounds.width - minWidth;
                newWidth = minWidth;
            }
        } else if ((resizeDirection & EAST) != 0) {
            newWidth = originalBounds.width + deltaX;
            if (newWidth < minWidth) {
                newWidth = minWidth;
            }
        }
        
        // Handle vertical resize
        if ((resizeDirection & NORTH) != 0) {
            newY = originalBounds.y + deltaY;
            newHeight = originalBounds.height - deltaY;
            if (newHeight < minHeight) {
                newY = originalBounds.y + originalBounds.height - minHeight;
                newHeight = minHeight;
            }
        } else if ((resizeDirection & SOUTH) != 0) {
            newHeight = originalBounds.height + deltaY;
            if (newHeight < minHeight) {
                newHeight = minHeight;
            }
        }
        
        frame.setBounds(newX, newY, newWidth, newHeight);
        frame.revalidate();
    }
    
    /**
     * Updates the cursor for a specific component based on mouse position
     */
    private void updateCursorForComponent(Component component, Point point) {
        Point pointOnFrame = SwingUtilities.convertPoint(component, point, frame);
        int direction = getResizeDirection(pointOnFrame);
        
        Cursor cursor = switch (direction) {
            case NORTH -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case SOUTH -> Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
            case WEST -> Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            case EAST -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case NORTHWEST -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case NORTHEAST -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case SOUTHWEST -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
            case SOUTHEAST -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
            default -> Cursor.getDefaultCursor();
        };
        
        component.setCursor(cursor);
    }
    
    /**
     * Determines which edge/corner is being hovered based on mouse position
     * relative to the frame
     */
    private int getResizeDirection(Point pointOnFrame) {
        int direction = NONE;
        
        if (pointOnFrame.x < borderWidth) {
            direction |= WEST;
        } else if (pointOnFrame.x > frame.getWidth() - borderWidth) {
            direction |= EAST;
        }
        
        if (pointOnFrame.y < borderWidth) {
            direction |= NORTH;
        } else if (pointOnFrame.y > frame.getHeight() - borderWidth) {
            direction |= SOUTH;
        }
        
        return direction;
    }
}
