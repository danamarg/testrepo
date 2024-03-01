import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MultiPath {

    // Attributes
    private List<Section> sections;
    private Map<Integer, Holds> holdSet;
    private Map<String, String> settings;

    // Constructor
    public MultiPath() {
        sections = new ArrayList<>();
        holdSet = new TreeMap<>();
        settings = new HashMap<>();
    }

    // Methods
    public int numSections() {
        return sections.size();
    }

    public Vector startConfig() {
        return sections.get(0).configs.get(0);
    }

    public Vector endConfig() {
        Section lastSection = sections.get(sections.size() - 1);
        List<Vector> lastSectionConfigs = lastSection.configs;
        return lastSectionConfigs.get(lastSectionConfigs.size() - 1);
    }

    public float startTime() {
        if (sections.isEmpty() || sections.get(0).times == null) return 0;
        return sections.get(0).times.get(0);
    }

    public float endTime() {
        if (sections.isEmpty()) return 0;
        Section lastSection = sections.get(sections.size() - 1);
        if (lastSection.times == null) return sections.size() - 1;
        return lastSection.times.get(lastSection.times.size() - 1);
    }

    public double duration() {
        return endTime() - startTime();
    }

    public boolean hasTiming() {
        return sections.get(0).times != null;
    }

    public boolean checkValid() throws IllegalArgumentException {
        double t0 = startTime();
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            if (s.times == null) {
                throw new IllegalArgumentException("MultiPath section 0 is timed but section " + i + " is not timed");
            }
            if (s.times.size() != s.configs.size()) {
                throw new IllegalArgumentException("MultiPath section " + i + " has invalid number of times");
            }
            for (Double t : s.times) {
                if (t < t0) {
                    throw new IllegalArgumentException("MultiPath section " + i + " times are not monotonically increasing");
                }
                t0 = t;
            }
        }
        return true;
    }

    public boolean isContinuous() {
        for (int i = 0; i < sections.size() - 1; i++) {
            Section currentSection = sections.get(i);
            Section nextSection = sections.get(i + 1);
            if (!currentSection.configs.get(currentSection.configs.size() - 1).equals(nextSection.configs.get(0))) {
                return false;
            }
        }
        return true;
    }

    public Pair<Double, Double> getSectionTiming(int section) {
        if (section < 0 || section >= sections.size()) {
            throw new IllegalArgumentException("Invalid section index");
        }

        Section sec = sections.get(section);
        if (hasTiming()) {
            return new Pair<>(sec.times.get(0), sec.times.get(sec.times.size() - 1));
        }

        double t0 = 0;
        for (int i = 0; i < section; i++) {
            t0 += sections.get(i).configs.size() - 1;
        }
        return new Pair<>(t0, t0 + sections.get(section).configs.size());
    }

    public List<Hold> getStance(int section) {
        Section sec = sections.get(section);
        List<Hold> res = new ArrayList<>(sec.holds);
        res.addAll(sec.holdIndices.stream().map(h -> holdSet.get(h)).collect(Collectors.toList()));
        sec.ikObjectives.forEach(g -> {
            Hold h = new Hold();
            h.link = g.link();
            h.ikConstraint = g;
            res.add(h);
        });
        return res;
    }

    public List<IKObjective> getIKProblem(int section) {
        Section sec = sections.get(section);
        List<IKObjective> res = new ArrayList<>(sec.holds.stream().map(h -> h.ikConstraint).collect(Collectors.toList()));
        res.addAll(sec.holdIndices.stream().map(h -> holdSet.get(h).ikConstraint).collect(Collectors.toList()));
        res.addAll(sec.ikObjectives);
        return res;
    }

    public void aggregateHolds(Double holdSimilarityThreshold) {
        Map<String, List<Pair<Hold, Integer>>> holdsByLink = new HashMap<>();
        for (Section s : sections) {
            for (Hold h : s.holds) {
                boolean found = false;
                if (holdSimilarityThreshold != null) {
                    for (Map.Entry<String, List<Pair<Hold, Integer>>> entry : holdsByLink.entrySet()) {
                        for (Pair<Hold, Integer> pair : entry.getValue()) {
                            if (sameHold(h, pair.first, holdSimilarityThreshold)) {
                                s.holdIndices.add(pair.second);
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                }
                if (!found) {
                    int index = holdSet.size();
                    s.holdIndices.add(index);
                    holdSet.put(index, h);
                    if (holdSimilarityThreshold != null) {
                        holdsByLink.computeIfAbsent(h.link, k -> new ArrayList<>()).add(new Pair<>(h, index));
                    }
                }
            }
            s.holds.clear();
        }
    }

    private boolean sameHold(Hold h1, Hold h2, Double tol) {
        // Implement the logic for comparing two holds
        return false;
    }

    public void deaggregateHolds() {
        for (Section s : sections) {
            for (Integer h : s.holdIndices) {
                s.holds.add(holdSet.get(h));
            }
            s.holdIndices.clear();
        }
        holdSet.clear();
    }

    public void setConfig(int sectionIndex, int configIndex, Vector q, Vector v, Double t, boolean maintainContinuity) {
        Section section = sections.get(sectionIndex);
        if (configIndex < 0) {
            configIndex = section.configs.size() - 1;
        }
        section.configs.set(configIndex, q);
        if (v != null) {
            assert section.velocities != null;
            section.velocities.set(configIndex, v);
        }
        if (t != null) {
            assert section.times != null;
            section.times.set(configIndex, t);
        }
        if (maintainContinuity) {
            int currentSectionIndex = sectionIndex;
            int currentConfigIndex = configIndex;
            while (currentSectionIndex > 0 && configIndex == 0) {
                currentSectionIndex--;
                currentConfigIndex = sections.get(currentSectionIndex).configs.size() - 1;
                setConfig(currentSectionIndex, currentConfigIndex, q, v, t, false);
            }
            currentSectionIndex = sectionIndex;
            currentConfigIndex = configIndex;
            while (currentSectionIndex + 1 < sections.size() && currentConfigIndex + 1 == sections.get(currentSectionIndex).configs.size()) {
                currentSectionIndex++;
                currentConfigIndex = 0;
                setConfig(currentSectionIndex, currentConfigIndex, q, v, t, false);
            }
        }
    }

    public void concat(MultiPath path) {
        List<Section> newSections = new ArrayList<>(path.sections);
        double dt = 0.0;
        if (path.hasTiming() && !sections.isEmpty()) {
            assert hasTiming();
            dt = endTime();
            for (Section s : newSections) {
                s.times.replaceAll(t -> t + dt);
            }
        }
        Map<String, Hold> newHolds = new HashMap<>(path.holdSet);
        Map<String, String> nameMap = new HashMap<>();
        for (Map.Entry<String, Hold> entry : path.holdSet.entrySet()) {
            String name = entry.getKey();
            Hold hold = entry.getValue();
            if (holdSet.containsKey(name)) {
                boolean found = false;
                for (int k = 2; k < 1000; k++) {
                    String newName = name + "(" + k + ")";
                    if (!holdSet.containsKey(newName)) {
                        nameMap.put(name, newName);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("Unable to merge hold name " + name);
                }
                newHolds.put(nameMap.get(name), hold);
            }
        }
        for (Section s : newSections) {
            List<Object> inds = new ArrayList<>();
            for (Object h : s.holdIndices) {
                if (h instanceof String) {
                    inds.add(nameMap.get(h));
                } else {
                    inds.add(h);
                }
            }
            s.holdIndices = inds;
        }
        sections.addAll(newSections);
        holdSet.putAll(newHolds);
    }

    public void save(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            Document document = saveXML();
            writer.write("<?xml version=\"1.0\"?>\n");
            writer.write(documentToString(document));
        }
    }

    public void load(String filename) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(filename));
        loadXML(document);
    }

    public Document saveXML() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            Element rootElement = document.createElement("multipath");
            document.appendChild(rootElement);

            for (Section sec : sections) {
                Element sectionElement = document.createElement("section");
                rootElement.appendChild(sectionElement);

                // Set section attributes

                for (IKObjective ikGoal : sec.ikObjectives) {
                    Element ikGoalElement = document.createElement("ikgoal");
                    ikGoalElement.setTextContent(ikGoal.toString());
                    sectionElement.appendChild(ikGoalElement);
                }

                for (Hold hold : sec.holds) {
                    Element holdElement = document.createElement("hold");
                    holdElement.setTextContent(hold.toString());
                    sectionElement.appendChild(holdElement);
                }

                for (Object h : sec.holdIndices) {
                    Element holdElement = document.createElement("hold");
                    if (h instanceof Integer) {
                        holdElement.setAttribute("index", h.toString());
                    } else {
                        holdElement.setAttribute("name", h.toString());
                    }
                    sectionElement.appendChild(holdElement);
                }

                for (int i = 0; i < sec.configs.size(); i++) {
                    Element milestoneElement = document.createElement("milestone");
                    milestoneElement.setAttribute("config", sec.configs.get(i).toString());
                    if (sec.times != null) {
                        milestoneElement.setAttribute("time", sec.times.get(i).toString());
                    }
                    if (sec.velocities != null) {
                        milestoneElement.setAttribute("velocity", sec.velocities.get(i).toString());
                    }
                    sectionElement.appendChild(milestoneElement);
                }
            }

            for (Map.Entry<Integer, Hold> entry : holdSet.entrySet()) {
                Element holdElement = document.createElement("hold");
                holdElement.setTextContent(entry.getValue().toString());
                rootElement.appendChild(holdElement);
            }

            return document;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void loadXML(Document document) {
        NodeList sectionNodes = document.getElementsByTagName("section");
        for (int i = 0; i < sectionNodes.getLength(); i++) {
            Element sectionElement = (Element) sectionNodes.item(i);
            Section section = new Section();
    
            // Parse section attributes
    
            NodeList ikGoalNodes = sectionElement.getElementsByTagName("ikgoal");
            for (int j = 0; j < ikGoalNodes.getLength(); j++) {
                Element ikGoalElement = (Element) ikGoalNodes.item(j);
                IKObjective ikObjective = IKObjective.fromString(ikGoalElement.getTextContent());
                section.ikObjectives.add(ikObjective);
            }
    
            NodeList holdNodes = sectionElement.getElementsByTagName("hold");
            for (int j = 0; j < holdNodes.getLength(); j++) {
                Element holdElement = (Element) holdNodes.item(j);
                String indexAttribute = holdElement.getAttribute("index");
                String nameAttribute = holdElement.getAttribute("name");
                if (!indexAttribute.isEmpty()) {
                    section.holdIndices.add(Integer.parseInt(indexAttribute));
                } else if (!nameAttribute.isEmpty()) {
                    section.holdIndices.add(nameAttribute);
                } else {
                    Hold hold = Hold.fromString(holdElement.getTextContent());
                    section.holds.add(hold);
                }
            }
    
            NodeList milestoneNodes = sectionElement.getElementsByTagName("milestone");
            for (int j = 0; j < milestoneNodes.getLength(); j++) {
                Element milestoneElement = (Element) milestoneNodes.item(j);
                Vector config = Vector.fromString(milestoneElement.getAttribute("config"));
                section.configs.add(config);
                String timeAttribute = milestoneElement.getAttribute("time");
                if (!timeAttribute.isEmpty()) {
                    section.times.add(Double.parseDouble(timeAttribute));
                }
                String velocityAttribute = milestoneElement.getAttribute("velocity");
                if (!velocityAttribute.isEmpty()) {
                    Vector velocity = Vector.fromString(velocityAttribute);
                    section.velocities.add(velocity);
                }
            }
    
            sections.add(section);
        }
    
        NodeList holdNodes = document.getElementsByTagName("hold");
        for (int i = 0; i < holdNodes.getLength(); i++) {
            Element holdElement = (Element) holdNodes.item(i);
            String nameAttribute = holdElement.getAttribute("name");
            Hold hold = Hold.fromString(holdElement.getTextContent());
            if (!nameAttribute.isEmpty()) {
                holdSet.put(nameAttribute, hold);
            } else {
                holdSet.put(Integer.toString(holdSet.size()), hold);
            }
        }
    }
    
    public int timeToSection(double t) {
        if (!hasTiming()) {
            return (int) Math.floor(t * sections.size());
        } else {
            if (t < startTime()) {
                return -1;
            }
            for (int i = 0; i < sections.size(); i++) {
                if (t < sections.get(i).times.get(sections.get(i).times.size() - 1)) {
                    return i;
                }
            }
            return sections.size();
        }
    }
    
    public Triple<Integer, Integer, Double> timeToSegment(double t) {
        int sectionIndex = timeToSection(t);
        if (sectionIndex < 0) {
            return new Triple<>(-1, 0, 0.0);
        } else if (sectionIndex >= sections.size()) {
            return new Triple<>(sectionIndex, 0, 0.0);
        }
        Section section = sections.get(sectionIndex);
        int milestoneIndex = 0;
        double param = 0.0;
        if (section.times.isEmpty()) {
            double usec = t * sections.size() - sectionIndex;
            double tsec = (section.configs.size() - 1) * usec;
            milestoneIndex = (int) Math.floor(tsec);
            param = tsec - milestoneIndex;
        } else {
            int i = Collections.binarySearch(section.times, t);
            milestoneIndex = i >= 0 ? i : -(i + 1) - 1;
            double t0 = section.times.get(milestoneIndex);
            double t1 = section.times.get(milestoneIndex + 1);
            param = (t - t0) / (t1 - t0);
            if (milestoneIndex == 0) {
                param = 0.0;
            }
        }
        return new Triple<>(sectionIndex, milestoneIndex, param);
    }
    
    public Vector eval(double t) {
        Triple<Integer, Integer, Double> segment = timeToSegment(t);
        int sectionIndex = segment.first;
        int milestoneIndex = segment.second;
        double param = segment.third;
        if (sectionIndex < 0) {
            return startConfig();
        } else if (sectionIndex >= sections.size()) {
            return endConfig();
        }
        Section section = sections.get(sectionIndex);
        if (param == 0.0) {
            return section.configs.get(milestoneIndex);
        }
        return Vector.interpolate(section.configs.get(milestoneIndex), section.configs.get(milestoneIndex + 1), param);
    }

    public void setTrajectory(Trajectory trajectory) {
        holdSet = new HashMap<>();
        settings.put("program", "setTrajectory");
        sections = new ArrayList<>();
        Section section = new Section();
        sections.add(section);

        if (trajectory instanceof HermiteTrajectory) {
            HermiteTrajectory hermiteTrajectory = (HermiteTrajectory) trajectory;
            section.times = hermiteTrajectory.times;
            section.configs = hermiteTrajectory.milestones;
        } else if (trajectory instanceof Trajectory) {
            section.configs = trajectory.milestones;
        }
    }

    public Trajectory getTrajectory(){
        
        //     def getTrajectory(self,robot=None,eps=None) -> trajectory.Trajectory:
        // """Returns a trajectory representation of this MultiPath.  If robot is provided, then a RobotTrajectory
        // is returned.  Otherwise, if velocity information is given, then a HermiteTrajectory is returned.
        // Otherwise, a Trajectory is returned.

        // If robot and eps is given, then the IK constraints along the trajectory are solved and the path is
        // discretized at resolution eps.
        // """
        // res = trajectory.Trajectory()
        // if robot is not None:
        //     res = trajectory.RobotTrajectory(robot)
        //     if self.sections[0].velocities is not None:
        //         warnings.warn("MultiPath.getTrajectory: can't discretize IK constraints with velocities specified")
        // elif self.sections[0].velocities is not None:
        //     res = trajectory.HermiteTrajectory()

        // if robot is not None and eps is not None:
        //     from ..plan.robotcspace import ClosedLoopRobotCSpace
        //     hastiming = self.hasTiming()
        //     for i,s in enumerate(self.sections):
        //         space = ClosedLoopRobotCSpace(robot,self.getIKProblem(i))
        //         for j in range(len(s.configs)-1):
        //             ikpath = space.interpolationPath(s.configs[j],s.configs[j+1],eps)
        //             if hastiming:
        //                 t0 = s.times[j]
        //                 t1 = s.times[j+1]
        //             else:
        //                 t0 = len(res.milestones)
        //                 t1 = t0 + 1
        //             iktimes = [t0 + float(k)/float(len(ikpath)-1)*(t1-t0) for k in range(len(ikpath))]
        //             res.milestones += ikpath[:-1]
        //             res.times += iktimes[:-1]
        //     res.milestones.append(self.sections[-1].configs[-1])
        // else:
        //     for s in self.sections:
        //         res.milestones += s.configs[:-1]
        //     res.milestones.append(self.sections[-1].configs[-1])
        //     if self.sections[0].velocities is not None:
        //         vels = []
        //         for s in self.sections:
        //             assert s.velocities is not None,"Some sections have velocities, some don't?"
        //             vels += s.velocities[:-1]
        //         vels.append(self.sections[-1].velocities[-1])
        //         for i,q in enumerate(res.milestones):
        //             assert len(vels[i]) == len(q),"Velocities don't have the right size?"
        //             res.milestones[i] = q + vels[i]
        //     if not self.hasTiming():
        //         res.times = list(range(len(res.milestones)))
        //     else:
        //         for s in self.sections:
        //             res.times += s.times[:-1]
        //         res.times.append(self.sections[-1].times[-1])
        // return res


    }






// private static helper methods

//     def _escape_nl(text):
//     return escape(text).replace('\n','&#x0A;')

// def _prettify(elem,indent_level=0):
//     """Return a pretty-printed XML string for the Element.
//     """
//     indent = "  "
//     res = indent_level*indent + '<'+elem.tag.encode('utf-8')
//     for k in elem.keys():
//         res += " "+k.encode('utf-8')+'="'+_escape_nl(elem.get(k)).encode('utf-8')+'"'
//     children  = elem.getchildren()
//     if len(children)==0 and not elem.text:
//         res += ' />'
//         return res
//     res += '>'
    
//     if elem.text:
//         res += _escape_nl(elem.text).encode('utf-8')
//     for c in children:
//         res += '\n'+_prettify(c,indent_level+1)
//     if len(children)>0:
//         res += '\n'+indent_level*indent
//     res += '</'+elem.tag.encode('utf-8')+'>'
//     return res