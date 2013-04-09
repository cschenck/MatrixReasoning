package experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matrices.patterns.Pattern;
import utility.Context;
import weka.core.Attribute;
import weka.core.Instances;

public interface Experiment {
	
	public enum ExperimentVariable {
		ROWS_COLS, FUNCTION, NUM_CONTEXTS, NUM_CANDIDATES, DIFFICULTY_TYPE, ACCURACY, STD_DEV
	}
	
	public enum ROWS_COLS_VALUES {
		ROWS_ONLY, COLS_ONLY, BOTH
	}
	
	public Instances runExperiment(List<Context> contexts, List<Integer> numCandidateObjects, 
			Map<ExperimentVariable, Attribute> attributes, ArrayList<Attribute> attributeList);
	
	public Set<Pattern> getValidPatterns();
	
	public String name();

}
