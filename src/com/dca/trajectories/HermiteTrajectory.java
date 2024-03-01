import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HermiteTrajectory extends Trajectory {

    public HermiteTrajectory(List<Double> times, List<Vector> milestones, List<Vector> dmilestones) {
        if (dmilestones == null) {
            super(times, milestones);
        } else {
            assert milestones != null;
            this.times = times;
            this.milestones = Vector.addLists(milestones, dmilestones);
        }
    }

    public void makeSpline(Trajectory waypointTrajectory, boolean preventOvershoot, boolean loop) {
        List<Vector> velocities = new ArrayList<>();
        Trajectory t = waypointTrajectory;
        int d = t.milestones.get(0).size();
        if (t.milestones.size() == 1) {
            velocities.add(new Vector(new double[d]));
        } else if (t.milestones.size() == 2) {
            if (loop) {
                velocities.add(new Vector(new double[d]));
            } else {
                double s = (t.times.get(1) - t.times.get(0) != 0) ? 1.0 / (t.times.get(1) - t.times.get(0)) : 0;
                Vector v = Vector.mul(Vector.sub(t.milestones.get(1), t.milestones.get(0)), s);
                velocities.add(v);
                velocities.add(v);
            }
        } else {
            double third = 1.0 / 3.0;
            int N = t.milestones.size();
            List<Integer[]> timeiter = new ArrayList<>();
            if (loop) {
                timeiter.add(new Integer[]{-2, 0, 1});
                for (int i = 1; i < N - 1; i++) {
                    timeiter.add(new Integer[]{i - 1, i, i + 1});
                }
                timeiter.add(new Integer[]{N - 2, N - 1, 0});
            } else {
                for (int i = 0; i < N - 2; i++) {
                    timeiter.add(new Integer[]{i, i + 1, i + 2});
                }
            }
            for (Integer[] indices : timeiter) {
                int p = indices[0];
                int i = indices[1];
                int n = indices[2];
                double dtp = (p < 0) ? t.times.get(t.times.size() - 1) - t.times.get(t.times.size() - 2) : t.times.get(i) - t.times.get(p);
                double dtn = (n <= i) ? t.times.get(1) - t.times.get(0) : t.times.get(n) - t.times.get(i);
                double s = (dtp + dtn != 0) ? 1.0 / (dtp + dtn) : 0;
                Vector v = Vector.mul(Vector.sub(t.milestones.get(n), t.milestones.get(p)), s);
                if (preventOvershoot) {
                    for (int j = 0; j < d; j++) {
                        double x = t.milestones.get(i).get(j);
                        double a = Math.min(t.milestones.get(p).get(j), t.milestones.get(n).get(j));
                        double b = Math.max(t.milestones.get(p).get(j), t.milestones.get(n).get(j));
                        if (x <= a || x >= b || (v.get(j) < 0 && x - v.get(j) * third * dtp >= a) || (v.get(j) > 0 && x - v.get(j) * third * dtp <= a)
                                || (v.get(j) < 0 && x + v.get(j) * third * dtn < b) || (v.get(j) > 0 && x + v.get(j) * third * dtn > b)) {
                            v.set(j, 0.0);
                        }
                    }
                }
                velocities.add(v);
            }
            if (!loop) {
                Vector x2 = Vector.madd(t.milestones.get(1), velocities.get(0), -third * (t.times.get(1) - t.times.get(0)));
                Vector x1 = Vector.madd(x2, Vector.sub(t.milestones.get(1), t.milestones.get(0)), -third);
                Vector v0 = Vector.mul(Vector.sub(x1, t.milestones.get(0)), 3.0 / (t.times.get(1) - t.times.get(0)));
                Vector xn_2 = Vector.madd(t.milestones.get(N - 2), velocities.get(velocities.size() - 1), third * (t.times.get(N - 1) - t.times.get(N - 2)));
                Vector xn_1 = Vector.madd(xn_2, Vector.sub(t.milestones.get(N - 1), t.milestones.get(N - 2)), third);
                Vector vn = Vector.mul(Vector.sub(t.milestones.get(N - 1), xn_1), 3.0 / (t.times.get(N - 1) - t.times.get(N - 2)));
                velocities.add(0, v0);
                velocities.add(vn);
            }
        }
        this.times = new ArrayList<>(waypointTrajectory.times);
        this.milestones = new ArrayList<>(waypointTrajectory.milestones);
        this.milestones.addAll(velocities);
    }

    public void makeBezier(List<Double> times, List<Vector> controlPoints) {
        int nsegs = times.size() - 1;
        if (nsegs * 3 + 1 != controlPoints.size()) {
            throw new IllegalArgumentException("To perform Bezier interpolation, need # of controlPoints to be 3*Nsegs+1");
        }
        List<Double> newtimes = new ArrayList<>();
        List<Vector> milestones = new ArrayList<>();
        List<Vector> outgoingVelocities = new ArrayList<>();
        for (int i = 0; i < times.size() - 1; i++) {
            Vector a = controlPoints.get(i * 3);
            Vector b = controlPoints.get(i * 3 + 1);
            Vector c = controlPoints.get(i * 3 + 2);
            Vector d = controlPoints.get(i * 3 + 3);
            double dt = times.get(i + 1) - times.get(i);
            if (dt <= 0) {
                throw new IllegalArgumentException("Times must be strictly monotonically increasing");
            }
            Vector lieDeriv0 = Vector.mul(Vector.sub(b, a), 3 / dt);
            Vector lieDeriv1 = Vector.mul(Vector.sub(c, d), -3 / dt);
            if (i > 0) {
                if (Vector.distance(lieDeriv0, outgoingVelocities.get(outgoingVelocities.size() - 1)) > 1e-4) {
                    // need to double up knot point
                    newtimes.add(newtimes.get(newtimes.size() - 1));
                    milestones.add(milestones.get(milestones.size() - 1));
                    outgoingVelocities.add(lieDeriv0);
                }
            } else {
                newtimes.add(times.get(i));
                milestones.add(a);
                outgoingVelocities.add(lieDeriv0);
            }
            newtimes.add(times.get(i + 1));
            milestones.add(d);
            outgoingVelocities.add(lieDeriv1);
        }
        this.times = newtimes;
        this.milestones = milestones;
        this.milestones.addAll(outgoingVelocities);
    }

    public void makeMinTimeSpline(List<Vector> milestones, List<Vector> velocities,
            Vector xmin, Vector xmax, Vector vmax, Vector amax) {
        if (vmax == null && amax == null) {
            throw new IllegalArgumentException("Either vmax or amax must be provided");
        }
        if (milestones.isEmpty() || milestones.get(0).isEmpty()) {
            throw new IllegalArgumentException("Milestones need to be provided and at least 1-d");
        }
        int n = milestones.get(0).size();
        for (Vector m : milestones) {
            if (m.size() != n) {
                throw new IllegalArgumentException("Invalid size of milestone");
            }
        }
        if (velocities != null) {
            if (velocities.size() != milestones.size()) {
                throw new IllegalArgumentException("Velocities need to have the same size as milestones");
            }
            for (Vector v : velocities) {
                if (v.size() != n) {
                    throw new IllegalArgumentException("Invalid size of velocity milestone");
                }
            }
        }
        double inf = Double.POSITIVE_INFINITY;
        if (xmin == null) {
            xmin = new Vector(n);
            xmin.fill(Double.NEGATIVE_INFINITY);
        } else {
            if (xmin.size() != n) {
                throw new IllegalArgumentException("Invalid size of lower bound");
            }
        }
        if (xmax == null) {
            xmax = new Vector(n);
            xmax.fill(Double.POSITIVE_INFINITY);
        } else {
            if (xmax.size() != n) {
                throw new IllegalArgumentException("Invalid size of upper bound");
            }
        }
        if (vmax == null) {
            vmax = new Vector(n);
            vmax.fill(Double.POSITIVE_INFINITY);
        } else {
            if (vmax.size() != n) {
                throw new IllegalArgumentException("Invalid size of velocity bound");
            }
        }
        if (amax == null) {
            // do a piecewise linear interpolation, ignore x bounds
            throw new UnsupportedOperationException("TODO: amax = null case");
        } else {
            if (amax.size() != n) {
                throw new IllegalArgumentException("Invalid size of acceleration bound");
            }
            Vector zeros = new Vector(n);
            List<Double> newTimes = new ArrayList<>();
            List<Vector> newMilestones = new ArrayList<>();
            List<Vector> newVelocities = new ArrayList<>();
            for (int i = 0; i < milestones.size() - 1; i++) {
                Vector m0 = milestones.get(i);
                Vector m1 = milestones.get(i + 1);
                if (velocities == null) {
                    List<Double> ts = new ArrayList<>();
                    List<Vector> xs = new ArrayList<>();
                    List<Vector> vs = new ArrayList<>();
                    motionplanning.interpolate_nd_min_time_linear(m0, m1, vmax, amax, ts, xs, vs);
                    for (int j = 1; j < ts.size(); j++) {
                        newTimes.add(newTimes.get(newTimes.size() - 1) + ts.get(j));
                        newMilestones.add(xs.get(j));
                        newVelocities.add(vs.get(j));
                    }
                } else {
                    Vector v0 = velocities.get(i);
                    Vector v1 = velocities.get(i + 1);
                    List<Double> ts = new ArrayList<>();
                    List<Vector> xs = new ArrayList<>();
                    List<Vector> vs = new ArrayList<>();
                    motionplanning.interpolate_nd_min_time(m0, v0, m1, v1, xmin, xmax, vmax, amax, ts, xs, vs);
                    ts.remove(0);
                    for (int j = 0; j < ts.size(); j++) {
                        newTimes.add(newTimes.get(newTimes.size() - 1) + ts.get(j));
                        newMilestones.add(xs.get(j));
                        newVelocities.add(vs.get(j));
                    }
                }
            }
            this.times = newTimes;
            this.milestones = newMilestones;
            this.milestones.addAll(newVelocities);
        }
    }



    public Vector waypoint(Vector state) {
        return state.subList(0, state.size() / 2);
    }

    public Vector eval_state(double t, String endBehavior) {
        return super.eval_state(t, endBehavior);
    }

    public Vector eval(double t, String endBehavior) {
        Vector res = eval_state(t, endBehavior);
        return res.subList(0, res.size() / 2);
    }

    public Vector deriv(double t, String endBehavior) {
        Vector res = eval_state(t, endBehavior);
        return res.subList(res.size() / 2, res.size());
    }

    public Vector eval_accel(double t, String endBehavior) {
        Vector res = deriv_state(t, endBehavior);
        return res.subList(res.size() / 2, res.size());
    }

    public Vector interpolate_state(Vector a, Vector b, double u, double dt) {
        assert a.size() == b.size();
        Vector x1 = a.subList(0, a.size() / 2);
        Vector v1 = Vector.mul(a.subList(a.size() / 2, a.size()), dt);
        Vector x2 = b.subList(0, b.size() / 2);
        Vector v2 = Vector.mul(b.subList(b.size() / 2, b.size()), dt);
        Vector x = Spline.hermite_eval(x1, v1, x2, v2, u);
        Vector dx = Vector.mul(Spline.hermite_deriv(x1, v1, x2, v2, u), 1.0 / dt);
        return Vector.add(x, dx);
    }

    public Vector difference_state(Vector a, Vector b, double u, double dt) {
        assert a.size() == b.size();
        Vector x1 = a.subList(0, a.size() / 2);
        Vector v1 = Vector.mul(a.subList(a.size() / 2, a.size()), dt);
        Vector x2 = b.subList(0, b.size() / 2);
        Vector v2 = Vector.mul(b.subList(b.size() / 2, b.size()), dt);
        Vector dx = Vector.mul(Spline.hermite_deriv(x1, v1, x2, v2, u, 1), 1.0 / dt);
        Vector ddx = Vector.mul(Spline.hermite_deriv(x1, v1, x2, v2, u, 2), 1.0 / Math.pow(dt, 2));
        return Vector.add(dx, ddx);
    }

    public Trajectory discretize(double dt) {
        List<Double> resTimes = new ArrayList<>();
        List<Vector> resMilestones = new ArrayList<>();
        int n = milestones.get(0).size() / 2;
        for (int i = 0; i < times.size() - 1; i++) {
            double t0 = times.get(i);
            double t1 = times.get(i + 1);
            Vector p0 = milestones.get(i).subList(0, n);
            Vector v0 = Vector.mul(milestones.get(i).subList(n, milestones.get(i).size()), dt);
            Vector p1 = milestones.get(i + 1).subList(0, n);
            Vector v1 = Vector.mul(milestones.get(i + 1).subList(n, milestones.get(i + 1).size()), dt);
            int steps = (int) Math.ceil((t1 - t0) / dt);
            double h = (t1 - t0) / steps;
            for (int j = 0; j <= steps; j++) {
                double u = (double) j / steps;
                resTimes.add(t0 + u * (t1 - t0));
                Vector p = Spline.hermite_eval(p0, v0, p1, v1, u);
                Vector v = Spline.hermite_deriv(p0, v0, p1, v1, u);
                resMilestones.add(Vector.add(p, Vector.mul(v, h)));
            }
        }
        return new Trajectory(resTimes, resMilestones);
    }

    public double length() {
        int n = milestones.get(0).size() / 2;
        double third = 1.0 / 3.0;
        DistanceFunction distanceFunction = (x, y) -> {
            Vector cp0 = x.subList(0, n);
            Vector cp1 = Vector.madd(cp0, x.subList(n, x.size()), third);
            Vector cp3 = y.subList(0, n);
            Vector cp2 = Vector.madd(cp3, y.subList(n, y.size()), -third);
            return third * Vector.norm(x.subList(n, x.size())) + Vector.distance(cp1, cp2)
                    + third * Vector.norm(y.subList(n, y.size()));
        };
        return length(distanceFunction);
    }

    public void checkValid() {
        super.checkValid();
        for (Vector m : milestones) {
            if (m.size() % 2 != 0) {
                throw new IllegalArgumentException("Milestone length isn't even?: " + m.size());
            }
        }
    }

    public HermiteTrajectory extractDofs(List<Integer> dofs) {
        if (times.isEmpty()) {
            return constructor();
        }
        int n = milestones.get(0).size() / 2;
        for (int d : dofs) {
            if (Math.abs(d) >= n) {
                throw new IllegalArgumentException("Invalid dof");
            }
        }
        List<Vector> extractedMilestones = new ArrayList<>();
        for (Vector m : milestones) {
            List<Double> extractedMilestone = new ArrayList<>();
            for (int j : dofs) {
                extractedMilestone.add(m.get(j));
            }
            for (int j : dofs) {
                extractedMilestone.add(m.get(n + j));
            }
            extractedMilestones.add(Vector.copy(extractedMilestone));
        }
        return constructor().apply(times, extractedMilestones);
    }

    public void stackDofs(List<HermiteTrajectory> trajs, boolean strict) {
        if (!(trajs instanceof List<?>)) {
            throw new IllegalArgumentException("HermiteTrajectory.stackDofs takes in a list of trajectories as input");
        }
        List<Double> allTimes = new ArrayList<>();
        for (HermiteTrajectory traj : trajs) {
            allTimes.addAll(traj.times);
        }
        List<Double> uniqueTimes = new ArrayList<>(Set.copyOf(allTimes));
        uniqueTimes.sort(null);
        List<HermiteTrajectory> stackTrajs = new ArrayList<>();
        for (HermiteTrajectory traj : trajs) {
            stackTrajs.add(traj.remesh(uniqueTimes));
        }
        List<Vector> stackMilestones = new ArrayList<>();
        for (int i = 0; i < uniqueTimes.size(); i++) {
            List<Double> stackedMilestone = new ArrayList<>();
            for (HermiteTrajectory traj : stackTrajs) {
                Vector milestone = traj.milestones.get(i);
                int n = milestone.size() / 2;
                for (int j = 0; j < n; j++) {
                    stackedMilestone.add(milestone.get(j));
                }
            }
            for (HermiteTrajectory traj : stackTrajs) {
                Vector milestone = traj.milestones.get(i);
                int n = milestone.size() / 2;
                for (int j = 0; j < n; j++) {
                    stackedMilestone.add(milestone.get(n + j));
                }
            }
            stackMilestones.add(Vector.copy(stackedMilestone));
        }
        times = uniqueTimes;
        milestones = stackMilestones;
    }

    public HermiteTrajectory constructor() {
        return new HermiteTrajectory();
    }
}
