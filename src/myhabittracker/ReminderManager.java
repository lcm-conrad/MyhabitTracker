/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package myhabittracker;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
/**
 *
 * @author conrad
 */
public class ReminderManager {
/**
 * Singleton manager: load/save reminders to Excel file and fire notifications.
 */
    private static ReminderManager instance;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<Reminder> reminders = new CopyOnWriteArrayList<>();
    private final File dataFile;
    private TrayIcon trayIcon = null;

    // check rate (seconds)
    private static final int CHECK_INTERVAL_SECONDS = 30;

    private ReminderManager(File dataFile) {
        this.dataFile = dataFile;
        setupTrayIcon();
    }

    public static synchronized ReminderManager getInstance() {
        if (instance == null) {
            File appData = new File(System.getProperty("user.home"), ".myhabittracker");
            if (!appData.exists()) appData.mkdirs();
            File dataFile = new File(appData, "reminders.xlsx");
            instance = new ReminderManager(dataFile);
        }
        return instance;
    }

    // initialize: load from disk and start scheduler
    public void start() {
        loadFromExcel();
        startScheduler();
    }

    public List<Reminder> getReminders() {
        return Collections.unmodifiableList(reminders);
    }

    public void addReminder(Reminder r) {
        reminders.add(r);
        saveToExcel();
    }

    public void removeReminder(String id) {
        reminders.removeIf(r -> r.getId().equals(id));
        saveToExcel();
    }

    public void updateReminder(Reminder r) {
        // simple replace by id
        for (int i=0;i<reminders.size();i++) {
            if (reminders.get(i).getId().equals(r.getId())) {
                reminders.set(i, r);
                saveToExcel();
                return;
            }
        }
    }

    // ---------- Scheduler ----------
    private void startScheduler() {
        scheduler.scheduleAtFixedRate(this::checkAndFire, 5, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void checkAndFire() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowRounded = now.withSecond(0).withNano(0);
        for (Reminder r : reminders) {
            try {
                if (r.shouldFire(nowRounded)) {
                    fireReminder(r);
                    r.setLastFiredDate(nowRounded.toLocalDate());
                    // save to persist last fired date to avoid double-fire if desired
                    saveToExcel();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ---------- Notifications ----------
    private void setupTrayIcon() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = null;
            // try to load a resource icon
            try (InputStream is = getClass().getResourceAsStream("/tray_icon.png")) {
                if (is != null) {
                    BufferedImage bi = ImageIO.read(is);
                    image = bi;
                }
            } catch (Exception ignore) {}
            if (image == null) {
                // create a tiny generated image
                BufferedImage bi = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = bi.createGraphics();
                g.setColor(Color.BLUE);
                g.fillOval(0,0,16,16);
                g.dispose();
                image = bi;
            }
            trayIcon = new TrayIcon(image, "MyHabitTracker");
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                trayIcon = null;
            }
        } catch (Throwable t) {
            trayIcon = null;
        }
    }

    private void fireReminder(Reminder r) {
        String title = "Habit reminder";
        String msg = r.getText() == null ? "Reminder" : r.getText();
        if (trayIcon != null) {
            trayIcon.displayMessage(title, msg, TrayIcon.MessageType.INFO);
        } else {
            // fallback on EDT
            javax.swing.SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE)
            );
        }
    }

    // ---------- Persistence with Apache POI ----------
    private void loadFromExcel() {
        if (!dataFile.exists()) return;
        try (FileInputStream fis = new FileInputStream(dataFile);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            reminders.clear();
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; } // header
                Cell idCell = row.getCell(0);
                if (idCell == null) continue;
                Reminder r = new Reminder();
                // ID (if present)
                String id = getStringCell(row,0);
                // if ID present we need to set it - but Reminder generates a new id; we use reflection-like trick
                // We'll keep the generated UUID but store ID in a custom field? To keep it simple, ignore ID preservation
                r.setText(getStringCell(row,1));
                String freqStr = getStringCell(row,2);
                r.setFrequency("WEEKLY".equalsIgnoreCase(freqStr) ? Reminder.Frequency.WEEKLY : Reminder.Frequency.DAILY);
                String timeStr = getStringCell(row,3);
                if (timeStr != null && !timeStr.isEmpty()) {
                    r.setTime(LocalTime.parse(timeStr));
                }
                String days = getStringCell(row,4);
                if (days != null && !days.isEmpty()) {
                    Set<DayOfWeek> dow = new HashSet<>();
                    for (String s : days.split(",")) {
                        s = s.trim();
                        if (s.isEmpty()) continue;
                        try {
                            dow.add(DayOfWeek.valueOf(s.toUpperCase()));
                        } catch (Exception ign) {}
                    }
                    r.setDaysOfWeek(dow);
                }
                String lastFired = getStringCell(row,5);
                if (lastFired != null && !lastFired.isEmpty()) {
                    r.setLastFiredDate(LocalDate.parse(lastFired));
                }
                String enabledCell = getStringCell(row,6);
                if ("false".equalsIgnoreCase(enabledCell)) r.setEnabled(false);
                reminders.add(r);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getStringCell(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (c == null) return "";
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue();
        if (c.getCellType() == CellType.NUMERIC) return String.valueOf(c.getNumericCellValue());
        if (c.getCellType() == CellType.BOOLEAN) return String.valueOf(c.getBooleanCellValue());
        return "";
    }

    private void saveToExcel() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("reminders");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("id");
            header.createCell(1).setCellValue("text");
            header.createCell(2).setCellValue("frequency");
            header.createCell(3).setCellValue("time");
            header.createCell(4).setCellValue("days");
            header.createCell(5).setCellValue("lastFiredDate");
            header.createCell(6).setCellValue("enabled");

            for (Reminder rem : reminders) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(rem.getId());
                row.createCell(1).setCellValue(rem.getText() == null ? "" : rem.getText());
                row.createCell(2).setCellValue(rem.getFrequency() == Reminder.Frequency.WEEKLY ? "WEEKLY" : "DAILY");
                row.createCell(3).setCellValue(rem.getTime() == null ? "" : rem.getTime().toString());
                StringBuilder days = new StringBuilder();
                if (rem.getDaysOfWeek() != null) {
                    boolean first = true;
                    for (DayOfWeek d : rem.getDaysOfWeek()) {
                        if (!first) days.append(",");
                        days.append(d.toString());
                        first = false;
                    }
                }
                row.createCell(4).setCellValue(days.toString());
                row.createCell(5).setCellValue(rem.getLastFiredDate() == null ? "" : rem.getLastFiredDate().toString());
                row.createCell(6).setCellValue(rem.isEnabled());
            }

            // ensure parent exists
            if (!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(dataFile)) {
                wb.write(fos);
            }
        } catch (Exception ex) {
        }
    }
}
