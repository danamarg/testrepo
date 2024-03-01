public class SE3Space extends GeodesicSpace {
    // The space of 3D rigid transforms SE(3). The representation is 9 entries
    // of SO(3) + 3 entries of translation.

    @Override
    public double intrinsicDimension() {
        return 6;
    }

    @Override
    public double extrinsicDimension() {
        return 12;
    }

    private double[][] to_se3(double[] x) {
        double[][] T = new double[2][];
        T[0] = new double[9];
        T[1] = new double[3];
        System.arraycopy(x, 0, T[0], 0, 9);
        System.arraycopy(x, 9, T[1], 0, 3);
        return T;
    }

    private double[] from_se3(double[][] T) {
        double[] x = new double[12];
        System.arraycopy(T[0], 0, x, 0, 9);
        System.arraycopy(T[1], 0, x, 9, 3);
        return x;
    }

    @Override
    public double distance(double[] a, double[] b) {
        double[][] Ta = to_se3(a);
        double[][] Tb = to_se3(b);
        return VectorOps.norm(SE3.error(Ta, Tb));
    }

    @Override
    public double[] interpolate(double[] a, double[] b, double u) {
        double[][] r = SE3.interpolate(to_se3(a), to_se3(b), u);
        double[] result = new double[12];
        System.arraycopy(r[0], 0, result, 0, 9);
        System.arraycopy(r[1], 0, result, 9, 3);
        return result;
    }

    @Override
    public double[] difference(double[] a, double[] b) {
        double[][] Tb = to_se3(b);
        double[] w = SE3.error(to_se3(a), Tb);
        double[] Rb = Tb[0];
        double[] tb = Tb[1];
        double[] result = new double[12];
        System.arraycopy(SO3.mul(Rb, SO3.cross_product(Arrays.copyOfRange(w, 0, 3))), 0, result, 0, 3);
        System.arraycopy(w, 3, result, 3, 3);
        System.arraycopy(tb, 0, result, 9, 3);
        return result;
    }

    @Override
    public double[] integrate(double[] x, double[] d) {
        assert x.length == 12;
        double[][] Rx = to_se3(x);
        double[] w = SO3.deskew(SO3.mul(SO3.inv(Rx[0]), Arrays.copyOfRange(d, 0, 9)));
        double[] v = Arrays.copyOfRange(d, 9, 12);
        double[] wR = SO3.from_moment(w);
        double[][] Tx = to_se3(x);
        double[] R = SO3.mul(Tx[0], wR);
        double[] t = VectorOps.add(Tx[1], v);
        double[] result = new double[12];
        System.arraycopy(R, 0, result, 0, 9);
        System.arraycopy(t, 0, result, 9, 3);
        return result;
    }
}
