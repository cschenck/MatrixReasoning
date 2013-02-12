package taskSolver.patternClassifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import utility.AdaBoost;
import utility.Context;
import utility.Utility;

import matrices.MatrixEntry;

public class AdaBoostContextClassifiers implements PatternClassifier {
	
	private Map<Context, PatternClassifier> classifiers;
	private Map<PatternClassifier, Double> weights;
	private Random rand;
	
	public AdaBoostContextClassifiers(List<Integer> trainExecutions, int testExecution, Set<Context> contexts, Random rand)
	{
		this.rand = rand;
		classifiers = new HashMap<Context, PatternClassifier>();
		for(Context c : contexts)
		{
			Set<Context> cs = new HashSet<Context>();
			cs.add(c);
			classifiers.put(c, new AllFeaturesSingleClassifier(trainExecutions, testExecution, cs, rand));
		}
	}

	@Override
	public void trainClassifier(Set<List<MatrixEntry>> positiveExamples, Set<List<MatrixEntry>> negativeExamples) {
		
		Utility.debugPrint("training classifiers...");
		for(PatternClassifier pc : classifiers.values())
			pc.trainClassifier(positiveExamples, negativeExamples);
		
		System.err.print("boosting classifiers...");
		Map<PatternClassifier, Map<List<MatrixEntry>, Boolean>> predictions = new HashMap<PatternClassifier, Map<List<MatrixEntry>,Boolean>>();
		for(PatternClassifier pc : classifiers.values())
		{
			predictions.put(pc, new HashMap<List<MatrixEntry>, Boolean>());
			for(List<MatrixEntry> list : positiveExamples)
			{
				if(pc.classifyPattern(list) > 0.5)
					predictions.get(pc).put(list, true);
				else
					predictions.get(pc).put(list, false);
			}
			
			for(List<MatrixEntry> list : negativeExamples)
			{
				if(pc.classifyPattern(list) > 0.5)
					predictions.get(pc).put(list, false);
				else
					predictions.get(pc).put(list, true);
			}
		}
		
		Set<List<MatrixEntry>> tasks = new HashSet<List<MatrixEntry>>();
		tasks.addAll(positiveExamples);
		tasks.addAll(negativeExamples);
		
		AdaBoost<PatternClassifier, List<MatrixEntry>> booster = 
				new AdaBoost<PatternClassifier, List<MatrixEntry>>(new ArrayList<PatternClassifier>(classifiers.values()), 
						predictions, tasks, rand);
		weights = booster.generateWeights();
		System.err.println("done");
	}

	@Override
	public double classifyPattern(List<MatrixEntry> list) {
		//let's do a weighted average
		double ret = 0;
		double sum = 0;
		for(PatternClassifier pc : classifiers.values())
		{
			ret += weights.get(pc)*pc.classifyPattern(list);
			sum += weights.get(pc);
		}
		
		return ret/sum;
	}

	@Override
	public void reset() {
		for(PatternClassifier pc : classifiers.values())
			pc.reset();
		
		weights = null;
	}

}
