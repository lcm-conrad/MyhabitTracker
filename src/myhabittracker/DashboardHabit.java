/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package myhabittracker;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EventObject;
import java.util.prefs.Preferences;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
<<<<<<< Updated upstream
import java.util.HashSet;
import java.util.Set;

=======
>>>>>>> Stashed changes
/**
 *
 * @author asus
 */
public class DashboardHabit extends javax.swing.JFrame {

    private addHabit habitWindow;
<<<<<<< HEAD
=======
    private PinPasswordHabit PinWindow;
    // class-level fields (near top of class)
    private DefaultTableModel model;
    private ImageIcon xIcon, checkIcon, doneIcon;
    private final Set<String> measurableHabits = new HashSet<>();

    // State constants
    private static final int STATE_X = 0;
    private static final int STATE_CHECK = 1;
    private static final int STATE_DONE = 2;

>>>>>>> e90560711233dd2e249a773ee86b4157cf974e5d
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DashboardHabit.class.getName());

    /**
     * Creates new form backScreen
     */
    public DashboardHabit() {
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

// ✅ Load icons safely
        xIcon = new ImageIcon(getClass().getResource("/resources/x.png"));
        checkIcon = new ImageIcon(getClass().getResource("/resources/check.png"));
        doneIcon = new ImageIcon(getClass().getResource("/resources/done.png"));
        Font customFont = loadCustomFont("/resources/fonts/Inter-Medium.otf", 14f);
        if (customFont != null) {
            //Apply to specific components (examples below)
            applyFontToComponents(customFont, this);
        } else {
            Font fallbackFont = new Font("Arial", Font.PLAIN, 14);
            applyFontToComponents(fallbackFont, this);
        }

        setLocationRelativeTo(null);
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
<<<<<<< HEAD
        setTitle("MyHabitsTracker");
        String savedPin = prefs.get("userPIN", null);
        if (savedPin == null) {
            jButtonSetupPin.setText("Set PIN");
        } else {
            jButtonSetupPin.setText("Reset PIN");
        }
=======
>>>>>>> e90560711233dd2e249a773ee86b4157cf974e5d
        //icon sa myHabitsTracker
        // If no PIN exists, show setup button
        jButtonSetupPin.setVisible(true);

        // Restore last position and size if available
        int x = prefs.getInt("windowX", -1);
        int y = prefs.getInt("windowY", -1);
        int w = prefs.getInt("windowW", -1);
        int h = prefs.getInt("windowH", -1);

        if (x != -1 && y != -1 && w > 0 && h > 0) {
            setBounds(x, y, w, h);
        } else {
            pack(); // or leave the NetBeans-designed size
            setLocationRelativeTo(null);
        }

        // Save position & size on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                prefs.putInt("windowX", getX());
                prefs.putInt("windowY", getY());
                prefs.putInt("windowW", getWidth());
                prefs.putInt("windowH", getHeight());
            }
        });
        // --- build columns (Habit + 6 days) ---
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        String[] columnNames = new String[7];
        columnNames[0] = "Habit";
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            columnNames[i + 1] = today.minusDays(i).format(formatter);
        }
<<<<<<< HEAD

        // Set custom model with Boolean checkboxes
         jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][]{}, columnNames
        ) {
            Class[] types = new Class[]{
                String.class, Boolean.class, Boolean.class, Boolean.class,
                Boolean.class, Boolean.class, Boolean.class
            };
            public Class getColumnClass(int col) { return types[col]; }
        });
=======
        // ✅ Model uses Integer for icon states
model = new DefaultTableModel(new Object[][]{}, columnNames) {
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        }
        for (int row = 0; row < getRowCount(); row++) {
            Object val = getValueAt(row, columnIndex);
            if (val instanceof Integer) {
                return Integer.class;
            }
            if (val instanceof String) {
                return String.class;
            }
        }
        return Object.class;
>>>>>>> e90560711233dd2e249a773ee86b4157cf974e5d
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (column == 0) return false;
        String habitName = (String) getValueAt(row, 0);
        return DashboardHabit.this.isMeasurableHabit(habitName);
    }
}; // ✅ Properly closes the anonymous class

// ✅ Use the NetBeans table
        jTable1.setModel(model);
        jTable1.setRowHeight(40);
// inside constructor, after jTable1.setModel(model);
        jTable1.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean isCellEditable(EventObject e) {
                if (jTable1.getSelectedRow() < 0) {
                    return false;
                }
                int row = jTable1.getSelectedRow();
                String habitName = jTable1.getValueAt(row, 0).toString();
                return isMeasurableHabit(habitName);
            }
        });

        // ✅ Tell JTable how to draw Integer cells
        jTable1.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setText("");
                label.setHorizontalAlignment(SwingConstants.CENTER);

                int state = (value instanceof Integer) ? (Integer) value : STATE_X;
                switch (state) {
                    case STATE_CHECK ->
                        label.setIcon(checkIcon);
                    case STATE_DONE ->
                        label.setIcon(doneIcon);
                    default ->
                        label.setIcon(xIcon);
                }
                return label;
            }
        });

        // ✅ Toggle state on click
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
<<<<<<< Updated upstream
        int row = jTable1.rowAtPoint(e.getPoint());
        int col = jTable1.columnAtPoint(e.getPoint());
        if (row < 0 || col < 1) return; // skip header or invalid clicks

        String habitName = jTable1.getValueAt(row, 0).toString();
        if (!isMeasurableHabit(habitName)) {
            Object val = model.getValueAt(row, col);
            int state = (val instanceof Integer) ? (Integer) val : STATE_X;
            int nextState = (state == STATE_X) ? STATE_CHECK : STATE_X;
            model.setValueAt(nextState, row, col);
        }
    }
=======
                int row = jTable1.rowAtPoint(e.getPoint());
                int col = jTable1.columnAtPoint(e.getPoint());
                if (row >= 0 && col > 0) {  // Only day columns (col > 0)
                    Object val = model.getValueAt(row, col);
                    int state = (val instanceof Integer) ? (Integer) val : STATE_X;

                    // Binary toggle: Only between X (0) and Check (1); ignore/force-reset Done (2)
                    int nextState = (state == STATE_X) ? STATE_CHECK : STATE_X;

                    model.setValueAt(nextState, row, col);

                }
            }
>>>>>>> Stashed changes
        });
        setVisible(true);
    }

<<<<<<< Updated upstream
public void addMeasurableHabit(String habitName, String valueWithUnit) {
    DefaultTableModel model = (DefaultTableModel) jTable1.getModel();

    // Find if habit already exists
    int habitRow = -1;
    for (int i = 0; i < model.getRowCount(); i++) {
        if (model.getValueAt(i, 0).equals(habitName)) {
            habitRow = i;
            break;
        }
    }

    if (habitRow == -1) {
        // Add new habit row with empty values
        Object[] newRow = new Object[model.getColumnCount()];
        newRow[0] = habitName;
        model.addRow(newRow);
        habitRow = model.getRowCount() - 1;
    }

    // Determine which date column to fill (today’s date)
    LocalDate today = LocalDate.now();
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
    String todayCol = fmt.format(today);

    int colIndex = -1;
    for (int c = 1; c < model.getColumnCount(); c++) {
        if (model.getColumnName(c).equals(todayCol)) {
            colIndex = c;
            break;
        }
    }

    // If today's column doesn't exist, optionally handle
    if (colIndex == -1) {
        JOptionPane.showMessageDialog(null, "Today's column not found!");
        return;
    }

    // Update cell
    model.setValueAt(valueWithUnit, habitRow, colIndex);
}

    private boolean isMeasurableHabit(String habitName) {
        return measurableHabits.contains(habitName);
    }

=======
    // ✅ NEW: Helper method to load the font (add this outside the constructor)
    private Font loadCustomFont(String fontPath, float size) {
        try (InputStream fontStream = getClass().getResourceAsStream(fontPath)) {
            if (fontStream == null) {
                logger.warning("Font file not found: " + fontPath);
                return null;
            }

            // Create and register the font
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(size);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(baseFont);

            logger.info("Custom font loaded successfully: " + baseFont.getName());
            return baseFont;
        } catch (FontFormatException | IOException e) {
            logger.severe("Failed to load font from " + fontPath + ": " + e.getMessage());
            return null;
        }
    }

    // ✅ UPDATED: Recursive apply for current frame only (uses global font)
    private void applyFontToComponents(Font font, Component component) {
        // Only apply if not already customized (avoids overriding)
        if (component.getFont() == null || component.getFont().getFamily().equals("dialog") || component.getFont().getFamily().equals("SansSerif")) {
            component.setFont(font);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyFontToComponents(font, child);
            }
        }
    }

>>>>>>> Stashed changes
    // When a habit is added, we’ll call this later
    // to populate its default icons
    // Example usage:
    // addHabitRow("Drink Water", model, xIcon);
    public void addHabitRow(String habitName) {
        Object[] row = new Object[7];
        row[0] = habitName;

        if (isMeasurableHabit(habitName)) {
            for (int i = 1; i < 7; i++) {
                row[i] = "0.0 unit";
            }
        } else {
            for (int i = 1; i < 7; i++) {
                row[i] = STATE_X;
            }
        }
        model.addRow(row);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        addHabit = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        fileMenu = new javax.swing.JComboBox<>();
        jButtonSetupPin = new javax.swing.JButton();

        jCheckBoxMenuItem1.setSelected(true);
        jCheckBoxMenuItem1.setText("jCheckBoxMenuItem1");

        jMenu1.setText("jMenu1");

        jMenu2.setText("jMenu2");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        addHabit.setText("Add Habit");
        addHabit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHabitActionPerformed(evt);
            }
        });

<<<<<<< HEAD
=======
        LockButton.setText("Lock");
        LockButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LockButtonActionPerformed(evt);
            }
        });

        jTable1.setFont(new java.awt.Font("Titillium Web SemiBold", 0, 12)); // NOI18N
>>>>>>> e90560711233dd2e249a773ee86b4157cf974e5d
        jTable1.setPreferredSize(new java.awt.Dimension(1280, 720));
        jScrollPane2.setViewportView(jTable1);

        fileMenu.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Options", "Export", "Import"}));
        fileMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuActionPerformed(evt);
            }
        });

        jButtonSetupPin.setText("Set pin");
        jButtonSetupPin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSetupPinActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addHabit)
<<<<<<< HEAD
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonSetupPin)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
=======
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 379, Short.MAX_VALUE)
                        .addComponent(LockButton)
                        .addGap(18, 18, 18)
>>>>>>> e90560711233dd2e249a773ee86b4157cf974e5d
                        .addComponent(fileMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addHabit)
                    .addComponent(fileMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSetupPin))
                .addGap(35, 35, 35)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addHabitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHabitActionPerformed
        // TODO add your handling code here:
<<<<<<< HEAD
    String savedPin = PinPasswordHabit.getSavedPin();
    if (savedPin == null) {
        // No PIN yet → force setup
        new PinPasswordHabit("SETUP", this).setVisible(true);
    } else {
        // PIN exists → require unlock first
        new PinPasswordHabit("UNLOCK", this).setVisible(true);
    }
    this.setVisible(false); // hide dashboard until PIN check is done
=======
        if (habitWindow == null || !habitWindow.isShowing()) {
            habitWindow = new addHabit(this); // ✅ pass the current DashboardHabit
            habitWindow.setVisible(true);
        } else {
            habitWindow.toFront();
            habitWindow.requestFocus();
        }
>>>>>>> e90560711233dd2e249a773ee86b4157cf974e5d
    }//GEN-LAST:event_addHabitActionPerformed

    private void fileMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileMenuActionPerformed
        // TODO add your handling code here:
        String selected = (String) fileMenu.getSelectedItem();
        switch (selected) {
            case "Export" -> {
                exportHabits();
                fileMenu.setSelectedIndex(0); // reset
            }
            case "Import" -> {
                importHabits();
                fileMenu.setSelectedIndex(0); // reset
            }
            default -> {
                // do nothing
            }
        }
    }//GEN-LAST:event_fileMenuActionPerformed

    private void jButtonSetupPinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSetupPinActionPerformed
        // TODO add your handling code here:
         int confirm = JOptionPane.showConfirmDialog(
        this,
        "Do you want to reset your PIN and security questions?",
        "Reset Confirmation",
        JOptionPane.YES_NO_OPTION
    );

    if (confirm == JOptionPane.YES_OPTION) {
        Preferences prefs = Preferences.userNodeForPackage(PinPasswordHabit.class);
        // remove PIN and the exact keys used by SecurityQuestionSetup
        prefs.remove("userPIN");
        prefs.remove("secQuestion");
        prefs.remove("secAnswer");

        JOptionPane.showMessageDialog(this,
            "Old PIN and security questions cleared. Let's set up your security questions first.");

        // Open the SecurityQuestionSetup window.
        // The SecurityQuestionSetup class already launches the PIN setup
        // after the user saves/verifies the security question.
        new SecurityQuestionSetup(null, this).setVisible(true);

        // hide dashboard while user sets up security Q + PIN
        this.setVisible(false);
    }
    }//GEN-LAST:event_jButtonSetupPinActionPerformed
    private void exportHabits() {
        JOptionPane.showMessageDialog(this, "Exporting habits...");
        // TODO: write table data to CSV/Excel
    }

    private void importHabits() {
        JOptionPane.showMessageDialog(this, "Importing habits...");
        // TODO: load table data from CSV/Excel
    }
// ✅ NEW: Static helper for main() (add this method; assumes DashboardHabit class access)

   // ✅ UPDATED: Static helper for main() – now uses safe logging
   private static Font loadGlobalFont(String fontPath, float size) {
       try (InputStream fontStream = DashboardHabit.class.getResourceAsStream(fontPath)) {
           if (fontStream == null) {
               logger.log(Level.WARNING, "Font file not found: " + fontPath); // ✅ Fixed
               return null;
           }
           Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(size);
           GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
           logger.log(Level.INFO, "Global font loaded: " + baseFont.getName()); // ✅ Fixed
           return baseFont;
       } catch (FontFormatException | IOException e) {
           logger.log(Level.SEVERE, "Failed to load global font from " + fontPath + ": " + e.getMessage()); // ✅ Fixed
           return null;
       }
   }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            logger.log(Level.SEVERE, "Failed to initialize FlatLaf", ex);
            // Fallback to system L&F
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException fallbackEx) {
                // Use default if all fails
            }
        }
        //</editor-fold>

        /* Create and display the form */
<<<<<<< HEAD
         try {
        FlatLightLaf.setup();
    } catch (Exception ex) {
        logger.log(java.util.logging.Level.SEVERE, "Failed to initialize FlatLaf", ex);
=======
        //SwingUtilities.invokeLater(DashboardHabit::new);
        java.awt.EventQueue.invokeLater(() -> new DashboardHabit().setVisible(true));
>>>>>>> e90560711233dd2e249a773ee86b4157cf974e5d
    }

    java.awt.EventQueue.invokeLater(() -> {
        DashboardHabit dash = new DashboardHabit();
        Preferences prefs = Preferences.userNodeForPackage(DashboardHabit.class);
        String savedPin = prefs.get("userPIN", null);

        if (savedPin == null) {
            // First-time setup
            new PinPasswordHabit("SETUP", dash).setVisible(true);
        } else {
            // Require unlock
            new PinPasswordHabit("UNLOCK", dash).setVisible(true);
        }
        dash.setVisible(false);
    });
    }

    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addHabit;
    private javax.swing.JComboBox<String> fileMenu;
    private javax.swing.JButton jButtonSetupPin;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
