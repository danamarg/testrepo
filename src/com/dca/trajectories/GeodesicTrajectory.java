import java.util.List;
import java.util.Optional;

public class GeodesicTrajectory extends Trajectory {
    private final GeodesicSpace geodesic;

    public GeodesicTrajectory(GeodesicSpace geodesic, List<Float> times, List<Vector> milestones) {
        super(times, milestones);
        this.geodesic = geodesic;
    }

    @Override
    protected Vector interpolateState(Vector a, Vector b, double u, double dt) {
        return geodesic.interpolate(a, b, u);
    }

    @Override
    protected Vector differenceState(Vector a, Vector b, double u, double dt) {
        Vector x = interpolateState(b, a, u, dt);
        return vectorops.mul(vectorops.sub(geodesic.difference(a, x), geodesic.difference(b, x)), 1.0 / dt);
    }

    public GeodesicTrajectory constructor() {
        return new GeodesicTrajectory(geodesic, null, null);
    }

    @Override
    public double length() {
        return length(null);
    }

    @Override
    public double length(Metric metric) {
        return (metric == null) ? super.length(geodesic.distance()) : super.length(metric);
    }

    @Override
    public void checkValid() {
        super.checkValid();
        try {
            int d = geodesic.extrinsicDimension();
            for (Vector m : milestones) {
                if (m.size() != d) {
                    throw new IllegalArgumentException("Milestone length doesn't match geodesic space's dimension: " + m.size() + " != " + d);
                }
            }
        } catch (UnsupportedOperationException e) {
            // Extrinsic dimension not implemented, ignore
        }
    }

    @Override
    public void extractDofs(List<Integer> dofs) {
        throw new UnsupportedOperationException("Cannot extract DOFs from a GeodesicTrajectory");
    }

    @Override
    public void stackDofs(List<GeodesicTrajectory> trajs) {
        stackDofs(trajs, false);
        try {
            checkValid();
        } catch (IllegalArgumentException e) {
            System.out.println("GeodesicTrajectory.stackDofs: the result doesn't match the geodesic's dimension");
        }
    }
}
