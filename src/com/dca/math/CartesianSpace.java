public class CartesianSpace extends GeodesicSpace {
    // The standard geodesic on R^d
    private int d;

    public CartesianSpace(int d) {
        this.d = d;
    }

    @Override
    public double intrinsicDimension() {
        return d;
    }

    @Override
    public double extrinsicDimension() {
        return d;
    }
}
