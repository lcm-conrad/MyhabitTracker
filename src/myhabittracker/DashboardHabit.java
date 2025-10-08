/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package myhabittracker;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author asus
 */
public class DashboardHabit extends javax.swing.JFrame {

    private addHabit habitWindow;
    private PinPasswordHabit PinWindow;
    // class-level fields (near top of class)
    private DefaultTableModel model;
    private ImageIcon xIcon, checkIcon, doneIcon;

    // State constants
    private static final int STATE_X = 0;
    private static final int STATE_CHECK = 1;
    private static final int STATE_DONE = 2;

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

        setLocationRelativeTo(null);
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        //icon sa myHabitsTracker
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
        // ✅ Model uses Integer for icon states
        model = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : Integer.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Disable editing for all cells to avoid text editor
            }
        };

// ✅ Use the NetBeans table
        jTable1.setModel(model);
        jTable1.setRowHeight(40);

        // ✅ Tell JTable how to draw Integer cells
        jTable1.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                label.setText(""); // clear text
                label.setHorizontalAlignment(SwingConstants.CENTER);

                int state = (value instanceof Integer) ? (Integer) value : STATE_X;

                switch (state) {
                    case STATE_CHECK:
                        label.setIcon(checkIcon);
                        break;
                    case STATE_DONE:
                        label.setIcon(doneIcon);
                        break;
                    default:
                        label.setIcon(xIcon);
                        break;
                }

                return label;
            }
        });

        // ✅ Toggle state on click
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
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
        });
        setVisible(true);
    }

    // When a habit is added, we’ll call this later
    // to populate its default icons
    // Example usage:
    // addHabitRow("Drink Water", model, xIcon);
    public void addHabitRow(String habitName) {
        Object[] row = new Object[7];
        row[0] = habitName;
        for (int i = 1; i < 7; i++) {
            row[i] = STATE_X;
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
        addHabit = new javax.swing.JButton();
        LockButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        fileMenu = new javax.swing.JComboBox<>();

        jCheckBoxMenuItem1.setSelected(true);
        jCheckBoxMenuItem1.setText("jCheckBoxMenuItem1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        addHabit.setText("Add Habit");
        addHabit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHabitActionPerformed(evt);
            }
        });

        LockButton.setText("Lock");
        LockButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LockButtonActionPerformed(evt);
            }
        });

        jTable1.setFont(new java.awt.Font("Titillium Web SemiBold", 0, 12)); // NOI18N
        jTable1.setPreferredSize(new java.awt.Dimension(1280, 720));
        jScrollPane2.setViewportView(jTable1);

        fileMenu.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Options", "Export", "Import"}));
        fileMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 621, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(addHabit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(LockButton)
                        .addGap(18, 18, 18)
                        .addComponent(fileMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(29, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addHabit)
                    .addComponent(LockButton)
                    .addComponent(fileMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 319, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addHabitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHabitActionPerformed
        // TODO add your handling code here:
        if (habitWindow == null || !habitWindow.isShowing()) {
            habitWindow = new addHabit(this); // ✅ pass the current DashboardHabit
            habitWindow.setVisible(true);
        } else {
            habitWindow.toFront();
            habitWindow.requestFocus();
        }
    }//GEN-LAST:event_addHabitActionPerformed

    private void LockButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LockButtonActionPerformed
        // TODO add your handling code here:
        // Open the PinPasswordHabit window
        if (PinWindow == null || !PinWindow.isShowing()) {
            PinWindow = new PinPasswordHabit();
            PinWindow.setVisible(true);
        } else {
            PinWindow.toFront();
            PinWindow.requestFocus();
        }
    }//GEN-LAST:event_LockButtonActionPerformed

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
    private void exportHabits() {
        JOptionPane.showMessageDialog(this, "Exporting habits...");
        // TODO: write table data to CSV/Excel
    }

    private void importHabits() {
        JOptionPane.showMessageDialog(this, "Importing habits...");
        // TODO: load table data from CSV/Excel
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
            javax.swing.UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DashboardHabit.class.getName()).log(
                    java.util.logging.Level.SEVERE, "Failed to initialize FlatLaf", ex);
            // Fallback to system L&F
            try {
                javax.swing.UIManager.setLookAndFeel(
                        javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException fallbackEx) {
                // Use default if all fails
            }
        }
        //</editor-fold>

        /* Create and display the form */
        //SwingUtilities.invokeLater(DashboardHabit::new);
        java.awt.EventQueue.invokeLater(() -> new DashboardHabit().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton LockButton;
    private javax.swing.JButton addHabit;
    private javax.swing.JComboBox<String> fileMenu;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
