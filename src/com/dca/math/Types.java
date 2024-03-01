import java.util.List;

public class Types {

    // Typedef for vectors
    public interface Vector extends List<Double> {}

    // Typedef for 2D vectors
    public interface Vector2 extends Vector {}

    // Typedef for 3D vectors
    public interface Vector3 extends Vector {}

    // Typedef for points
    public interface Point extends Vector {}

    // Typedef for 3x3 matrices
    public interface Matrix3 extends List<Vector> {}

    // Typedef for rotations
    public interface Rotation extends Vector {}

    // Typedef for rigid transforms
    public interface RigidTransform extends Tuple<Vector, Vector> {}

    // Typedef for integer arrays
    public interface IntArray extends List<Integer> {}

    // Typedef for string arrays
    public interface StringArray extends List<String> {}

    // Typedef for configuration
    public interface Config extends Vector {}

    // Typedef for list of configurations
    public interface Configs extends List<Config> {}
}
