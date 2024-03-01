import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.IntStream;

public class GeodesicHermiteTrajectory extends Trajectory {
    GeodesicSpace geodesic;
    boolean _skip_deriv;

    public GeodesicHermiteTrajectory(GeodesicSpace geodesic,
                                      List<Float> times,
                                      List<Vector> milestones,
                                      List<Vector> outgoingLieDerivatives) {
        this.geodesic = geodesic;
        if (outgoingLieDerivatives != null) {
            assert milestones != null;
            for (int i = 0; i < milestones.size(); i++) {
                Vector concatenated = new Vector(milestones.get(i).size() + outgoingLieDerivatives.get(i).size());
                concatenated.addAll(milestones.get(i));
                concatenated.addAll(outgoingLieDerivatives.get(i));
                milestones.set(i, concatenated);
            }
        }
        if (milestones != null) {
            assert milestones.stream().allMatch(m -> m.size() == geodesic.extrinsicDimension() * 2) :
                    "Milestones must be a concatenation of the point and outgoing milestone";
        }
        super(times, milestones);
        this._skip_deriv = false;
    }

    public void makeSpline(Trajectory waypointTrajectory, boolean loop) {
        // Implement makeSpline method
        if (loop && !waypointTrajectory.milestones.get(waypointTrajectory.milestones.size() - 1)
                .equals(waypointTrajectory.milestones.get(0))) {
            System.out.println(waypointTrajectory.milestones.get(waypointTrajectory.milestones.size() - 1)
                    + "!=" + waypointTrajectory.milestones.get(0));
            throw new IllegalArgumentException("Asking for a loop trajectory but the endpoints don't match up");
        }
        List<Vector> velocities = new ArrayList<>();
        Trajectory t = waypointTrajectory;
        int d = t.milestones.get(0).size();
        float third = 1.0f / 3.0f;
        if (t.milestones.size() == 1) {
            velocities.add(new Vector(d).fill(0.0f));
        } else if (t.milestones.size() == 2) {
            if (loop) {
                Vector v = new Vector(d).fill(0.0f);
                velocities.add(v);
                velocities.add(v);
            } else {
                float s = (t.times.get(1) - t.times.get(0) != 0) ? (1.0f / (t.times.get(1) - t.times.get(0))) : 0;
                Vector v = geodesic.difference(t.milestones.get(1), t.milestones.get(0)).mul(s);
                velocities.add(v);
                Vector v2 = geodesic.difference(t.milestones.get(0), t.milestones.get(1)).mul(-s);
                velocities.add(v2);
            }
        } else {
            int N = waypointTrajectory.milestones.size();
            Iterator<Integer> it;
            if (loop) {
                it = Arrays.asList(-2).iterator();
            } else {
                it = IntStream.range(0, N - 2).iterator();
            }
            for (int p, i, n; it.hasNext(); ) {
                i = it.next();
                n = loop ? (i + 1) % N : (i + 1);
                if (p < 0) {
                    d = t.times.get(t.times.size() - 1) - t.times.get(t.times.size() - 2);
                } else {
                    d = t.times.get(i) - t.times.get(p);
                }
                assert d >= 0;
                float dtp = d;
                if (n <= i) {
                    dtn = t.times.get(1) - t.times.get(0);
                } else {
                    dtn = t.times.get(n) - t.times.get(i);
                }
                assert dtp >= 0 && dtn >= 0;
                float s2 = (dtn != 0) ? (1.0f / dtn) : 0;
                Vector v2 = geodesic.difference(t.milestones.get(n), t.milestones.get(i)).mul(s2);
                float s1 = (dtp != 0) ? (1.0f / dtp) : 0;
                Vector v1 = geodesic.difference(t.milestones.get(p), t.milestones.get(i)).mul(-s1);
                Vector v = (v1.add(v2)).mul(0.5f);
                velocities.add(v);
            }
            if (!loop) {
                Vector v0 = geodesic.difference(t.milestones.get(1), t.milestones.get(0))
                        .mul(1.0f / (t.times.get(1) - t.times.get(0)));
                Vector vn = geodesic.difference(t.milestones.get(t.milestones.size() - 2),
                        t.milestones.get(t.milestones.size() - 1))
                        .mul(-1.0f / (t.times.get(t.times.size() - 1) - t.times.get(t.times.size() - 2)));
                velocities.add(0, v0);
                velocities.add(vn);
            } else {
                assert velocities.size() == N;
            }
        }
        this.geodesic = geodesic;
        List<Float> times = new ArrayList<>(waypointTrajectory.times);
        List<Vector> milestones = new ArrayList<>(waypointTrajectory.milestones);
        this._skip_deriv = false;
        this.times = times;
        this.milestones = milestones;
    }

    public void makeBezier(Vector times, List<Vector> controlPoints) {
        // Implement makeBezier method
        int nsegs = times.size() - 1;
        if (nsegs * 3 + 1 != controlPoints.size()) {
            throw new IllegalArgumentException("To perform Bezier interpolation, need # of controlPoints to be 3*Nsegs+1");
        }
        List<Float> newTimes = new ArrayList<>();
        List<Vector> newMilestones = new ArrayList<>();
        List<Vector> outgoingLieDerivatives = new ArrayList<>();
        for (int i = 0; i < times.size() - 1; i++) {
            Vector a = controlPoints.get(i * 3);
            Vector b = controlPoints.get(i * 3 + 1);
            Vector c = controlPoints.get(i * 3 + 2);
            Vector d = controlPoints.get(i * 3 + 3);
            float dt = times.get(i + 1) - times.get(i);
            if (dt <= 0) {
                throw new IllegalArgumentException("Times must be strictly monotonically increasing");
            }
            Vector lieDeriv0 = geodesic.difference(b, a).mul(3 / dt);
            Vector lieDeriv1 = geodesic.difference(c, d).mul(-3 / dt);
            if (i > 0) {
                if (lieDeriv0.distance(outgoingLieDerivatives.get(outgoingLieDerivatives.size() - 1)) > 1e-4) {
                    // need to double up knot point
                    newTimes.add(newTimes.get(newTimes.size() - 1));
                    newMilestones.add(newMilestones.get(newMilestones.size() - 1));
                    outgoingLieDerivatives.add(lieDeriv0);
                }
            } else {
                newTimes.add(times.get(i));
                newMilestones.add(a);
                outgoingLieDerivatives.add(lieDeriv0);
            }
            newTimes.add(times.get(i + 1));
            newMilestones.add(d);
            outgoingLieDerivatives.add(lieDeriv1);
        }
        this.geodesic = geodesic;
        this.times = newTimes;
        this.milestones = newMilestones;
    }

    public Vector waypoint(Vector state) {
        // Implement waypoint method
        return state.subList(0, state.size() / 2);
    }

    public Vector interpolateState(Vector a, Vector b, float u, float dt) {
        // Implement interpolateState method
        int n = geodesic.extrinsicDimension();
        Vector c0 = a.subList(0, n);
        Vector v0 = a.subList(n, a.size());
        Vector c3 = b.subList(0, n);
        Vector v3 = b.subList(n, b.size());
        float third = 1.0f / 3.0f;
        Vector c1 = geodesic.integrate(c0, v0.mul(third * dt));
        Vector c2 = geodesic.integrate(c3, v3.mul(-third * dt));
        Vector d0 = geodesic.interpolate(c0, c1, u);
        Vector d1 = geodesic.interpolate(c1, c2, u);
        Vector d2 = geodesic.interpolate(c2, c3, u);
        Vector e0 = geodesic.interpolate(d0, d1, u);
        Vector e1 = geodesic.interpolate(d1, d2, u);
        Vector f = geodesic.interpolate(e0, e1, u);
        if (_skip_deriv) {
            return new Vector(n).fill(0.0f);
        } else {
            float eps = 1e-6f;
            float u2 = u + eps;
            Vector d0_ = geodesic.interpolate(c0, c1, u2);
            Vector d1_ = geodesic.interpolate(c1, c2, u2);
            Vector d2_ = geodesic.interpolate(c2, c3, u2);
            Vector e0_ = geodesic.interpolate(d0_, d1_, u2);
            Vector e1_ = geodesic.interpolate(d1_, d2_, u2);
            Vector f2 = geodesic.interpolate(e0_, e1_, u2);
            Vector v = f2.sub(f).mul(1.0f / eps);
            return f.add(v);
        }
    }

    public Vector differenceState(Vector a, Vector b, float u, float dt) {
        throw new UnsupportedOperationException("Can't do derivatives of Bezier geodesic yet");
    }

    public Vector evalState(float t, String endBehavior) {
        // Implement evalState method
        Vector result = eval(t, endBehavior);
        return new Vector(result.subList(0, result.size() / 2));
    }

    public float length() {
        // Implement length method
        int n = geodesic.extrinsicDimension();
        float l = 0;
        for (int i = 0; i < milestones.size() - 1; i++) {
            float dt = times.get(i + 1) - times.get(i);
            Vector c0 = milestones.get(i).subList(0, n);
            Vector v0 = milestones.get(i).subList(n, milestones.get(i).size()).mul(dt);
            Vector c3 = milestones.get(i + 1).subList(0, n);
            Vector v3 = milestones.get(i + 1).subList(n, milestones.get(i + 1).size()).mul(dt);
            float third = 1.0f / 3.0f;
            Vector c1 = geodesic.integrate(c0, v0, third);
            Vector c2 = geodesic.integrate(c3, v3, -third);
            l += geodesic.distance(c0, c1);
            l += geodesic.distance(c1, c2);
            l += geodesic.distance(c2, c3);
        }
        return l;
    }

    public GeodesicTrajectory discretize(float dt) {
        // Implement discretize method
        List<Float> discreteTimes = new ArrayList<>();
        List<Vector> discreteMilestones = new ArrayList<>();
        this._skip_deriv = true;
        for (float t = times.get(0); t < times.get(times.size() - 1); t += dt) {
            Vector state = evalState(t, "halt");
            discreteTimes.add(t);
            discreteMilestones.add(state);
        }
        return new GeodesicTrajectory(geodesic, discreteTimes, discreteMilestones);
    }

    public float calculateLength() {
        // Implement calculateLength method
        int n = geodesic.extrinsicDimension();
        float third = 1.0f / 3.0f;
        Function<Vector, Float> distance = (x, y) -> {
            Vector cp0 = x.subList(0, n);
            Vector cp1 = geodesic.integrate(cp0, x.subList(n, x.size()).mul(third));
            Vector cp3 = y.subList(0, n);
            Vector cp2 = geodesic.integrate(cp3, y.subList(n, y.size()).mul(-third));
            return geodesic.distance(cp0, cp1) + geodesic.distance(cp1, cp2) + geodesic.distance(cp2, cp3);
        };
        return super.length(distance);
    }

    public void checkValid() {
        // Implement checkValid method
        super.checkValid();
        int n = geodesic.extrinsicDimension();
        for (Vector milestone : milestones) {
            if (milestone.size() != n * 2) {
                throw new IllegalArgumentException("Invalid length of milestone: " + milestone.size() + " != " + n * 2);
            }
        }
    }

    public void extractDofs(Vector dofs) {
        // Implement extractDofs method
        throw new UnsupportedOperationException("Cannot extract DOFs from a GeodesicHermiteTrajectory");
    }

    public void stackDofs(List<GeodesicHermiteTrajectory> trajs, boolean strict) {
        // Implement stackDofs method
        throw new UnsupportedOperationException("Cannot stack DOFs for a GeodesicHermiteTrajectory");
    }

    public Object constructor() {
        return (times, milestones) -> new GeodesicHermiteTrajectory(this.geodesic, (List<Float>) times, (List<Vector>) milestones, null);
    }