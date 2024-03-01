rt java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Section {
    /**
     * Contains a path or time-parameterized trajectory, as well as a list of holds and ik constraints
     * If the times member is set, this is time parameterized.  Otherwise, it is just a path.
     */
    
    // Attributes
    private Map<String, String> settings;  // dict
    private List<List<Float>> configs;     // List<List<Float>>
    private List<List<Float>> velocities; // Optional<List<List<Float>>>
    private List<Float> times;             // Optional<List<Float>>
    private List<Holds> holds;             // List<Holds>
    private List<Integer> holdIndices;     // List<Integer>
    private List<IKObjectives> ikObjectives; // List<IKObjectives>
    
    // Constructor
    public Section() {
        settings = new HashMap<>();  // dict
        configs = new ArrayList<>(); // List<List<Float>>
        velocities = null;           // Optional<List<List<Float>>>
        times = null;                 // Optional<List<Float>>
        holds = new ArrayList<>();    // List<Holds>
        holdIndices = new ArrayList<>(); // List<Integer>
        ikObjectives = new ArrayList<>(); // List<IKObjectives>
    }
}