package assignment3;

import assignment3.solutions.GpuSolution;
import assignment3.solutions.ImperativeSolution;
import assignment3.solutions.ParallelSolution;
import picocli.CommandLine;

import java.util.Arrays;

public class Main implements Runnable {
    enum Mode {
        IMPERATIVE,
        PARALLEL,
        GPU
    }

    @CommandLine.Option(names = {"-m", "--mode"}, defaultValue = "parallel")
    Mode mode;

    @CommandLine.Option(names = {"-h", "--height"}, defaultValue = "128")
    int height;

    int width;

    @CommandLine.Option(names = {"-i", "--iterations"}, defaultValue = "2500")
    int maxIterations;

    @CommandLine.Option(names = "-t", defaultValue = "100")
    float t;

    @CommandLine.Option(names = "-s", defaultValue = "100")
    float s;

    @CommandLine.Option(names = "-g", defaultValue = "4")
    int granularity;

    int threshold;

    @CommandLine.Option(names = "-c1", defaultValue = "0.75")
    float c1;

    @CommandLine.Option(names = "-c2", defaultValue = "1.0")
    float c2;

    @CommandLine.Option(names = "-c3", defaultValue = "1.25")
    float c3;

    float[] thermalConstants;

    public static void main(String[] args) {
        new CommandLine(new Main())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
    }

    @Override
    public void run() {
        width = height * 4;
        threshold = width * height / 2;
        thermalConstants = new float[]{c1, c2, c3};

        System.out.println("Running " + mode + " mode.");
        System.out.println("Solution space is " + width + " by " + height + ".");
        System.out.println("Iterating " + maxIterations + " times.");
        System.out.println("S: " + s + ", T: " + t);
        System.out.println("Granularity: " + granularity);
        System.out.println("Threshold: " + threshold);
        System.out.println("Thermal constants: " + Arrays.toString(thermalConstants));

        var solution = switch (mode) {
            case IMPERATIVE -> new ImperativeSolution(width, height, maxIterations, s, t, thermalConstants, threshold);
            case PARALLEL -> new ParallelSolution(width, height, maxIterations, s, t, thermalConstants, threshold);
            case GPU -> new GpuSolution(width, height, maxIterations, s, t, thermalConstants, threshold);
        };

        solution.run();
        System.out.println("Done!");
    }
}
