import java.util.ArrayList;
import java.util.List;

public class Spline {
    
    
    public static double[] hermiteEval(double[] x1, double[] v1, double[] x2, double[] v2, double u) {
        assert x1.length == v1.length && x1.length == x2.length && x1.length == v2.length;
        double u2 = u * u;
        double u3 = u * u * u;
        double cx1 = 2.0 * u3 - 3.0 * u2 + 1.0;
        double cx2 = -2.0 * u3 + 3.0 * u2;
        double cv1 = u3 - 2.0 * u2 + u;
        double cv2 = u3 - u2;
        double[] x = new double[x1.length];
        for (int i = 0; i < x1.length; i++) {
            x[i] = cx1 * x1[i] + cx2 * x2[i] + cv1 * v1[i] + cv2 * v2[i];
        }
        return x;
    }

    public static double[] hermiteDeriv(double[] x1, double[] v1, double[] x2, double[] v2, double u, int order) {
        assert x1.length == v1.length && x1.length == x2.length && x1.length == v2.length;
        if (order == 1) {
            double u2 = u * u;
            double dcx1 = 6.0 * u2 - 6.0 * u;
            double dcx2 = -6.0 * u2 + 6.0 * u;
            double dcv1 = 3.0 * u2 - 4.0 * u + 1.0;
            double dcv2 = 3.0 * u2 - 2.0 * u;
            double[] dx = new double[x1.length];
            for (int i = 0; i < x1.length; i++) {
                dx[i] = dcx1 * x1[i] + dcx2 * x2[i] + dcv1 * v1[i] + dcv2 * v2[i];
            }
            return dx;
        } else if (order == 2) {
            double ddcx1 = 12 * u - 6.0;
            double ddcx2 = -12.0 * u + 6.0;
            double ddcv1 = 6.0 * u - 4.0;
            double ddcv2 = 6.0 * u - 2.0;
            double[] ddx = new double[x1.length];
            for (int i = 0; i < x1.length; i++) {
                ddx[i] = ddcx1 * x1[i] + ddcx2 * x2[i] + ddcv1 * v1[i] + ddcv2 * v2[i];
            }
            return ddx;
        } else if (order == 3) {
            double cx1 = 12;
            double cx2 = -12.0;
            double cv1 = 6.0;
            double cv2 = 6.0;
            double[] dddx = new double[x1.length];
            for (int i = 0; i < x1.length; i++) {
                dddx[i] = cx1 * x1[i] + cx2 * x2[i] + cv1 * v1[i] + cv2 * v2[i];
            }
            return dddx;
        } else if (order == 0) {
            return hermiteEval(x1, v1, x2, v2, u);
        } else {
            return new double[x1.length];
        }
    }

    public static double[][][] hermiteSubdivide(double[] x1, double[] v1, double[] x2, double[] v2, double u) {
        double[] xm = hermiteEval(x1, v1, x2, v2, u);
        double[] vm = hermiteDeriv(x1, v1, x2, v2, u, 1);
        double[][] firstCurve = new double[][] {x1, vectorops.mul(v1, u), xm, vectorops.mul(vm, u)};
        double[][] secondCurve = new double[][] {xm, vectorops.mul(vm, 1.0 - u), x2, vectorops.mul(v2, 1.0 - u)};
        return new double[][][] {firstCurve, secondCurve};
    }

    public static double hermiteLengthBound(double[] x1, double[] v1, double[] x2, double[] v2) {
        double[][] bezierCurve = hermiteToBezier(x1, v1, x2, v2);
        return bezierLengthBound(bezierCurve[0], bezierCurve[1], bezierCurve[2], bezierCurve[3]);
    }

    public static double[][] hermiteToBezier(double[] x1, double[] v1, double[] x2, double[] v2) {
        double[] c1 = vectorops.madd(x1, v1, 1.0 / 3.0);
        double[] c2 = vectorops.madd(x2, v2, -1.0 / 3.0);
        return new double[][] {x1, c1, c2, x2};
    }

    public static double[][][] bezierSubdivide(double[] x1, double[] x2, double[] x3, double[] x4, double u) {
        double[] p1 = vectorops.interpolate(x1, x2, u);
        double[] p2 = vectorops.interpolate(x2, x3, u);
        double[] p3 = vectorops.interpolate(x3, x4, u);
        double[] q1 = vectorops.interpolate(p1, p2, u);
        double[] q2 = vectorops.interpolate(p2, p3, u);
        double[] r1 = vectorops.interpolate(q1, q2, u);
        double[][] firstCurve = new double[][] {x1, p1, q1, r1};
        double[][] secondCurve = new double[][] {r1, q2, p3, x4};
        return new double[][][] {firstCurve, secondCurve};
    }

    public static double bezierLengthBound(double[] x1, double[] x2, double[] x3, double[] x4) {
        return vectorops.distance(x1, x2) + vectorops.distance(x2, x3) + vectorops.distance(x3, x4);
    }

      public static List<double[]> bezierDiscretize(double[] x1, double[] x2, double[] x3, double[] x4, double res, boolean returnParams) {
        List<double[]> stack = new ArrayList<>();
        stack.add(new double[][] {x1, x2, x3, x4});
        List<double[]> path = new ArrayList<>();
        List<Double> params = new ArrayList<>();
        if (returnParams) {
            params.add(0.0);
        }
        while (!stack.isEmpty()) {
            double[][] c = stack.remove(stack.size() - 1);
            if (returnParams) {
                double[] ab = params.remove(params.size() - 1);
                double a = ab[0];
                double b = ab[1];
                if (bezierLengthBound(c[0], c[1], c[2], c[3]) > res) {
                    double[][][] halves = bezierSubdivide(c[0], c[1], c[2], c[3]);
                    stack.add(halves[1]);
                    stack.add(halves[0]);
                    double m = (a + b) * 0.5;
                    params.add(new double[] {m, b});
                    params.add(new double[] {a, m});
                } else {
                    assert path.isEmpty() || path.get(path.size() - 1) == c[0];
                    path.add(c[1]);
                    path.add(c[2]);
                    path.add(c[3]);
                    params.add(b);
                }
            } else {
                if (bezierLengthBound(c[0], c[1], c[2], c[3]) > res) {
                    double[][][] halves = bezierSubdivide(c[0], c[1], c[2], c[3]);
                    stack.add(halves[1]);
                    stack.add(halves[0]);
                } else {
                    assert path.isEmpty() || path.get(path.size() - 1) == c[0];
                    path.add(c[1]);
                    path.add(c[2]);
                    path.add(c[3]);
                }
            }
        }
        if (returnParams) {
            return List.of(path, params);
        }
        return path;
    }

    public static double[][] bezierToHermite(double[] x1, double[] x2, double[] x3, double[] x4) {
        double[] v1 = vectorops.mul(vectorops.sub(x2, x1), 3.0);
        double[] v2 = vectorops.mul(vectorops.sub(x4, x3), 3.0);
        return new double[][] {x1, v1, x4, v2};
    }
}
