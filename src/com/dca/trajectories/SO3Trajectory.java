import java.util.List;
import java.util.Optional;

public class SO3Trajectory extends GeodesicTrajectory {
    
    public SO3Trajectory(List<Double> times, List<Vector> milestones) {
        super(new SO3Space(), times, milestones);
    }
    
    public Vector3 deriv_angvel(double t, String endBehavior) {
        Vector cw = super.deriv(t, endBehavior);
        return so3.deskew(cw);
    }
    
    public void preTransform(Rotation R) {
        for (int i = 0; i < milestones.size(); i++) {
            milestones.set(i, so3.mul(R, milestones.get(i)));
        }
    }
    
    public void postTransform(Rotation R) {
        for (int i = 0; i < milestones.size(); i++) {
            milestones.set(i, so3.mul(milestones.get(i), R));
        }
    }
    
    public Trajectory getPointTrajectory(Vector3 localPt) {
        List<Vector3> transformedMilestones = new ArrayList<>();
        for (Vector m : milestones) {
            transformedMilestones.add(so3.apply(m, localPt));
        }
        return new Trajectory(times, transformedMilestones);
    }
    
    public void checkValid() {
        super.checkValid();
        for (Vector m : milestones) {
            if (m.size() != 9) {
                throw new IllegalArgumentException("Invalid length of milestone: " + m.size() + " != 9");
            }
        }
    }
    
    @Override
    public SO3Trajectory constructor() {
        return new SO3Trajectory(times, milestones);
    }
}
