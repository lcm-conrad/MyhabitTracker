package myhabittracker;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

/**
 * The ComponentResizer allows you to resize a component by dragging a border
 * of the component.
 */
public class ComponentResizer extends MouseAdapter {
    private final static Dimension MINIMUM_SIZE = new Dimension(10, 10);
    private final static Dimension MAXIMUM_SIZE = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private static Map<Integer, Integer> cursors = new HashMap<Integer, Integer>();
    {
        cursors.put(1, Cursor.N_RESIZE_CURSOR);
        cursors.put(2, Cursor.W_RESIZE_CURSOR);
        cursors.put(4, Cursor.S_RESIZE_CURSOR);
        cursors.put(8, Cursor.E_RESIZE_CURSOR);
        cursors.put(3, Cursor.NW_RESIZE_CURSOR);
        cursors.put(9, Cursor.NE_RESIZE_CURSOR);
        cursors.put(6, Cursor.SW_RESIZE_CURSOR);
        cursors.put(12, Cursor.SE_RESIZE_CURSOR);
    }

    private Insets dragInsets;
    private Dimension snapSize;

    private int direction;
    protected static final int NORTH = 1;
    protected static final int WEST = 2;
    protected static final int SOUTH = 4;
    protected static final int EAST = 8;

    private Cursor sourceCursor;
    private boolean resizing;
    private Rectangle bounds;
    private Point pressed;
    private boolean autoscrolls;

    private Dimension minimumSize = MINIMUM_SIZE;
    private Dimension maximumSize = MAXIMUM_SIZE;

    /**
     * Convenience constructor. All borders are resizable in increments of
     * a single pixel. Components must be registered separately.
     */
    public ComponentResizer() {
        this(new Insets(5, 5, 5, 5), new Dimension(1, 1));
    }

    /**
     * Convenience constructor. All borders are resizable in increments of
     * a single pixel. Components can be registered when the class is created
     * or they can be registered separately afterwards.
     *
     * @param components components to be automatically registered
     */
    public ComponentResizer(Component... components) {
        this(new Insets(5, 5, 5, 5), new Dimension(1, 1), components);
    }

    /**
     * Convenience constructor. Eligible borders are resizable in increments of
     * a single pixel. Components can be registered when the class is created
     * or they can be registered separately afterwards.
     *
     * @param dragInsets Insets specifying which borders are eligible to be resized.
     * @param components components to be automatically registered
     */
    public ComponentResizer(Insets dragInsets, Component... components) {
        this(dragInsets, new Dimension(1, 1), components);
    }

    /**
     * Create a ComponentResizer.
     *
     * @param dragInsets Insets specifying which borders are eligible to be resized.
     * @param snapSize Specify the dimension to which the border will snap to when resizing
     * @param components components to be automatically registered
     */
    public ComponentResizer(Insets dragInsets, Dimension snapSize, Component... components) {
        setDragInsets(dragInsets);
        setSnapSize(snapSize);
        registerComponent(components);
    }

    /**
     * Get the drag insets
     *
     * @return the drag insets
     */
    public Insets getDragInsets() {
        return dragInsets;
    }

    /**
     * Set the drag dragInsets. The insets specify an area where mouseDragged
     * events are recognized from the edge of the border inwards. A value of
     * 0 for any size will imply that the border is not resizable. Otherwise
     * the border is resizable.
     *
     * @param dragInsets Insets to control which borders are resizable.
     */
    public void setDragInsets(Insets dragInsets) {
        validateMinimumAndInsets(minimumSize, dragInsets);
        this.dragInsets = dragInsets;
    }

    /**
     * Get the components maximum size.
     *
     * @return the maximum size
     */
    public Dimension getMaximumSize() {
        return maximumSize;
    }

    /**
     * Specify the maximum size for the component. The component will still
     * be constrained by the size of its parent.
     *
     * @param maximumSize the maximum size for a component.
     */
    public void setMaximumSize(Dimension maximumSize) {
        this.maximumSize = maximumSize;
    }

    /**
     * Get the components minimum size.
     *
     * @return the minimum size
     */
    public Dimension getMinimumSize() {
        return minimumSize;
    }

    /**
     * Specify the minimum size for the component. The minimum size is
     * constrained by the drag insets.
     *
     * @param minimumSize the minimum size for a component.
     */
    public void setMinimumSize(Dimension minimumSize) {
        validateMinimumAndInsets(minimumSize, dragInsets);
        this.minimumSize = minimumSize;
    }

    /**
     * Remove listeners from the specified component
     *
     * @param components the component the listeners are removed from
     */
    public void deregisterComponent(Component... components) {
        for (Component component : components) {
            component.removeMouseListener(this);
            component.removeMouseMotionListener(this);
        }
    }

    /**
     * Add the required listeners to the specified component
     *
     * @param components the component the listeners are added to
     */
    public void registerComponent(Component... components) {
        for (Component component : components) {
            component.addMouseListener(this);
            component.addMouseMotionListener(this);
        }
    }

    /**
     * Get the snap size.
     *
     * @return the snap size.
     */
    public Dimension getSnapSize() {
        return snapSize;
    }

    /**
     * Control how many pixels a border must be dragged before the size of
     * the component is changed. The border will snap to the size once
     * dragging has passed the halfway mark.
     *
     * @param snapSize Dimension object allows you to separately specify a
     * horizontal and vertical snap size.
     */
    public void setSnapSize(Dimension snapSize) {
        this.snapSize = snapSize;
    }

    /**
     * When the components minimum size is less than the drag insets then
     * we can't determine which border should be resized so we need to
     * prevent this from happening.
     */
    private void validateMinimumAndInsets(Dimension minimum, Insets drag) {
        int minimumWidth = drag.left + drag.right;
        int minimumHeight = drag.top + drag.bottom;

        if (minimum.width < minimumWidth || minimum.height < minimumHeight) {
            String message = "Minimum size cannot be less than drag insets";
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Component source = e.getComponent();
        Point location = e.getPoint();
        direction = 0;

        if (location.x < dragInsets.left)
            direction += WEST;

        if (location.x > source.getWidth() - dragInsets.right - 1)
            direction += EAST;

        if (location.y < dragInsets.top)
            direction += NORTH;

        if (location.y > source.getHeight() - dragInsets.bottom - 1)
            direction += SOUTH;

        if (direction == 0)
            source.setCursor(sourceCursor);
        else {
            int cursorType = cursors.get(direction);
            Cursor cursor = Cursor.getPredefinedCursor(cursorType);
            source.setCursor(cursor);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (!resizing) {
            Component source = e.getComponent();
            sourceCursor = source.getCursor();
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (!resizing) {
            Component source = e.getComponent();
            source.setCursor(sourceCursor);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (direction == 0) return;

        resizing = true;

        Component source = e.getComponent();
        pressed = e.getPoint();
        SwingUtilities.convertPointToScreen(pressed, source);
        bounds = source.getBounds();

        if (source instanceof JComponent) {
            JComponent jc = (JComponent) source;
            autoscrolls = jc.getAutoscrolls();
            jc.setAutoscrolls(false);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        resizing = false;

        Component source = e.getComponent();
        source.setCursor(sourceCursor);

        if (source instanceof JComponent) {
            ((JComponent) source).setAutoscrolls(autoscrolls);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (resizing == false) return;

        Component source = e.getComponent();
        Point dragged = e.getPoint();
        SwingUtilities.convertPointToScreen(dragged, source);

        changeBounds(source, direction, bounds, pressed, dragged);
    }

    protected void changeBounds(Component source, int direction, Rectangle bounds, Point pressed, Point current) {
        int x = bounds.x;
        int y = bounds.y;
        int width = bounds.width;
        int height = bounds.height;

        int drag = getDragDistance(pressed.x, current.x, snapSize.width);

        if ((direction & WEST) == WEST) {
            int maximum = Math.min(width + x, maximumSize.width);
            drag = getDragBounded(drag, snapSize.width, width, minimumSize.width, maximum);

            x -= drag;
            width += drag;
        }

        if ((direction & EAST) == EAST) {
            int drag2 = getOppositeDirection(pressed.x, current.x, snapSize.width);
            
            Dimension boundingSize = getBoundingSize(source);
            int maximum = Math.min(boundingSize.width - x, maximumSize.width);
            drag2 = getDragBounded(drag2, snapSize.width, width, minimumSize.width, maximum);
            width += drag2;
        }

        drag = getDragDistance(pressed.y, current.y, snapSize.height);

        if ((direction & NORTH) == NORTH) {
            int maximum = Math.min(height + y, maximumSize.height);
            drag = getDragBounded(drag, snapSize.height, height, minimumSize.height, maximum);

            y -= drag;
            height += drag;
        }

        if ((direction & SOUTH) == SOUTH) {
            int drag2 = getOppositeDirection(pressed.y, current.y, snapSize.height);
            
            Dimension boundingSize = getBoundingSize(source);
            int maximum = Math.min(boundingSize.height - y, maximumSize.height);
            drag2 = getDragBounded(drag2, snapSize.height, height, minimumSize.height, maximum);
            height += drag2;
        }

        source.setBounds(x, y, width, height);
        source.validate();
    }

    private int getOppositeDirection(int pressed, int current, int snapSize) {
        int drag = current - pressed;
        drag += (drag < 0) ? -snapSize / 2 : snapSize / 2;
        drag = (drag / snapSize) * snapSize;
        return drag;
    }

    private int getOppositeDragDistance(int larger, int smaller, int snapSize) {
        int halfway = snapSize / 2;
        int drag = larger - smaller - halfway;
        drag = (drag / snapSize) * snapSize;
        return drag;
    }

    private int getDragDistance(int larger, int smaller, int snapSize) {
        int halfway = snapSize / 2;
        int drag = smaller - larger - halfway;
        drag = (drag / snapSize) * snapSize;
        return drag;
    }

    private int getDragBounded(int drag, int snapSize, int dimension, int minimum, int maximum) {
        while (dimension + drag < minimum)
            drag += snapSize;

        while (dimension + drag > maximum)
            drag -= snapSize;

        return drag;
    }

    private Dimension getBoundingSize(Component source) {
        if (source instanceof Window) {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = env.getMaximumWindowBounds();
            return new Dimension(bounds.width, bounds.height);
        } else {
            return source.getParent().getSize();
        }
    }
}