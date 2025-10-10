/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package myhabittracker;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.event.KeyEvent;
import javax.swing.JTextField;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;

public class PinPasswordHabit extends javax.swing.JFrame {

    private static final Preferences prefs = Preferences.userNodeForPackage(PinPasswordHabit.class);
    private static final String PIN_KEY = "userPIN";

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(PinPasswordHabit.class.getName());

    /**
     * Creates new form PinPassword
     */
    // Helper to get the full PIN from 4 fields
    private String getPin(javax.swing.JTextField... fields) {
        StringBuilder pin = new StringBuilder();
        for (JTextField field : fields) {
            pin.append(field.getText());
        }
        return pin.toString();
    }

    // Verify if both PINs match
    private void clearPinFields() {
        jTextField1.setText("");
        jTextField2.setText("");
        jTextField3.setText("");
        jTextField4.setText("");
        if ("SETUP".equals(mode)) {  // Only clear second row in SETUP
            jTextField5.setText("");
            jTextField6.setText("");
            jTextField7.setText("");
            jTextField8.setText("");
        }
        jTextField1.requestFocus();
    }

    private void verifyPins() {
        if ("SETUP".equals(mode)) {
            String pin1 = getPin(jTextField1, jTextField2, jTextField3, jTextField4);
            String pin2 = getPin(jTextField5, jTextField6, jTextField7, jTextField8);

            if (pin1.equals(pin2) && pin1.length() == 4) {
                if (pinExistsInExcel(pin1)) {
                    JOptionPane.showMessageDialog(this, "⚠️ This PIN already exists.");
                    clearPinFields();
                    return;
                }
                prefs.put(PIN_KEY, pin1);
                savePinToExcel(pin1);
                JOptionPane.showMessageDialog(this, "✅ PIN successfully set!");
                this.dispose();
                dashboard.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "❌ PINs do not match or invalid length.");
                clearPinFields();
            }
          } else { // UNLOCK
            String enteredPin = getPin(jTextField1, jTextField2, jTextField3, jTextField4);
            if (enteredPin.length() != 4) {
                JOptionPane.showMessageDialog(this, "❌ Enter a full 4-digit PIN.");
                clearPinFields();
                jTextField1.requestFocus();
                return;
            }
            String savedPin = prefs.get(PIN_KEY, null);
            if (enteredPin.equals(savedPin)) {
                JOptionPane.showMessageDialog(this, "🔓 Dashboard unlocked!");
                this.dispose();
                dashboard.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "❌ Wrong PIN!");
                clearPinFields();
                jTextField1.requestFocus();
            }
        }
    }
    private String mode;               // "SETUP" or "UNLOCK"
    private DashboardHabit dashboard;

    public PinPasswordHabit() {
        this("SETUP", null); // default to SETUP mode
    }

    public PinPasswordHabit(String mode, DashboardHabit dashboard) {
        this.mode = mode;
        this.dashboard = dashboard;
        initComponents();
        JTextField[] pinFields = { jTextField1, jTextField2, jTextField3, jTextField4,
                           jTextField5, jTextField6, jTextField7, jTextField8 };

   for (int i = 0; i < pinFields.length; i++) {
    final int index = i;

    pinFields[i].addKeyListener(new java.awt.event.KeyAdapter() {
        @Override
        public void keyReleased(java.awt.event.KeyEvent e) {
            String text = pinFields[index].getText();

            // Move to next field after typing 1 digit
            if (text.length() == 1 && index < pinFields.length - 1) {
                pinFields[index + 1].requestFocus();
            }

            // Handle backspace: move back if field is empty
            if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE && index > 0 && text.isEmpty()) {
                pinFields[index - 1].requestFocus();
            }
        }
    });
}

// Auto jump from 4th field to confirmation (5th)
pinFields[3].addKeyListener(new java.awt.event.KeyAdapter() {
    @Override
    public void keyReleased(java.awt.event.KeyEvent e) {
        if (!pinFields[3].getText().isEmpty()) {
            pinFields[4].requestFocus();
        }
    }
});
        // Configure buttons in jPanel1 (reuse existing jButton2 and jButton3)
        jButton4.setText("Forgot PIN?");
        jButton2.setText("Submit");
        jButton3.setText("Cancel");
        jButton2.addActionListener(evt -> verifyPins());
        jButton3.addActionListener(evt -> {
            clearPinFields();
            if ("UNLOCK".equals(mode)) {
                this.dispose();
                dashboard.setVisible(true);
            } else {
                this.dispose();  // Or just clear in SETUP
            }
        });
        jButton4.addActionListener(evt -> {
            new SecurityQuestionSetup(this, dashboard).setVisible(true);
            this.dispose();
        });
        if (mode.equals("SETUP")) {
            jLabel1.setText("Set your 4-digit PIN");
            jButton1.setText("Save PIN");
        } else {
            jLabel1.setText("Enter your PIN to Unlock");
            jButton1.setText("Unlock");
        }
        // Set titles and visibility based on mode
        if ("SETUP".equals(mode)) {
            setTitle("Set Your PIN");
            jLabel1.setText("SET YOUR PIN");
            jLabel2.setVisible(true);
            jTextField5.setVisible(true);
            jTextField6.setVisible(true);
            jTextField7.setVisible(true);
            jTextField8.setVisible(true);
        } else {
            setTitle("Enter PIN to Unlock");
            jLabel1.setText("ENTER PIN");
            jLabel2.setVisible(false);
            jTextField5.setVisible(false);
            jTextField6.setVisible(false);
            jTextField7.setVisible(false);
            jTextField8.setVisible(false);
        }
         getContentPane().revalidate();  // Recalculates constraints after visibility changes
        getContentPane().repaint();     // Forces redraw of components
        pack();                         // Resizes frame to preferred size of visible components
        setMinimumSize(new java.awt.Dimension(320, 250));  // Minimum for UNLOCK mode (adjust as needed)
        // NEW: Set initial focus
        jTextField1.requestFocus();
    }

    private boolean pinExistsInExcel(String pin) {
        String fileName = "PinRecords.xlsx";
        File file = new File(fileName);

        if (!file.exists()) {
            return false; // no Excel yet, so no PINs stored
        }

        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // start from row 1 (row 0 is header)
            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell pinCell = row.getCell(1); // column 1 = PIN
                    if (pinCell != null && pin.equals(pinCell.getStringCellValue())) {
                        return true; // PIN already exists
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false; // not found
    }

    private void restrictToSingleDigit(KeyEvent evt, JTextField currentField, JTextField nextField) {
        char c = evt.getKeyChar();

        if (Character.isDigit(c)) {
            if (currentField.getText().length() == 0) {
                currentField.setText(String.valueOf(c));
                evt.consume(); // prevent duplicate input

                if (nextField != null && nextField.isVisible()) {
                    nextField.requestFocus();
                }
            } else {
                evt.consume(); // block input if already filled
            }
        } else if (!Character.isISOControl(c)) {
            evt.consume(); // block non-digit input
        }
    }

    private void savePinToExcel(String pin) {
        String fileName = "PinRecords.xlsx"; // file will be created in project root
        try {
            Workbook workbook;
            Sheet sheet;
            File file = new File(fileName);

            if (file.exists()) {
                // If file exists, open it
                FileInputStream fis = new FileInputStream(file);
                workbook = new XSSFWorkbook(fis);
                fis.close();
                sheet = workbook.getSheetAt(0);
            } else {
                // If file doesn’t exist, create new workbook & sheet
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("PIN Data");

                // Create header row
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Timestamp");
                header.createCell(1).setCellValue("PIN");
            }

            // Append new PIN record
            int lastRow = sheet.getPhysicalNumberOfRows();
            Row row = sheet.createRow(lastRow);
            row.createCell(0).setCellValue(java.time.LocalDateTime.now().toString());
            row.createCell(1).setCellValue(pin);

            // Write back to file
            FileOutputStream fos = new FileOutputStream(fileName);
            workbook.write(fos);
            fos.close();
            workbook.close();

            javax.swing.JOptionPane.showMessageDialog(this, "✅ PIN saved to Excel file: " + fileName);
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "❌ Failed to save PIN to Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
            @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jTextField8 = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();

        jButton1.setText("jButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMaximumSize(null);
        setResizable(false);

        jLabel1.setText("SET YOUR PIN");

        jLabel2.setText("REENTER PIN");

        jTextField5.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField5.setMaximumSize(null);
        jTextField5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField5ActionPerformed(evt);
            }
        });
        jTextField5.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField5KeyTyped(evt);
            }
        });

        jTextField6.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField6.setMaximumSize(null);
        jTextField6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField6ActionPerformed(evt);
            }
        });
        jTextField6.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField6KeyTyped(evt);
            }
        });

        jTextField7.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField7.setMaximumSize(null);
        jTextField7.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField7KeyTyped(evt);
            }
        });

        jTextField8.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField8.setMaximumSize(null);
        jTextField8.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField8KeyTyped(evt);
            }
        });

        jPanel1.setPreferredSize(new java.awt.Dimension(320, 500));

        jButton3.setText("jButton3");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton2.setText("jButton2");
        jButton2.setAlignmentX(0.5F);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton4.setText("jButton4");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addComponent(jButton3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addComponent(jButton4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton2)
                .addGap(37, 37, 37))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jButton2)
                    .addComponent(jButton4))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        jTextField1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField1.setMaximumSize(null);
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField1KeyTyped(evt);
            }
        });

        jTextField2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField2.setMaximumSize(null);
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });
        jTextField2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField2KeyTyped(evt);
            }
        });

        jTextField3.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField3.setMaximumSize(null);
        jTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField3ActionPerformed(evt);
            }
        });
        jTextField3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField3KeyTyped(evt);
            }
        });

        jTextField4.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField4.setMaximumSize(null);
        jTextField4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField4ActionPerformed(evt);
            }
        });
        jTextField4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField4KeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField4KeyTyped(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(46, 46, 46)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(47, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(26, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    
    private void jTextField1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyTyped
        // TODO add your handling code here:
        restrictToSingleDigit(evt, jTextField1, jTextField2);

    }//GEN-LAST:event_jTextField1KeyTyped

    private void jTextField2KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField2KeyTyped
        // TODO add your handling code here:\
        restrictToSingleDigit(evt, jTextField2, jTextField3);

    }//GEN-LAST:event_jTextField2KeyTyped

    private void jTextField3KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField3KeyTyped
        // TODO add your handling code here:
        restrictToSingleDigit(evt, jTextField3, jTextField4);

    }//GEN-LAST:event_jTextField3KeyTyped

    private void jTextField4KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField4KeyTyped
        // TODO add your handling code here:
        restrictToSingleDigit(evt, jTextField4, null);


    }//GEN-LAST:event_jTextField4KeyTyped

    private void jTextField5KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField5KeyTyped
        // TODO add your handling code here:
        restrictToSingleDigit(evt, jTextField5, jTextField6);

    }//GEN-LAST:event_jTextField5KeyTyped

    private void jTextField6KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField6KeyTyped
        // TODO add your handling code here:
        restrictToSingleDigit(evt, jTextField6, jTextField7);

    }//GEN-LAST:event_jTextField6KeyTyped

    private void jTextField7KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField7KeyTyped
        // TODO add your handling code here:
        restrictToSingleDigit(evt, jTextField7, jTextField8);
    }//GEN-LAST:event_jTextField7KeyTyped

    private void jTextField8KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField8KeyTyped
        // TODO add your handling code here:                                    
        restrictToSingleDigit(evt, jTextField8, null);
    }//GEN-LAST:event_jTextField8KeyTyped

    private void jTextField4KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField4KeyPressed
        // TODO add your handling code here:
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if ("UNLOCK".equals(mode)) {
                verifyPins();  // Submit directly in UNLOCK
            } else {
                jTextField5.requestFocus();  // Move to second row in SETUP
            }
            evt.consume();
        }
    }//GEN-LAST:event_jTextField4KeyPressed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField3ActionPerformed

    private void jTextField4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField4ActionPerformed

    private void jTextField5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField5ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField5ActionPerformed

    private void jTextField6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField6ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField6ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
           verifyPins();
    }//GEN-LAST:event_jButton2ActionPerformed

    public static String getSavedPin() {
        return prefs.get(PIN_KEY, null);
    }

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
            // Better way to apply FlatLaf
            FlatLightLaf.setup();
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to initialize FlatLaf", ex);
        }
       java.awt.EventQueue.invokeLater(() -> {
          PinPasswordHabit frame = new PinPasswordHabit("SETUP", null);
          frame.setVisible(true);
          // Debug: Print sizes
          System.out.println("Frame size: " + frame.getWidth() + "x" + frame.getHeight());
       });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    // End of variables declaration//GEN-END:variables
}
