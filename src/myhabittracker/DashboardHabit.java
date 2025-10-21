/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package myhabittracker;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.awt.Color;
import java.awt.SystemTray;

/**
 * Main dashboard for MyHabitTracker application. Displays habits in a table
 * format with support for both Yes/No and Measurable habits.
 *
 * OPTIMIZATIONS: - Debounced Excel saving (saves 2 seconds after last edit) -
 * Improved resource management with proper cleanup - Fixed edit mode to
 * preserve all reminder data
 *
 * @author asus
 */
public class DashboardHabit extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(DashboardHabit.class.getName());

    // File path constants
    private static final String DATA_FILE_NAME = "MyHabitTracker_Data.xlsx";
    private static final String SHEET_METADATA = "HabitMetadata";
    private static final String SHEET_REMINDERS = "ReminderData";
    private static final String SHEET_DATA = "HabitData";

    // Save delay constant (milliseconds)
    private static final int SAVE_DELAY_MS = 2000;

    // Window references
    private addHabit habitWindow;
    private PinPasswordHabit pinWindow;

    // Table model
    private DefaultTableModel model;

    // Icons
    private ImageIcon xIcon, checkIcon, addCircleIcon, editIcon, deleteIcon;

    // Save timer for debouncing
    private Timer saveTimer;

    // --- Data Storage ---
    private final Set<String> measurableHabits = new HashSet<>();
    private final Map<String, String> habitUnits = new HashMap<>();
    private final Map<String, Double> habitTargets = new HashMap<>();
    private final Map<String, String> habitThresholds = new HashMap<>();
    private final Map<String, String> habitNotes = new HashMap<>();
    private final Map<String, Reminder> habitReminders = new HashMap<>();

    // --- UI State ---
    private final boolean isSelectColumnVisible = false;

// --- UI Colors ---
    private static final Color FRAME_COLOR = new Color(238, 222, 223); // #EEDEDF
    private static final Color BUTTON_COLOR = new Color(229, 222, 208); // #E5DED0

    // State constants for Yes/No habits
    private static final int STATE_X = 0;
    private static final int STATE_CHECK = 1;
    private static final int STATE_DONE = 2; // Represents a day off for weekly habits

    public DashboardHabit() {
        initComponents();
        setupFrame();
        loadIcons();
        setupWindowPersistence();
        setupTable();
        setupTableHeaderRenderer();
        loadHabitsFromExcel();  // Load data after table is fully configured

        ReminderManager.getInstance().setDashboard(this);
        setupTrayMinimize();

    }

    private void setupTrayMinimize() {
        // Change the default close operation to DO_NOTHING_ON_CLOSE
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Add window listener to handle close button
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Save preferences
                Preferences prefs = Preferences.userNodeForPackage(DashboardHabit.class);
                prefs.putInt("windowX", getX());
                prefs.putInt("windowY", getY());
                prefs.putInt("windowW", getWidth());
                prefs.putInt("windowH", getHeight());

                // Show dialog asking user what they want to do
                if (SystemTray.isSupported()) {
                    Object[] options = {"Minimize to Tray", "Exit Application", "Cancel"};
                    int choice = JOptionPane.showOptionDialog(
                            DashboardHabit.this,
                            "What would you like to do?",
                            "Close Window",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]
                    );

                    switch (choice) {
                        case 0: // Minimize to Tray
                            ReminderManager.getInstance().minimizeToTray(DashboardHabit.this);
                            break;
                        case 1: // Exit Application
                            // Stop and save immediately
                            if (saveTimer != null && saveTimer.isRunning()) {
                                saveTimer.stop();
                                saveHabitsToExcel();
                            }
                            // Shutdown reminder scheduler
                            ReminderManager.getInstance().shutdown();
                            dispose();
                            System.exit(0);
                            break;
                        case 2: // Cancel
                        case JOptionPane.CLOSED_OPTION:
                            // Do nothing - keep window open
                            break;
                    }
                } else {
                    // If tray is not supported, just ask to exit
                    int confirm = JOptionPane.showConfirmDialog(
                            DashboardHabit.this,
                            "Are you sure you want to exit the application?",
                            "Exit Application",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );

                    if (confirm == JOptionPane.YES_OPTION) {
                        // Stop and save immediately
                        if (saveTimer != null && saveTimer.isRunning()) {
                            saveTimer.stop();
                            saveHabitsToExcel();
                        }
                        // Shutdown reminder scheduler
                        ReminderManager.getInstance().shutdown();
                        dispose();
                        System.exit(0);
                    }
                }
            }
        });
    }

    /**
     * Initializes the main frame settings.
     */
    private void setupFrame() {
        setTitle("MyHabitTracker");
        setLocationRelativeTo(null);
        getContentPane().setBackground(FRAME_COLOR);
    }

    /**
     * Loads icons for the application. Note: Custom font is loaded via
     * FontLoader in the main method.
     */
    private void loadIcons() {
        try {
            addCircleIcon = new ImageIcon(getClass().getResource("/resources/addcircle.png"));
            editIcon = new ImageIcon(getClass().getResource("/resources/edit.png"));
            xIcon = CheckboxIconGenerator.createEmptyCheckbox();
            checkIcon = CheckboxIconGenerator.createFilledCheckbox();
            deleteIcon = new ImageIcon(getClass().getResource("/resources/delete.png"));

            // Set icons on buttons
            addHabit.setIcon(addCircleIcon);
            EditButton.setIcon(editIcon);
            DeleteButton.setIcon(deleteIcon);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load icons.", e);
        }
    }

    /**
     * Sets up window position and size persistence across sessions.
     */
    private void setupWindowPersistence() {
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        int x = prefs.getInt("windowX", -1);
        int y = prefs.getInt("windowY", -1);
        int w = prefs.getInt("windowW", getWidth());
        int h = prefs.getInt("windowH", getHeight());

        if (x != -1 && y != -1) {
            setBounds(x, y, w, h);
        }

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                prefs.putInt("windowX", getX());
                prefs.putInt("windowY", getY());
                prefs.putInt("windowW", getWidth());
                prefs.putInt("windowH", getHeight());
            }
        });
    }

    /**
     * Initializes the habits table with column headers and cell renderers.
     */
    private void setupTable() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        String[] columnNames = new String[7];
        columnNames[0] = "Habit";
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            columnNames[i + 1] = today.minusDays(i).format(formatter);
        }

        model = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (isSelectColumnVisible && columnIndex == 0) {
                    return Boolean.class; // Checkbox column
                }
                return Object.class; // Handles String, Integer, etc.
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                if (isSelectColumnVisible && column == 0) {
                    return true; // Checkbox is editable
                }
                if (column == getHabitNameColumnIndex()) {
                    return false; // Habit name is not editable
                }
                String habitName = (String) getValueAt(row, getHabitNameColumnIndex());
                return isMeasurableHabit(habitName);
            }
        };

        jTable1.setModel(model);
        jTable1.setRowHeight(40);
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jTable1.getTableHeader().setReorderingAllowed(false);

        // OPTIMIZATION: Debounced save - only saves 2 seconds after last edit
        setupDebouncedSave();

        // Setup custom renderers and editors
        setupTableRenderer();
        setupTableCellEditor();
        setupTableMouseListener();
    }

    /**
     * Sets up debounced saving to prevent excessive Excel writes. Saves occur 2
     * seconds after the last table modification.
     */
    private void setupDebouncedSave() {
        saveTimer = new Timer(SAVE_DELAY_MS, e -> saveHabitsToExcel());
        saveTimer.setRepeats(false);

        model.addTableModelListener(e -> {
            if (saveTimer.isRunning()) {
                saveTimer.restart();
            } else {
                saveTimer.start();
            }
        });
    }

    /**
     * Sets up custom header renderer for the table. Call this method after
     * setupTable() in the constructor.
     */
    private void setupTableHeaderRenderer() {
        jTable1.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                // Set header background color
                label.setBackground(BUTTON_COLOR);
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);

                label.setFont(label.getFont().deriveFont(14f)); // 16pt font size

                // Optional: Set border to match table header style
                /*          label.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY),
                javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
                 */
                return label;
            }
        });
    }

    private void setupTableRenderer() {
        jTable1.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                // Handle the Boolean checkbox column
                if (isSelectColumnVisible && column == 0 && value instanceof Boolean) {
                    return table.getDefaultRenderer(Boolean.class)
                            .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setIcon(null); // Reset icon

                if (value instanceof Integer state) { // Yes/No habit state
                    label.setText("");
                    switch (state) {
                        case STATE_CHECK ->
                            label.setIcon(checkIcon);
                        case STATE_DONE ->
                            label.setIcon(null);
                        default ->
                            label.setIcon(xIcon);
                    }
                } else if (value != null) { // Measurable or Habit Name
                    label.setText(value.toString());
                } else { // Empty cell
                    label.setText("");
                }
                return label;
            }
        });
    }

    /**
     * Configures custom cell editor for measurable habits with validation.
     */
    private void setupTableCellEditor() {
        jTable1.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean isCellEditable(EventObject e) {
                int row = jTable1.getSelectedRow();
                if (row < 0) {
                    return false;
                }
                String habitName = jTable1.getValueAt(row, getHabitNameColumnIndex()).toString();
                return isMeasurableHabit(habitName);
            }

            @Override
            public boolean stopCellEditing() {
                String value = (String) getCellEditorValue();
                int row = jTable1.getEditingRow();
                if (row < 0) {
                    return super.stopCellEditing();
                }

                String habitName = (String) jTable1.getValueAt(row, getHabitNameColumnIndex());
                String unit = getHabitUnit(habitName);

                if (unit != null && !unit.isEmpty()) {
                    String cleanValue = value.trim().replace(unit, "").trim();
                    try {
                        double numValue = cleanValue.isEmpty() ? 0.0 : Double.parseDouble(cleanValue);
                        ((JTextField) getComponent()).setText(numValue + " " + unit);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(jTable1,
                                "Please enter a valid number.",
                                "Invalid Input",
                                JOptionPane.ERROR_MESSAGE);
                        return false; // Don't stop editing
                    }
                }
                return super.stopCellEditing();
            }
        });
    }

    /**
     * Sets up mouse listener for table interactions (double-click edit,
     * single-click toggle).
     */
    private void setupTableMouseListener() {
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = jTable1.rowAtPoint(e.getPoint());
                int col = jTable1.columnAtPoint(e.getPoint());

                // If clicked outside the table bounds, clear selection
                if (row < 0 || col < 0) {
                    jTable1.clearSelection();
                    return;
                }

                // Handle Double-click to Edit
                if (e.getClickCount() == 2 && col == getHabitNameColumnIndex()) {
                    jTable1.setRowSelectionInterval(row, row);
                    EditButtonActionPerformed(null);
                    return;
                }

                // Handle Single-click for Yes/No Toggling
                int firstDataCol = isSelectColumnVisible ? 2 : 1;
                if (col < firstDataCol) {
                    return;
                }

                String habitName = (String) jTable1.getValueAt(row, getHabitNameColumnIndex());
                if (!isMeasurableHabit(habitName)) {
                    Object val = model.getValueAt(row, col);
                    if (val instanceof Integer && (Integer) val == STATE_DONE) {
                        return; // Don't toggle "Done" states
                    }
                    int state = (val instanceof Integer) ? (Integer) val : STATE_X;
                    int nextState = (state == STATE_X) ? STATE_CHECK : STATE_X;
                    model.setValueAt(nextState, row, col);
                }
            }
        });

        // Add listener to the scroll pane to detect clicks outside the table
        jScrollPane2.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                jTable1.clearSelection();
            }
        });

        // Add click listener to the viewport (the area containing the table)
        jScrollPane2.getViewport().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Check if click is outside table bounds
                if (jTable1.rowAtPoint(e.getPoint()) < 0) {
                    jTable1.clearSelection();
                }
            }
        });

        // Also add listener to content pane for clicks completely outside scroll pane
        getContentPane().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // If click is not on the table or scroll pane
                if (!jScrollPane2.getBounds().contains(e.getPoint())) {
                    jTable1.clearSelection();
                }
            }
        });
    }

    //<editor-fold defaultstate="collapsed" desc="Helper and Utility Methods">
    /**
     * Gets the current column index for the "Habit" name column, accounting for
     * whether the "Select" column is visible.
     *
     * @return The index of the habit name column.
     */
    private int getHabitNameColumnIndex() {
        return isSelectColumnVisible ? 1 : 0;
    }

    /**
     * Checks if a habit is measurable (has units and targets).
     *
     * @param habitName The name of the habit
     * @return true if the habit is measurable, false otherwise
     */
    private boolean isMeasurableHabit(String habitName) {
        return measurableHabits.contains(habitName);
    }

    /**
     * Gets the unit of measurement for a habit.
     *
     * @param habitName The name of the habit
     * @return The unit string, or empty string if not found
     */
    public String getHabitUnit(String habitName) {
        return habitUnits.getOrDefault(habitName, "");
    }

    /**
     * Extracts string value from an Excel cell, handling different cell types.
     *
     * @param cell The Excel cell
     * @return The cell value as a string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING ->
                cell.getStringCellValue();
            case NUMERIC ->
                String.valueOf(cell.getNumericCellValue());
            case BOOLEAN ->
                String.valueOf(cell.getBooleanCellValue());
            default ->
                "";
        };
    }

    /**
     * Gets the file path for the Excel data file.
     *
     * @return The complete file path
     */
    private String getDataFilePath() {
        return System.getProperty("user.home") + File.separator + DATA_FILE_NAME;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="CRUD and Data Handling Methods">
    /**
     * Adds a new habit row to the table with all its metadata.
     *
     * @param habitName The name of the habit
     * @param question The reminder question text
     * @param unit The unit of measurement (for measurable habits)
     * @param target The target value (for measurable habits)
     * @param threshold The threshold type (for measurable habits)
     * @param frequency The reminder frequency
     * @param daysOfWeek The days of the week for reminders
     * @param notes Additional notes about the habit
     */
    public void addHabitRow(String habitName, String question, String unit, double target,
            String threshold, Reminder.Frequency frequency, Set<DayOfWeek> daysOfWeek, String notes) {
        // 1. Store metadata
        if (unit != null && !unit.trim().isEmpty()) {
            measurableHabits.add(habitName);
            habitUnits.put(habitName, unit);
            habitTargets.put(habitName, target);
            habitThresholds.put(habitName, threshold);
        } else {
            measurableHabits.remove(habitName);
        }
        habitNotes.put(habitName, notes == null ? "" : notes);

        // 2. Create and store Reminder metadata
        Reminder reminderData = new Reminder();
        reminderData.setName(habitName);
        reminderData.setText(question);
        reminderData.setFrequency(frequency);
        reminderData.setDaysOfWeek(daysOfWeek);
        reminderData.setNotes(notes);

        if (isMeasurableHabit(habitName)) {
            reminderData.setType(Reminder.HabitType.MEASURABLE);
            reminderData.setUnit(unit);
            reminderData.setTargetValue(target);
            reminderData.setThreshold(threshold);
        } else {
            reminderData.setType(Reminder.HabitType.YES_NO);
        }
        habitReminders.put(habitName, reminderData);

        // 3. Prepare row data for the table
        Object[] row = new Object[model.getColumnCount()];
        int habitColIdx = getHabitNameColumnIndex();
        int firstDataCol = habitColIdx + 1;

        if (isSelectColumnVisible) {
            row[0] = Boolean.FALSE;
        }
        row[habitColIdx] = habitName;

        // 4. Initialize daily data columns
        if (isMeasurableHabit(habitName)) {
            for (int i = firstDataCol; i < row.length; i++) {
                row[i] = "0 " + unit;
            }
        } else {
            LocalDate today = LocalDate.now();
            for (int i = firstDataCol; i < row.length; i++) {
                LocalDate date = today.minusDays(i - firstDataCol);
                if (frequency == Reminder.Frequency.WEEKLY && daysOfWeek != null
                        && !daysOfWeek.contains(date.getDayOfWeek())) {
                    row[i] = STATE_DONE;
                } else {
                    row[i] = STATE_X;
                }
            }
        }
        model.addRow(row);
    }

    /**
     * Updates an existing habit with new metadata.
     *
     * @param rowIndex The row index in the table
     * @param oldName The current habit name
     * @param newName The new habit name
     * @param isMeasurable Whether the habit is measurable
     * @param newUnit The new unit of measurement
     * @param newTarget The new target value
     * @param newThreshold The new threshold type
     */
    /**
     * Updates an existing habit with new metadata.
     *
     * @param rowIndex The row index in the table
     * @param oldName The current habit name
     * @param newName The new habit name
     * @param isMeasurable Whether the habit is measurable
     * @param newUnit The new unit of measurement
     * @param newTarget The new target value
     * @param newThreshold The new threshold type
     * @param question The reminder question text
     * @param frequency The reminder frequency
     * @param daysOfWeek The days of the week for reminders
     * @param notes Additional notes about the habit
     */
    public void updateHabit(int rowIndex, String oldName, String newName, boolean isMeasurable,
            String newUnit, double newTarget, String newThreshold, String question,
            Reminder.Frequency frequency, Set<DayOfWeek> daysOfWeek, String notes) {

        // 1. Remove old metadata
        measurableHabits.remove(oldName);
        habitUnits.remove(oldName);
        habitTargets.remove(oldName);
        habitThresholds.remove(oldName);
        habitNotes.remove(oldName);
        Reminder oldReminder = habitReminders.remove(oldName);

        // 2. Add new metadata
        if (isMeasurable) {
            measurableHabits.add(newName);
            habitUnits.put(newName, newUnit);
            habitTargets.put(newName, newTarget);
            habitThresholds.put(newName, newThreshold);
        }
        habitNotes.put(newName, notes == null ? "" : notes);

        // 3. Create and store updated Reminder metadata
        Reminder reminderData = new Reminder();
        reminderData.setName(newName);
        reminderData.setText(question);
        reminderData.setFrequency(frequency);
        reminderData.setDaysOfWeek(daysOfWeek);
        reminderData.setNotes(notes);

        if (isMeasurable) {
            reminderData.setType(Reminder.HabitType.MEASURABLE);
            reminderData.setUnit(newUnit);
            reminderData.setTargetValue(newTarget);
            reminderData.setThreshold(newThreshold);
        } else {
            reminderData.setType(Reminder.HabitType.YES_NO);
        }

        // Preserve time from old reminder if it exists
        if (oldReminder != null && oldReminder.getTime() != null) {
            reminderData.setTime(oldReminder.getTime());
        }

        habitReminders.put(newName, reminderData);

        // 4. Update ReminderManager
        ReminderManager.getInstance().removeReminder(oldName);
        ReminderManager.getInstance().addReminder(reminderData);

        // 5. Update the table model
        model.setValueAt(newName, rowIndex, getHabitNameColumnIndex());

        // 6. Reset daily data for the updated row
        int firstDataCol = getHabitNameColumnIndex() + 1;
        if (isMeasurable) {
            for (int i = firstDataCol; i < model.getColumnCount(); i++) {
                model.setValueAt("0 " + newUnit, rowIndex, i);
            }
        } else {
            LocalDate today = LocalDate.now();
            for (int i = firstDataCol; i < model.getColumnCount(); i++) {
                LocalDate date = today.minusDays(i - firstDataCol);
                if (frequency == Reminder.Frequency.WEEKLY && daysOfWeek != null
                        && !daysOfWeek.contains(date.getDayOfWeek())) {
                    model.setValueAt(STATE_DONE, rowIndex, i);
                } else {
                    model.setValueAt(STATE_X, rowIndex, i);
                }
            }
        }
    }

    /**
     * Deletes the selected rows from the table after confirmation.
     */
    private void deleteSelectedRows() {
        int[] selectedRows = jTable1.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select one or more habits to delete.",
                    "No Selection",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the selected habit(s)?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // Iterate backwards to avoid index shifting issues
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int rowIndex = selectedRows[i];
                String habitName = (String) model.getValueAt(rowIndex, getHabitNameColumnIndex());

                // Clean up all metadata
                measurableHabits.remove(habitName);
                habitUnits.remove(habitName);
                habitTargets.remove(habitName);
                habitThresholds.remove(habitName);
                habitNotes.remove(habitName);
                habitReminders.remove(habitName);

                // Remove from ReminderManager
                ReminderManager.getInstance().removeReminder(habitName);

                model.removeRow(rowIndex);
            }

            // Immediately save to Excel after deletion
            if (saveTimer != null && saveTimer.isRunning()) {
                saveTimer.stop();
            }
            saveHabitsToExcel();

            JOptionPane.showMessageDialog(this,
                    "The selected habits have been deleted.",
                    "Deletion Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Saves all habit data to an Excel file in the user's home directory.
     */
    private void saveHabitsToExcel() {
        String filePath = getDataFilePath();

        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(filePath)) {

            // Sheet 1: Habit Metadata
            saveHabitMetadata(workbook);

            // Sheet 2: Reminder Metadata
            saveReminderMetadata(workbook);

            // Sheet 3: Habit Data (Table View)
            saveHabitData(workbook);

            workbook.write(fileOut);
            logger.info("Habits saved successfully to " + filePath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error saving habits to Excel", e);
            JOptionPane.showMessageDialog(this,
                    "Failed to save data: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Saves habit metadata to the Excel workbook.
     *
     * @param workbook The Excel workbook
     */
    private void saveHabitMetadata(Workbook workbook) {
        Sheet metadataSheet = workbook.createSheet(SHEET_METADATA);
        Row metaHeader = metadataSheet.createRow(0);
        metaHeader.createCell(0).setCellValue("Habit Name");
        metaHeader.createCell(1).setCellValue("Type");
        metaHeader.createCell(2).setCellValue("Unit");
        metaHeader.createCell(3).setCellValue("Target");
        metaHeader.createCell(4).setCellValue("Threshold");
        metaHeader.createCell(5).setCellValue("Notes");

        int habitColIdx = getHabitNameColumnIndex();
        int metaRowNum = 1;
        for (int i = 0; i < model.getRowCount(); i++) {
            String habitName = (String) model.getValueAt(i, habitColIdx);
            Row row = metadataSheet.createRow(metaRowNum++);
            row.createCell(0).setCellValue(habitName);
            row.createCell(5).setCellValue(habitNotes.getOrDefault(habitName, ""));

            if (isMeasurableHabit(habitName)) {
                row.createCell(1).setCellValue("Measurable");
                row.createCell(2).setCellValue(getHabitUnit(habitName));
                row.createCell(3).setCellValue(habitTargets.getOrDefault(habitName, 0.0));
                row.createCell(4).setCellValue(habitThresholds.getOrDefault(habitName, ""));
            } else {
                row.createCell(1).setCellValue("YesNo");
            }
        }
    }

    /**
     * Saves reminder metadata to the Excel workbook.
     *
     * @param workbook The Excel workbook
     */
    private void saveReminderMetadata(Workbook workbook) {
        Sheet reminderSheet = workbook.createSheet(SHEET_REMINDERS);
        Row reminderHeader = reminderSheet.createRow(0);
        reminderHeader.createCell(0).setCellValue("Habit Name");
        reminderHeader.createCell(1).setCellValue("Question");
        reminderHeader.createCell(2).setCellValue("Frequency");
        reminderHeader.createCell(3).setCellValue("Days Of Week");
        reminderHeader.createCell(4).setCellValue("Time");

        int remRowNum = 1;
        for (Map.Entry<String, Reminder> entry : habitReminders.entrySet()) {
            Row row = reminderSheet.createRow(remRowNum++);
            Reminder rem = entry.getValue();
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(rem.getText());
            row.createCell(2).setCellValue(rem.getFrequency().toString());

            if (rem.getDaysOfWeek() != null && !rem.getDaysOfWeek().isEmpty()) {
                row.createCell(3).setCellValue(
                        rem.getDaysOfWeek().stream()
                                .map(DayOfWeek::toString)
                                .collect(Collectors.joining(",")));
            }

            if (rem.getTime() != null) {
                row.createCell(4).setCellValue(rem.getTime().toString());
            }
        }
    }

    /**
     * Saves habit data (table contents) to the Excel workbook.
     *
     * @param workbook The Excel workbook
     */
    private void saveHabitData(Workbook workbook) {
        Sheet dataSheet = workbook.createSheet(SHEET_DATA);
        Row dataHeader = dataSheet.createRow(0);
        for (int col = 0; col < model.getColumnCount(); col++) {
            dataHeader.createCell(col).setCellValue(model.getColumnName(col));
        }

        for (int rowIdx = 0; rowIdx < model.getRowCount(); rowIdx++) {
            Row dataRow = dataSheet.createRow(rowIdx + 1);
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(rowIdx, col);
                Cell cell = dataRow.createCell(col);

                if (value instanceof String s) {
                    cell.setCellValue(s);
                } else if (value instanceof Integer i) {
                    cell.setCellValue(i);
                } else if (value instanceof Boolean b) {
                    cell.setCellValue(b);
                }
            }
        }
    }

    /**
     * Loads all habit data from the Excel file in the user's home directory.
     */
    private void loadHabitsFromExcel() {
        String filePath = getDataFilePath();
        File file = new File(filePath);
        if (!file.exists()) {
            logger.info("No existing data file found. Starting with empty table.");
            return;
        }

        try (FileInputStream fileIn = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fileIn)) {

            // Load Metadata
            loadHabitMetadata(workbook);

            // Load Reminders
            loadReminderMetadata(workbook);

            // Load Table Data
            loadHabitData(workbook);

            logger.info("Habits loaded successfully from " + filePath);
        } catch (IOException | NumberFormatException e) {
            logger.log(Level.SEVERE, "Error loading habits from Excel", e);
            JOptionPane.showMessageDialog(this,
                    "Failed to load data: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads habit metadata from the Excel workbook.
     *
     * @param workbook The Excel workbook
     */
    private void loadHabitMetadata(Workbook workbook) {
        Sheet metadataSheet = workbook.getSheet(SHEET_METADATA);
        if (metadataSheet == null) {
            return;
        }

        for (int i = 1; i <= metadataSheet.getLastRowNum(); i++) {
            Row row = metadataSheet.getRow(i);
            if (row == null) {
                continue;
            }

            String habitName = getCellValueAsString(row.getCell(0));
            if (habitName.isEmpty()) {
                continue;
            }

            if ("Measurable".equals(getCellValueAsString(row.getCell(1)))) {
                measurableHabits.add(habitName);
                habitUnits.put(habitName, getCellValueAsString(row.getCell(2)));

                String targetStr = getCellValueAsString(row.getCell(3));
                if (!targetStr.isEmpty()) {
                    habitTargets.put(habitName, Double.valueOf(targetStr));
                }

                habitThresholds.put(habitName, getCellValueAsString(row.getCell(4)));
            }
            habitNotes.put(habitName, getCellValueAsString(row.getCell(5)));
        }
    }

    /**
     * Loads reminder metadata from the Excel workbook.
     *
     * @param workbook The Excel workbook
     */
    private void loadReminderMetadata(Workbook workbook) {
        Sheet reminderSheet = workbook.getSheet(SHEET_REMINDERS);
        if (reminderSheet == null) {
            return;
        }

        for (int i = 1; i <= reminderSheet.getLastRowNum(); i++) {
            Row row = reminderSheet.getRow(i);
            if (row == null) {
                continue;
            }

            String habitName = getCellValueAsString(row.getCell(0));
            if (habitName.isEmpty()) {
                continue;
            }

            Reminder rem = new Reminder();
            rem.setText(getCellValueAsString(row.getCell(1)));

            String frequencyStr = getCellValueAsString(row.getCell(2));
            if (!frequencyStr.isEmpty()) {
                rem.setFrequency(Reminder.Frequency.valueOf(frequencyStr));
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

            habitReminders.put(habitName, rem);
        }
    }

    /**
     * Loads habit data (table contents) from the Excel workbook.
     *
     * @param workbook The Excel workbook
     */
    private void loadHabitData(Workbook workbook) {
        Sheet dataSheet = workbook.getSheet(SHEET_DATA);
        if (dataSheet == null) {
            return;
        }

        model.setRowCount(0);
        for (int rowIdx = 1; rowIdx <= dataSheet.getLastRowNum(); rowIdx++) {
            Row excelRow = dataSheet.getRow(rowIdx);
            if (excelRow == null) {
                continue;
            }

            Object[] tableRow = new Object[model.getColumnCount()];

            // First, read the habit name to determine its type
            String habitName = getCellValueAsString(excelRow.getCell(getHabitNameColumnIndex()));
            if (habitName.isEmpty()) {
                continue;
            }

            for (int colIdx = 0; colIdx < model.getColumnCount(); colIdx++) {
                Cell cell = excelRow.getCell(colIdx);

                if (colIdx == getHabitNameColumnIndex()) {
                    tableRow[colIdx] = habitName;
                } else if (isSelectColumnVisible && colIdx == 0) {
                    tableRow[colIdx] = cell != null && cell.getCellType() == CellType.BOOLEAN
                            && cell.getBooleanCellValue();
                } else {
                    if (isMeasurableHabit(habitName)) {
                        tableRow[colIdx] = getCellValueAsString(cell);
                    } else {
                        tableRow[colIdx] = (cell != null && cell.getCellType() == CellType.NUMERIC)
                                ? (int) cell.getNumericCellValue() : STATE_X;
                    }
                }
            }
            model.addRow(tableRow);
        }
    }
    //</editor-fold>

    /**
     * OPTIMIZATION: Proper resource cleanup on window close
     */
    @Override
    public void dispose() {
        // Stop and save immediately
        if (saveTimer != null && saveTimer.isRunning()) {
            saveTimer.stop();
            saveHabitsToExcel();
        }

        // Shutdown reminder scheduler
        try {
            ReminderManager.getInstance().shutdown();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error shutting down ReminderManager", e);
        }

        super.dispose();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jPopupMenu1 = new javax.swing.JPopupMenu();
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
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        addHabit.setBackground(BUTTON_COLOR);
        addHabit.setText("Add");
        addHabit.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        addHabit.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        addHabit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHabitActionPerformed(evt);
            }
        });

        LockButton.setBackground(BUTTON_COLOR);
        LockButton.setText("Lock");
        LockButton.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        LockButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LockButtonActionPerformed(evt);
            }
        });

        jScrollPane2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));

        jTable1.setBackground(BUTTON_COLOR);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jTable1.setFont(javax.swing.UIManager.getFont("Table.font").deriveFont(12f));
        jTable1.setName(""); // NOI18N
        jTable1.setPreferredSize(new java.awt.Dimension(1280, 720));
        jScrollPane2.setViewportView(jTable1);

        fileMenu.setBackground(BUTTON_COLOR);
        fileMenu.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Options", "Export", "Import"}));
        fileMenu.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        fileMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuActionPerformed(evt);
            }
        });

        DeleteButton.setBackground(BUTTON_COLOR);
        DeleteButton.setText("Delete");
        DeleteButton.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        DeleteButton.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        DeleteButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        DeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteButtonActionPerformed(evt);
            }
        });

        EditButton.setBackground(BUTTON_COLOR);
        EditButton.setText("Edit");
        EditButton.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        EditButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(addHabit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(EditButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(DeleteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(LockButton)
                        .addGap(18, 18, 18)
                        .addComponent(fileMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 678, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LockButton)
                    .addComponent(fileMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addHabit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(EditButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(DeleteButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addHabitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHabitActionPerformed
        // TODO add your handling code here:
        if (habitWindow == null || !habitWindow.isShowing()) {
            habitWindow = new addHabit(this);
            habitWindow.setVisible(true);
        } else {
            habitWindow.toFront();
            habitWindow.requestFocus();
        }
    }//GEN-LAST:event_addHabitActionPerformed

    private void LockButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LockButtonActionPerformed
        // TODO add your handling code here:
        // Open the PinPasswordHabit window
        if (pinWindow == null || !pinWindow.isShowing()) {
            pinWindow = new PinPasswordHabit();
            pinWindow.setVisible(true);
        } else {
            pinWindow.toFront();
            pinWindow.requestFocus();
        }
    }//GEN-LAST:event_LockButtonActionPerformed

    private void fileMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileMenuActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_fileMenuActionPerformed

    private void DeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteButtonActionPerformed
        // TODO add your handling code here:
        deleteSelectedRows();
    }//GEN-LAST:event_DeleteButtonActionPerformed

    private void EditButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditButtonActionPerformed
        // TODO add your handling code here:
        int selectedRow = jTable1.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a habit to edit.",
                    "No Selection",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String habitName = (String) model.getValueAt(selectedRow, getHabitNameColumnIndex());
        Reminder storedReminder = habitReminders.get(habitName);
        String notes = habitNotes.getOrDefault(habitName, "");

        if (isMeasurableHabit(habitName)) {
            MeasurableJFrame measurableWindow = new MeasurableJFrame(this, storedReminder);
            measurableWindow.populateForEdit(
                    selectedRow,
                    habitName,
                    storedReminder != null ? storedReminder.getText() : "",
                    habitUnits.getOrDefault(habitName, ""),
                    habitTargets.getOrDefault(habitName, 0.0),
                    habitThresholds.getOrDefault(habitName, "At least"),
                    storedReminder != null ? storedReminder.getFrequency() : Reminder.Frequency.DAILY,
                    storedReminder != null && storedReminder.getDaysOfWeek() != null
                    ? storedReminder.getDaysOfWeek()
                    : new HashSet<>(),
                    storedReminder != null ? storedReminder.getTime() : null,
                    notes
            );
            measurableWindow.setVisible(true);
        } else {
            YesNoJFrame yesNoWindow = new YesNoJFrame(this, storedReminder);
            yesNoWindow.populateForEdit(
                    selectedRow,
                    habitName,
                    storedReminder != null ? storedReminder.getText() : "",
                    storedReminder != null ? storedReminder.getFrequency() : Reminder.Frequency.DAILY,
                    storedReminder != null && storedReminder.getDaysOfWeek() != null
                    ? storedReminder.getDaysOfWeek()
                    : new HashSet<>(),
                    storedReminder != null ? storedReminder.getTime() : null,
                    notes
            );
            yesNoWindow.setVisible(true);
        }
    }//GEN-LAST:event_EditButtonActionPerformed

    /**
     * Main entry point for the application. Applies custom font via FontLoader
     * before creating the main window.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String args[]) {
        try {
            // Set Look and Feel
            UIManager.setLookAndFeel(new FlatLightLaf());

            // Apply custom font globally using FontLoader
            FontLoader.applyCustomFont();

        } catch (UnsupportedLookAndFeelException ex) {
            logger.log(Level.SEVERE, "Failed to initialize FlatLaf", ex);
        }

        // Launch the application
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
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane2;
    public javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
