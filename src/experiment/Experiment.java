package experiment;

import java.util.List;

import utility.Context;
import utility.Tuple;

public interface Experiment {
	
	public Tuple<Double, String> runExperiment(List<Context> contexts);
	
	public String name();

}
