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
                    break;
                }
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Error loading scheduled days", e);
        }
    }

    private void loadDataFromExcel() {
        String filePath = System.getProperty("user.home") + File.separator + "MyHabitTracker_Data.xlsx";
        File file = new File(filePath);

        if (!file.exists()) {
            logger.warning("Data file not found: " + filePath);
            showErrorDialog("Data file not found at: " + filePath);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet dataSheet = workbook.getSheet("HabitData");
            if (dataSheet == null) {
                showErrorDialog("HabitData sheet not found in Excel file");
                return;
            }

            Row headerRow = dataSheet.getRow(0);
            if (headerRow == null) {
                return;
            }

            int habitRowIndex = -1;
            for (int i = 1; i <= dataSheet.getLastRowNum(); i++) {
                Row row = dataSheet.getRow(i);
                if (row != null) {
                    Cell habitCell = row.getCell(0);
                    if (habitCell != null && habitName.equals(getCellValueAsString(habitCell))) {
                        habitRowIndex = i;
                        break;
                    }
                }
            }

            if (habitRowIndex == -1) {
                logger.warning("Habit not found in Excel: " + habitName);
                return;
            }

            Row habitRow = dataSheet.getRow(habitRowIndex);
            LocalDate today = LocalDate.now();

            for (int col = 1; col < headerRow.getLastCellNum(); col++) {
                Cell headerCell = headerRow.getCell(col);
                if (headerCell == null) {
                    continue;
                }

                String dateStr = getCellValueAsString(headerCell);
                LocalDate date = parseDateFromHeader(dateStr, today);

                if (date != null) {
                    Cell dataCell = habitRow.getCell(col);
                    double value = parseDataValue(dataCell);
                    dataPoints.put(date, value);
                }
            }

        } catch (Exception e) {
            String errorMsg = "Error loading data from Excel: " + e.getMessage();
            logger.log(java.util.logging.Level.SEVERE, errorMsg, e);
            showErrorDialog(errorMsg);
        }
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, 
            message, 
            "Data Loading Error", 
            JOptionPane.ERROR_MESSAGE);
    }
    
    public static LocalDate parseDateFromHeader(String dateStr, LocalDate today) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try dashboard format first (MMM dd)
            try {
                String dateWithYear = dateStr + " " + today.getYear();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd yyyy");
                LocalDate date = LocalDate.parse(dateWithYear, formatter);
                
                // If the date is in the future, use previous year
                if (date.isAfter(today)) {
                    date = date.minusYears(1);
                }
                return date;
            } catch (Exception e) {
                // Try other formats
            }
            
            // Handle various date formats that might be in Excel
            String[] formats = {
                "MMM dd yyyy", "MMM d yyyy", "MMM dd", "MMM d"
            };
            
            for (String format : formats) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                    LocalDate date = LocalDate.parse(dateStr.trim(), formatter);
                    
                    // If year is missing, use current year
                    if (format.length() <= 6) { // No year in format
                        date = date.withYear(today.getYear());
                        // If the date is in the future, use previous year
                        if (date.isAfter(today)) {
                            date = date.minusYears(1);
                        }
                    }
                    return date;
                } catch (Exception e) {
                    // Try next format
                }
            }
            
            logger.warning("Could not parse date with any format: " + dateStr);
        } catch (Exception e) {
            logger.warning("Error parsing date: " + dateStr + " - " + e.getMessage());
        }
        return null;
    }
    
    private double parseDataValue(Cell cell) {
        if (cell == null) return 0.0;
        
        if (isMeasurable) {
            String valueStr = getCellValueAsString(cell);
            try {
                valueStr = valueStr.replaceAll("[^0-9.]", "").trim();
                return valueStr.isEmpty() ? 0.0 : Double.parseDouble(valueStr);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        } else {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue() == 1 ? 1.0 : 0.0;
            }
            return 0.0;
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }
    
    private void updateStatistics() {
        System.out.println("\n=== UPDATE STATISTICS ===");
    System.out.println("Mode: " + currentMode.label + " (" + currentMode.days + " days)");
    System.out.println("Total data points: " + dataPoints.size());
    
    LocalDate today = LocalDate.now();
    LocalDate cutoffDate = today.minusDays(currentMode.days);
    
    System.out.println("Today: " + today);
    System.out.println("Cutoff: " + cutoffDate);
    
    Map<LocalDate, Double> filteredData = new TreeMap<>();
    
    for (Map.Entry<LocalDate, Double> entry : dataPoints.entrySet()) {
        LocalDate entryDate = entry.getKey();
        if (!entryDate.isBefore(cutoffDate) && !entryDate.isAfter(today)) {
            filteredData.put(entryDate, entry.getValue());
        }
    }
    
    System.out.println("Filtered: " + filteredData.size() + " points");
    if (!filteredData.isEmpty()) {
        // Get first and last dates
        List<LocalDate> dates = new ArrayList<>(filteredData.keySet());
        System.out.println("Date range: " + dates.get(0) + " to " + dates.get(dates.size()-1));
    }
    
    if (!filteredData.isEmpty()) {
        FlexibleLineChartPanel chartPanel = new FlexibleLineChartPanel(filteredData);
        chartPanel.setPreferredSize(new Dimension(650, 350));
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(chartPanel, BorderLayout.CENTER);
        
        jScrollPane1.setViewportView(wrapper);
        updateStatisticsLabels(filteredData);
    } else {
        showNoDataMessage();
    }
    
    jScrollPane1.revalidate();
    jScrollPane1.repaint();
    revalidate();
    repaint();
    }

    private void updateStatisticsLabels(Map<LocalDate, Double> filteredData) {
        int streak = calculateStreak(filteredData);
        double successRate = calculateFlexibleSuccessRate(filteredData);
        int totalCompleted = (int) filteredData.values().stream().filter(v -> v > 0).count();
        int totalScheduled = countScheduledDays(filteredData.keySet());
        
        // Update streak label
        streakLabel.setText("Current Streak: " + streak + " days");
        
        // Update other labels based on habit type
        if (isMeasurable) {
            double average = calculateAverage(filteredData);
            double total = calculateTotal(filteredData);
            averageLabel.setText(String.format("Average: %.1f %s", average, unit));
            totalLabel.setText(String.format("Total: %.1f %s", total, unit));
        } else {
            if (scheduledDays.isEmpty()) {
                int totalDays = filteredData.size();
                double percentage = totalDays > 0 ? (totalCompleted / (double) totalDays) * 100 : 0;
                averageLabel.setText(String.format("Completion Rate: %.1f%%", percentage));
                totalLabel.setText(String.format("Days Completed: %d/%d", totalCompleted, totalDays));
            } else {
                averageLabel.setText(String.format("Success Rate: %.1f%%", successRate));
                totalLabel.setText(String.format("Scheduled Days: %d/%d", totalCompleted, totalScheduled));
            }
        }
    }

    private void showNoDataMessage() {
        JLabel noDataLabel = new JLabel("No data available for the selected period", SwingConstants.CENTER);
        noDataLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        noDataLabel.setForeground(Color.GRAY);
        noDataLabel.setPreferredSize(new Dimension(600, 200));
        
        jScrollPane1.setViewportView(noDataLabel);
        
        streakLabel.setText("Current Streak: No data");
        averageLabel.setText("Success Rate: N/A");
        totalLabel.setText("Completed: N/A");
    }
    
    private double calculateFlexibleSuccessRate(Map<LocalDate, Double> data) {
        if (scheduledDays.isEmpty()) {
            long successfulDays = data.values().stream().filter(v -> v > 0).count();
            return data.isEmpty() ? 0.0 : (successfulDays / (double) data.size()) * 100.0;
        }
        
        int successfulScheduledDays = 0;
        int totalScheduledDays = 0;
        
        for (Map.Entry<LocalDate, Double> entry : data.entrySet()) {
            if (isScheduledDay(entry.getKey())) {
                totalScheduledDays++;
                if (entry.getValue() > 0) {
                    successfulScheduledDays++;
                }
            }
        }
        
        return totalScheduledDays > 0 
                ? (successfulScheduledDays / (double) totalScheduledDays) * 100.0 
                : 0.0;
    }
    
    private boolean isScheduledDay(LocalDate date) {
        return scheduledDays.isEmpty() || scheduledDays.contains(date.getDayOfWeek());
    }
    
    private int countScheduledDays(Set<LocalDate> dates) {
        if (scheduledDays.isEmpty()) {
            return dates.size();
        }
        return (int) dates.stream().filter(this::isScheduledDay).count();
    }
    
    private int calculateStreak(Map<LocalDate, Double> data) {
        int streak = 0;
        LocalDate date = LocalDate.now();
        
        while (data.containsKey(date)) {
            if (isScheduledDay(date)) {
                double value = data.get(date);
                if (value > 0) {
                    streak++;
                    date = date.minusDays(1);
                } else {
                    break;
                }
            } else {
                date = date.minusDays(1);
            }
        }
        return streak;
    }
    
    private double calculateAverage(Map<LocalDate, Double> data) {
        return data.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    private double calculateTotal(Map<LocalDate, Double> data) {
        return data.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    // ... (Keep the FlexibleLineChartPanel class exactly as it was - it's correct)
    private class FlexibleLineChartPanel extends JPanel {
        private final Map<LocalDate, Double> data;
        private final int PADDING = 70;
        
        private void drawLegend(Graphics2D g2, int width) {
    int legendX = width - PADDING - 200;
    int legendY = 50;
    
    g2.setFont(new Font("Arial", Font.PLAIN, 11));
    
    // Scheduled day success marker
    g2.setColor(new Color(76, 175, 80));
    g2.fillOval(legendX, legendY, 10, 10);
    g2.setColor(Color.BLACK);
    g2.drawString("Scheduled Day (Success)", legendX + 15, legendY + 9);
    
    // Scheduled day failure marker
    legendY += 20;
    g2.setColor(new Color(244, 67, 54));
    g2.setStroke(new BasicStroke(2));
    g2.drawLine(legendX + 2, legendY + 2, legendX + 8, legendY + 8);
    g2.drawLine(legendX + 2, legendY + 8, legendX + 8, legendY + 2);
    g2.setStroke(new BasicStroke(1));
    g2.setColor(Color.BLACK);
    g2.drawString("Scheduled Day (Missed)", legendX + 15, legendY + 9);
    
    // Goal line
    legendY += 20;
    g2.setColor(GOAL_LINE_COLOR);
    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
    g2.drawLine(legendX, legendY + 5, legendX + 10, legendY + 5);
    g2.setStroke(new BasicStroke(1));
    g2.setColor(Color.BLACK);
    g2.drawString("Goal (80%)", legendX + 15, legendY + 9);
}

        
        public FlexibleLineChartPanel(Map<LocalDate, Double> data) {
            this.data = new TreeMap<>(data);
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(700, 400));
            setMinimumSize(new Dimension(500, 300));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            if (data.isEmpty()) {
                drawNoDataMessage(g2);
                return;
            }
            
            int width = getWidth();
            int height = getHeight();
            
            if (isMeasurable) {
                drawMeasurableChart(g2, width, height);
            } else {
                drawSuccessRateChart(g2, width, height);
            }
            
            // Draw chart title
            drawChartTitle(g2, width);
        }
        
        private void drawNoDataMessage(Graphics2D g2) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            g2.setColor(Color.GRAY);
            String message = "No chart data available";
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2;
            g2.drawString(message, x, y);
        }
        
        private void drawChartTitle(Graphics2D g2, int width) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(Color.BLACK);
            String title = isMeasurable ? "Progress Over Time" : "Success Rate Over Time";
            FontMetrics fm = g2.getFontMetrics();
            int x = (width - fm.stringWidth(title)) / 2;
            g2.drawString(title, x, 25);
        }
            
        private void drawMeasurableChart(Graphics2D g2, int width, int height) {
            double maxValue = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            if (maxValue == 0) maxValue = 1.0;
            double minValue = 0;
            
            double xScale = data.size() > 1 ? (double) (width - 2 * PADDING) / (data.size() - 1) : width - 2 * PADDING;
            double yScale = (double) (height - 2 * PADDING) / (maxValue - minValue);
            
            drawAxes(g2, width, height);
            drawYAxisLabels(g2, width, height, minValue, maxValue, false);
            
            List<LocalDate> dates = new ArrayList<>(data.keySet());
            List<Point2D> points = calculatePoints(dates, xScale, yScale, height, minValue);
            
            drawXAxisLabels(g2, dates, points, height);
            drawLine(g2, points, GRAPH_LINE_COLOR);
            drawDataPoints(g2, points, GRAPH_LINE_COLOR);
            
            drawAxisLabel(g2, width, height, "Date", unit);
        }
        
private void drawSuccessRateChart(Graphics2D g2, int width, int height) {
   Map<LocalDate, Double> cumulativeRates = new TreeMap<>();
    int cumulativeSuccesses = 0;
    int cumulativeScheduled = 0;
    
    // FIXED: Include ALL dates in the graph, but only count scheduled days for rate
    for (LocalDate date : data.keySet()) {
        // Check if this is a scheduled day (or if no schedule exists, treat all as scheduled)
        if (isScheduledDay(date)) {
            cumulativeScheduled++;
            if (data.get(date) > 0) {
                cumulativeSuccesses++;
            }
        }
        
        // CRITICAL FIX: Add the cumulative rate for EVERY date, not just scheduled ones
        // This ensures the graph shows continuous data
        double rate = cumulativeScheduled > 0 
                ? (cumulativeSuccesses / (double) cumulativeScheduled) * 100.0 
                : 0.0;
        cumulativeRates.put(date, rate);
    }
    
    // If no data at all, show message
    if (cumulativeRates.isEmpty()) {
        drawNoDataMessage(g2);
        return;
    }
    
    double minValue = 0;
    double maxValue = 100;
    
    double xScale = cumulativeRates.size() > 1 
            ? (double) (width - 2 * PADDING) / (cumulativeRates.size() - 1) 
            : width - 2 * PADDING;
    double yScale = (double) (height - 2 * PADDING) / (maxValue - minValue);
    
    drawAxes(g2, width, height);
    drawYAxisLabels(g2, width, height, minValue, maxValue, true);
    drawGoalLine(g2, width, height, 80.0, minValue, maxValue);
    
    List<LocalDate> dates = new ArrayList<>(cumulativeRates.keySet());
    List<Point2D> points = new ArrayList<>();
    
    for (int i = 0; i < dates.size(); i++) {
        LocalDate date = dates.get(i);
        double rate = cumulativeRates.get(date);
        
        int x = PADDING + (int) (i * xScale);
        int y = height - PADDING - (int) ((rate - minValue) * yScale);
        points.add(new Point2D.Double(x, y));
    }
    
    drawXAxisLabels(g2, dates, points, height);
    drawLine(g2, points, CUMULATIVE_LINE_COLOR);
    drawScheduledDayMarkers(g2, dates, points);
    drawAxisLabel(g2, width, height, "Date", "Success Rate (%)");
    drawLegend(g2,width);
}
        
        private void drawScheduledDayMarkers(Graphics2D g2, List<LocalDate> dates, List<Point2D> points) {
            for (int i = 0; i < dates.size(); i++) {
        LocalDate date = dates.get(i);
        
        // Only draw markers for scheduled days
        if (isScheduledDay(date)) {
            Point2D point = points.get(i);
            double value = data.get(date);
            
            if (value > 0) {
                // Success - green filled circle
                g2.setColor(new Color(76, 175, 80));
                g2.fillOval((int) point.getX() - 5, (int) point.getY() - 5, 10, 10);
            } else {
                // Failure - red X
                g2.setColor(new Color(244, 67, 54));
                g2.setStroke(new BasicStroke(2));
                int x = (int) point.getX();
                int y = (int) point.getY();
                g2.drawLine(x - 4, y - 4, x + 4, y + 4);
                g2.drawLine(x - 4, y + 4, x + 4, y - 4);
                g2.setStroke(new BasicStroke(1)); // Reset stroke
            }
        }
        // For non-scheduled days, don't draw any marker (line will pass through smoothly)
    }
}
        
        private void drawGoalLine(Graphics2D g2, int width, int height, double goalValue, double minValue, double maxValue) {
            double yScale = (double) (height - 2 * PADDING) / (maxValue - minValue);
            int goalY = height - PADDING - (int) ((goalValue - minValue) * yScale);
            
            g2.setColor(GOAL_LINE_COLOR);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            g2.drawLine(PADDING, goalY, width - PADDING, goalY);
            
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Goal: " + (int) goalValue + "%", width - PADDING + 5, goalY + 4);
        }
        
        private List<Point2D> calculatePoints(List<LocalDate> dates, double xScale, double yScale, 
                                               int height, double minValue) {
            List<Point2D> points = new ArrayList<>();
            
            for (int i = 0; i < dates.size(); i++) {
                LocalDate date = dates.get(i);
                double value = data.get(date);
                
                int x = PADDING + (int) (i * xScale);
                int y = height - PADDING - (int) ((value - minValue) * yScale);
                points.add(new Point2D.Double(x, y));
            }
            
            return points;
        }
        
        private void drawAxes(Graphics2D g2, int width, int height) {
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(PADDING, height - PADDING, width - PADDING, height - PADDING);
            g2.drawLine(PADDING, PADDING, PADDING, height - PADDING);
        }
        
        private void drawYAxisLabels(Graphics2D g2, int width, int height, double minValue, double maxValue, boolean showPercent) {
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(1));
            int numYDivisions = 5;
            
            for (int i = 0; i <= numYDivisions; i++) {
                int y = height - PADDING - (i * (height - 2 * PADDING) / numYDivisions);
                g2.drawLine(PADDING, y, width - PADDING, y);
                
                double value = minValue + (maxValue - minValue) * i / numYDivisions;
                String label = showPercent 
                        ? String.format("%.0f%%", value)
                        : (isMeasurable ? String.format("%.1f", value) : String.format("%.0f", value));
                g2.setColor(Color.BLACK);
                g2.drawString(label, PADDING - 40, y + 5);
                g2.setColor(GRID_COLOR);
            }
        }
        
        private void drawXAxisLabels(Graphics2D g2, List<LocalDate> dates, List<Point2D> points, int height) {
            int labelFrequency = Math.max(1, dates.size() / 7);
            g2.setColor(Color.BLACK);
            
            for (int i = 0; i < dates.size(); i++) {
                if (i % labelFrequency == 0 || i == dates.size() - 1) {
                    Point2D point = points.get(i);
                    String dateLabel = dates.get(i).format(DateTimeFormatter.ofPattern("MM/dd"));
                    g2.drawString(dateLabel, (int) point.getX() - 15, height - PADDING + 20);
                }
            }
        }
        
        private void drawLine(Graphics2D g2, List<Point2D> points, Color color) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(3));
            
            for (int i = 0; i < points.size() - 1; i++) {
                Point2D p1 = points.get(i);
                Point2D p2 = points.get(i + 1);
                g2.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            }
        }
        
        private void drawDataPoints(Graphics2D g2, List<Point2D> points, Color color) {
            g2.setColor(color.darker());
            for (Point2D point : points) {
                g2.fillOval((int) point.getX() - 4, (int) point.getY() - 4, 8, 8);
            }
        }
        
        private void drawAxisLabel(Graphics2D g2, int width, int height, String xLabel, String yLabel) {
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString(xLabel, width / 2 - 20, height - 10);
            
            Graphics2D g2d = (Graphics2D) g2.create();
            g2d.rotate(-Math.PI / 2);
            g2d.drawString(yLabel, -height / 2 - 30, 20);
            g2d.dispose();
        }
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        monthlyButton = new javax.swing.JButton();
        streakLabel = new javax.swing.JLabel();
        totalLabel = new javax.swing.JLabel();
        averageLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE)
        );

        jButton1.setText("Weekly");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        monthlyButton.setText("Monthly");
        monthlyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                monthlyButtonActionPerformed(evt);
            }
        });

        streakLabel.setText("jLabel1");

        totalLabel.setText("jLabel2");

        averageLabel.setText("jLabel3");

        jLabel1.setText("jLabel1");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(39, 39, 39)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(totalLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(averageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(streakLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(monthlyButton, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE))
                .addGap(70, 70, 70))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(totalLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(averageLabel))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(monthlyButton)
                        .addGap(6, 6, 6)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(streakLabel)
                .addContainerGap(26, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
         currentMode = ViewMode.WEEKLY;
        updateViewModeButton();
        updateStatistics();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void monthlyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monthlyButtonActionPerformed
        // TODO add your handling code here:
        currentMode = ViewMode.MONTHLY;
    updateViewModeButton();
    updateStatistics();
    repaint();
    }//GEN-LAST:event_monthlyButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        /* Set the Nimbus look and feel */
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

    /* Create and display the form with actual parameters */
    java.awt.EventQueue.invokeLater(() -> {
        // Replace with an actual habit name from your Excel file
        new StatisticsProjection().setVisible(true);
    });
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel averageLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton monthlyButton;
    private javax.swing.JLabel streakLabel;
    private javax.swing.JLabel totalLabel;
    // End of variables declaration//GEN-END:variables
}
