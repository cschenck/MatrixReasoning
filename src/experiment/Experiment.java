package experiment;

import java.util.List;
import java.util.Map;

import utility.Context;
import utility.Tuple;

public interface Experiment {
	
	public Map<Integer, Tuple<Double, String>> runExperiment(List<Context> contexts, List<Integer> numCandidateObjects);
	
	public String name();

}
