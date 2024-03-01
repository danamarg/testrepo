import java.util.List;
import java.util.Optional;

public class RobotTrajectory extends Trajectory {
    private final RobotModel robot;

    public RobotTrajectory(RobotModel robot, List<Float> times, List<Vector> milestones) {
        super(times, milestones);
        if (!(robot instanceof RobotModel || robot instanceof SubRobotModel)) {
            throw new IllegalArgumentException("RobotTrajectory must be provided with a RobotModel or SubRobotModel as first argument");
        }
        this.robot = robot;
    }

    @Override
    protected Vector interpolateState(Vector a, Vector b, double u, double dt) {
        return robot.interpolate(a, b, u);
    }

    @Override
    protected Vector differenceState(Vector a, Vector b, double u, double dt) {
        if (a.size() != robot.numLinks()) {
            throw new IllegalArgumentException("Invalid config " + a.toString() + " should have length " + robot.numLinks());
        }
        if (b.size() != robot.numLinks()) {
            throw new IllegalArgumentException("Invalid config " + b.toString() + " should have length " + robot.numLinks());
        }
        return vectorops.mul(robot.interpolateDeriv(b, a), 1.0 / dt);
    }

    public SE3Trajectory getLinkTrajectory(int link, List<Float> discretization) {
        if (discretization != null) {
            return this.discretize(discretization).getLinkTrajectory(link);
        }
        RobotModelLink linkObject = (link instanceof String) ? robot.link(link) : (RobotModelLink) link;
        List<Transform> Rmilestones = new ArrayList<>();
        for (Vector m : milestones) {
            robot.setConfig(m);
            Rmilestones.add(linkObject.getTransform());
        }
        return new SE3Trajectory(times, Rmilestones);
    }

    @Override
    public double length() {
        return length(null);
    }

    @Override
    public double length(Metric metric) {
        return (metric == null) ? super.length(this.robot.distance()) : super.length(metric);
    }

    @Override
    public void checkValid() {
        super.checkValid();
        for (Vector m : milestones) {
            if (m.size() != robot.numLinks()) {
                throw new IllegalArgumentException("Invalid length of milestone: " + m.size() + " != " + robot.numLinks());
            }
        }
    }

    public RobotTrajectory extractDofs(List<Integer> dofs) {
        SubRobotModel subRobot = new SubRobotModel(robot, dofs);
        if (times.isEmpty()) {
            return new RobotTrajectory(subRobot, null, null);
        }
        List<Vector> extractedMilestones = new ArrayList<>();
        for (Vector m : milestones) {
            List<Double> subMilestone = new ArrayList<>();
            for (int j : subRobot._links) {
                subMilestone.add(m.get(j));
            }
            extractedMilestones.add(subMilestone);
        }
        return new RobotTrajectory(subRobot, times, extractedMilestones);
    }

    public void stackDofs(List<RobotTrajectory> trajs) {
        stackDofs(trajs, false);
        if (!milestones.isEmpty() && milestones.get(0).size() != robot.numDofs()) {
            System.out.println("RobotTrajectory.stackDofs: the result doesn't match the robot's #DOF");
        }
    }
}
