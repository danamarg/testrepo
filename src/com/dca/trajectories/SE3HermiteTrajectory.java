import java.util.List;

public class SE3HermiteTrajectory extends GeodesicHermiteTrajectory {
    public SE3HermiteTrajectory(List<Double> times, List<Vector> milestones, List<Vector> outgoingLieDerivatives) {
        super(new SE3Space(), times, milestones, outgoingLieDerivatives);
        if (milestones != null && !milestones.isEmpty() && milestones.get(0).size() == 2) {
            milestones.forEach(pair -> {
                pair.set(0, pair.get(0).add(pair.get(1)));
            });
        }
        if (outgoingLieDerivatives != null && !outgoingLieDerivatives.isEmpty() && outgoingLieDerivatives.get(0).size() == 2) {
            outgoingLieDerivatives.forEach(pair -> {
                pair.set(0, pair.get(0).add(pair.get(1)));
            });
        }
    }

    public RigidTransform toSE3(Vector state) {
        return new RigidTransform(state.subList(0, 9), state.subList(9, 12));
    }

    public Vector fromSE3(RigidTransform T) {
        List<Double> translation = T.t;
        List<Double> rotation = T.R;
        Vector state = new Vector(12);
        for (int i = 0; i < 9; i++) {
            state.set(i, rotation.get(i));
        }
        for (int i = 0; i < 3; i++) {
            state.set(i + 9, translation.get(i));
        }
        return state;
    }

    public RigidTransform waypoint(Vector state) {
        return toSE3(state);
    }

    public RigidTransform eval(double t, String endBehavior) {
        Vector res = super.eval(t, endBehavior);
        return toSE3(res);
    }

    public RigidTransform deriv(double t, String endBehavior) {
        Vector res = super.deriv(t, endBehavior);
        return toSE3(res.subList(0, 12));
    }

    public Vector derivScrew(double t, String endBehavior) {
        RigidTransform dT = deriv(t, endBehavior);
        return VectorOps.add(so3.deskew(dT.R), dT.t);
    }

    public void preTransform(RigidTransform T) {
        for (int i = 0; i < milestones.size(); i++) {
            Vector m = milestones.get(i);
            assert m.size() == 24;
            RigidTransform mq = toSE3(m.subList(0, 12));
            RigidTransform mv = toSE3(m.subList(12, 24));
            RigidTransform transformedMQ = SE3.mul(T, mq);
            RigidTransform transformedMV = new RigidTransform(
                    so3.mul(T.R, mv.R),
                    so3.apply(T.R, mv.t)
            );
            milestones.set(i, fromSE3(transformedMQ).add(fromSE3(transformedMV)));
        }
    }

    public void postTransform(RigidTransform T) {
        for (int i = 0; i < milestones.size(); i++) {
            Vector m = milestones.get(i);
            assert m.size() == 24;
            RigidTransform mq = toSE3(m.subList(0, 12));
            RigidTransform mv = toSE3(m.subList(12, 24));
            RigidTransform transformedMQ = SE3.mul(mq, T);
            RigidTransform transformedMV = new RigidTransform(
                    so3.mul(mv.R, T.R),
                    so3.apply(so3.inv(T.R), mv.t)
            );
            milestones.set(i, fromSE3(transformedMQ).add(fromSE3(transformedMV)));
        }
    }

    public SE3Trajectory discretize(double dt) {
        _skipDeriv = true;
        TrajectoryResult res = discretizeState(dt);
        _skipDeriv = false;
        int n = 12;
        List<Vector> resMilestones = new ArrayList<>();
        for (Vector m : res.milestones) {
            resMilestones.add(m.subList(0, n));
        }
        return new SE3Trajectory(res.times, resMilestones);
    }

    public SE3HermiteTrajectory constructor() {
        return new SE3HermiteTrajectory(times, milestones, outgoingLieDerivatives);
    }
}
