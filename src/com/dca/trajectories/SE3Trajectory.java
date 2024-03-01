import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

public class SE3Trajectory extends GeodesicTrajectory {
    
    public SE3Trajectory(List<Double> times, List<Vector> milestones) {
        super(new SE3Space(), times, milestones);
        if (milestones != null && !milestones.isEmpty() && milestones.get(0).size() == 2) {
            List<Vector> concatenatedMilestones = new ArrayList<>();
            for (Vector m : milestones) {
                concatenatedMilestones.add(Vector.add(m.get(0), m.get(1)));
            }
            this.milestones = concatenatedMilestones;
        }
    }
    
    public RigidTransform to_se3(Vector state) {
        return new RigidTransform(state.subList(0, 9), state.subList(9, 12));
    }
    
    public RigidTransform waypoint(Vector state) {
        return to_se3(state);
    }
    
    public Vector from_se3(RigidTransform T) {
        List<Double> list = new ArrayList<>(T.getRotation().getList());
        list.addAll(T.getTranslation().getList());
        return new Vector(list);
    }
    
    public RigidTransform eval(double t, String endBehavior) {
        Vector res = eval_state(t, endBehavior);
        return to_se3(res);
    }
    
    public RigidTransform deriv(double t, String endBehavior) {
        Vector res = deriv_state(t, endBehavior);
        return to_se3(res);
    }
    
    public Vector deriv_screw(double t, String endBehavior) {
        RigidTransform dT = deriv(t, endBehavior);
        return Vector.add(so3.deskew(dT.getRotation()), dT.getTranslation());
    }
    
    public void preTransform(RigidTransform T) {
        for (int i = 0; i < milestones.size(); i++) {
            RigidTransform Tm = to_se3(milestones.get(i));
            milestones.set(i, from_se3(se3.mul(T, Tm)));
        }
    }
    
    public void postTransform(RigidTransform T) {
        for (int i = 0; i < milestones.size(); i++) {
            RigidTransform Tm = to_se3(milestones.get(i));
            milestones.set(i, from_se3(se3.mul(Tm, T)));
        }
    }
    
    public SO3Trajectory getRotationTrajectory() {
        List<Vector> rotations = new ArrayList<>();
        for (Vector m : milestones) {
            rotations.add(m.subList(0, 9));
        }
        return new SO3Trajectory(times, rotations);
    }
    
    public Trajectory getPositionTrajectory(Vector3 localPt) {
        List<Vector3> positions = new ArrayList<>();
        for (Vector m : milestones) {
            if (localPt == null) {
                positions.add(new Vector3(m.subList(9, 12)));
            } else {
                positions.add(se3.apply(to_se3(m), localPt));
            }
        }
        return new Trajectory(times, positions);
    }
    
    public void checkValid() {
        super.checkValid();
        for (Vector m : milestones) {
            if (m.size() != 12) {
                throw new IllegalArgumentException("Invalid length of milestone: " + m.size() + " != 12");
            }
        }
    }
    
    public Trajectory extractDofs(List<Integer> dofs) {
        if (dofs.equals(range(9))) {
            Trajectory traj = super.extractDofs(dofs);
            return new SO3Trajectory(traj.times, traj.milestones);
        } else if (dofs.stream().allMatch(d -> d >= 9)) {
            return super.extractDofs(dofs);
        } else {
            throw new IllegalArgumentException("Cannot extract DOFs from a SE3Trajectory");
        }
    }
    
    @Override
    public SE3Trajectory constructor() {
        return new SE3Trajectory(times, milestones);
    }
}
