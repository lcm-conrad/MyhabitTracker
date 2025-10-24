/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package myhabittracker;

import java.util.Random;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.*;
import java.io.File;
import java.io.FileInputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Statistics Projection Window with Flexible Success Rate Tracking
 * Shows habit progress over time with support for scheduled/flexible habits
 * @author Jeff
 */
public class StatisticsProjection extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = 
            java.util.logging.Logger.getLogger(StatisticsProjection.class.getName());

    private static final Color FRAME_COLOR = new Color(238, 222, 223);
    private static final Color BUTTON_COLOR = new Color(229, 222, 208);
    private static final Color GRAPH_LINE_COLOR = new Color(145, 204, 161);
    private static final Color CUMULATIVE_LINE_COLOR = new Color(100, 149, 237);
    private static final Color GRID_COLOR = new Color(200, 200, 200);
    private static final Color GOAL_LINE_COLOR = new Color(255, 165, 0);
    
    private String habitName;
    private boolean isMeasurable;
    private String unit;
    private Map<LocalDate, Double> dataPoints;
    private Set<DayOfWeek> scheduledDays;
    private ViewMode currentMode = ViewMode.WEEKLY;
    
    private enum ViewMode {
        WEEKLY(7, "Weekly View"),
        MONTHLY(30, "Monthly View");
        
        final int days;
        final String label;
        
        ViewMode(int days, String label) {
            this.days = days;
            this.label = label;
        }
    }

    public StatisticsProjection(String habitName, boolean isMeasurable, String unit) {
        this.habitName = habitName;
        this.isMeasurable = isMeasurable;
        this.unit = unit != null ? unit : "";
        this.dataPoints = new TreeMap<>();
        this.scheduledDays = new HashSet<>();
        
        initComponents();
        setupUI();
        loadScheduledDays();
        loadDataFromExcel();
        
        // Debug info
        System.out.println("Statistics for: " + habitName);
        System.out.println("Is measurable: " + isMeasurable);
        System.out.println("Unit: " + unit);
        System.out.println("Data points loaded: " + dataPoints.size());
        if (!dataPoints.isEmpty()) {
            System.out.println("Sample data: " + dataPoints.entrySet().iterator().next());
        }
        
        updateStatistics();
        
        // Ensure window is properly sized and visible
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public StatisticsProjection() {
        this.habitName = "Sample Habit";
        this.isMeasurable = false;
        this.unit = "";
        this.dataPoints = new TreeMap<>();
        this.scheduledDays = new HashSet<>();

        initComponents();
        setupUI();
        loadSampleData();
        updateStatistics();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * New constructor that accepts data directly from Dashboard
     */
    public StatisticsProjection(String habitName, boolean isMeasurable, String unit, Map<LocalDate, Double> dashboardData) {
        this.habitName = habitName;
        this.isMeasurable = isMeasurable;
        this.unit = unit != null ? unit : "";
        this.dataPoints = new TreeMap<>();
        this.scheduledDays = new HashSet<>();
        
        initComponents();
        setupUI();
        loadScheduledDays();
        
        // Load data directly from the provided Map<LocalDate, Double>
        if (dashboardData != null && !dashboardData.isEmpty()) {
            this.dataPoints.putAll(dashboardData);
            System.out.println("Loaded " + dataPoints.size() + " data points from dashboard");
            for (Map.Entry<LocalDate, Double> entry : dataPoints.entrySet()) {
                System.out.println("Data point: " + entry.getKey() + " = " + entry.getValue());
            }
        } else {
            System.out.println("No dashboard data provided, using Excel data");
            loadDataFromExcel();
        }
        
        updateStatistics();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadDataFromDashboard(Map<String, Object> dashboardData) {
        if (dashboardData == null || dashboardData.isEmpty()) {
            System.out.println("No dashboard data provided");
            return;
        }
        
        System.out.println("Loading data from dashboard for: " + habitName);
        System.out.println("Dashboard data keys: " + dashboardData.keySet());
        
        LocalDate today = LocalDate.now();
        DateTimeFormatter dashboardFormatter = DateTimeFormatter.ofPattern("MMM dd");
        
        for (Map.Entry<String, Object> entry : dashboardData.entrySet()) {
            String dateStr = entry.getKey();
            Object value = entry.getValue();
            
            // Skip non-date columns
            if (dateStr.equals("isMeasurable") || dateStr.equals("unit") || 
                dateStr.equals("target") || dateStr.equals("notes")) {
                continue;
            }
            
            try {
                // Parse date from dashboard format (e.g., "Nov 25")
                LocalDate date = parseDashboardDate(dateStr, today);
                if (date != null) {
                    double numericValue = convertValueToDouble(value);
                    dataPoints.put(date, numericValue);
                    System.out.println("Added data point: " + date + " = " + numericValue);
                }
            } catch (Exception e) {
                System.out.println("Error processing date: " + dateStr + " - " + e.getMessage());
            }
        }
        
        System.out.println("Total data points from dashboard: " + dataPoints.size());
    }

    private LocalDate parseDashboardDate(String dateStr, LocalDate today) {
        try {
            // Dashboard uses format like "Nov 25" - add current year
            String dateWithYear = dateStr + " " + today.getYear();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd yyyy");
            LocalDate date = LocalDate.parse(dateWithYear, formatter);
            
            // If the date is in the future, use previous year
            if (date.isAfter(today)) {
                date = date.minusYears(1);
            }
            
            return date;
        } catch (Exception e) {
            System.out.println("Failed to parse dashboard date: " + dateStr);
            return null;
        }
    }

    private double convertValueToDouble(Object value) {
        if (value == null) return 0.0;
        
        if (isMeasurable) {
            // For measurable habits, value is like "5.0 km" - extract the number
            if (value instanceof String) {
                String strValue = (String) value;
                try {
                    // Remove unit and extract number
                    String numberStr = strValue.replaceAll("[^0-9.]", "").trim();
                    return numberStr.isEmpty() ? 0.0 : Double.parseDouble(numberStr);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } else {
            // For yes/no habits: 1 = checked, 0 = unchecked, 2 = day off
            if (value instanceof Integer) {
                int intValue = (Integer) value;
                return (intValue == 1) ? 1.0 : 0.0; // Only count checked as success
            } else if (value instanceof String) {
                String strValue = (String) value;
                return strValue.equals("1") ? 1.0 : 0.0;
            }
        }
        
        return 0.0;
    }

    private void loadSampleData() {
        LocalDate today = LocalDate.now();
        Random random = new Random();
        
        // Add 30 days of sample data
        for (int i = 30; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            if (isMeasurable) {
                // For measurable habits, random values between 0-100
                dataPoints.put(date, random.nextDouble() * 100);
            } else {
                // For yes/no habits, simulate 80% success rate
                dataPoints.put(date, random.nextDouble() > 0.2 ? 1.0 : 0.0);
            }
        }
        
        // Add sample scheduled days (Mon, Wed, Fri)
        scheduledDays.add(DayOfWeek.MONDAY);
        scheduledDays.add(DayOfWeek.WEDNESDAY);
        scheduledDays.add(DayOfWeek.FRIDAY);
        
        System.out.println("Loaded " + dataPoints.size() + " sample data points for testing");
    }

    private void setupUI() {
        setTitle("Habit Statistics - " + habitName);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Setup main title label
        jLabel1.setText("Habit: " + habitName);
        jLabel1.setFont(new Font("Segoe UI", Font.BOLD, 16));
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Setup view mode buttons
        jButton1.setText("Weekly View");
        jButton1.setBackground(BUTTON_COLOR);
        monthlyButton.setText("Monthly View");
        monthlyButton.setBackground(BUTTON_COLOR);
        
        // Setup statistics labels
        streakLabel.setText("Current Streak: Calculating...");
        streakLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        averageLabel.setText("Success Rate: Calculating...");
        averageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        totalLabel.setText("Completed: Calculating...");
        totalLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        updateViewModeButton();
        
        // Configure the main chart area
        jPanel1.setBackground(Color.WHITE);
        jPanel1.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Configure the scroll pane for chart
        jScrollPane1.setBorder(BorderFactory.createEmptyBorder());
        jScrollPane1.getViewport().setBackground(Color.WHITE);
        
        // Remove the table - we don't need it
        jScrollPane1.setViewportView(null);
        
        // Setup stats panel
        jPanel2.setBackground(FRAME_COLOR);
        jPanel2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
    }

    private void formComponentResized(java.awt.event.ComponentEvent evt) {
        // Repaint chart when window is resized
        if (jScrollPane1.getViewport().getView() != null) {
            jScrollPane1.getViewport().getView().repaint();
        }
    }
    
    private void updateViewModeButton() {
        if (currentMode == ViewMode.WEEKLY) {
            jButton1.setEnabled(false);
            monthlyButton.setEnabled(true);
        } else {
            jButton1.setEnabled(true);
            monthlyButton.setEnabled(false);
        }
        repaint();
    }

    private void loadScheduledDays() {
        String filePath = System.getProperty("user.home") + File.separator + "MyHabitTracker_Data.xlsx";
        File file = new File(filePath);
        
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet reminderSheet = workbook.getSheet("ReminderData");
            if (reminderSheet == null) return;
            
            for (int i = 1; i <= reminderSheet.getLastRowNum(); i++) {
                Row row = reminderSheet.getRow(i);
                if (row == null) continue;
                
                String name = getCellValueAsString(row.getCell(0));
                if (habitName.equals(name)) {
                    String daysStr = getCellValueAsString(row.getCell(3));
                    if (!daysStr.isEmpty()) {
                        for (String day : daysStr.split(",")) {
                            try {
                                scheduledDays.add(DayOfWeek.valueOf(day.trim()));
                            } catch (IllegalArgumentException e) {
                                logger.warning("Invalid day: " + day);
                            }
                        }
                    }
              