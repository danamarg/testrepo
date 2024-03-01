import java.util.List;
import java.util.function.Function;

public class RigidTransformOperationsSE3 {
    // Identity transformation
    public static double[][] identity() {
        return new double[][]{{1.,0.,0.},{0.,1.,0.},{0.,0.,1.}};
    }

    // Inverse transformation
    public static double[][] inv(double[][] T) {
        double[][] R = new double[3][3];
        double[] t = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                R[i][j] = T[j][i]; // Transpose of rotation matrix
            }
            t[i] = -(R[i][0] * T[0][3] + R[i][1] * T[1][3] + R[i][2] * T[2][3]); // Inverse translation
        }
        return new double[][]{R[0], R[1], R[2], t};
    }

    // Apply transformation to a point
    public static double[] apply(double[][] T, double[] point) {
        double[] transformedPoint = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                transformedPoint[i] += T[i][j] * point[j];
            }
            transformedPoint[i] += T[i][3]; // Translation part
        }
        return transformedPoint;
    }

    // Apply rotation part of transformation
    public static double[] applyRotation(double[][] T, double[] point) {
        double[][] R = new double[][]{T[0], T[1], T[2]};
        return apply(R, point);
    }

    // Extract rotation matrix from transformation
    public static double[][] rotation(double[][] T) {
        return new double[][]{T[0], T[1], T[2]};
    }

    // Create transformation from rotation matrix
    public static double[][] fromRotation(double[][] mat) {
        return new double[][]{mat[0], mat[1], mat[2], {0., 0., 0.}};
    }

    // Extract translation vector from transformation
    public static double[] translation(double[][] T) {
        return T[3];
    }

    // Create transformation from translation vector
    public static double[][] fromTranslation(double[] t) {
        return new double[][]{{1., 0., 0.}, {0., 1., 0.}, {0., 0., 1.}, t};
    }

    // Convert transformation to homogeneous matrix
    public static double[][] homogeneous(double[][] T) {
        return new double[][]{{T[0][0], T[0][1], T[0][2], T[0][3]},
                {T[1][0], T[1][1], T[1][2], T[1][3]},
                {T[2][0], T[2][1], T[2][2], T[2][3]},
                {0., 0., 0., 1.}};
    }

    // Create transformation from homogeneous matrix
    public static double[][] fromHomogeneous(double[][] mat) {
        return new double[][]{{mat[0][0], mat[0][1], mat[0][2]},
                {mat[1][0], mat[1][1], mat[1][2]},
                {mat[2][0], mat[2][1], mat[2][2]}};
    }

    // Compose two transformations
    public static double[][] mul(double[][] T1, double[][] T2) {
        double[][] R1 = new double[][]{T1[0], T1[1], T1[2]};
        double[][] R2 = new double[][]{T2[0], T2[1], T2[2]};
        double[] t1 = T1[3];
        double[] t2 = T2[3];
        double[][] R = so3.mul(R1, R2);
        double[] t = VectorOps.add(VectorOps.apply(R1, t2), t1);
        return new double[][]{R[0], R[1], R[2], t};
    }

    // Distance metric between two transformations
    public static double distance(double[][] T1, double[][] T2, double Rweight, double tweight) {
        double[] t1 = T1[3];
        double[] t2 = T2[3];
        double rotationDistance = so3.distance(rotation(T1), rotation(T2)) * Rweight;
        double translationDistance = VectorOps.distance(t1, t2) * tweight;
        return rotationDistance + translationDistance;
    }

    // Difference vector between two transformations
    public static double[] error(double[][] T1, double[][] T2) {
        double[] t1 = T1[3];
        double[] t2 = T2[3];
        double[] rotationError = so3.error(rotation(T1), rotation(T2));
        return VectorOps.sub(t1, t2);
    }

    // Interpolate between two transformations
    public static double[][] interpolate(double[][] T1, double[][] T2, double u) {
        double[][] interpolatedRotation = so3.interpolate(rotation(T1), rotation(T2), u);
        double[] interpolatedTranslation = VectorOps.interpolate(translation(T1), translation(T2), u);
        return new double[][]{interpolatedRotation[0], interpolatedRotation[1], interpolatedRotation[2], interpolatedTranslation};
    }

    // Interpolator function for two transformations
    public static Function<Double, double[][]> interpolator(double[][] T1, double[][] T2) {
        double[][] R1 = new double[][]{T1[0], T1[1], T1[2]};
        double[][] R2 = new double[][]{T2[0], T2[1], T2[2]};
        double[] t1 = T1[3];
        double[] t2 = T2[3];
        double[] dt = VectorOps.sub(t2, t1);
        return (Double u) -> {
            double[][] interpolatedRotation = so3.interpolator(R1, R2).apply(u);
            double[] interpolatedTranslation = VectorOps.madd(t1, dt, u);
            return new double[][]{interpolatedRotation[0], interpolatedRotation[1], interpolatedRotation[2], interpolatedTranslation};
        };
    }
}
