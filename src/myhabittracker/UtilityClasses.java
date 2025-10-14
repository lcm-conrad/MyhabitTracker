/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package myhabittracker;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Comprehensive utility class containing all helper methods for the habit tracker application.
 * 
 * @author asus
 */
public class UtilityClasses {
    
    // ===== INPUT VALIDATION UTILITIES =====
    
    public static class InputValidator {
        
        public static String validateHabitName(String name, String placeholder) {
            if (name == null || name.trim().isEmpty() || name.equals(placeholder)) {
                throw new IllegalArgumentException("Habit name cannot be empty.");
            }
            return name.trim();
        }
        
        public static String validateQuestion(String question, String placeholder) {
            if (question == null || question.trim().isEmpty() || question.equals(placeholder)) {
                throw new IllegalArgumentException("Habit question cannot be empty.");
            }
            return question.trim();
        }
        
        public static String validateUnit(String unit, String placeholder) {
            if (unit == null || unit.trim().isEmpty() || unit.equals(placeholder)) {
                throw new IllegalArgumentException("Unit cannot be empty for measurable habits.");
            }
            return unit.trim();
        }
        
        public static double validateTargetNumber(String targetStr, String placeholder) {
            if (targetStr == null || targetStr.trim().isEmpty() || targetStr.equals(placeholder)) {
                throw new IllegalArgumentException("Target number cannot be empty for measurable habits.");
            }
            try {
                return Double.parseDouble(targetStr.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Please enter a valid number for the target.");
            }
        }
        
        public static String validateNotes(String notes, String placeholder) {
            return (notes == null || notes.equals(placeholder)) ? "" : notes.trim();
        }
        
        public static void validateWeeklyDays(Set<DayOfWeek> daysOfWeek) {
            if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                throw new IllegalArgumentException("Must select at least one day for Weekly frequency.");
            }
        }
    }
    
    // ===== TIME PARSING UTILITIES =====
    
    public static class TimeParser {
        
        public static LocalTime parse12HourTime(String hourStr, String minuteStr, String ampm) {
            try {
                int hour = Integer.parseInt(hourStr.trim());
                int minute = Integer.parseInt(minuteStr.trim());
                
                if (hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                    throw new IllegalArgumentException("Invalid time format. Hours: 1-12, Minutes: 0-59");
                }
                
                // Convert to 24-hour format
                if ("PM".equals(ampm) && hour != 12) {
                    hour += 12;
                } else if ("AM".equals(ampm) && hour == 12) {
                    hour = 0;
                }
                
                return LocalTime.of(hour, minute);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Please enter valid numbers for time.");
            }
        }
        
        public static String[] to12HourFormat(LocalTime time) {
            if (time == null) {
                return new String[]{"00", "00", "AM"};
            }
            
            int hour = time.getHour();
            int minute = time.getMinute();
            String ampm = "AM";
            
            if (hour >= 12) {
                ampm = "PM";
                if (hour > 12) hour -= 12;
            }
            if (hour == 0) hour = 12;
            
            return new String[]{
                String.format("%02d", hour),
                String.format("%02d", minute),
                ampm
            };
        }
    }
    
    // ===== DAY OF WEEK UTILITIES =====
    
    public static class DayOfWeekParser {
        
        public static Set<DayOfWeek> getSelectedDays(JPanel daysPanel) {
            Set<DayOfWeek> daysOfWeek = new HashSet<>();
            
            for (Component comp : daysPanel.getComponents()) {
                if (comp instanceof JCheckBox checkBox && checkBox.isSelected()) {
                    String dayText = checkBox.getText().toUpperCase();
                    switch (dayText) {
                        case "MON": daysOfWeek.add(DayOfWeek.MONDAY); break;
                        case "TUE": daysOfWeek.add(DayOfWeek.TUESDAY); break;
                        case "WED": daysOfWeek.add(DayOfWeek.WEDNESDAY); break;
                        case "THU": daysOfWeek.add(DayOfWeek.THURSDAY); break;
                        case "FRI": daysOfWeek.add(DayOfWeek.FRIDAY); break;
                        case "SAT": daysOfWeek.add(DayOfWeek.SATURDAY); break;
                        case "SUN": daysOfWeek.add(DayOfWeek.SUNDAY); break;
                    }
                }
            }
            return daysOfWeek;
        }
        
        public static void setSelectedDays(JPanel daysPanel, Set<DayOfWeek> daysOfWeek) {
            if (daysOfWeek == null) return;
            
            for (Component comp : daysPanel.getComponents()) {
                if (comp instanceof JCheckBox checkBox) {
                    String dayText = checkBox.getText().toUpperCase();
                    DayOfWeek day = null;
                    switch (dayText) {
                        case "MON": day = DayOfWeek.MONDAY; break;
                        case "TUE": day = DayOfWeek.TUESDAY; break;
                        case "WED": day = DayOfWeek.WEDNESDAY; break;
                        case "THU": day = DayOfWeek.THURSDAY; break;
                        case "FRI": day = DayOfWeek.FRIDAY; break;
                        case "SAT": day = DayOfWeek.SATURDAY; break;
                        case "SUN": day = DayOfWeek.SUNDAY; break;
                    }
                    checkBox.setSelected(day != null && daysOfWeek.contains(day));
                }
            }
        }
    }
    
    // ===== FREQUENCY FORMATTING UTILITIES =====
    
    public static class FrequencyFormatter {
        
        public static Reminder.Frequency parse(String frequencyStr) {
            if (frequencyStr == null) {
                return Reminder.Frequency.DAILY;
            }
            return Reminder.Frequency.valueOf(frequencyStr.toUpperCase());
        }
        
        public static String format(Reminder.Frequency frequency) {
            if (frequency == null) {
                return "Daily";
            }
            return frequency.toString().charAt(0) + frequency.toString().substring(1).toLowerCase();
        }
    }
    
    // ===== PLACEHOLDER UTILITIES =====
    
    public static class PlaceholderUtils {
        private static final Color PLACEHOLDER_COLOR = new Color(204, 204, 204);
        private static final Color TEXT_COLOR = Color.BLACK;
        
        public static void setupTextFieldPlaceholder(JTextField textField, String placeholder) {
            // Skip if field already has real data
            if (textField.getForeground().equals(TEXT_COLOR) && !textField.getText().isEmpty()) {
                return;
            }
            
            textField.setForeground(PLACEHOLDER_COLOR);
            textField.setText(placeholder);
            
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
                    if (textField.getText().trim().isEmpty()) {
                        textField.setForeground(PLACEHOLDER_COLOR);
                        textField.setText(placeholder);
                    }
                }
            });
        }
        
        public static void setupTextAreaPlaceholder(JTextArea textArea, String placeholder) {
            // Skip if field already has real data
            if (textArea.getForeground().equals(TEXT_COLOR) && !textArea.getText().isEmpty()) {
                return;
            }
            
            textArea.setForeground(PLACEHOLDER_COLOR);
            textArea.setText(placeholder);
            
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
                    if (textArea.getText().trim().isEmpty()) {
                        textArea.setForeground(PLACEHOLDER_COLOR);
                        textArea.setText(placeholder);
                    }
                }
            });
        }
        
        public static String getInputText(JTextField textField, String placeholder) {
            String text = textField.getText().trim();
            return (text.isEmpty() || text.equals(placeholder)) ? null : text;
        }
        
        public static String getInputText(JTextArea textArea, String placeholder) {
            String text = textArea.getText().trim();
            return (text.isEmpty() || text.equals(placeholder) || text.equals("(Optional)")) ? null : text;
        }
    }
    
    // ===== UI HELPER UTILITIES =====
    
    public static class UIUtils {
        
        public static void showErrorDialog(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        public static void showInfoDialog(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
        }
        
        public static void showSuccessDialog(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
        }
        
        public static boolean confirmDialog(Component parent, String message) {
            int result = JOptionPane.showConfirmDialog(parent, message, "Confirmation", 
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            return result == JOptionPane.YES_OPTION;
        }
    }
}
