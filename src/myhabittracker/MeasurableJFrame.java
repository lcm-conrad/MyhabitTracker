/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package myhabittracker;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import myhabittracker.Reminder.Frequency;
import myhabittracker.Reminder.HabitType;

/**
 * OPTIMIZED MeasurableJFrame with: - Fixed constructor to support both ADD and
 * EDIT modes - Consolidated time parsing using TimeParser utility -
 * Consolidated day parsing using DayOfWeekParser utility - Fixed edit mode to
 * pass all parameters to updateHabit - Removed duplicate validation code -
 * Improved placeholder handling
 */
public class MeasurableJFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(MeasurableJFrame.class.getName());

    private final DashboardHabit dashboard;
    private Reminder existingReminder;

    // Day checkboxes
    private JCheckBox monCheck, tueCheck, wedCheck, thuCheck, friCheck, satCheck, sunCheck;

    // Placeholder constants
    private static final String NAME_PLACEHOLDER = "e.g. Run";
    private static final String QUESTION_PLACEHOLDER = "e.g. How many km did you cover today?";
    private static final String CLOCK_HH_PLACEHOLDER = "00";
    private static final String CLOCK_MM_PLACEHOLDER = "00";
    private static final String NOTES_PLACEHOLDER = "";
    private static final String UNIT_PLACEHOLDER = "e.g. km or cups";
    private static final String TARGET_NUM_PLACEHOLDER = "e.g. 10";
    private static final Color PLACEHOLDER_COLOR = new Color(204, 204, 204);
    private static final Color TEXT_COLOR = Color.BLACK;

    // Edit mode tracking
    private int editingRowIndex = -1;
    private boolean isEditMode = false;

    /**
     * Creates new form MeasurablePanel
     *
     * @param dashboard
     */
    public MeasurableJFrame(DashboardHabit dashboard) {
        this(dashboard, null);
    }

    /**
     * FIXED: Constructor for EDIT mode
     */
    public MeasurableJFrame(DashboardHabit dashboard, Reminder existingReminder) {
        this.dashboard = dashboard;
        this.existingReminder = existingReminder; // Save the reference

        initComponents();
        initializeUI();

        // Load data AFTER UI setup but BEFORE placeholders
        if (existingReminder != null) {
            loadReminderData(existingReminder);
        } else {
            setupPlaceholderBehavior();
        }
    }

    private void initializeUI() {
        ReminderManager.getInstance().startScheduler();

        setSize(getPreferredSize());
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Setup days panel
        setupDaysPanel();
    }

    private void setupDaysPanel() {
        daysPanel.setLayout(new FlowLayout());
        monCheck = new JCheckBox("Mon");
        tueCheck = new JCheckBox("Tue");
        wedCheck = new JCheckBox("Wed");
        thuCheck = new JCheckBox("Thu");
        friCheck = new JCheckBox("Fri");
        satCheck = new JCheckBox("Sat");
        sunCheck = new JCheckBox("Sun");

        daysPanel.add(monCheck);
        daysPanel.add(tueCheck);
        daysPanel.add(wedCheck);
        daysPanel.add(thuCheck);
        daysPanel.add(friCheck);
        daysPanel.add(satCheck);
        daysPanel.add(sunCheck);

        // Hide by default
        daysPanel.setVisible(false);
        // Initialize placeholder behavior
        setupPlaceholderBehavior();
    }
    /**
 * Constructor for ADD mode (no existing reminder)
 * @param dashboard
 */
public MeasurableJFrame(DashboardHabit dashboard) {
    this(dashboard, null); // Call the main constructor with null reminder
}


private void setupPlaceholderBehavior() {
    UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(NameTextField, NAME_PLACEHOLDER);
    UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(QuestionTextField, QUESTION_PLACEHOLDER);
    UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(UnitTextField, UNIT_PLACEHOLDER);
    UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(TargetNumTextField, TARGET_NUM_PLACEHOLDER);
    // Setup placeholders for time fields
    UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(ClockTextFieldHH, CLOCK_HH_PLACEHOLDER);
    UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(ClockTextFieldMM, CLOCK_MM_PLACEHOLDER);
    UtilityClasses.PlaceholderUtils.setupTextAreaPlaceholder(notesArea, NOTES_PLACEHOLDER);
}


// In getInputText() methods (replace with direct calls):

    private String getInputText(javax.swing.JTextArea textArea, String placeholder) {
        return UtilityClasses.PlaceholderUtils.getInputText(textArea, placeholder);
    }

    /**
     * OPTIMIZED: Uses utility classes for validation and parsing
     */
private Reminder createMeasurableReminder() throws IllegalArgumentException {
    String name = UtilityClasses.PlaceholderUtils.getInputText(NameTextField, NAME_PLACEHOLDER);
    String question = UtilityClasses.PlaceholderUtils.getInputText(QuestionTextField, QUESTION_PLACEHOLDER);
    String unit = UtilityClasses.PlaceholderUtils.getInputText(UnitTextField, UNIT_PLACEHOLDER);
    String targetNumStr = UtilityClasses.PlaceholderUtils.getInputText(TargetNumTextField, TARGET_NUM_PLACEHOLDER);

    name = UtilityClasses.InputValidator.validateHabitName(name, NAME_PLACEHOLDER);
    question = UtilityClasses.InputValidator.validateQuestion(question, QUESTION_PLACEHOLDER);
    unit = UtilityClasses.InputValidator.validateUnit(unit, UNIT_PLACEHOLDER);
    double targetValue = UtilityClasses.InputValidator.validateTargetNumber(targetNumStr, TARGET_NUM_PLACEHOLDER);

    // Parse frequency
    String freqStr = (String) FreqButton.getSelectedItem();
    Reminder.Frequency frequency = UtilityClasses.FrequencyFormatter.parse(freqStr);

    // FIXED: Parse time - now OPTIONAL
    String hhStr = UtilityClasses.PlaceholderUtils.getInputText(ClockTextFieldHH, CLOCK_HH_PLACEHOLDER);
    String mmStr = UtilityClasses.PlaceholderUtils.getInputText(ClockTextFieldMM, CLOCK_MM_PLACEHOLDER);
    String ampm = (String) AMPMCombo.getSelectedItem();

    LocalTime time = null;
    // Only validate time if BOTH fields have input
    if (hhStr != null && mmStr != null) {
        time = UtilityClasses.TimeParser.parse12HourTime(hhStr, mmStr, ampm);
    }

    // REMOVED: Don't throw error if time is null - it's now optional
    // if (time == null) {
    //     throw new IllegalArgumentException("Reminder time must be set.");
    // }

    // Get days
    Set<DayOfWeek> daysOfWeek = UtilityClasses.DayOfWeekParser.getSelectedDays(daysPanel);
    if (frequency == Reminder.Frequency.WEEKLY) {
        UtilityClasses.InputValidator.validateWeeklyDays(daysOfWeek);
    }

    // Create Reminder object
    Reminder reminder = new Reminder();
    reminder.setType(HabitType.MEASURABLE);
    reminder.setName(name);
    reminder.setText(question);
    reminder.setFrequency(frequency);
    reminder.setTime(time); // Can be null now
    reminder.setDaysOfWeek(daysOfWeek);
    reminder.setNotes(UtilityClasses.InputValidator.validateNotes(
        getInputText(notesArea, NOTES_PLACEHOLDER), NOTES_PLACEHOLDER));

    // Measurable-specific fields
    reminder.setTargetValue(targetValue);
    reminder.setUnit(unit);
    reminder.setThreshold((String) ThresholdComboBox.getSelectedItem());

    return reminder;
}

public void populateForEdit(int rowIndex, String name, String question, String unit,
        double target, String threshold, Frequency frequency,
        Set<DayOfWeek> daysOfWeek, LocalTime time, String notes) {
    this.editingRowIndex = rowIndex;
    this.isEditMode = true;

    // Populate name
    NameTextField.setText(name);
    NameTextField.setForeground(TEXT_COLOR);

    // Populate question
    if (question != null) {
        QuestionTextField.setText(question);
        QuestionTextField.setForeground(TEXT_COLOR);
    }

    // Populate unit
    if (unit != null) {
        UnitTextField.setText(unit);
        UnitTextField.setForeground(TEXT_COLOR);
    }

    // Populate target
    TargetNumTextField.setText(String.valueOf(target));
    TargetNumTextField.setForeground(TEXT_COLOR);

    // Set threshold
    if (threshold != null) {
        ThresholdComboBox.setSelectedItem(threshold);
    }

    // Set frequency
    if (frequency != null) {
        FreqButton.setSelectedItem(UtilityClasses.FrequencyFormatter.format(frequency));
        
        // Trigger visibility of days panel
        if (frequency == Frequency.WEEKLY) {
            daysPanel.setVisible(true);
            UtilityClasses.DayOfWeekParser.setSelectedDays(daysPanel, daysOfWeek);
        }
    }

    // FIXED: Set time using normal text color (not placeholder)
    if (time != null) {
        String[] timeComponents = UtilityClasses.TimeParser.to12HourFormat(time);
        ClockTextFieldHH.setText(timeComponents[0]);
        ClockTextFieldHH.setForeground(TEXT_COLOR); // BLACK, not placeholder
        ClockTextFieldMM.setText(timeComponents[1]);
        ClockTextFieldMM.setForeground(TEXT_COLOR); // BLACK, not placeholder
        AMPMCombo.setSelectedItem(timeComponents[2]);
    } else {
        // If no time, leave as placeholder
        ClockTextFieldHH.setText(CLOCK_HH_PLACEHOLDER);
        ClockTextFieldHH.setForeground(PLACEHOLDER_COLOR);
        ClockTextFieldMM.setText(CLOCK_MM_PLACEHOLDER);
        ClockTextFieldMM.setForeground(PLACEHOLDER_COLOR);
    }
    
    // Set notes
    if (notes != null && !notes.isEmpty()) {
        notesArea.setText(notes);
        notesArea.setForeground(TEXT_COLOR);
    }

    // Change button text
    saveButton.setText("Update");
}

private void loadReminderData(Reminder rem) {
    // Populate name
    NameTextField.setText(rem.getName());
    NameTextField.setForeground(TEXT_COLOR);
    
    // Populate question
    if (rem.getText() != null) {
        QuestionTextField.setText(rem.getText());
        QuestionTextField.setForeground(TEXT_COLOR);
    }

    // Set frequency
    if (rem.getFrequency() != null) {
        FreqButton.setSelectedItem(UtilityClasses.FrequencyFormatter.format(rem.getFrequency()));
        
        if (rem.getFrequency() == Frequency.WEEKLY && rem.getDaysOfWeek() != null) {
            daysPanel.setVisible(true);
            UtilityClasses.DayOfWeekParser.setSelectedDays(daysPanel, rem.getDaysOfWeek());
        }
    }

    // Measurable-specific fields
    if (rem.getTargetValue() > 0) {
        TargetNumTextField.setText(String.valueOf(rem.getTargetValue()));
        TargetNumTextField.setForeground(TEXT_COLOR);
    }
    
    if (rem.getUnit() != null) {
        UnitTextField.setText(rem.getUnit());
        UnitTextField.setForeground(TEXT_COLOR);
    }
    
    if (rem.getThreshold() != null) {
        ThresholdComboBox.setSelectedItem(rem.getThreshold());
    }

    // FIXED: Set time with normal text color
    if (rem.getTime() != null) {
        String[] timeComponents = UtilityClasses.TimeParser.to12HourFormat(rem.getTime());
        ClockTextFieldHH.setText(timeComponents[0]);
        ClockTextFieldHH.setForeground(TEXT_COLOR); // BLACK, not placeholder
        ClockTextFieldMM.setText(timeComponents[1]);
        ClockTextFieldMM.setForeground(TEXT_COLOR); // BLACK, not placeholder
        AMPMCombo.setSelectedItem(timeComponents[2]);
    } else {
        // Leave as placeholder if no time
        ClockTextFieldHH.setText(CLOCK_HH_PLACEHOLDER);
        ClockTextFieldHH.setForeground(PLACEHOLDER_COLOR);
        ClockTextFieldMM.setText(CLOCK_MM_PLACEHOLDER);
        ClockTextFieldMM.setForeground(PLACEHOLDER_COLOR);
    }
    
    // Set notes
    if (rem.getNotes() != null && !rem.getNotes().isEmpty()) {
        notesArea.setText(rem.getNotes());
        notesArea.setForeground(TEXT_COLOR);
    }
}


    // Generated code (initComponents method)
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        NameLabel = new javax.swing.JLabel();
        QuestionLabel = new javax.swing.JLabel();
        FrequencyLabel = new javax.swing.JLabel();
        ReminderLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        NameTextField = new javax.swing.JTextField();
        QuestionTextField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        notesArea = new javax.swing.JTextArea();
        saveButton = new javax.swing.JButton();
        UnitLabel = new javax.swing.JLabel();
        UnitTextField = new javax.swing.JTextField();
        UnitLabel1 = new javax.swing.JLabel();
        TargetNumTextField = new javax.swing.JTextField();
        ThresholdComboBox = new javax.swing.JComboBox<>();
        AMPMCombo = new javax.swing.JComboBox<>();
        ClockTextFieldMM = new javax.swing.JTextField();
        ClockTextFieldHH = new javax.swing.JTextField();
        ClockButton1 = new javax.swing.JButton();
        FreqButton = new javax.swing.JComboBox<>();
        ReminderLabel1 = new javax.swing.JLabel();
        daysPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        NameLabel.setText("Name:");

        QuestionLabel.setText("Question:");

        FrequencyLabel.setText("Frequency:");

        ReminderLabel.setText("Reminder:");

        jLabel1.setText("Notes:");

        NameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NameTextFieldActionPerformed(evt);
            }
        });

        notesArea.setColumns(20);
        notesArea.setRows(5);
        jScrollPane1.setViewportView(notesArea);

        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        UnitLabel.setText("Unit:");

        UnitTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UnitTextFieldActionPerformed(evt);
            }
        });

        UnitLabel1.setText("Target:");

        TargetNumTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TargetNumTextFieldActionPerformed(evt);
            }
        });

        ThresholdComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "At least", "At most" }));

        AMPMCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AM", "PM"}));
        AMPMCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AMPMComboActionPerformed(evt);
            }
        });

        ClockTextFieldMM.setForeground(new java.awt.Color(204, 204, 204));
        ClockTextFieldMM.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ClockTextFieldMM.setText("00");
        ClockTextFieldMM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockTextFieldMMActionPerformed(evt);
            }
        });

        ClockTextFieldHH.setForeground(new java.awt.Color(204, 204, 204));
        ClockTextFieldHH.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ClockTextFieldHH.setText("00");
        ClockTextFieldHH.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockTextFieldHHActionPerformed(evt);
            }
        });

        ClockButton1.setText("Set");
        ClockButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockButton1ActionPerformed(evt);
            }
        });

        FreqButton.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Daily", "Weekly", "Monthly"}));
        FreqButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FreqButtonActionPerformed(evt);
            }
        });

        ReminderLabel1.setText(":");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(ClockButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ClockTextFieldHH, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ReminderLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ClockTextFieldMM, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(AMPMCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(UnitLabel)
                        .addGap(142, 142, 142)
                        .addComponent(UnitLabel1))
                    .addComponent(ReminderLabel)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(saveButton)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 398, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(QuestionTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                        .addComponent(FrequencyLabel, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(NameLabel, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(QuestionLabel, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(NameTextField, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                            .addComponent(UnitTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(34, 34, 34)
                            .addComponent(TargetNumTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(ThresholdComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(FreqButton, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(daysPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(33, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(NameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(QuestionLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(QuestionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UnitLabel)
                    .addComponent(UnitLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(UnitTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(TargetNumTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ThresholdComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 22, Short.MAX_VALUE)
                .addComponent(FrequencyLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(FreqButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(daysPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(ReminderLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ClockButton1)
                    .addComponent(ClockTextFieldHH)
                    .addComponent(ClockTextFieldMM)
                    .addComponent(AMPMCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ReminderLabel1))
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveButton)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void NameTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NameTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_NameTextFieldActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        // TODO add your handling code here:
        try {
            // Use the validation method to create reminder - this handles ALL validation
            Reminder reminder = createMeasurableReminder();
            String notes = UtilityClasses.InputValidator.validateNotes(
                    UtilityClasses.PlaceholderUtils.getInputText(notesArea, NOTES_PLACEHOLDER), NOTES_PLACEHOLDER);

            // Add to ReminderManager
            ReminderManager.getInstance().addReminder(reminder);

            if (isEditMode) {
                // FIXED: Pass all required parameters to updateHabit
                dashboard.updateHabit(
                        editingRowIndex,
                        reminder.getName(), // oldName
                        reminder.getName(), // newName
                        true, // isMeasurable
                        reminder.getUnit(), // newUnit
                        reminder.getTargetValue(), // newTarget
                        reminder.getThreshold(), // newThreshold
                        reminder.getText(), // question
                        reminder.getFrequency(), // frequency
                        reminder.getDaysOfWeek(),// daysOfWeek
                        notes // notes
                );
                UtilityClasses.UIUtils.showSuccessDialog(this,
                        "Measurable Habit '" + reminder.getName() + "' updated successfully!");
            } else {
                // Add new habit
                dashboard.addHabitRow(
                        reminder.getName(),
                        reminder.getText(),
                        reminder.getUnit(),
                        reminder.getTargetValue(),
                        reminder.getThreshold(),
                        reminder.getFrequency(),
                        reminder.getDaysOfWeek(),
                        notes
                );
                UtilityClasses.UIUtils.showSuccessDialog(this,
                        "Measurable Habit '" + reminder.getName() + "' saved successfully!");
            }

            this.dispose();

        } catch (IllegalArgumentException e) {
            UtilityClasses.UIUtils.showErrorDialog(this, e.getMessage());
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Error saving habit", e);
            UtilityClasses.UIUtils.showErrorDialog(this,
                    "An unexpected error occurred: " + e.getMessage());
        }
    }//GEN-LAST:event_saveButtonActionPerformed

    private void ClockTextFieldMMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockTextFieldMMActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ClockTextFieldMMActionPerformed

    private void ClockTextFieldHHActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockTextFieldHHActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ClockTextFieldHHActionPerformed

    private void ClockButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockButton1ActionPerformed
        // TODO add your handling code here:
        // inside SaveButtonActionPerformed(...) after you validated/saved the habit
        try {
            String text = QuestionTextField.getText().trim();
            if (text.isEmpty() || text.equals(QUESTION_PLACEHOLDER)) {
                JOptionPane.showMessageDialog(this, "Please enter habit text.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Get hours, minutes, and AM/PM values
            String hoursStr = ClockTextFieldHH.getText().trim();
            String minutesStr = ClockTextFieldMM.getText().trim();
            String ampm = (String) AMPMCombo.getSelectedItem();

            // Check if placeholder values are still present
            if (hoursStr.equals(CLOCK_HH_PLACEHOLDER) || minutesStr.equals(CLOCK_MM_PLACEHOLDER)) {
                JOptionPane.showMessageDialog(this, "Please set a valid reminder time.", "Invalid time", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate hours (1-12 for 12-hour format)
            int hours;
            try {
                hours = Integer.parseInt(hoursStr);
                if (hours < 1 || hours > 12) {
                    JOptionPane.showMessageDialog(this, "Hours must be between 1 and 12.", "Invalid hours", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter valid hours (1-12).", "Invalid hours", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate minutes (0-59)
            int minutes;
            try {
                minutes = Integer.parseInt(minutesStr);
                if (minutes < 0 || minutes > 59) {
                    JOptionPane.showMessageDialog(this, "Minutes must be between 0 and 59.", "Invalid minutes", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter valid minutes (0-59).", "Invalid minutes", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Convert to 24-hour format
            if ("PM".equals(ampm) && hours != 12) {
                hours += 12;
            } else if ("AM".equals(ampm) && hours == 12) {
                hours = 0;
            }

            // Create LocalTime object
            java.time.LocalTime time = java.time.LocalTime.of(hours, minutes);

            // Determine frequency and days
            Reminder rem = new Reminder();
            rem.setText(text);
            rem.setType(Reminder.HabitType.MEASURABLE);
            rem.setName(NameTextField.getText().trim());

            String freqSelected = "Daily";
            try {
                freqSelected = FreqButton.getSelectedItem().toString();
            } catch (Exception ign) {
            }

            if ("Weekly".equalsIgnoreCase(freqSelected)) {
                rem.setFrequency(Reminder.Frequency.WEEKLY);
                Set<java.time.DayOfWeek> days = new HashSet<>();
                for (Component c : daysPanel.getComponents()) {
                    if (c instanceof javax.swing.JCheckBox) {
                        javax.swing.JCheckBox cb = (javax.swing.JCheckBox) c;
                        if (cb.isSelected()) {
                            String txt = cb.getText().toUpperCase();
                            switch (txt) {
                                case "MON", "MONDAY" -> days.add(java.time.DayOfWeek.MONDAY);
                                case "TUE", "TUES", "TUESDAY" -> days.add(java.time.DayOfWeek.TUESDAY);
                                case "WED", "WEDNESDAY" -> days.add(java.time.DayOfWeek.WEDNESDAY);
                                case "THU", "THURSDAY" -> days.add(java.time.DayOfWeek.THURSDAY);
                                case "FRI", "FRIDAY" -> days.add(java.time.DayOfWeek.FRIDAY);
                                case "SAT", "SATURDAY" -> days.add(java.time.DayOfWeek.SATURDAY);
                                case "SUN", "SUNDAY" -> days.add(java.time.DayOfWeek.SUNDAY);
                            }
                        }
                    }
                }
                rem.setDaysOfWeek(days);
            } else {
                rem.setFrequency(Reminder.Frequency.DAILY);
            }

            rem.setTime(time);

            // register
            ReminderManager.getInstance().addReminder(rem);
            JOptionPane.showMessageDialog(this, "Reminder saved for " + time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")) + ".", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (HeadlessException ex) {
            JOptionPane.showMessageDialog(this, "Failed to create reminder: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_ClockButton1ActionPerformed

    private void FreqButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FreqButtonActionPerformed
        // TODO add your handling code here:
        String selected = (String) FreqButton.getSelectedItem();
        daysPanel.setVisible("Weekly".equals(selected));
        revalidate();
        repaint();
    }//GEN-LAST:event_FreqButtonActionPerformed

    private void UnitTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UnitTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_UnitTextFieldActionPerformed

    private void TargetNumTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TargetNumTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TargetNumTextFieldActionPerformed

    private void AMPMComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AMPMComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_AMPMComboActionPerformed

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
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        //java.awt.EventQueue.invokeLater(() -> new MeasurableJFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> AMPMCombo;
    private javax.swing.JButton ClockButton1;
    private javax.swing.JTextField ClockTextFieldHH;
    private javax.swing.JTextField ClockTextFieldMM;
    private javax.swing.JComboBox<String> FreqButton;
    private javax.swing.JLabel FrequencyLabel;
    private javax.swing.JLabel NameLabel;
    private javax.swing.JTextField NameTextField;
    private javax.swing.JLabel QuestionLabel;
    private javax.swing.JTextField QuestionTextField;
    private javax.swing.JLabel ReminderLabel;
    private javax.swing.JLabel ReminderLabel1;
    private javax.swing.JTextField TargetNumTextField;
    private javax.swing.JComboBox<String> ThresholdComboBox;
    private javax.swing.JLabel UnitLabel;
    private javax.swing.JLabel UnitLabel1;
    private javax.swing.JTextField UnitTextField;
    private javax.swing.JPanel daysPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea notesArea;
    private javax.swing.JButton saveButton;
    // End of variables declaration//GEN-END:variables
}
