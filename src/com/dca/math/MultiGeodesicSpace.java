import java.util.ArrayList;
import java.util.List;

public class MultiGeodesicSpace extends GeodesicSpace {
    // This forms the cartesian product of one or more GeodesicSpace's.
    // Distances are simply added together.

    private List<GeodesicSpace> components;
    private List<Integer> componentWeights;

    public MultiGeodesicSpace(GeodesicSpace... components) {
        this.components = new ArrayList<>();
        for (GeodesicSpace component : components) {
            this.components.add(component);
        }
        this.componentWeights = new ArrayList<>();
        for (int i = 0; i < this.components.size(); i++) {
            this.componentWeights.add(1);
        }
    }

    @Override
    public double intrinsicDimension() {
        double sum = 0;
        for (GeodesicSpace component : components) {
            sum += component.intrinsicDimension();
        }
        return sum;
    }

    @Override
    public double extrinsicDimension() {
        double sum = 0;
        for (GeodesicSpace component : components) {
            sum += component.extrinsicDimension();
        }
        return sum;
    }

    public List<double[]> split(double[] x) {
        int i = 0;
        List<double[]> res = new ArrayList<>();
        for (GeodesicSpace c : components) {
            int d = (int) c.extrinsicDimension();
            double[] component = new double[d];
            System.arraycopy(x, i, component, 0, d);
            res.add(component);
            i += d;
        }
        return res;
    }

    public double[] join(List<double[]> xs) {
        int len = xs.stream().mapToInt(arr -> arr.length).sum();
        double[] res = new double[len];
        int index = 0;
        for (double[] arr : xs) {
            System.arraycopy(arr, 0, res, index, arr.length);
            index += arr.length;
        }
        return res;
    }

    @Override
    public double distance(double[] a, double[] b) {
        int i = 0;
        double res = 0.0;
        for (int j = 0; j < components.size(); j++) {
            GeodesicSpace c = components.get(j);
            int d = (int) c.extrinsicDimension();
            res += Math.pow(c.distance(sliceArray(a, i, i + d), sliceArray(b, i, i + d)), 2) * componentWeights.get(j);
            i += d;
        }
        return Math.sqrt(res);
    }

    @Override
    public double[] interpolate(double[] a, double[] b, double u) {
        int i = 0;
        double[] res = new double[a.length];
        for (GeodesicSpace c : components) {
            int d = (int) c.extrinsicDimension();
            double[] interpolation = c.interpolate(sliceArray(a, i, i + d), sliceArray(b, i, i + d), u);
            System.arraycopy(interpolation, 0, res, i, d);
            i += d;
        }
        return res;
    }

    @Override
    public double[] difference(double[] a, double[] b) {
        int i = 0;
        double[] res = new double[a.length];
        for (GeodesicSpace c : components) {
            int d = (int) c.extrinsicDimension();
            double[] difference = c.difference(sliceArray(a, i, i + d), sliceArray(b, i, i + d));
            System.arraycopy(difference, 0, res, i, d);
            i += d;
        }
        return res;
    }

    @Override
    public double[] integrate(double[] x, double[] diff) {
        int i = 0;
        double[] res = new double[x.length];
        for (GeodesicSpace c : components) {
            int d = (int) c.extrinsicDimension();
            double[] integration = c.integrate(sliceArray(x, i, i + d), sliceArray(diff, i, i + d));
            System.arraycopy(integration, 0, res, i, d);
            i += d;
        }
        return res;
    }

    private double[] sliceArray(double[] array, int start, int end) {
        double[] result = new double[end - start];
        System.arraycopy(array, start, result, 0, end - start);
        return result;
    }
}
