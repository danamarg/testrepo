import java.util.List;
import java.util.Optional;

public class SO3HermiteTrajectory extends GeodesicHermiteTrajectory {
    public SO3HermiteTrajectory(List<Double> times, List<Vector> milestones, List<Vector> outgoingLieDerivatives) {
        super(new SO3Space(), times, milestones, outgoingLieDerivatives);
    }

    @Override
    public void preTransform(Rotation R) {
        for (int i = 0; i < milestones.size(); i++) {
            Vector m = milestones.get(i);
            assert m.size() == 18;
            Vector mq = m.copy(0, 9);
            Vector mv = m.copy(9, 18);
            milestones.set(i, so3.mul(R, mq).add(so3.mul(R, mv)));
        }
    }

    @Override
    public Vector3 deriv_angvel(double t, String endBehavior) {
        Vector dR = GeodesicHermiteTrajectory.eval_velocity(this, t, endBehavior);
        return so3.deskew(dR);
    }

    @Override
    public void postTransform(Rotation R) {
        for (int i = 0; i < milestones.size(); i++) {
            Vector m = milestones.get(i);
            assert m.size() == 18;
            Vector mq = m.copy(0, 9);
            Vector mv = m.copy(9, 18);
            milestones.set(i, so3.mul(mq, R).add(so3.mul(mv, R)));
        }
    }

    public SO3Trajectory discretize(double dt) {
        _skip_deriv = true;
        DiscretizationResult res = discretize_state(dt);
        _skip_deriv = false;
        int n = 9;
        return new SO3Trajectory(res.times, res.milestones.subList(0, n));
    }

    @Override
    public SO3HermiteTrajectory constructor() {
        return new SO3HermiteTrajectory(times, milestones, outgoingLieDerivatives);
    }
}
