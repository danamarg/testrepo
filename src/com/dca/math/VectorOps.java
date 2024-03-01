import java.util.Arrays;

public class VectorOps {

    public static double[] add(double[]... items) {
        if (items.length == 0) return new double[0];
        int n = items[0].length;
        for (double[] v : items) {
            if (n != v.length) {
                throw new RuntimeException("Vector dimensions not equal");
            }
        }
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            for (double[] v : items) {
                result[i] += v[i];
            }
        }
        return result;
    }

    public static double[] madd(double[] a, double[] b, double c) {
        if (a.length != b.length) {
            throw new RuntimeException("Vector dimensions not equal");
        }
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] + c * b[i];
        }
        return result;
    }

    public static double[] sub(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new RuntimeException("Vector dimensions not equal");
        }
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] - b[i];
        }
        return result;
    }

    public static double[] mul(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new RuntimeException("Vector dimensions not equal");
        }
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] * b[i];
        }
        return result;
    }

    public static double[] div(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new RuntimeException("Vector dimensions not equal");
        }
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] / b[i];
        }
        return result;
    }

    public static double[] maximum(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = Math.max(a[i], b[i]);
        }
        return result;
    }

    public static double[] minimum(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = Math.min(a[i], b[i]);
        }
        return result;
    }

    public static double dot(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new RuntimeException("Vector dimensions not equal");
        }
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    public static double normSquared(double[] a) {
        double sum = 0;
        for (double ai : a) {
            sum += ai * ai;
        }
        return sum;
    }

    public static double norm(double[] a) {
        return Math.sqrt(normSquared(a));
    }

    public static double[] unit(double[] a, double epsilon) {
        double n = norm(a);
        if (n > epsilon) {
            return Arrays.stream(a).map(ai -> ai / n).toArray();
        }
        return Arrays.copyOf(a, a.length);
    }

    public static double[] unit(double[] a) {
        return unit(a, 1e-5);
    }

    public static double[] interpolate(double[] a, double[] b, double u) {
        return madd(a, mul(sub(b, a), u));
    }

    public static double normL1(double[] a) {
        double sum = 0;
        for (double ai : a) {
            sum += Math.abs(ai);
        }
        return sum;
    }

    public static double normLinf(double[] a) {
        double max = Double.NEGATIVE_INFINITY;
        for (double ai : a) {
            max = Math.max(max, Math.abs(ai));
        }
        return max;
    }

    public static double distanceSquared(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new RuntimeException("Vector dimensions not equal");
        }
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return sum;
    }

    public static double distance(double[] a, double[] b) {
        return Math.sqrt(distanceSquared(a, b));
    }

    public static double[] cross(double[] a, double[] b) {
        if (a.length != b.length || (a.length != 2 && a.length != 3)) {
            throw new RuntimeException("Vectors must be 2D or 3D");
        }
        if (a.length == 2) {
            return new double[]{a[0] * b[1] - a[1] * b[0]};
        }
        return new double[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }
}
