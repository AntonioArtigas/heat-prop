package assignment3.solutions;

public class ImperativeSolution extends SwingSolution {
    public ImperativeSolution(int width, int height, int maxIterations, float t, float s, float[] thermalConstants, int threshold) {
        super(width, height, maxIterations, s, t, thermalConstants, threshold);
    }

    @Override
    protected void compute() {
        for (int y = 1; y <= height; y++) {
            for (int x = 1; x <= width; x++) {
                float temp = 0f;

                // This is horrid, but I wanted to make sure my calculations were right!
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

                // Might need more tuning.
                renderBuffer[x][y] = temp;// * randomNoise();
            }
        }
    }

    @Override
    public void run() {
        for (int iter = 0; iter < maxIterations; iter++) {
            displayBuffer[1][1] = s;
            displayBuffer[paddedWidth - 2][paddedHeight - 2] = t;

            compute();
            swapBuffers();
            display(displayBuffer);
        }
    }
}
