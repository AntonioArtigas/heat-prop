package assignment3;

public record Alloy(int[] values) {
    public Alloy {
        if (values.length != 3) {
            throw new IllegalArgumentException("values must be 3 elements!");
        }
    }

    public float percent(int index) {
        return values[index] / 100f;
    }

    public static Alloy from3(int a, int b, int c) {
        return new Alloy(new int[]{a, b, c});
    }
}
