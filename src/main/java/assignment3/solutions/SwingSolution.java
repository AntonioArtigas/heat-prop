package assignment3.solutions;

import assignment3.Alloy;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * Solutions that are built upon the Swing toolkit.
 */
public abstract class SwingSolution extends Solution {
    private static final Color COOL = new Color(0xFF);
    private static final Color WARM = new Color(0xFF0000);
    private static final Color HOT = new Color(0xFFFCBE);

    private static final Color[] GRADIENT = new Color[]{
            COOL, WARM, HOT
    };

    public static int interpolate(float step) {
        step /= 100f;
        int firstColorIndex = (int) (step * (GRADIENT.length - 1));

        // Special case: last color (step >= 1.0f).
        if (firstColorIndex >= GRADIENT.length - 1) {
            return GRADIENT[GRADIENT.length - 1].getRGB();
        }

        // Calculate localStep between local GRADIENT:

        // stepAtFirstColorIndex will be a bit smaller than step
        float stepAtFirstColorIndex = (float) firstColorIndex / (GRADIENT.length - 1);

        // Multiply to increase values to range between 0.0f and 1.0f.
        float localStep = (step - stepAtFirstColorIndex) * (GRADIENT.length - 1);

        return SwingSolution.interpolateTwoColors(localStep, GRADIENT[firstColorIndex], GRADIENT[firstColorIndex + 1]);
    }

    // Based on this gist: https://gist.github.com/Tetr4/3c7a4eddb78ae537c995
    private static int interpolateTwoColors(float step, Color color1, Color color2) {
        // Cutoff to range between 0.0f and 1.0f.
        step = Math.max(Math.min(step, 1.0f), 0.0f);

        // Calculate difference between alpha, red, green and blue channels.
        int deltaAlpha = color2.getAlpha() - color1.getAlpha();
        int deltaRed = color2.getRed() - color1.getRed();
        int deltaGreen = color2.getGreen() - color1.getGreen();
        int deltaBlue = color2.getBlue() - color1.getBlue();

        // Result channel lies between first and second colors channel.
        int resultAlpha = (int) (color1.getAlpha() + (deltaAlpha * step));
        int resultRed = (int) (color1.getRed() + (deltaRed * step));
        int resultGreen = (int) (color1.getGreen() + (deltaGreen * step));
        int resultBlue = (int) (color1.getBlue() + (deltaBlue * step));

        // Cutoff to ranges between 0 and 255.
        resultAlpha = Math.max(Math.min(resultAlpha, 255), 0);
        resultRed = Math.max(Math.min(resultRed, 255), 0);
        resultGreen = Math.max(Math.min(resultGreen, 255), 0);
        resultBlue = Math.max(Math.min(resultBlue, 255), 0);

        // Combine results.
        return resultAlpha << 24 | resultRed << 16 | resultGreen << 8 | resultBlue;
    }

    public class BufferPanel extends JPanel {
        private final BufferedImage image = new BufferedImage(paddedWidth, paddedHeight, BufferedImage.TYPE_INT_RGB);

        public BufferPanel() {
            setPreferredSize(new Dimension(paddedWidth + 2, paddedHeight + 2));
        }

        void display(float[][] buffer) {
            for (int y = 0; y < paddedHeight; y++) {
                for (int x = 0; x < paddedWidth; x++) {
                    int color = interpolate(buffer[x][y]);
                    image.setRGB(x, y, color);
                }
            }

            repaint();
        }

        // Special version of the above display method. OpenCL gives us the image back as a FloatBuffer.
        void displayBuffer(FloatBuffer buffer) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    float value = buffer.get(index);
                    int color = interpolate(value);
                    image.setRGB(x, y, color);
                }
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), 1, 1, width, height,  null);
        }
    }

    public final int paddedWidth;
    public final int paddedHeight;

    protected final Alloy[][] alloys;
    protected float[][] renderBuffer;
    protected float[][] displayBuffer;

    private final BufferPanel bufferPanel;

    private final JFrame frame;

    public SwingSolution(int width, int height, int maxIterations, float s, float t, float[] thermalConstants, int threshold) {
        super(width, height, maxIterations, s, t, thermalConstants, threshold);
        paddedWidth = width + 2;
        paddedHeight = height + 2;

        alloys = new Alloy[paddedWidth][paddedHeight];
        renderBuffer = new float[paddedWidth][paddedHeight];
        displayBuffer = new float[paddedWidth][paddedHeight];

        for (int row = 0; row < renderBuffer.length; row++) {
            // Fill out alloys array.
            for (int col = 0; col < renderBuffer[row].length; col++) {
                alloys[row][col] = Alloy.from3(34, 34, 34);
            }

            // Setup buffer.
            Arrays.fill(renderBuffer[row], 0f);
            Arrays.fill(displayBuffer[row], 0f);
        }

        renderBuffer[1][1] = t;
        renderBuffer[paddedWidth - 3][paddedHeight - 3] = s;
        swapBuffers();

        bufferPanel = new BufferPanel();

        frame = new JFrame("HW3");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(bufferPanel);
        frame.pack();
        frame.setVisible(true);
    }

    protected void display(float[][] buffer) {
        bufferPanel.display(buffer);
    }

    protected void displayBuffer(FloatBuffer buffer) {
        bufferPanel.displayBuffer(buffer);
    }

    protected void swapBuffers() {
        // We swap the buffer references as we need to refer to previous calculations.
        // Thus, we need a copy of the array to use.
        var temp = displayBuffer;
        displayBuffer = renderBuffer;
        renderBuffer = temp;
    }

}
