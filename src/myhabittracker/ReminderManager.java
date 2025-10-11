package myhabittracker;

import myhabittracker.Reminder.HabitType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

/**
 * Singleton manager: load/save reminders to Excel file and fire notifications.
 *
 * @author conrad
 */
public class ReminderManager {

    private static ReminderManager instance;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<Reminder> reminders = new CopyOnWriteArrayList<>();
    private final File dataFile;
    private TrayIcon trayIcon = null;

    // check rate (seconds)
    private static final int CHECK_INTERVAL_SECONDS = 30;

    // IMPROVEMENT: Using constants for column indices makes the code much cleaner
    // and less error-prone if the Excel structure changes.
    private static final int COL_ID = 0;
    private static final int COL_TEXT = 1; // Question/Prompt
    private static final int COL_FREQUENCY = 2;
    private static final int COL_TIME = 3;
    private static final int COL_DAYS = 4;
    private static final int COL_LAST_FIRED = 5;
    private static final int COL_ENABLED = 6;
    
    // NEW COLUMNS FOR HABIT DATA
    private static final int COL_TYPE = 7;
    private static final int COL_NAME = 8;
    private static final int COL_TARGET = 9;
    private static final int COL_UNIT = 10;
    private static final int COL_THRESHOLD = 11;
    private static final int COL_NOTES = 12;

    private ReminderManager(File dataFile) {
        this.dataFile = dataFile;
        setupTrayIcon();
        loadFromExcel(); // Load reminders on startup
        startScheduler(); // Start the background checker
    }

    public static synchronized ReminderManager getInstance() {
        if (instance == null) {
            File appData = new File(System.getProperty("user.home"), ".myhabittracker");
            if (!appData.exists()) {
                appData.mkdirs();
            }
            File dataFile = new File(appData, "reminders.xlsx");
            instance = new ReminderManager(dataFile);
        }
        return instance;
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
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getId().equals(r.getId())) {
                reminders.set(i, r);
                saveToExcel();
                return;
            }
        }
    }

    // ---------- Scheduler ----------
    public void startScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkReminders();
            } catch (Exception e) {
                System.err.println("Error in reminder check thread: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void checkReminders() {
        LocalDateTime now = LocalDateTime.now();
        // IMPROVEMENT: Pass the interval to the check logic. We add a small buffer for safety.
        Duration checkInterval = Duration.ofSeconds(CHECK_INTERVAL_SECONDS + 2);

        // PERFORMANCE FIX: Use a flag to save only once after checking all reminders.
        boolean needsSave = false;

        for (Reminder reminder : reminders) {
            // The core logic is now more robust.
            if (reminder.shouldFire(now, checkInterval)) {
                fireReminder(reminder);
                reminder.setLastFiredDate(now.toLocalDate());
                needsSave = true; // Mark that a change has occurred.
            }
        }

        // PERFORMANCE FIX: Only write to the file if one or more reminders were updated.
        if (needsSave) {
            saveToExcel();
        }
    }

    private void fireReminder(Reminder reminder) {
        if (trayIcon != null) {
            trayIcon.displayMessage( null,
                    reminder.getName() + reminder.getText() + " at "
                    + reminder.getTime().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")),
                    TrayIcon.MessageType.INFO
            );
        } else {
            System.out.println("REMINDER FIRED: " + reminder.getName() + " - " + reminder.getText() + " at " + reminder.getTime());
        }
    }

    // ---------- Notifications ----------
    private void setupTrayIcon() {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray not supported on this platform");
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image;
            try (InputStream is = getClass().getResourceAsStream("/tray_icon.png")) {
                if (is != null) {
                    image = ImageIO.read(is);
                } else {
                    throw new IOException("Icon resource not found");
                }
            } catch (Exception e) {
                System.out.println("Could not load tray icon from resources, creating a default one. Error: " + e.getMessage());
                BufferedImage bi = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = bi.createGraphics();
                g.setColor(Color.BLUE);
                g.fillOval(0, 0, 16, 16);
                g.dispose();
                image = bi;
            }
            trayIcon = new TrayIcon(image, "MyHabitTracker");
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("Failed to add tray icon: " + e.getMessage());
                trayIcon = null;
            }
        } catch (Throwable t) {
            System.err.println("Error setting up tray icon: " + t.getMessage());
            trayIcon = null;
        }
    }

    // ---------- Persistence with Apache POI ----------
    // FIX: Synchronized to prevent race conditions while reloading the list.
    private synchronized void loadFromExcel() {
        if (!dataFile.exists()) {
            System.out.println("No existing reminders file found at: " + dataFile.getAbsolutePath());
            return;
        }
        try (FileInputStream fis = new FileInputStream(dataFile); Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            reminders.clear(); // Using an iterator is safer than a for-each loop when skipping the first element.
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip header row
            }
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell idCell = row.getCell(COL_ID);
                if (idCell == null || getStringCell(row, COL_ID).isEmpty()) {
                    continue;
                }
                Reminder r = new Reminder();
                // Preserve the ID from the file
                r.setId(getStringCell(row, COL_ID));
                r.setText(getStringCell(row, COL_TEXT));
                String freqStr = getStringCell(row, COL_FREQUENCY);
                if (!freqStr.isEmpty()) {
                    r.setFrequency(Reminder.Frequency.valueOf(freqStr));
                }
                String timeStr = getStringCell(row, COL_TIME);
                if (!timeStr.isEmpty()) {
                    r.setTime(LocalTime.parse(timeStr));
                }
                String daysStr = getStringCell(row, COL_DAYS);
                if (!daysStr.isEmpty()) {
                    Set<DayOfWeek> days = new HashSet<>();
                    for (String day : daysStr.split(",")) {
                        days.add(DayOfWeek.valueOf(day));
                    }
                    r.setDaysOfWeek(days);
                }
                String lastFiredStr = getStringCell(row, COL_LAST_FIRED);
                if (!lastFiredStr.isEmpty()) {
                    r.setLastFiredDate(LocalDate.parse(lastFiredStr));
                }
                // Default to true if not present (older files)
                String enabledStr = getStringCell(row, COL_ENABLED);
                r.setEnabled(enabledStr.isEmpty() || Boolean.parseBoolean(enabledStr));

                // NEW FIELDS LOADING
                String typeStr = getStringCell(row, COL_TYPE);
                try {
                    // COL_TYPE might be empty for old reminders
                    r.setType(typeStr.isEmpty() ? HabitType.YES_NO : HabitType.valueOf(typeStr));
                } catch (IllegalArgumentException ex) {
                    r.setType(HabitType.YES_NO); // Default to YES_NO if type is invalid/new column not yet used
                }
                
                r.setName(getStringCell(row, COL_NAME));
                r.setTargetValue(getDoubleCell(row, COL_TARGET));
                r.setUnit(getStringCell(row, COL_UNIT));
                r.setThreshold(getStringCell(row, COL_THRESHOLD));
                r.setNotes(getStringCell(row, COL_NOTES));

                reminders.add(r);
            }
        } catch (Exception ex) {
            System.err.println("Error loading reminders from Excel: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // FIX: Added synchronization to prevent race condition during save
    private synchronized void saveToExcel() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Reminders");
            Row header = sheet.createRow(0);
            header.createCell(COL_ID).setCellValue("ID");
            header.createCell(COL_TEXT).setCellValue("TEXT");
            header.createCell(COL_FREQUENCY).setCellValue("FREQUENCY");
            header.createCell(COL_TIME).setCellValue("TIME");
            header.createCell(COL_DAYS).setCellValue("DAYS");
            header.createCell(COL_LAST_FIRED).setCellValue("LAST_FIRED");
            header.createCell(COL_ENABLED).setCellValue("ENABLED");
            // NEW HEADERS
            header.createCell(COL_TYPE).setCellValue("TYPE");
            header.createCell(COL_NAME).setCellValue("NAME");
            header.createCell(COL_TARGET).setCellValue("TARGET_VALUE");
            header.createCell(COL_UNIT).setCellValue("UNIT");
            header.createCell(COL_THRESHOLD).setCellValue("THRESHOLD");
            header.createCell(COL_NOTES).setCellValue("NOTES");

            int rowNum = 1;
            for (Reminder rem : reminders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(COL_ID).setCellValue(rem.getId());
                row.createCell(COL_TEXT).setCellValue(rem.getText());
                row.createCell(COL_FREQUENCY).setCellValue(rem.getFrequency().toString());
                row.createCell(COL_TIME).setCellValue(rem.getTime() == null ? "" : rem.getTime().toString());

                // IMPROVEMENT: Use String.join for cleaner code
                if (rem.getDaysOfWeek() != null && !rem.getDaysOfWeek().isEmpty()) {
                    String days = String.join(",", rem.getDaysOfWeek().stream().map(DayOfWeek::toString).toArray(String[]::new));
                    row.createCell(COL_DAYS).setCellValue(days);
                } else {
                    row.createCell(COL_DAYS).setCellValue("");
                }

                row.createCell(COL_LAST_FIRED).setCellValue(rem.getLastFiredDate() == null ? "" : rem.getLastFiredDate().toString());
                row.createCell(COL_ENABLED).setCellValue(String.valueOf(rem.isEnabled()));
                
                // NEW FIELDS SAVING
                row.createCell(COL_TYPE).setCellValue(rem.getType().toString());
                row.createCell(COL_NAME).setCellValue(rem.getName());
                row.createCell(COL_TARGET).setCellValue(rem.getTargetValue());
                row.createCell(COL_UNIT).setCellValue(rem.getUnit());
                row.createCell(COL_THRESHOLD).setCellValue(rem.getThreshold());
                row.createCell(COL_NOTES).setCellValue(rem.getNotes());


            }

            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(dataFile)) {
                wb.write(fos);
            }
        } catch (Exception ex) {
            System.err.println("Error saving reminders to Excel: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    // Helper method to safely retrieve String content from a cell
    private String getStringCell(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
    
    // NEW Helper method to safely retrieve double content from a cell
    private double getDoubleCell(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return 0.0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return Double.parseDouble(cell.getStringCellValue());
            }
        } catch (NumberFormatException | IllegalStateException e) {
            // Log or handle error, return 0.0 on failure
        }
        return 0.0;
    }


    // Cleanup method to shutdown scheduler gracefully
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}