package myhabittracker;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Simple reminder model.
 * @author conrad
 */
public class Reminder {

    public enum Frequency {
        DAILY, WEEKLY, MONTHLY // Added MONTHLY as seen in JFrames
    }
    
    // NEW: Enum to differentiate habit types
    public enum HabitType {
        YES_NO, MEASURABLE
    }

    // FIX: Removed 'final' to allow ID to be set when loading from a file.
    private String id;
    private String name; // NEW: Habit Name
    private String text; // Used for Question/Prompt
    private Frequency frequency;
    private LocalTime time;              // time of day to fire
    private Set<DayOfWeek> daysOfWeek;   // used when frequency == WEEKLY
    private boolean enabled = true;

    // Tracks last date when fired to avoid duplicate fires same day
    private LocalDate lastFiredDate = null;
    
    // NEW FIELDS for Habit Tracking
    private HabitType type;
    private double targetValue;
    private String unit;
    private String threshold; // e.g., "At least", "At most"
    private String notes;

    public Reminder() {
        this.id = UUID.randomUUID().toString();
        this.daysOfWeek = new HashSet<>();
    }

    // --- Getters / setters ---
    public String getId() {
        return id;
    }

    // FIX: Added setId to allow persistence layer to set the ID on load.
    public void setId(String id) {
        this.id = id;
    }

    public String getName() { // NEW
        return name;
    }

    public void setName(String name) { // NEW
        this.name = name;
    }
    
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDate getLastFiredDate() {
        return lastFiredDate;
    }

    public void setLastFiredDate(LocalDate lastFiredDate) {
        this.lastFiredDate = lastFiredDate;
    }
    
    // NEW GETTERS / SETTERS for Habit Type Data
    public HabitType getType() { 
        return type; 
    }
    public void setType(HabitType type) { 
        this.type = type; 
    }
    public double getTargetValue() { 
        return targetValue; 
    }
    public void setTargetValue(double targetValue) { 
        this.targetValue = targetValue; 
    }
    public String getUnit() { 
        return unit; 
    }
    public void setUnit(String unit) { 
        this.unit = unit; 
    }
    public String getThreshold() { 
        return threshold; 
    }
    public void setThreshold(String threshold) { 
        this.threshold = threshold; 
    }
    public String getNotes() { 
        return notes; 
    }
    public void setNotes(String notes) { 
        this.notes = notes; 
    }

    /**
     * IMPROVEMENT: Redesigned the firing logic to be robust.
     * Checks if the reminder should fire at the given time, considering a check interval.
     * This avoids the bug of missing the exact second of the reminder.
     *
     * @param now The current date and time.
     * @param checkInterval The interval at which the scheduler runs.
     * @return true if the reminder should fire, false otherwise.
     */
    public boolean shouldFire(LocalDateTime now, Duration checkInterval) {
        if (!enabled || time == null || frequency == null) {
            return false;
        }

        LocalDate today = now.toLocalDate();

        // 1. Check if the reminder has already fired today
        if (lastFiredDate != null && lastFiredDate.equals(today)) {
            return false;
        }

        // 2. Check if today is a valid day for the reminder
        if (frequency == Frequency.WEEKLY) {
            if (daysOfWeek == null || !daysOfWeek.contains(today.getDayOfWeek())) {
                return false;
            }
        }
        // For DAILY, every day is valid.
        // NOTE: MONTHLY logic is not fully implemented but the basic DAILY/WEEKLY check remains.

        // 3. Check if the reminder time is within the last check interval.
        // This is the core logic fix.
        LocalTime reminderTime = this.time.withSecond(0).withNano(0);
        LocalTime nowTime = now.toLocalTime().withSecond(0).withNano(0);
        LocalTime checkStartTime = nowTime.minus(checkInterval);

        // A reminder at 10:00 should fire if the check runs at 10:00, 10:01, etc.
        // It means the reminder time is NOT after the current time,
        // and it IS after the time the last check started.
        // This handles the case where a check runs at e.g. 10:00:15 for a 10:00:00 reminder.

        // Handle midnight wrap-around issue
        if (checkStartTime.isAfter(nowTime)) {
            // Check interval spans across midnight. If the reminder time is exactly 
            // the start time or between the start and end (which is 'now').
            return reminderTime.equals(checkStartTime) || reminderTime.isAfter(checkStartTime) || reminderTime.equals(nowTime);
        } else {
            // Check interval is within the same day (e.g., 9:59 to 10:00)
            // It fires if:
            // 1. reminderTime is EQUAL to nowTime (e.g., 10:00 == 10:00)
            // 2. OR reminderTime is strictly BETWEEN checkStartTime (09:59) and nowTime (10:00)
            return (reminderTime.equals(nowTime) || reminderTime.isBefore(nowTime)) && 
                   (reminderTime.equals(checkStartTime) || reminderTime.isAfter(checkStartTime));
        }
    }
}