public class SO2Space extends GeodesicSpace {
    // The space of 2D rotations SO(2).

    @Override
    public double intrinsicDimension() {
        return 1;
    }

    @Override
    public double extrinsicDimension() {
        return 1;
    }

    @Override
    public double distance(double[] a, double[] b) {
        return Math.abs(SO2.diff(a[0], b[0]));
    }

    @Override
    public double[] interpolate(double[] a, double[] b, double u) {
        return new double[]{SO2.interp(a[0], b[0], u)};
    }

    @Override
    public double[] difference(double[] a, double[] b) {
        return new double[]{SO2.diff(a[0], b[0])};
    }

    @Override
    public double[] integrate(double[] x, double[] d) {
        return new double[]{SO2.normalize(x[0] + d[0])};
    }