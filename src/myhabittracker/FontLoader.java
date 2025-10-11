/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package myhabittracker;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.plaf.FontUIResource;
/**
 *
 * @author conrad
 */
public class FontLoader {
    
        public static final String FONT_FILE_PATH = "/fonts/Inter-VariableFont_opsz,wght.ttf";
    public static final int DEFAULT_FONT_SIZE = 14; // Adjust size as needed
    
        private static Font loadCustomFont() {
        try (InputStream is = FontLoader.class.getResourceAsStream(FONT_FILE_PATH)) {
            if (is == null) {
                // If the font file is not found, print an error and return a fallback font
                System.err.println("Custom font file not found: " + FONT_FILE_PATH);
                return new Font("SansSerif", Font.PLAIN, DEFAULT_FONT_SIZE);
            }
            // Create the font and derive it to the desired size
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, DEFAULT_FONT_SIZE);
        } catch (IOException | FontFormatException e) {
            System.err.println("Error loading custom font: " + e.getMessage());
            // Fallback to a default system font
            return new Font("SansSerif", Font.PLAIN, DEFAULT_FONT_SIZE);
        }
    }

    // Method to apply the loaded font to the UIManager defaults
    public static void applyCustomFont() {
        Font customFont = loadCustomFont();
        // FlatLaf looks for a key named "defaultFont" to set the base font for all components
        javax.swing.UIManager.put("defaultFont", new FontUIResource(customFont));
    }
}
