package myhabittracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

/**
 * A custom, draggable title bar component for undecorated JFrames.
 * Provides minimize and close buttons with a clean, modern appearance.
 * 
 * Usage:
 * <pre>
 * JFrame frame = new JFrame();
 * frame.setUndecorated(true);
 * CustomTitleBar titleBar = new CustomTitleBar(frame, "My Application", frameColor);
 * titleBar.install();
 * </pre>
 * 
 * @author asus
 */
public class CustomTitleBar {
    
    private final JFrame parentFrame;
    private final String title;
    private final Color backgroundColor;
    private final int titleBarHeight;
    
    private JPanel titlePanel;
    private Point mouseDownCoords;
    
    /**
     * Creates a custom title bar with default height (30px)
     * 
     * @param parentFrame The JFrame to attach this title bar to
     * @param title The title text to display
     * @param backgroundColor The background color for the title bar
     */
    public CustomTitleBar(JFrame parentFrame, String title, Color backgroundColor) {
        this(parentFrame, title, backgroundColor, 30);
    }
    
    /**
     * Creates a custom title bar with specified height
     * 
     * @param parentFrame The JFrame to attach this title bar to
     * @param title The title text to display
     * @param backgroundColor The background color for the title bar
     * @param height The height of the title bar in pixels
     */
    public CustomTitleBar(JFrame parentFrame, String title, Color backgroundColor, int height) {
        this.parentFrame = parentFrame;
        this.title = title;
        this.backgroundColor = backgroundColor;
        this.titleBarHeight = height;
    }
    
    /**
     * Installs the custom title bar on the parent frame.
     * Call this method after the frame's initComponents() has been called.
     */
    public void install() {
        createTitlePanel();
        attachToFrame();
        addContentPadding();
        addResizeListener();
    }
    
    /**
     * Creates the title bar panel with all components
     */
    private void createTitlePanel() {
        titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(backgroundColor);
        titlePanel.setBounds(0, 0, parentFrame.getWidth(), titleBarHeight);
        
        // Create and add title label
        JLabel titleLabel = new JLabel("   " + title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        
        // Create button panel
        JPanel buttonPanel = createButtonPanel();
        
        // Add components to title panel
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(buttonPanel, BorderLayout.EAST);
        
        // Add drag functionality
        addDragListeners();
    }
    
    /**
     * Creates the panel containing minimize and close buttons
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 2));
        buttonPanel.setBackground(backgroundColor);
        
        // Create minimize button
        JButton minimizeButton = createButton("-");
        minimizeButton.addActionListener(e -> parentFrame.setState(java.awt.Frame.ICONIFIED));
        
        // Create close button
        JButton closeButton = createButton("×");
        closeButton.setFont(closeButton.getFont().deriveFont(16f));
        closeButton.addActionListener(e -> {
            parentFrame.dispatchEvent(
                new java.awt.event.WindowEvent(parentFrame, java.awt.event.WindowEvent.WINDOW_CLOSING)
            );
        });
        
        buttonPanel.add(minimizeButton);
        buttonPanel.add(closeButton);
        
        return buttonPanel;
    }
    
    /**
     * Creates a styled button for the title bar
     */
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setPreferredSize(new java.awt.Dimension(45, 26));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setContentAreaFilled(true);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setContentAreaFilled(false);
            }
        });
        
        return button;
    }
    
    /**
     * Adds mouse listeners to enable window dragging
     */
    private void addDragListeners() {
        titlePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseDownCoords = e.getPoint();
            }
        });
        
        titlePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                parentFrame.setLocation(
                    currCoords.x - mouseDownCoords.x,
                    currCoords.y - mouseDownCoords.y
                );
            }
        });
    }
    
    /**
     * Attaches the title panel to the frame's layered pane
     */
    private void attachToFrame() {
        parentFrame.getLayeredPane().add(titlePanel, JLayeredPane.PALETTE_LAYER);
    }
    
    /**
     * Adds padding to the content pane to prevent overlap with title bar
     */
    private void addContentPadding() {
        Container contentPane = parentFrame.getContentPane();
        if (contentPane instanceof JPanel) {
            ((JPanel) contentPane).setBorder(
                BorderFactory.createEmptyBorder(titleBarHeight, 0, 0, 0)
            );
        }
    }
    
    /**
     * Adds a listener to resize the title bar when the window is resized
     */
    private void addResizeListener() {
        parentFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (titlePanel != null) {
                    titlePanel.setBounds(0, 0, parentFrame.getWidth(), titleBarHeight);
                }
            }
        });
    }
    
    /**
     * Updates the title text displayed in the title bar
     * 
     * @param newTitle The new title text
     */
    public void setTitle(String newTitle) {
        if (titlePanel != null) {
            JLabel titleLabel = (JLabel) ((JPanel) titlePanel.getComponent(0)).getComponent(0);
            titleLabel.setText("   " + newTitle);
        }
    }
    
    /**
     * Gets the height of the title bar
     * 
     * @return The title bar height in pixels
     */
    public int getTitleBarHeight() {
        return titleBarHeight;
    }
    
    /**
     * Gets the title panel component
     * 
     * @return The JPanel containing the title bar
     */
    public JPanel getTitlePanel() {
        return titlePanel;
    }
}