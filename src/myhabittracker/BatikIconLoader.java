package myhabittracker;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Icon loader using Apache Batik for high-quality SVG rendering
 */
public class BatikIconLoader {
    
    /**
     * Custom transcoder that renders SVG to BufferedImage
     */
    private static class BufferedImageTranscoder extends ImageTranscoder {
        private BufferedImage image;
        
        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        
        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) {
            this.image = img;
        }
        
        public BufferedImage getBufferedImage() {
            return image;
        }
    }
    
    /**
     * Loads and scales an SVG icon using Apache Batik
     * 
     * @param path Resource path to the SVG file (e.g., "/resources/icon.svg")
     * @param targetWidth Desired width in pixels
     * @param targetHeight Desired height in pixels
     * @return ImageIcon with the scaled SVG
     */
    public static ImageIcon loadSvgIcon(String path, int targetWidth, int targetHeight) {
        try {
            // Create a custom transcoder that renders to BufferedImage
            BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
            
            // Set the desired dimensions
            transcoder.addTranscodingHint(
                ImageTranscoder.KEY_WIDTH, 
                (float) targetWidth
            );
            transcoder.addTranscodingHint(
                ImageTranscoder.KEY_HEIGHT, 
                (float) targetHeight
            );
            
            // Enable anti-aliasing for smooth rendering
            transcoder.addTranscodingHint(
                ImageTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES,
                Boolean.TRUE
            );
            
            try ( // Load the SVG file
                    InputStream svgStream = BatikIconLoader.class.getResourceAsStream(path)) {
                if (svgStream == null) {
                    throw new IllegalArgumentException("SVG file not found: " + path);
                }
                
                TranscoderInput input = new TranscoderInput(svgStream);
                
                // Transcode the SVG to a BufferedImage
                transcoder.transcode(input, null);
            }
            
            BufferedImage image = transcoder.getBufferedImage();
            return new ImageIcon(image);
            
        } catch (Exception e) {
            System.err.println("Failed to load SVG icon: " + path);
            e.printStackTrace();
            return null;
        }
    }
}
