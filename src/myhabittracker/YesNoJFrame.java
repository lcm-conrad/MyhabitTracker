/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package myhabittracker;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
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
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Setup days panel
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
        
        daysPanel.setVisible(false);
    }

    private void setupPlaceholderBehavior() {
        // NameTextField
        setupTextFieldPlaceholder(NameTextField, NAME_PLACEHOLDER);

        // QuestionTextField
        setupTextFieldPlaceholder(QuestionTextField, QUESTION_PLACEHOLDER);

        // ClockTextField (HH)
        setupTextFieldPlaceholder(ClockTextFieldHH, CLOCK_HH_PLACEHOLDER);

        // ClockTextField1 (MM)
        setupTextFieldPlaceholder(ClockTextFieldMM, CLOCK_MM_PLACEHOLDER);

        // Notes TextArea
        setupTextAreaPlaceholder(notesArea, NOTES_PLACEHOLDER);
    }

    private void setupTextFieldPlaceholder(javax.swing.JTextField textField, String placeholder) {
        // Don't setup placeholder if field already has real data
        if (!textField.getForeground().equals(TEXT_COLOR)) {
        textField.setForeground(PLACEHOLDER_COLOR);
        textField.setText(placeholder);
        }

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholder) && 
                    textField.getForeground().equals(PLACEHOLDER_COLOR)) {
                    textField.setText("");
                    textField.setForeground(TEXT_COLOR);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setForeground(PLACEHOLDER_COLOR);
                    textField.setText(placeholder);
                }
            }
        });
    }

    private void setupTextAreaPlaceholder(javax.swing.JTextArea textArea, String placeholder) {
        // Don't setup placeholder if field already has real data
        if (!textArea.getForeground().equals(TEXT_COLOR)) {
        textArea.setForeground(PLACEHOLDER_COLOR);
        textArea.setText(placeholder);
        }

        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textArea.getText().equals(placeholder) && 
                    textArea.getForeground().equals(PLACEHOLDER_COLOR)) {
                    textArea.setText("");
                    textArea.setForeground(TEXT_COLOR);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textArea.getText().isEmpty()) {
                    textArea.setForeground(PLACEHOLDER_COLOR);
                    textArea.setText(placeholder);
                }
            }
        });
    }

    // NEW: Helper to get input, ignoring placeholder text
    private String getInputText(javax.swing.JTextField textField, String placeholder) {
        String text = textField.getText().trim();
        return text.equals(placeholder) || text.isEmpty() ? null : text;
    }

    private String getInputText(javax.swing.JTextArea textArea, String placeholder) {
        String text = textArea.getText().trim();
        // Also check against "(Optional)" text for notes
        return text.equals(placeholder) || text.isEmpty() || text.equals("(Optional)") ? null : text;
    }

    // NEW: Method to collect data and create Reminder
    private Reminder createReminder() throws IllegalArgumentException {
        // 1. Validate mandatory fields
        String name = getInputText(NameTextField, NAME_PLACEHOLDER);
        String question = getInputText(QuestionTextField, QUESTION_PLACEHOLDER);

        if (name == null) {
            throw new IllegalArgumentException("Habit name cannot be empty.");
        }
        if (question == null) {
            throw new IllegalArgumentException("Habit question cannot be empty.");
        }

        String freqStr = (String) FreqButton.getSelectedItem();
        if (freqStr == null) {
            throw new IllegalArgumentException("Frequency must be selected.");
        }
        Frequency frequency = Frequency.valueOf(freqStr.toUpperCase());

        // 2. Parse time
        LocalTime time = null;
        String hhStr = getInputText(ClockTextFieldHH, CLOCK_HH_PLACEHOLDER);
        String mmStr = getInputText(ClockTextFieldMM, CLOCK_MM_PLACEHOLDER);
        String ampm = (String) AMPMCombo.getSelectedItem();

        if (hhStr != null && mmStr != null) {
            try {
                int hour = Integer.parseInt(hhStr);
                int minute = Integer.parseInt(mmStr);

                if (hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                    throw new NumberFormatException();
                }

                if (ampm.equals("PM") && hour != 12) {
                    hour += 12;
                } else if (ampm.equals("AM") && hour == 12) {
                    hour = 0; // 12 AM is 00:xx in 24hr format
                }

                time = LocalTime.of(hour, minute);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid time format.");
            }
        }

        if (time == null) {
            throw new IllegalArgumentException("Reminder time must be set.");
        }

        // Get DaysOfWeek for WEEKLY frequency
        Set<DayOfWeek> daysOfWeek = new HashSet<>();
        if (frequency == Frequency.WEEKLY) {
            if (monCheck.isSelected()) daysOfWeek.add(DayOfWeek.MONDAY);
            if (tueCheck.isSelected()) daysOfWeek.add(DayOfWeek.TUESDAY);
            if (wedCheck.isSelected()) daysOfWeek.add(DayOfWeek.WEDNESDAY);
            if (thuCheck.isSelected()) daysOfWeek.add(DayOfWeek.THURSDAY);
            if (friCheck.isSelected()) daysOfWeek.add(DayOfWeek.FRIDAY);
            if (satCheck.isSelected()) daysOfWeek.add(DayOfWeek.SATURDAY);
            if (sunCheck.isSelected()) daysOfWeek.add(DayOfWeek.SUNDAY);

            if (daysOfWeek.isEmpty()) {
                throw new IllegalArgumentException("Must select at least one day for Weekly frequency.");
            }
        }

        // 4. Create and populate Reminder object
        Reminder reminder = new Reminder();
        reminder.setType(HabitType.YES_NO);
        reminder.setName(name);
        reminder.setText(question);
        reminder.setFrequency(frequency);
        reminder.setTime(time);
        reminder.setDaysOfWeek(daysOfWeek);
        reminder.setNotes(getInputText(notesArea, NOTES_PLACEHOLDER));
        // Measurable fields are left at their default values (0.0, null, null)

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
            String freqDisplay = frequency.toString().charAt(0) + 
                               frequency.toString().substring(1).toLowerCase();
            FreqButton.setSelectedItem(freqDisplay);
        }

        // Set days of week if weekly
        if (frequency == Frequency.WEEKLY && daysOfWeek != null) {
            daysPanel.setVisible(true);
            monCheck.setSelected(daysOfWeek.contains(DayOfWeek.MONDAY));
            tueCheck.setSelected(daysOfWeek.contains(DayOfWeek.TUESDAY));
            wedCheck.setSelected(daysOfWeek.contains(DayOfWeek.WEDNESDAY));
            thuCheck.setSelected(daysOfWeek.contains(DayOfWeek.THURSDAY));
            friCheck.setSelected(daysOfWeek.contains(DayOfWeek.FRIDAY));
            satCheck.setSelected(daysOfWeek.contains(DayOfWeek.SATURDAY));
            sunCheck.setSelected(daysOfWeek.contains(DayOfWeek.SUNDAY));
        }

        // Set time
        if (time != null) {
            int hour = time.getHour();
            int minute = time.getMinute();
            String ampm = "AM";

            if (hour >= 12) {
                ampm = "PM";
                if (hour > 12) hour -= 12;
                }
            if (hour == 0) hour = 12;

            ClockTextFieldHH.setText(String.format("%02d", hour));
            ClockTextFieldHH.setForeground(TEXT_COLOR);
            ClockTextFieldMM.setText(String.format("%02d", minute));
            ClockTextFieldMM.setForeground(TEXT_COLOR);
            AMPMCombo.setSelectedItem(ampm);
        }

        // Set notes
        if (notes != null && !notes.isEmpty()) {
            notesArea.setText(notes);
            notesArea.setForeground(TEXT_COLOR);
        }

        // Change Save button text
        SaveButton.setText("Update");
    }

    private void loadReminderData(Reminder rem) {
        // 1. Name Field
        NameTextField.setText(rem.getName());
        NameTextField.setForeground(Color.BLACK);

        if (rem.getText() != null) {
        QuestionTextField.setText(rem.getText());
            QuestionTextField.setForeground(TEXT_COLOR);
        }

        if (rem.getFrequency() != null) {
            String freqDisplay = rem.getFrequency().toString().charAt(0) + 
                               rem.getFrequency().toString().substring(1).toLowerCase();
            FreqButton.setSelectedItem(freqDisplay);
        }

        if (rem.getFrequency() == Frequency.WEEKLY && rem.getDaysOfWeek() != null) {
            daysPanel.setVisible(true);
        for (Component comp : daysPanel.getComponents()) {
            if (comp instanceof JCheckBox check) {
                // DayOfWeek enum names must match JCheckBox text exactly (e.g., "Monday")
                try {
                    DayOfWeek day = DayOfWeek.valueOf(check.getText().toUpperCase());
                    check.setSelected(rem.getDaysOfWeek().contains(day));
                } catch (IllegalArgumentException e) {
                        // Ignore non-matching checkboxes
                }
            }
        }
        }

        // 5. Time
        LocalTime time = rem.getTime();
        if (time != null) {
            int hour = time.getHour();
            int minute = time.getMinute();

            // Convert to 12-hour format for the UI fields
            String ampm = (hour < 12) ? "AM" : "PM";
            if (hour == 0) {
                hour = 12; // Midnight
            } else if (hour > 12) {
                hour -= 12; // PM hours
            }

            ClockTextFieldHH.setText(String.format("%02d", hour));
            ClockTextFieldMM.setText(String.format("%02d", minute));
            AMPMCombo.setSelectedItem(ampm);

            ClockTextFieldHH.setForeground(Color.BLACK);
            ClockTextFieldMM.setForeground(Color.BLACK);
        }

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

        FreqButton.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Daily", "Weekly", "Monthly"}));
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
        String habitName = NameTextField.getText().trim();

        if (habitName.isEmpty() || habitName.equals(NAME_PLACEHOLDER)) {
            JOptionPane.showMessageDialog(this, "Please enter a habit name.");
            return;
        }

        try {
            Reminder reminder = createReminder();
            String notes = getInputText(notesArea, NOTES_PLACEHOLDER);

            ReminderManager.getInstance().addReminder(reminder);

            if (isEditMode) {
                // Update existing habit
                dashboard.updateHabit(
            editingRowIndex,
            reminder.getName(),      // oldName
            reminder.getName(),      // newName
            true,                    // isMeasurable
            reminder.getUnit(),      // newUnit
            reminder.getTargetValue(), // newTarget
            reminder.getThreshold(), // newThreshold
            reminder.getText(),      // question
            reminder.getFrequency(), // frequency
            reminder.getDaysOfWeek(),// daysOfWeek
            notes                    // notes
                );
                JOptionPane.showMessageDialog(this, 
                    "Habit '" + reminder.getName() + "' updated successfully!");
            } else {
                // Add new habit
                dashboard.addHabitRow(
                        reminder.getName(),
                        reminder.getText(),
                        null,
                        0.0,
                        null,
                        reminder.getFrequency(),
                        reminder.getDaysOfWeek(),
                        notes
                );
                JOptionPane.showMessageDialog(this, 
                    "Habit '" + reminder.getName() + "' added successfully!");
            }

            this.dispose();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), 
                "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_SaveButtonActionPerformed

    private void QuestionTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QuestionTextFieldActionPerformed
        // TODO add your handling code here:
        String Question = QuestionTextField.getText().trim();

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
                                case "MON":
                                case "MONDAY":
                                    days.add(java.time.DayOfWeek.MONDAY);
                                    break;
                                case "TUE":
                                case "TUES":
                                case "TUESDAY":
                                    days.add(java.time.DayOfWeek.TUESDAY);
                                    break;
                                case "WED":
                                case "WEDNESDAY":
                                    days.add(java.time.DayOfWeek.WEDNESDAY);
                                    break;
                                case "THU":
                                case "THURSDAY":
                                    days.add(java.time.DayOfWeek.THURSDAY);
                                    break;
                                case "FRI":
                                case "FRIDAY":
                                    days.add(java.time.DayOfWeek.FRIDAY);
                                    break;
                                case "SAT":
                                case "SATURDAY":
                                    days.add(java.time.DayOfWeek.SATURDAY);
                                    break;
                                case "SUN":
                                case "SUNDAY":
                                    days.add(java.time.DayOfWeek.SUNDAY);
                                    break;
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
        } catch (Exception ex) {
            ex.printStackTrace();
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
