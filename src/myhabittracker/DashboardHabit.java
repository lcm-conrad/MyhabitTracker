/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package myhabittracker;

import com.formdev.flatlaf.FlatLightLaf;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EventObject;
import java.util.HashMap;
import java.util.prefs.Preferences;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.logging.Level;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;

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
    private final Set<String> measurableHabits = new HashSet<>();
    private final Map<String, String> habitUnits = new HashMap<>(); // NEW: Store units
    private final Map<String, Double> habitTargets = new HashMap<>();
    private final Map<String, String> habitThresholds = new HashMap<>(); // "At least" or "At most"
    private final Map<String, String> habitNotes = new HashMap<>(); // Store notes for each habit
    private final Map<String, Reminder> habitReminders = new HashMap<>();
    private boolean isSelectColumnVisible = false;

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
        setTitle("MyHabitTracker");
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
                // MODIFIED: Check for the 'Select' column
                if (isSelectColumnVisible && columnIndex == 0) {
                    return Boolean.class; // This column holds checkboxes
                }
                return Object.class; // Let renderer handle all types (String, Integer)
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                //Adjust column index based on visibility
                int habitColIndex = isSelectColumnVisible ? 1 : 0;

                if (column == habitColIndex) {
                    return false; // Habit Name column is not editable
                }
                // Allow editing of the selection column itself
                if (isSelectColumnVisible && column == 0) {
                    return true;
                }

                String habitName = (String) getValueAt(row, habitColIndex);
                return DashboardHabit.this.isMeasurableHabit(habitName);
            }

        };

// Load saved habits
        loadHabitsFromExcel();

// ✅ Use the NetBeans table
        jTable1.setModel(model);
        jTable1.setRowHeight(40);

        // enable shift-clicking
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        model.addTableModelListener(e -> {
            // Save whenever table data changes
            saveHabitsToExcel();
        });

        //custom renderer to allow yes/no and measurable habits to coincide the table
        jTable1.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    javax.swing.JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                // NEW: Handle the Boolean checkbox column
                if (isSelectColumnVisible && column == 0 && value instanceof Boolean) {
                    // Use the table's default renderer for Boolean (which is a JCheckBox)
                    return table.getDefaultRenderer(Boolean.class).getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, column);
                }
                javax.swing.JLabel label = (javax.swing.JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

                // Check what type of value this cell contains
                if (value instanceof Integer) {
                    // Render as icon for yes/no habits
                    label.setText("");
                    int state = (Integer) value;
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
                } else if (value instanceof String) {
                    // Render as text for measurable habits
                    label.setIcon(null);
                    label.setText((String) value);
                } else {
                    // Empty or null
                    label.setIcon(null);
                    label.setText("");
                }

                return label;
            }
        });

// Custom editor for measurable habits
        jTable1.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean isCellEditable(EventObject e) {
                if (jTable1.getSelectedRow() < 0) {
                    return false;
                }
                int row = jTable1.getSelectedRow();
                // FIX: Use the correct index for habit name
                String habitName = jTable1.getValueAt(row, getHabitNameColumnIndex()).toString();
                return isMeasurableHabit(habitName);
            }

            @Override
            public boolean stopCellEditing() {
                // Get the current editing value
                String value = (String) getCellEditorValue();

                // Get the habit name and unit
                int row = jTable1.getEditingRow();
                if (row >= 0) {
                    // FIX: Use the correct index for habit name
                    String habitName = (String) jTable1.getValueAt(row, getHabitNameColumnIndex());
                    String unit = getHabitUnit(habitName);

                    if (unit != null && !unit.isEmpty()) {
                        // Clean the input - remove any existing unit
                        String cleanValue = value.trim();

                        // Remove the unit if user typed it
                        if (cleanValue.endsWith(unit)) {
                            cleanValue = cleanValue.substring(0, cleanValue.length() - unit.length()).trim();
                        }

                        // Parse the number
                        double numValue = 0.0;
                        if (!cleanValue.isEmpty()) {
                            try {
                                numValue = Double.parseDouble(cleanValue);
                            } catch (NumberFormatException e) {
                                // Invalid number - show error and keep editing
                                JOptionPane.showMessageDialog(jTable1,
                                        "Please enter a valid number.",
                                        "Invalid Input",
                                        JOptionPane.ERROR_MESSAGE);
                                return false; // Don't stop editing
                            }
                        }

                        // Format with unit
                        String formattedValue = numValue + " " + unit;

                        // Update the cell editor value
                        ((JTextField) getComponent()).setText(formattedValue);
                    }
                }

                return super.stopCellEditing();
            }
        });

        // ✅ Toggle state on click
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = jTable1.rowAtPoint(e.getPoint());
                int col = jTable1.columnAtPoint(e.getPoint());

                // MODIFIED: Skip header, the 'Select' col (0), or 'Habit' col (1)
                int firstDataCol = isSelectColumnVisible ? 2 : 1;

                if (row < 0 || col < firstDataCol) {
                    return; // skip header, select col, habit col, or invalid clicks
                }

                // MODIFIED: Adjust column index for habit name
                int habitColIndex = isSelectColumnVisible ? 1 : 0;

                String habitName = jTable1.getValueAt(row, habitColIndex).toString();
                if (!isMeasurableHabit(habitName)) {
                    Object val = model.getValueAt(row, col);
                    int state = (val instanceof Integer) ? (Integer) val : STATE_X;
                    int nextState = (state == STATE_X) ? STATE_CHECK : STATE_X;
                    model.setValueAt(nextState, row, col);
                }
            }
        });
        setVisible(true);

        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click detected
                    int row = jTable1.rowAtPoint(e.getPoint());
                    int col = jTable1.columnAtPoint(e.getPoint());
                    int habitColIndex = getHabitNameColumnIndex();

                    // Check if double-clicked on habit name column
                    if (row >= 0 && col == habitColIndex) {
                        jTable1.setRowSelectionInterval(row, row);
                        EditButtonActionPerformed(null); // Trigger edit
                    }
                }
            }
        });

    }

// NEW METHOD: Toggles the visibility of the Select column
    private int getHabitNameColumnIndex() {
        return isSelectColumnVisible ? 1 : 0;
    }

    private void toggleSelectColumn() {
        if (isSelectColumnVisible) {
            // REMOVE the column

            // 1. Shift data back left (overwriting the data in column 0)
            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 1; j < model.getColumnCount(); j++) {
                    model.setValueAt(model.getValueAt(i, j), i, j - 1);
                }
            }

            // 2. Reduce column count and remove the last column (which is now empty)
            model.setColumnCount(model.getColumnCount() - 1);

            // 3. Rename columns back to base names
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
            LocalDate today = LocalDate.now();
            String[] newNames = new String[7];
            newNames[0] = "Habit";
            for (int i = 0; i < 6; i++) {
                newNames[i + 1] = today.minusDays(i).format(formatter);
            }
            model.setColumnIdentifiers(newNames);

            // 4. SYNCHRONIZE STATE AND BUTTON
            DeleteButton.setVisible(false);
            isSelectColumnVisible = false; // <-- SET STATE TO FALSE
        } else {
            // ADD the column (to the left of Habit)

            int originalColumnCount = model.getColumnCount();

            // 1. Increase column count
            model.setColumnCount(originalColumnCount + 1);

            // 2. Shift data right (from col N to col N+1) and set selection column value
            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = originalColumnCount - 1; j >= 0; j--) {
                    model.setValueAt(model.getValueAt(i, j), i, j + 1);
                }
                // Set the new column value
                model.setValueAt(Boolean.FALSE, i, 0); // New column is all FALSE
            }

            // 3. Set new column identifiers
            String[] oldNames = new String[originalColumnCount];
            for (int i = 0; i < originalColumnCount; i++) {
                oldNames[i] = model.getColumnName(i);
            }

            String[] newNames = new String[originalColumnCount + 1];
            newNames[0] = "Select";
            for (int i = 0; i < originalColumnCount; i++) {
                newNames[i + 1] = oldNames[i];
            }
            model.setColumnIdentifiers(newNames);

            // 4. SYNCHRONIZE STATE AND BUTTON
            DeleteButton.setVisible(true);
            isSelectColumnVisible = true; // <-- SET STATE TO TRUE
        }

        // Refresh the table view
        jTable1.revalidate();
        jTable1.repaint();
    }

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
            // Habit doesn't exist yet - shouldn't happen if addHabitRow was called first
            JOptionPane.showMessageDialog(null, "Habit not found: " + habitName);
            return;
        }

        // Determine which date column to fill (today's date)
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

        // Update cell with the actual value
        model.setValueAt(valueWithUnit, habitRow, colIndex);
    }

    public String getHabitUnit(String habitName) {
        return habitUnits.getOrDefault(habitName, "");
    }

    private boolean isMeasurableHabit(String habitName) {
        return measurableHabits.contains(habitName);
    }

    // When a habit is added, we’ll call this later
    // to populate its default icons
    // Example usage:
    // addHabitRow("Drink Water", model, xIcon);
    public void addHabitRow(String habitName, String question, String unit, double target, String threshold, Reminder.Frequency frequency, Set<DayOfWeek> daysOfWeek, String notes) {
        // 1. Store metadata
        if (unit != null && !unit.trim().isEmpty()) {
            habitUnits.put(habitName, unit);
            habitTargets.put(habitName, target);
            habitThresholds.put(habitName, threshold);
            measurableHabits.add(habitName);
        } else {
            // Clear measurable fields for Yes/No habit
            measurableHabits.remove(habitName);
            habitUnits.remove(habitName);
            habitTargets.remove(habitName);
            habitThresholds.remove(habitName);
        }

        // Store notes regardless of habit type
        habitNotes.put(habitName, notes == null ? "" : notes);

        // NEW: Create and store Reminder metadata
        Reminder reminderData = new Reminder();
        reminderData.setName(habitName);
        reminderData.setText(question);
        reminderData.setFrequency(frequency);
        reminderData.setDaysOfWeek(daysOfWeek);

        if (unit != null && !unit.trim().isEmpty()) {
            reminderData.setType(Reminder.HabitType.MEASURABLE);
            reminderData.setUnit(unit);
            reminderData.setTargetValue(target);
            reminderData.setThreshold(threshold);
        } else {
            reminderData.setType(Reminder.HabitType.YES_NO);
        }
        reminderData.setNotes(notes);
        habitReminders.put(habitName, reminderData);

        // 2. Prepare row data array (JTable display - NO NOTES COLUMN)
        Object[] row = new Object[isSelectColumnVisible ? 8 : 7];
        int habitColIndex = getHabitNameColumnIndex();
        row[habitColIndex] = habitName;

        if (isSelectColumnVisible) {
            row[0] = Boolean.FALSE;
        }

        // The first column containing actual daily data (Today)
        int firstDataCol = habitColIndex + 1;

        // 3. Initialize daily data columns
        if (unit != null && !unit.trim().isEmpty()) {
            // Measurable habit: initialize with '0 unit'
            for (int i = firstDataCol; i < row.length; i++) {
                row[i] = "0 " + unit;
            }
        } else {
            // Yes/No habit: APPLY WEEKLY LOGIC
            LocalDate today = LocalDate.now();

            for (int i = firstDataCol; i < row.length; i++) {
                int dayOffset = i - firstDataCol;
                LocalDate date = today.minusDays(dayOffset);

                if (frequency == Reminder.Frequency.WEEKLY) {
                    DayOfWeek dayOfWeek = date.getDayOfWeek();
                    if (daysOfWeek != null && !daysOfWeek.contains(dayOfWeek)) {
                        row[i] = STATE_DONE;
                    } else {
                        row[i] = STATE_X;
                    }
                } else {
                    row[i] = STATE_X;
                }
            }
        }

        model.addRow(row);
        saveHabitsToExcel();
    }

    public void updateHabit(int rowIndex, String oldName, String newName, boolean isMeasurable, String newUnit, double newTarget, String newThreshold) {
        // 1. Remove old metadata
        measurableHabits.remove(oldName);
        habitUnits.remove(oldName);
        habitTargets.remove(oldName);
        habitThresholds.remove(oldName);

        // 2. Add new metadata
        if (isMeasurable) {
            measurableHabits.add(newName);
            habitUnits.put(newName, newUnit);
            habitTargets.put(newName, newTarget);
            habitThresholds.put(newName, newThreshold);
        }

        // 3. Update the table model
        model.setValueAt(newName, rowIndex, 0);

        // 4. Reset daily data for the updated row
        if (isMeasurable) {
            for (int i = 1; i < model.getColumnCount(); i++) {
                model.setValueAt("0 " + newUnit, rowIndex, i);
            }
        } else { // It's now a Yes/No habit
            for (int i = 1; i < model.getColumnCount(); i++) {
                model.setValueAt(STATE_X, rowIndex, i);
            }
        }

        // 5. Save changes
        saveHabitsToExcel();
    }

// RENAMED AND UPDATED METHOD
    private void deleteSelectedRows() {
        // Get an array of all selected row indices
        int[] selectedRows = jTable1.getSelectedRows();

        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select one or more habits to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Updated confirmation message for single or multiple items
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the selected habit(s)? This action cannot be undone.",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // CRITICAL: Iterate backwards to avoid index shifting issues during removal
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int rowIndex = selectedRows[i];

                // Get habit name before removing the row
                String habitName = (String) model.getValueAt(rowIndex, 0);

                // Clean up internal metadata for each selected habit
                measurableHabits.remove(habitName);
                habitUnits.remove(habitName);
                habitTargets.remove(habitName);
                habitThresholds.remove(habitName);
                habitNotes.remove(habitName);
                // Remove the row from the table model
                model.removeRow(rowIndex);
            }

            // Save to Excel once after all deletions are complete
            saveHabitsToExcel();
            JOptionPane.showMessageDialog(this, "The selected habits have been deleted.", "Deletion Successful", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void saveHabitsToExcel() {
        try {
            Workbook workbook = new XSSFWorkbook();

            // Sheet 1: Habit Metadata
            Sheet metadataSheet = workbook.createSheet("HabitMetadata");
            Row headerRow = metadataSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Habit Name");
            headerRow.createCell(1).setCellValue("Type");
            headerRow.createCell(2).setCellValue("Unit");
            headerRow.createCell(3).setCellValue("Target");
            headerRow.createCell(4).setCellValue("Threshold");
            headerRow.createCell(5).setCellValue("Notes"); // NEW COLUMN HEADER

// FIX: Get the correct column index before the loop
            int habitColIndex = getHabitNameColumnIndex();

            int rowNum = 1;
            for (int i = 0; i < model.getRowCount(); i++) {
                String habitName = (String) model.getValueAt(i, habitColIndex);
                Row row = metadataSheet.createRow(rowNum++);
                String notes = habitNotes.getOrDefault(habitName, "");

                row.createCell(0).setCellValue(habitName);

                if (isMeasurableHabit(habitName)) {
                    row.createCell(1).setCellValue("Measurable");
                    row.createCell(2).setCellValue(getHabitUnit(habitName));

                    Double target = habitTargets.get(habitName);
                    if (target != null) {
                        row.createCell(3).setCellValue(target);
                    }

                    String threshold = habitThresholds.get(habitName);
                    if (threshold != null) {
                        row.createCell(4).setCellValue(threshold);
                    }

                    // FIX: Save notes for measurable habits too
                    row.createCell(5).setCellValue(notes);
                } else {
                    row.createCell(1).setCellValue("YesNo");
                    row.createCell(2).setCellValue("");
                    row.createCell(3).setCellValue("");
                    row.createCell(4).setCellValue("");
                    row.createCell(5).setCellValue(notes);
                }
            }

            // Sheet 2: Habit Data (the table content)
            Sheet dataSheet = workbook.createSheet("HabitData");

            // Sheet 3: Reminder Metadata
            Sheet reminderSheet = workbook.createSheet("ReminderData");
            Row reminderHeaderRow = reminderSheet.createRow(0);
            reminderHeaderRow.createCell(0).setCellValue("Habit Name");
            reminderHeaderRow.createCell(1).setCellValue("Question");
            reminderHeaderRow.createCell(2).setCellValue("Frequency");
            reminderHeaderRow.createCell(3).setCellValue("Days Of Week");
            reminderHeaderRow.createCell(4).setCellValue("Time");
            reminderHeaderRow.createCell(5).setCellValue("Notes");

            rowNum = 1;
            for (Map.Entry<String, Reminder> entry : habitReminders.entrySet()) {
                Row row = reminderSheet.createRow(rowNum++);
                Reminder rem = entry.getValue();

                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(rem.getText() != null ? rem.getText() : "");
                row.createCell(2).setCellValue(rem.getFrequency() != null ? rem.getFrequency().toString() : "");

                // Days of week as comma-separated string
                if (rem.getDaysOfWeek() != null && !rem.getDaysOfWeek().isEmpty()) {
                    String days = rem.getDaysOfWeek().stream()
                            .map(DayOfWeek::toString)
                            .collect(java.util.stream.Collectors.joining(","));
                    row.createCell(3).setCellValue(days);
                }

                row.createCell(4).setCellValue(rem.getTime() != null ? rem.getTime().toString() : "");
                row.createCell(5).setCellValue(rem.getNotes() != null ? rem.getNotes() : "");
            }

            // Write column headers
            Row dataHeaderRow = dataSheet.createRow(0);
            for (int col = 0; col < model.getColumnCount(); col++) {
                dataHeaderRow.createCell(col).setCellValue(model.getColumnName(col));
            }

            // Write data rows
            for (int row = 0; row < model.getRowCount(); row++) {
                Row dataRow = dataSheet.createRow(row + 1);
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object value = model.getValueAt(row, col);
                    Cell cell = dataRow.createCell(col);

                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Integer) {
                        cell.setCellValue((Integer) value);
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            // Auto-size columns
            for (int col = 0; col < model.getColumnCount(); col++) {
                metadataSheet.autoSizeColumn(col);
                dataSheet.autoSizeColumn(col);
            }

            // Save to file
            String userHome = System.getProperty("user.home");
            String filePath = userHome + File.separator + "MyHabitTracker_Data.xlsx";

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            workbook.close();
            logger.log(Level.INFO, "Habits saved to: {0}", filePath);

        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error saving habits to Excel", e);
            JOptionPane.showMessageDialog(this,
                    "Failed to save habits: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadHabitsFromExcel() {
        try {
            String userHome = System.getProperty("user.home");
            String filePath = userHome + File.separator + "MyHabitTracker_Data.xlsx";

            File file = new File(filePath);
            if (!file.exists()) {
                logger.info("No saved habits file found. Starting fresh.");
                return;
            }

            try (FileInputStream fileIn = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fileIn)) {

                // Load metadata first
                Sheet metadataSheet = workbook.getSheet("HabitMetadata");
                if (metadataSheet != null) {
                    for (int i = 1; i <= metadataSheet.getLastRowNum(); i++) {
                        Row row = metadataSheet.getRow(i);
                        if (row == null) {
                            continue;
                        }

                        String habitName = getCellValueAsString(row.getCell(0));
                        String type = getCellValueAsString(row.getCell(1));
                        String unit = getCellValueAsString(row.getCell(2));

                        if ("Measurable".equals(type)) {
                            measurableHabits.add(habitName);
                            if (!unit.isEmpty()) {
                                habitUnits.put(habitName, unit);
                            }

                            // Load target and threshold
                            Cell targetCell = row.getCell(3);
                            if (targetCell != null && targetCell.getCellType() == CellType.NUMERIC) {
                                habitTargets.put(habitName, targetCell.getNumericCellValue());
                            }

                            String threshold = getCellValueAsString(row.getCell(4));
                            if (!threshold.isEmpty()) {
                                habitThresholds.put(habitName, threshold);
                            }
                        }

                        // FIX: Load notes for ALL habit types (moved outside the if-else)
                        String notes = getCellValueAsString(row.getCell(5));
                        habitNotes.put(habitName, notes);
                    }
                }

                // Load table data
                Sheet dataSheet = workbook.getSheet("HabitData");
                if (dataSheet != null) {
                    // Clear existing data
                    model.setRowCount(0);

                    // Skip header row, start from row 1
                    for (int rowIdx = 1; rowIdx <= dataSheet.getLastRowNum(); rowIdx++) {
                        Row excelRow = dataSheet.getRow(rowIdx);
                        if (excelRow == null) {
                            continue;
                        }

                        Object[] tableRow = new Object[model.getColumnCount()];

                        for (int colIdx = 0; colIdx < model.getColumnCount(); colIdx++) {
                            Cell cell = excelRow.getCell(colIdx);

                            if (colIdx == 0) {
                                // Habit name (always string)
                                tableRow[colIdx] = getCellValueAsString(cell);
                            } else {
                                // Data columns
                                String habitName = (String) tableRow[0];

                                if (isMeasurableHabit(habitName)) {
                                    // Measurable habit - store as string
                                    tableRow[colIdx] = getCellValueAsString(cell);
                                } else {
                                    // Yes/No habit - store as integer state
                                    if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                                        tableRow[colIdx] = (int) cell.getNumericCellValue();
                                    } else {
                                        tableRow[colIdx] = STATE_X; // Default
                                    }
                                }
                            }
                        }

                        model.addRow(tableRow);
                    }
                }
                // Load reminder data
                Sheet reminderSheet = workbook.getSheet("ReminderData");
                if (reminderSheet != null) {
                    for (int i = 1; i <= reminderSheet.getLastRowNum(); i++) {
                        Row row = reminderSheet.getRow(i);
                        if (row == null) {
                            continue;
                        }

                        String habitName = getCellValueAsString(row.getCell(0));
                        Reminder rem = new Reminder();
                        rem.setName(habitName);
                        rem.setText(getCellValueAsString(row.getCell(1)));

                        String freqStr = getCellValueAsString(row.getCell(2));
                        if (!freqStr.isEmpty()) {
                            rem.setFrequency(Reminder.Frequency.valueOf(freqStr));
                        }

                        String daysStr = getCellValueAsString(row.getCell(3));
                        if (!daysStr.isEmpty()) {
                            Set<DayOfWeek> days = new HashSet<>();
                            for (String day : daysStr.split(",")) {
                                days.add(DayOfWeek.valueOf(day.trim()));
                            }
                            rem.setDaysOfWeek(days);
                        }

                        String timeStr = getCellValueAsString(row.getCell(4));
                        if (!timeStr.isEmpty()) {
                            rem.setTime(LocalTime.parse(timeStr));
                        }

                        rem.setNotes(getCellValueAsString(row.getCell(5)));

                        // Set type based on whether it's measurable
                        if (isMeasurableHabit(habitName)) {
                            rem.setType(Reminder.HabitType.MEASURABLE);
                            rem.setUnit(habitUnits.get(habitName));
                            rem.setTargetValue(habitTargets.getOrDefault(habitName, 0.0));
                            rem.setThreshold(habitThresholds.get(habitName));
                        } else {
                            rem.setType(Reminder.HabitType.YES_NO);
                        }

                        habitReminders.put(habitName, rem);
                    }
                }
                logger.info("Habits loaded from: " + filePath);
            }

        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error loading habits from Excel", e);
            JOptionPane.showMessageDialog(this,
                    "Failed to load habits: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

// Helper method to safely get cell value as string
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
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
        LockButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        fileMenu = new javax.swing.JComboBox<>();
        DeleteButton = new javax.swing.JButton();
        EditButton = new javax.swing.JButton();

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

        LockButton.setText("Lock");
        LockButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LockButtonActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jTable1.setFont(javax.swing.UIManager.getFont("Table.font").deriveFont(12f));
        jTable1.setPreferredSize(new java.awt.Dimension(1280, 720));
        jScrollPane2.setViewportView(jTable1);

        fileMenu.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Options", "Export", "Import"}));
        fileMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuActionPerformed(evt);
            }
        });

        DeleteButton.setText("Delete");
        DeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteButtonActionPerformed(evt);
            }
        });

        EditButton.setText("Edit");
        EditButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 626, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addHabit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(DeleteButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(EditButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(LockButton)
                        .addGap(18, 18, 18)
                        .addComponent(fileMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addHabit)
                    .addComponent(LockButton)
                    .addComponent(fileMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(DeleteButton)
                    .addComponent(EditButton))
                .addGap(35, 35, 35)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(7, Short.MAX_VALUE))
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

    private void DeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteButtonActionPerformed
        // TODO add your handling code here:
        deleteSelectedRows();
    }//GEN-LAST:event_DeleteButtonActionPerformed

    private void EditButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditButtonActionPerformed
        // TODO add your handling code here:
        int selectedRow = jTable1.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a habit to edit.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int habitColIndex = getHabitNameColumnIndex();
        String habitName = (String) model.getValueAt(selectedRow, habitColIndex);
        boolean isMeasurable = isMeasurableHabit(habitName);

        // Get stored reminder data
        Reminder storedReminder = habitReminders.get(habitName);
        String notes = habitNotes.getOrDefault(habitName, "");

        if (isMeasurable) {
            String unit = habitUnits.getOrDefault(habitName, "");
            double target = habitTargets.getOrDefault(habitName, 0.0);
            String threshold = habitThresholds.getOrDefault(habitName, "At least");

            MeasurableJFrame measurableWindow = new MeasurableJFrame(this);
            measurableWindow.populateForEdit(
                    selectedRow,
                    habitName,
                    storedReminder != null ? storedReminder.getText() : null,
                    unit,
                    target,
                    threshold,
                    storedReminder != null ? storedReminder.getFrequency() : Reminder.Frequency.DAILY,
                    storedReminder != null ? storedReminder.getDaysOfWeek() : new HashSet<>(),
                    storedReminder != null ? storedReminder.getTime() : null,
                    notes
            );
            measurableWindow.setVisible(true);
        } else {
            YesNoJFrame yesNoWindow = new YesNoJFrame(this); // For ADD mode
            Reminder storedReminder = habitReminders.get(habitName);
YesNoJFrame yesNoWindow = new YesNoJFrame(this, storedReminder);
            yesNoWindow.populateForEdit(
                    selectedRow,
                    habitName,
                    storedReminder != null ? storedReminder.getText() : null,
                    storedReminder != null ? storedReminder.getFrequency() : Reminder.Frequency.DAILY,
                    storedReminder != null ? storedReminder.getDaysOfWeek() : new HashSet<>(),
                    storedReminder != null ? storedReminder.getTime() : null,
                    notes
            );
            yesNoWindow.setVisible(true);
        }
    }//GEN-LAST:event_EditButtonActionPerformed

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

        //Load and apply custom font before FlatLaF
        FontLoader.applyCustomFont();

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
    private javax.swing.JButton DeleteButton;
    private javax.swing.JButton EditButton;
    private javax.swing.JButton LockButton;
    private javax.swing.JButton addHabit;
    private javax.swing.JComboBox<String> fileMenu;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
