package experiment.scoredChangeExps;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import matrices.Matrix;
import matrices.MatrixCompletionTask;
import matrices.MatrixEntry;
import matrices.MatrixGenerator;
import matrices.patterns.DecrementPattern;
import matrices.patterns.DifferentPattern;
import matrices.patterns.IncrementPattern;
import matrices.patterns.OneSameOneDifferentPattern;
import matrices.patterns.Pattern;
import matrices.patterns.SamePattern;
import taskSolver.ScoredChangeSolver;
import taskSolver.TaskSolver;
import taskSolver.comparisonFunctions.ComparisonFunction;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import taskSolver.comparisonFunctions.DistanceComparatorLogisticsNormalization;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import featureExtraction.FeatureExtractionManager;

public class ScoredChangeExp {
	
	private final static int NUM_TASKS = 50; 
	private final static int NUM_CHOICES = 5;
	private final static long RANDOM_SEED = 1;
	
	private final static Map<String, List<String>> ORDERED_PROPERTIES = DEFINE_PROPERTIES(); 
	
	private static Map<String, List<String>> DEFINE_PROPERTIES()
	{
		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		
		//weight, with light, medium, heavy
		List<String> values = new ArrayList<String>();
		values.add("light"); values.add("medium"); values.add("heavy");
		ret.put("weight", values);
		
		return ret;
	}
	
	private TaskSolver solver;
	private List<ComparisonFunction> comparators;
	private List<MatrixCompletionTask> tasks;
	private List<MatrixEntry> objects;
	
	private Set<Pattern> rowPatterns = new HashSet<Pattern>();
	private Set<Pattern> colPatterns = new HashSet<Pattern>();
	private Map<Pattern, Boolean> validPatterns = new HashMap<Pattern, Boolean>();
	
	private Random rand;
	
	public ScoredChangeExp(String objectFilepath)
	{
		rand = new Random(RANDOM_SEED);
		System.out.println("loading objects");
		this.initializeObjects(objectFilepath);
		System.out.println("initalizing patterns");
		this.intializePatterns();
		System.out.println("generating tasks");
		this.initializeTasks();
		System.out.println("initializing comparators");
		this.initializeComparators();
		System.out.println("initializing solver");
		this.initializeSolver();
		System.out.println("initialization complete");
	}
	
	public void runExperiment(String logfile)
	{
		FileWriter fw = null;
		try {
			fw = new FileWriter(logfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int correct = 0;
		int total = 0;
		int progress = 0;
		
		for(MatrixCompletionTask task : tasks)
		{
			Map<MatrixEntry, Double> results = solver.solveTask(task, comparators);
			
			Entry<MatrixEntry, Double> max = null;
			for(Entry<MatrixEntry, Double> e : results.entrySet())
			{
				if(max == null || e.getValue() > max.getValue())
					max = e;
			}
			
			total++;
			if(task.isCorrect(max.getKey()))
				correct++;
			
			try {
				if(!task.isCorrect(max.getKey()))
					fw.write("############## WRONG #################\n");
				fw.write(task.toString() + "\n");
				MatrixEntry correctChoice = null;
				for(Entry<MatrixEntry, Double> e : results.entrySet())
				{
					fw.write(e.getKey().toString() + ":" + (100*e.getValue()) + "%\n");
					if(task.isCorrect(e.getKey()))
						correctChoice = e.getKey();
				}
				fw.write("Correct = " + correctChoice.toString() + "\n");
				fw.write("==================================================================\n");
				fw.flush();
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			progress++;
			System.out.println("Completed " + progress + " out of " + NUM_TASKS);
		}
		
		System.out.println("correct  =" + correct);
		System.out.println("incorrect=" + (total - correct));
		System.out.println("total    =" + total);
		
		try {
			fw.write("correct  =" + correct + "\n");
			fw.write("incorrect=" + (total - correct) + "\n");
			fw.write("total    =" + total + "\n");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initializeObjects(String objectFilepath)
	{
		try {
			objects = MatrixEntry.loadMatrixEntryFile(objectFilepath);
			FeatureExtractionManager feManager = new FeatureExtractionManager(rand);
			feManager.assignFeatures(objects, getContexts());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("The objects file was not found at " + objectFilepath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Set<Context> getContexts() {
		Set<Context> contexts = new HashSet<Context>();
		//add each context explicitly so we know which ones we're using
		//audio contexts
		contexts.add(new Context(Behavior.crush, Modality.audio));
		contexts.add(new Context(Behavior.grasp, Modality.audio));
		contexts.add(new Context(Behavior.high_velocity_shake, Modality.audio));
		contexts.add(new Context(Behavior.hold, Modality.audio));
		contexts.add(new Context(Behavior.lift_slow, Modality.audio));
		contexts.add(new Context(Behavior.low_drop, Modality.audio));
		contexts.add(new Context(Behavior.poke, Modality.audio));
		contexts.add(new Context(Behavior.push, Modality.audio));
		contexts.add(new Context(Behavior.shake, Modality.audio));
		contexts.add(new Context(Behavior.tap, Modality.audio));
		//proprioception contexts
		contexts.add(new Context(Behavior.crush, Modality.proprioception));
		contexts.add(new Context(Behavior.grasp, Modality.proprioception));
		contexts.add(new Context(Behavior.high_velocity_shake, Modality.proprioception));
		contexts.add(new Context(Behavior.hold, Modality.proprioception));
		contexts.add(new Context(Behavior.lift_slow, Modality.proprioception));
		contexts.add(new Context(Behavior.low_drop, Modality.proprioception));
		contexts.add(new Context(Behavior.poke, Modality.proprioception));
		contexts.add(new Context(Behavior.push, Modality.proprioception));
		contexts.add(new Context(Behavior.shake, Modality.proprioception));
		contexts.add(new Context(Behavior.tap, Modality.proprioception));
		//color contexts
		contexts.add(new Context(Behavior.look, Modality.color));
		
		return contexts;
	}

	private void initializeSolver()
	{
		solver = new ScoredChangeSolver();
	}
	
	private void initializeComparators()
	{
		comparators = new ArrayList<ComparisonFunction>();
		
		 /*
		//cheating comparators for testing purposes
		comparators.add(new CheatingComparator("weight", ORDERED_PROPERTIES.get("weight")));
		comparators.add(new CheatingComparator("color"));
		comparators.add(new CheatingComparator("contents"));
		
		/*/
		
		//distance function comparators
		
		for(Context c : getContexts())
		{
			if(c.modality.equals(Modality.color))
				comparators.add(new DistanceComparatorLogisticsNormalization(c, DistanceFunction.Euclidean, objects));
//				comparators.add(new CheatingComparator("color"));
			else
				comparators.add(new DistanceComparatorLogisticsNormalization(c, DistanceFunction.Euclidean, objects));
		}
		
		// */
	}
	
	private void initializeTasks()
	{
		if(objects == null)
			throw new IllegalStateException("The matrix completion tasks cannot be initialized until after the objects are initialized");
		
		tasks = new ArrayList<MatrixCompletionTask>();
		for(int i = 0; i < NUM_TASKS; i++)
		{
			Matrix m = MatrixGenerator.generateMatrix(objects, rowPatterns, colPatterns, validPatterns, rand);
			List<MatrixEntry> choices = new ArrayList<MatrixEntry>();
			choices.add(m.getEntry(m.getNumRows() - 1, m.getNumCols() - 1));
			while(choices.size() < NUM_CHOICES)
			{
				MatrixEntry choice = objects.get(rand.nextInt(objects.size()));
				//make sure we don't have any repeates or objects that are in the matrix
				if(!choices.contains(choice) && !m.contains(choice))
				{
					Matrix test = new Matrix(m);
					test.setEntry(test.getNumRows() - 1, test.getNumCols() - 1, choice);
					//make sure that there aren't two correct choices
					if(!test.isValidMatrix(rowPatterns, colPatterns, validPatterns))
						choices.add(choice);
				}
			}
			tasks.add(new MatrixCompletionTask(m, m.getEntry(m.getNumRows() - 1, m.getNumCols() - 1), choices, rand));
			
		}
	}
	
	private void intializePatterns() {
		
		for(String property: objects.get(0).getDefinedProperties())
		{
			Pattern sp = new SamePattern(property, rand);
			Pattern osod = new OneSameOneDifferentPattern(property, rand);
			
			rowPatterns.add(sp); rowPatterns.add(osod);
			colPatterns.add(sp); colPatterns.add(osod);
			validPatterns.put(sp, true); validPatterns.put(osod, false);
			
			if(ORDERED_PROPERTIES.keySet().contains(property))
			{
				Pattern inc = new IncrementPattern(property, ORDERED_PROPERTIES.get(property), rand);
				Pattern dec = new DecrementPattern(property, ORDERED_PROPERTIES.get(property), rand);
				Pattern dp = new DifferentPattern(property, new HashSet<MatrixEntry>(objects), rand);
				
				rowPatterns.add(inc); rowPatterns.add(dec); rowPatterns.add(dp);
				colPatterns.add(inc); colPatterns.add(dec); colPatterns.add(dp);
				validPatterns.put(inc, true); validPatterns.put(dec, true); validPatterns.put(dp, false); 
			}
			else
			{
				Pattern dp = new DifferentPattern(property, new HashSet<MatrixEntry>(objects), rand);
				rowPatterns.add(dp);
				colPatterns.add(dp);
				validPatterns.put(dp, true); 
			}
			
		}
	}

}
