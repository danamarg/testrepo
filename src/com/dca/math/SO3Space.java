public class SO3Space extends GeodesicSpace {
    // The space of 3D rotations SO(3). The representation is 9 entries of the
    // rotation matrix, laid out in column-major form, like the math.so3 module.

    @Override
    public double intrinsicDimension() {
        return 3;
    }

    @Override
    public double extrinsicDimension() {
        return 9;
    }

    @Override
    public double distance(double[] a, double[] b) {
        return VectorOps.norm(SO3.error(a, b));
    }

    @Override
    public double[] interpolate(double[] a, double[] b, double u) {
        return SO3.interpolate(a, b, u);
    }

    @Override
    public double[] difference(double[] a, double[] b) {
        double[] w = SO3.error(a, b);
        return SO3.mul(SO3.cross_product(w), b);
    }

    @Override
    public double[] integrate(double[] x, double[] d) {
        double[] wcross = SO3.mul(d, SO3.inv(x));
        double[] w = SO3.deskew(wcross);
        return SO3.mul(SO3.from_moment(w), x);
    }
}
