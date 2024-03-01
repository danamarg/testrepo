public abstract class GeodesicSpace {
    // Base class for geodesic spaces.
    // A geodesic is equipped with a geodesic (interpolation via the interpolate(a,b,u) method),
    // a natural arc length distance metric (distance(a,b) method), an intrinsic dimension
    // (intrinsicDimension() method), an extrinsic dimension (extrinsicDimension() method),
    // and natural tangents (the difference and integrate methods).

    public abstract double intrinsicDimension();
    public abstract double extrinsicDimension();
    public double distance(double[] a, double[] b) {
        return VectorOps.distance(a, b);
    }
    public double[] interpolate(double[] a, double[] b, double u) {
        return VectorOps.interpolate(a, b, u);
    }
    public double[] difference(double[] a, double[] b) {
        // For Lie groups, returns a difference vector that, when integrated would get to a from b.
        // In Cartesian spaces it is a-b.  In other spaces, it should be d/du interpolate(b,a,u) at u=0.
        return VectorOps.sub(a, b);
    }
    public double[] integrate(double[] x, double[] d) {
        // For Lie groups, returns the point that would be arrived at via integrating the difference vector d starting from x.
        // Must satisfy the relationship a = integrate(b,difference(a,b)).
        // In Cartesian spaces it is x+d
        return VectorOps.add(x, d);
    }