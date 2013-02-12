package taskSolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import matrices.MatrixCompletionTask;
import matrices.MatrixEntry;
import matrices.patterns.Pattern;
import taskSolver.comparisonFunctions.ComparisonFunction;
import taskSolver.patternClassifiers.PatternClassifier;
import utility.RunningMean;

public class PatternTrainerSolver implements TaskSolver {
	
	public final static int NUM_TRAINING_EXAMPLES = 200;
	
	private Set<PatternClassifier> classifiers;
	private int matrixDim;
	private Random rand;
	
	public PatternTrainerSolver(Map<Pattern, PatternClassifier> classifiers, Set<MatrixEntry> objects, 
			int matrixDim, Random rand)
	{
		//assumes a square matrix
		this.matrixDim = matrixDim;
		this.rand = rand;
		this.trainClassifiers(classifiers, objects);
	}
	
	private void trainClassifiers(Map<Pattern, PatternClassifier> classifierMap, Set<MatrixEntry> objects)
	{
		this.classifiers = new HashSet<PatternClassifier>();
		for(Entry<Pattern, PatternClassifier> e : classifierMap.entrySet())
		{
			Set<List<MatrixEntry>> positiveExamples = new HashSet<List<MatrixEntry>>();
			Set<List<MatrixEntry>> negativeExamples = new HashSet<List<MatrixEntry>>();
			this.generateTrainingExamples(e.getKey(), objects, positiveExamples, negativeExamples);
			
			e.getValue().trainClassifier(positiveExamples, negativeExamples);
			this.classifiers.add(e.getValue());
		}
	}
	
	private void generateTrainingExamples(Pattern p, Set<MatrixEntry> objects, 
			Set<List<MatrixEntry>> positiveExamples, Set<List<MatrixEntry>> negativeExamples)
	{
		MatrixEntry[] objectArray = objects.toArray(new MatrixEntry[objects.size()]);
		
		//add in the negative examples
		negativeExamples.clear();
		while(negativeExamples.size() < NUM_TRAINING_EXAMPLES/2)
		{
			List<MatrixEntry> example = new ArrayList<MatrixEntry>();
			while(example.size() < this.matrixDim)
			{
				MatrixEntry obj = objectArray[rand.nextInt(objectArray.length)];
				if(!example.contains(obj))
					example.add(obj);
			}
			
			if(!negativeExamples.contains(example) && !p.detectPattern(example))
				negativeExamples.add(example);
		}
		
		//now do the positive examples
		positiveExamples.clear();
		while(positiveExamples.size() < NUM_TRAINING_EXAMPLES/2)
		{
			List<MatrixEntry> example = new ArrayList<MatrixEntry>();
			while(example.size() < this.matrixDim)
			{
				MatrixEntry obj = objectArray[rand.nextInt(objectArray.length)];
				if(!example.contains(obj))
					example.add(obj);
			}
			
			example = p.align(example, objects);
			
			if(!positiveExamples.contains(example))
				positiveExamples.add(example);
		}
	}

	@Override //in hindsight, I should not have made that list of comparators part of this method call
	public Map<MatrixEntry, Double> solveTask(MatrixCompletionTask task, List<ComparisonFunction> comparators) {
		
		//first let's compute the probability that each pattern exists across the rows
		Map<PatternClassifier, Double> rowProbs = new HashMap<PatternClassifier, Double>();
		for(PatternClassifier pc : classifiers)
		{
			double prob = 1.0;
			for(int i = 0; i < task.getNumRows() - 1; i++)
				prob *= pc.classifyPattern(task.getRow(i));
			rowProbs.put(pc, prob);
		}
		
		//now do the same for the columns
		Map<PatternClassifier, Double> colProbs = new HashMap<PatternClassifier, Double>();
		for(PatternClassifier pc : classifiers)
		{
			double prob = 1.0;
			for(int i = 0; i < task.getNumCols() - 1; i++)
				prob *= pc.classifyPattern(task.getCol(i));
			colProbs.put(pc, prob);
		}
		
		//now lets compute the probability that each object is the correct object
		Map<MatrixEntry, Double> ret = new HashMap<MatrixEntry, Double>();
		for(MatrixEntry obj : task.getChoices())
		{
			double prob = 1.0;
//			double prob = 0.0;
//			double sum = 0.0;
			for(PatternClassifier pc : classifiers)
			{
				List<MatrixEntry> row = new ArrayList<MatrixEntry>(task.getRow(task.getNumRows() - 1));
				row.add(obj);
				prob *= 1 - (rowProbs.get(pc)*(1 - pc.classifyPattern(row)));
//				prob += rowProbs.get(pc)*pc.classifyPattern(row); //the probability that this pattern is in the matrix * 
//				sum += rowProbs.get(pc);						  //the prob. that this row has that pattern
				
				List<MatrixEntry> col = new ArrayList<MatrixEntry>(task.getCol(task.getNumCols() - 1));
				col.add(obj);
				prob *= 1 - (colProbs.get(pc)*(1 - pc.classifyPattern(col)));
//				prob += colProbs.get(pc)*pc.classifyPattern(col);
//				sum += colProbs.get(pc);
			}
//			ret.put(obj, prob/sum);
			ret.put(obj, prob);
		}
		
		return ret;
	}

}
