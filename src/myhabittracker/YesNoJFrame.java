/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package myhabittracker;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import myhabittracker.Reminder.Frequency;

/**
 *
 * @author asus
 */
public class YesNoJFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(YesNoJFrame.class.getName());
    private final DashboardHabit dashboard;
    private Reminder existingReminder;

    JCheckBox monCheck, tueCheck, wedCheck, thuCheck, friCheck, satCheck, sunCheck;
    // Placeholder text and color
    private final String NAME_PLACEHOLDER = "e.g. Study";
    private final String QUESTION_PLACEHOLDER = "e.g. Did you study today?";
    private final String CLOCK_HH_PLACEHOLDER = "00";
    private final String CLOCK_MM_PLACEHOLDER = "00";
    private final String NOTES_PLACEHOLDER = "";
    private final Color PLACEHOLDER_COLOR = new Color(204, 204, 204);
    private final Color TEXT_COLOR = Color.BLACK;

    private int editingRowIndex = -1;
    private boolean isEditMode = false;

    // Constructor for ADD mode
    public YesNoJFrame(DashboardHabit dashboard) {
        this.dashboard = dashboard;
        initComponents();
        initializeUI();
        setupPlaceholderBehavior();
    }

    // Constructor for EDIT mode (overloaded)
    public YesNoJFrame(DashboardHabit dashboard, Reminder existingReminder) {
        this.dashboard = dashboard;
        initComponents();
        initializeUI();

        // Load data BEFORE setting up placeholders to avoid conflicts
        if (existingReminder != null) {
            loadReminderData(existingReminder);
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
    
    private void setupPlaceholderBehavior() {
        UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(NameTextField, NAME_PLACEHOLDER);
        UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(QuestionTextField, QUESTION_PLACEHOLDER);

        // Setup placeholders for time fields
        UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(ClockTextFieldHH, CLOCK_HH_PLACEHOLDER);
        UtilityClasses.PlaceholderUtils.setupTextFieldPlaceholder(ClockTextFieldMM, CLOCK_MM_PLACEHOLDER);
        UtilityClasses.PlaceholderUtils.setupTextAreaPlaceholder(notesArea, NOTES_PLACEHOLDER);
    }
    private String getInputText(javax.swing.JTextArea textArea, String placeholder) {
        return UtilityClasses.PlaceholderUtils.getInputText(textArea, placeholder);
    }
private Reminder createYesNoReminder() throws IllegalArgumentException {
    String name = UtilityClasses.PlaceholderUtils.getInputText(NameTextField, NAME_PLACEHOLDER);
    String question = UtilityClasses.PlaceholderUtils.getInputText(QuestionTextField, QUESTION_PLACEHOLDER);

    name = UtilityClasses.InputValidator.validateHabitName(name, NAME_PLACEHOLDER);
    question = UtilityClasses.InputValidator.validateQuestion(question, QUESTION_PLACEHOLDER);

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
    reminder.setType(Reminder.HabitType.YES_NO);
    reminder.setName(name);
    reminder.setText(question);
    reminder.setFrequency(frequency);
    reminder.setTime(time); // Can be null now
    reminder.setDaysOfWeek(daysOfWeek);
    reminder.setNotes(UtilityClasses.InputValidator.validateNotes(
        getInputText(notesArea, NOTES_PLACEHOLDER), NOTES_PLACEHOLDER));

    return reminder;
}

    public void populateForEdit(int rowIndex, String name, String question, Frequency frequency,
            Set<DayOfWeek> daysOfWeek, LocalTime time, String notes) {
        this.editingRowIndex = rowIndex;
        this.isEditMode = true;

        // Populate fields
        NameTextField.setText(name);
        NameTextField.setForeground(TEXT_COLOR);

        if (question != null) {
            QuestionTextField.setText(question);
            QuestionTextField.setForeground(TEXT_COLOR);
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
        SaveButton.setText("Update");
    }

    private void loadReminderData(Reminder rem) {
        // Name Field
        NameTextField.setText(rem.getName());
        NameTextField.setForeground(TEXT_COLOR);

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

    /**
     * Creates new form HabitPanel
     */
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        NameLabel = new javax.swing.JLabel();
        QuestionLabel = new javax.swing.JLabel();
        FrequencyLabel = new javax.swing.JLabel();
        ReminderLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        NameTextField = new javax.swing.JTextField();
        QuestionTextField = new javax.swing.JTextField();
        ClockButton = new javax.swing.JButton();
        ClockTextFieldHH = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        notesArea = new javax.swing.JTextArea();
        SaveButton = new javax.swing.JButton();
        FreqButton = new javax.swing.JComboBox<>();
        daysPanel = new javax.swing.JPanel();
        ClockTextFieldMM = new javax.swing.JTextField();
        ReminderLabel1 = new javax.swing.JLabel();
        AMPMCombo = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        NameLabel.setText("Name:");

        QuestionLabel.setText("Question:");

        FrequencyLabel.setText("Frequency:");

        ReminderLabel.setText("Reminder:");

        jLabel1.setText("Notes:");

        NameTextField.setForeground(new java.awt.Color(204, 204, 204));
        NameTextField.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        NameTextField.setText("e.g. Study");
        NameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NameTextFieldActionPerformed(evt);
            }
        });

        QuestionTextField.setForeground(new java.awt.Color(204, 204, 204));
        QuestionTextField.setText("e.g. Did you study today?");
        QuestionTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                QuestionTextFieldActionPerformed(evt);
            }
        });

        ClockButton.setText("Set");
        ClockButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClockButtonActionPerformed(evt);
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

        notesArea.setColumns(20);
        notesArea.setForeground(new java.awt.Color(204, 204, 204));
        notesArea.setRows(5);
        notesArea.setText("(Optional)");
        jScrollPane1.setViewportView(notesArea);

        SaveButton.setText("Save");
        SaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveButtonActionPerformed(evt);
            }
        });

        FreqButton.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Daily", "Weekly"}));
        FreqButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FreqButtonActionPerformed(evt);
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

        ReminderLabel1.setText(":");

        AMPMCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AM", "PM"}));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(SaveButton)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 398, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(FreqButton, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(QuestionTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                        .addComponent(FrequencyLabel, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(NameLabel, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(QuestionLabel, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(NameTextField, javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(ClockButton)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(ClockTextFieldHH, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGap(3, 3, 3)
                            .addComponent(ReminderLabel1)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(ClockTextFieldMM, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(AMPMCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(ReminderLabel, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(daysPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(27, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(NameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(QuestionLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(QuestionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(FrequencyLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(FreqButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(daysPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(ReminderLabel)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ClockButton)
                    .addComponent(ClockTextFieldHH)
                    .addComponent(ClockTextFieldMM)
                    .addComponent(ReminderLabel1)
                    .addComponent(AMPMCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(SaveButton)
                .addGap(22, 22, 22))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void NameTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NameTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_NameTextFieldActionPerformed

    private void ClockTextFieldHHActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockTextFieldHHActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ClockTextFieldHHActionPerformed

    private void SaveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveButtonActionPerformed
        // TODO add your handling code here:
        try {
            // Use the validation method to create reminder - this handles ALL validation
            Reminder reminder = createYesNoReminder();
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
                        false, // isMeasurable
                        null, // newUnit
                        0.0, // newTarget
                        null, // newThreshold
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
    }//GEN-LAST:event_SaveButtonActionPerformed

    private void QuestionTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QuestionTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_QuestionTextFieldActionPerformed

    private void FreqButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FreqButtonActionPerformed
        // TODO add your handling code here:
        String selected = (String) FreqButton.getSelectedItem();
        daysPanel.setVisible("Weekly".equals(selected));
        revalidate();
        repaint();
    }//GEN-LAST:event_FreqButtonActionPerformed

    private void ClockButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockButtonActionPerformed
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
            rem.setType(Reminder.HabitType.YES_NO);
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
                                case "MON", "MONDAY" ->
                                    days.add(java.time.DayOfWeek.MONDAY);
                                case "TUE", "TUES", "TUESDAY" ->
                                    days.add(java.time.DayOfWeek.TUESDAY);
                                case "WED", "WEDNESDAY" ->
                                    days.add(java.time.DayOfWeek.WEDNESDAY);
                                case "THU", "THURSDAY" ->
                                    days.add(java.time.DayOfWeek.THURSDAY);
                                case "FRI", "FRIDAY" ->
                                    days.add(java.time.DayOfWeek.FRIDAY);
                                case "SAT", "SATURDAY" ->
                                    days.add(java.time.DayOfWeek.SATURDAY);
                                case "SUN", "SUNDAY" ->
                                    days.add(java.time.DayOfWeek.SUNDAY);
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
    }//GEN-LAST:event_ClockButtonActionPerformed

    private void ClockTextFieldMMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClockTextFieldMMActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ClockTextFieldMMActionPerformed

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
        //java.awt.EventQueue.invokeLater(() -> new YesNoJFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> AMPMCombo;
    private javax.swing.JButton ClockButton;
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
    private javax.swing.JButton SaveButton;
    private javax.swing.JPanel daysPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea notesArea;
    // End of variables declaration//GEN-END:variables
}
