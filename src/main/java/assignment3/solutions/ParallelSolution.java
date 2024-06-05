package assignment3.solutions;

import java.util.concurrent.Phaser;
import java.util.concurrent.RecursiveAction;

public class ParallelSolution extends SwingSolution {
    public ParallelSolution(int width, int height, int maxIterations, float s, float t, float[] thermalConstants, int threshold) {
        super(width, height, maxIterations, s, t, thermalConstants, threshold);
    }

    class Segment extends RecursiveAction {
        public final int lowX;
        public final int lowY;
        public final int highX;
        public final int highY;

        public Segment(int lowX, int lowY, int highX, int highY) {
            this.lowX = lowX;
            this.lowY = lowY;
            this.highX = highX;
            this.highY = highY;
        }

        @Override
        protected void compute() {
            int sectorWidth = highX - lowX;
            int sectorHeight = highY - lowY;
            int numPixels = sectorWidth * sectorHeight;

            phaser.register();

            if (numPixels > threshold) {
                // Break down into four more smaller segments.
                int splitX = (lowX + highX) / 2;
                int splitY = (lowY + highY) / 2;

                new Segment(lowX, splitY, splitX, highY).fork(); // Top left.
                new Segment(splitX, splitY, highX, highY).fork(); // Top right.
                new Segment(lowX, lowY, splitX, splitY).fork(); // Bottom left.
                new Segment(splitX, lowY, highX, splitY).fork(); // Bottom right.
            } else {
                // Actually do the work sequentially.
                for (int y = lowY; y < highY; y++) {
                    for (int x = lowX; x < highX; x++) {
                        float temp = 0f;

                        for (int i = 0; i < 3; i++) {
                            float neighborTemp = 0f;

                            var northAlloy = alloys[x][y - 1];
                            var eastAlloy = alloys[x + 1][y];
                            var southAlloy = alloys[x][y + 1];
                            var westAlloy = alloys[x - 1][y];

                            var northTemp = displayBuffer[x][y - 1];
                            var eastTemp = displayBuffer[x + 1][y];
                            var southTemp = displayBuffer[x][y + 1];
                            var westTemp = displayBuffer[x - 1][y];

                            int neighbors = 0;
                            if (y - 1 != 0) {
                                neighbors++;
                                neighborTemp += northTemp * northAlloy.percent(i);
                            }
                            if (x + 1 != paddedWidth - 1) {
                                neighbors++;
                                neighborTemp += eastTemp * eastAlloy.percent(i);
                            }
                            if (y + 1 != paddedHeight - 1) {
                                neighbors++;
                                neighborTemp += southTemp * southAlloy.percent(i);
                            }
                            if (x - 1 != 0) {
                                neighbors++;
                                neighborTemp += westTemp * westAlloy.percent(i);
                            }

                            temp += thermalConstants[i] * neighborTemp / neighbors;
                        }

                        renderBuffer[x][y] = temp;// * randomNoise();
                    }
                }
            }

            phaser.arriveAndDeregister();
        }
    }

    private final Phaser phaser = new Phaser(1);

    @Override
    protected void compute() {
        new Segment(1, 1, paddedWidth - 1, paddedHeight - 1).fork();
    }

    @Override
    public void run() {
        for (int i = 0; i < maxIterations; i++) {
            renderBuffer[1][1] = t;
            renderBuffer[paddedWidth - 2][paddedHeight - 2] = s;

            compute();

            phaser.arriveAndAwaitAdvance();

            swapBuffers();
            display(displayBuffer);
        }
    }
}
