import java.util.List;

public class SO2 {
    private static final double twopi = Math.PI * 2;

    // Identity rotation
    public static double identity() {
        return 0;
    }

    // Inverse rotation
    public static double inv(double a) {
        return -a;
    }

    // Apply rotation to a 2D point
    public static double[] apply(double a, double[] pt) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[]{c * pt[0] - s * pt[1], s * pt[0] + c * pt[1]};
    }

    // Normalize angle to range [0, 2pi]
    public static double normalize(double a) {
        double res = a % twopi;
        if (res < 0)
            return res + twopi;
        return res;
    }

    // CCW difference between two angles in range [-pi, pi]
    public static double diff(double a, double b) {
        double d = normalize(a) - normalize(b);
        if (d < -Math.PI) return d + twopi;
        if (d > Math.PI) return d - twopi;
        return d;
    }

    // Interpolate between two angles
    public static double interp(double a, double b, double u) {
        return a + diff(b, a) * u;
    }

    // Compose two rotations
    public static double compose(double a, double b) {
        return a + b;
    }

    // Rotation matrix for a given angle
    public static double[][] matrix(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[][]{{c, -s}, {s, c}};
    }

    // Angle from rotation matrix
    public static double fromMatrix(double[][] R) {
        return Math.atan2(R[1][0], R[0][0]);
    }

    // Numpy array representing a 2D rotation
    public static double[][] ndarray(double a) {
        return matrix(a);
    }

    // Angle from Numpy array representing a 2D rotation
    public static double fromNdarray(double[][] R) {
        return fromMatrix(R);
    }
}
