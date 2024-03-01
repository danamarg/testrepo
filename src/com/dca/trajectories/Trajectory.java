import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Trajectory {
    private List<Float> times;
    private List<List<Float>> milestones;

    public Trajectory(List<Float> times, List<List<Float>> milestones) {
        this.times = times;
        this.milestones = milestones;
    }

    public Trajectory() {
        this.times = new ArrayList<>();
        this.milestones = new ArrayList<>();
    }

    public void load(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        times.clear();
        milestones.clear();
        while ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            List<Float> timedMilestone = new ArrayList<>();
            for (int i = 0; i < parts.length; i++) {
                timedMilestone.add(Float.parseFloat(parts[i]));
            }
            times.add(timedMilestone.get(0));
            milestones.add(timedMilestone.subList(1, timedMilestone.size()));
        }
        reader.close();
    }

    public void save(String filename) throws IOException {
        FileWriter writer = new FileWriter(filename);
        for (int i = 0; i < times.size(); i++) {
            writer.write(times.get(i) + "\t" + milestones.get(i).size() + " ");
            for (Float milestone : milestones.get(i)) {
                writer.write(milestone + " ");
            }
            writer.write("\n");
        }
        writer.close();
    }

    public float startTime() {
        return times.isEmpty() ? 0.0f : times.get(0);
    }

    public float endTime() {
        return times.isEmpty() ? 0.0f : times.get(times.size() - 1);
    }

    public float duration() {
        return endTime() - startTime();
    }

    public void checkValid() {
        if (times.size() != milestones.size()) {
            throw new IllegalArgumentException("Times and milestones are not the same length");
        }
        if (times.isEmpty()) {
            throw new IllegalArgumentException("Trajectory is empty");
        }
        for (int i = 0; i < times.size() - 1; i++) {
            if (times.get(i) > times.get(i + 1)) {
                throw new IllegalArgumentException("Timing is not sorted");
            }
        }
        int n = milestones.get(0).size();
        for (List<Float> milestone : milestones) {
            if (milestone.size() != n) {
                throw new IllegalArgumentException("Invalid milestone size");
            }
        }
    }

    public int getSegment(float t, String endBehavior) {
        if (times.isEmpty()) {
            throw new IllegalArgumentException("Empty trajectory");
        }
        if (times.size() == 1) {
            return -1;
        }
        if (t > times.get(times.size() - 1)) {
            if (endBehavior.equals("loop")) {
                t = t % times.get(times.size() - 1);
            } else {
                return milestones.size() - 1;
            }
        }
        if (t >= times.get(times.size() - 1)) {
            return milestones.size() - 1;
        }
        if (t <= times.get(0)) {
            return -1;
        }
        int i = Collections.binarySearch(times, t);
        int p = i - 1;
        assert i > 0 && i < times.size();
        float u = (t - times.get(p)) / (times.get(i) - times.get(p));
        if (i == 0) {
            if (endBehavior.equals("loop")) {
                t += times.get(times.size() - 1);
                p = -2;
                u = (t - times.get(p)) / (times.get(times.size() - 1) - times.get(p));
            } else {
                return -1;
            }
        }
        assert u >= 0 && u <= 1;
        return p;
    }

    public List<Float> eval(float t, String endBehavior) {
        int i = getSegment(t, endBehavior);
        if (i < 0) {
            return milestones.get(0);
        } else if (i + 1 >= milestones.size()) {
            return milestones.get(milestones.size() - 1);
        }
        float u = (t - times.get(i)) / (times.get(i + 1) - times.get(i));
        return interpolateState(milestones.get(i), milestones.get(i + 1), u, times.get(i + 1) - times.get(i));
    }

    public List<Float> deriv(float t, String endBehavior) {
        int i = getSegment(t, endBehavior);
        if (i < 0) {
            return new ArrayList<>(Collections.nCopies(milestones.get(0).size(), 0.0f));
        } else if (i + 1 >= milestones.size()) {
            return new ArrayList<>(Collections.nCopies(milestones.get(milestones.size() - 1).size(), 0.0f));
        }
        return differenceState(milestones.get(i + 1), milestones.get(i), (t - times.get(i)) / (times.get(i + 1) - times.get(i)), times.get(i + 1) - times.get(i));
    }

    public List<Float> waypoint(List<Float> state) {
        return new ArrayList<>(state);
    }

    public List<Float> evalState(float t, String endBehavior) {
        int i = getSegment(t, endBehavior);
        if (i < 0) {
            return milestones.get(0);
        } else if (i + 1 >= milestones.size()) {
            return milestones.get(milestones.size() - 1);
        }
        float u = (t - times.get(i)) / (times.get(i + 1) - times.get(i));
        return interpolateState(milestones.get(i), milestones.get(i + 1), u, times.get(i + 1) - times.get(i));
    }

    public List<Float> derivState(float t, String endBehavior) {
        int i = getSegment(t, endBehavior);
        if (i < 0) {
            return new ArrayList<>(Collections.nCopies(milestones.get(0).size(), 0.0f));
        } else if (i + 1 >= milestones.size()) {
            return new ArrayList<>(Collections.nCopies(milestones.get(milestones.size() - 1).size(), 0.0f));
        }
        return differenceState(milestones.get(i + 1), milestones.get(i), (t - times.get(i)) / (times.get(i + 1) - times.get(i)), times.get(i + 1) - times.get(i));
    }

    public List<Float> interpolateState(List<Float> a, List<Float> b, float u, float dt) {
        List<Float> interpolatedState = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            interpolatedState.add(a.get(i) + (b.get(i) - a.get(i)) * u);
        }
        return interpolatedState;
    }

    public List<Float> differenceState(List<Float> a, List<Float> b, float u, float dt) {
        List<Float> difference = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            difference.add((a.get(i) - b.get(i)) / dt);
        }
        return difference;
    }

    public Trajectory concat(Trajectory suffix, boolean relative, String jumpPolicy) {
        if (getClass() != suffix.getClass()) {
            throw new IllegalArgumentException("Can only concatenate like Trajectory classes");
        }
        float offset = relative && !times.isEmpty() ? times.get(times.size() - 1) : 0;
        if (!relative || times.isEmpty()) {
            offset = 0;
        }
        if (!relative && !times.isEmpty() && suffix.times.get(0) + offset < times.get(times.size() - 1)) {
            throw new IllegalArgumentException("Invalid concatenation, suffix startTime precedes endTime");
        }
        List<Float> newTimes = new ArrayList<>();
        List<List<Float>> newMilestones = new ArrayList<>();
        if (!relative || times.isEmpty()) {
            newTimes.addAll(times);
            newMilestones.addAll(milestones);
        } else {
            newTimes.addAll(times.subList(0, times.size() - 1));
            newMilestones.addAll(milestones.subList(0, milestones.size() - 1));
        }
        if (relative && !times.isEmpty() && suffix.times.get(0) + offset == times.get(times.size() - 1)) {
            if (jumpPolicy.equals("strict") && !suffix.milestones.get(0).equals(milestones.get(milestones.size() - 1))) {
                throw new IllegalArgumentException("Concatenation would cause a jump in configuration");
            }
            if (jumpPolicy.equals("strict") || (jumpPolicy.equals("blend") && !suffix.milestones.get(0).equals(milestones.get(milestones.size() - 1)))) {
                newTimes.add(suffix.times.get(0) + offset);
                newMilestones.add(suffix.milestones.get(0));
            }
        }
        newTimes.addAll(suffix.times);
        newMilestones.addAll(suffix.milestones);
        return new Trajectory(newTimes, newMilestones);
    }

    public int insert(float time) {
        if (times.isEmpty()) {
            times.add(time);
            milestones.add(new ArrayList<>());
            return 0;
        }
        if (time <= times.get(0)) {
            if (time < times.get(0)) {
                times.add(0, time);
                milestones.add(0, new ArrayList<>(milestones.get(0)));
            }
            return 0;
        } else if (time >= times.get(times.size() - 1)) {
            if (time > times.get(times.size() - 1)) {
                times.add(time);
                milestones.add(new ArrayList<>(milestones.get(milestones.size() - 1)));
            }
            return times.size() - 1;
        } else {
            int i = Collections.binarySearch(times, time);
            int p = i >= 0 ? i : -(i + 1);
            if (p >= 0 && p < times.size() && times.get(p) == time) {
                return p;
            }
            float u = (time - times.get(p - 1)) / (times.get(p) - times.get(p - 1));
            if (u == 0) {
                return p - 1;
            } else if (u == 1) {
                return p;
            } else {
                List<Float> q = interpolateState(milestones.get(p - 1), milestones.get(p), u, times.get(p) - times.get(p - 1));
                times.add(p, time);
                milestones.add(p, q);
                return p;
            }
        }
    }

    public Trajectory[] split(float time) {
        List<Float> times1 = new ArrayList<>();
        List<List<Float>> milestones1 = new ArrayList<>();
        List<Float> times2 = new ArrayList<>();
        List<List<Float>> milestones2 = new ArrayList<>();

        if (time <= times.get(0)) {
            times1.add(time);
            times2.addAll(times);
            milestones1.add(milestones.get(0));
            milestones2.addAll(milestones);
        } else if (time >= times.get(times.size() - 1)) {
            times1.addAll(times);
            times2.add(time);
            milestones1.addAll(milestones);
            milestones2.add(milestones.get(milestones.size() - 1));
        } else {
            int idx = 0;
            while (times.get(idx) < time) {
                idx++;
            }
            times1.addAll(times.subList(0, idx));
            times1.add(time);
            times2.add(time);
            times2.addAll(times.subList(idx, times.size()));

            milestones1.addAll(milestones.subList(0, idx));
            milestones2.add(interpolateState(milestones.get(idx - 1), milestones.get(idx), (time - times.get(idx - 1)) / (times.get(idx) - times.get(idx - 1)), times.get(idx) - times.get(idx - 1)));
            milestones2.addAll(milestones.subList(idx, milestones.size()));
        }
        return new Trajectory[]{new Trajectory(times1, milestones1), new Trajectory(times2, milestones2)};
    }

    public Trajectory before(float time) {
        return split(time)[0];
    }

    public Trajectory after(float time) {
        return split(time)[1];
    }

    public Trajectory splice(Trajectory suffix, Float time, boolean relative, String jumpPolicy) {
        Trajectory before = before(relative && time == null ? suffix.times.get(0) + (times.isEmpty() ? 0 : times.get(times.size() - 1)) : time);
        return before.concat(suffix, relative, jumpPolicy);
    }

    public Trajectory discretizeState(float dt) {
        List<Float> newTimes = new ArrayList<>();
        List<List<Float>> newMilestones = new ArrayList<>();
        for (int i = 0; i < times.size() - 1; i++) {
            float duration = times.get(i + 1) - times.get(i);
            int steps = (int) Math.ceil(duration / dt);
            for (int j = 0; j < steps; j++) {
                float t = times.get(i) + j * dt;
                newTimes.add(t);
                newMilestones.add(evalState(t, "loop"));
            }
        }
        newTimes.add(times.get(times.size() - 1));
        newMilestones.add(milestones.get(milestones.size() - 1));
        return new Trajectory(newTimes, newMilestones);
    }

}

