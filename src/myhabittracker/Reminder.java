/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package myhabittracker;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author conrad
 */
public class Reminder {


/**
 * Simple reminder model.
 */
    public enum Frequency { DAILY, WEEKLY }

    private final String id;
    private String text;
    private Frequency frequency;
    private LocalTime time;              // time of day to fire
    private Set<DayOfWeek> daysOfWeek;   // used when frequency == WEEKLY
    private boolean enabled = true;

    // Tracks last date when fired to avoid duplicate fires same day
    private LocalDate lastFiredDate = null;

    public Reminder() {
        this.id = UUID.randomUUID().toString();
        this.daysOfWeek = new HashSet<>();
    }

    // --- Getters / setters ---
    public String getId() { return id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public Set<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDate getLastFiredDate() { return lastFiredDate; }
    public void setLastFiredDate(LocalDate lastFiredDate) { this.lastFiredDate = lastFiredDate; }

    // Utility: check whether it should fire on given date/time
    public boolean shouldFire(java.time.LocalDateTime now) {
        if (!enabled || time == null) return false;
        if (!now.toLocalTime().equals(time) && now.toLocalTime().isBefore(time)) return false;
        // We'll compare hours/minutes, not seconds (the scheduler uses minute resolution).
        java.time.LocalTime nowTime = now.toLocalTime().withSecond(0).withNano(0);
        if (!nowTime.equals(time.withSecond(0).withNano(0))) return false;

        java.time.LocalDate today = now.toLocalDate();
        if (lastFiredDate != null && lastFiredDate.equals(today)) return false; // already fired today

        if (frequency == Frequency.DAILY) {
            return true;
        } else if (frequency == Frequency.WEEKLY) {
            java.time.DayOfWeek dow = now.getDayOfWeek();
            return daysOfWeek.contains(dow);
        }
        return false;
    }
}

