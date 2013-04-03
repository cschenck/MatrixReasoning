package experiment.scoredChangeExps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import matrices.Matrix;
import matrices.MatrixCompletionTask;
import matrices.MatrixEntry;
import matrices.MatrixGenerator;
import matrices.patterns.DecrementPattern;
import matrices.patterns.DifferentPattern;
import matrices.patterns.IncrementPattern;
import matrices.patterns.ORMetaPattern;
import matrices.patterns.OneSameOneDifferentPattern;
import matrices.patterns.Pattern;
import matrices.patterns.SamePattern;
import matrices.patterns.XORMetaPattern;
import taskSolver.ScoredChangeSolver;
import taskSolver.TaskSolver;
import taskSolver.comparisonFunctions.CheatingComparator;
import taskSolver.comparisonFunctions.ClusterDiffComparator;
import taskSolver.comparisonFunctions.ComparisonFunction;
import taskSolver.comparisonFunctions.WeightedComparisonFunction;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import taskSolver.comparisonFunctions.DistanceComparatorLogisticsNormalization;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.Tuple;
import utility.Utility;
import experiment.Experiment;
import featureExtraction.FeatureExtractionManager;

public class ScoredChangeExpGDescentClustering implements Experiment {
	
	private final static int NUM_TASKS = 500; 
	private final static int NUM_CHOICES = 5;
	private final static int NUM_FOLDS = 10;
	private final static long RANDOM_SEED = 1;
	private final static String TASK_CACHE_FILE = "cachedTasks.txt";
	
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
	private Map<Context, ComparisonFunction> allComparators;
	private List<MatrixCompletionTask> tasks;
	private List<MatrixEntry> objects;
	private Set<Context> allContexts;
	
	private Set<Pattern> rowPatterns = new HashSet<Pattern>();
	private Set<Pattern> colPatterns = new HashSet<Pattern>();
	private Map<Pattern, Boolean> validPatterns = new HashMap<Pattern, Boolean>();
	
	private Random rand;
	
	public ScoredChangeExpGDescentClustering(List<MatrixEntry> objects, Set<Context> allContexts)
	{
		rand = new Random(RANDOM_SEED);
//		System.out.println("loading objects");
//		this.initializeObjects(objectFilepath);
		this.objects = objects;
		this.allContexts = allContexts;
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
	
	public Tuple<Double, String> runExperiment(List<Context> contexts)
	{
		List<String> output = new ArrayList<String>();
		for(int i = 0; i < tasks.size(); i++)
			output.add("");
		
		int correct = 0;
		int total = 0;
		
		List<ComparisonFunction> comparators = new ArrayList<ComparisonFunction>();
		for(Context c : contexts)
			comparators.add(allComparators.get(c));
		
		for(int fold = 0; fold < NUM_FOLDS; fold++)
		{
			Map<ComparisonFunction, Double> weights = trainWeights(fold, comparators);
			List<ComparisonFunction> weightedComparators = new ArrayList<ComparisonFunction>();
			for(Entry<ComparisonFunction, Double> e : weights.entrySet())
				weightedComparators.add(new WeightedComparisonFunction(e.getKey(), e.getValue()));
			
			for(int i = 0; i < tasks.size(); i++)
			{
				if(i % NUM_FOLDS != fold)
					continue;
				
				MatrixCompletionTask task = tasks.get(i);
			
				Map<MatrixEntry, Double> results = solver.solveTask(task, weightedComparators);
			
				Entry<MatrixEntry, Double> max = null;
				for(Entry<MatrixEntry, Double> e : results.entrySet())
				{
					if(max == null || e.getValue() > max.getValue())
						max = e;
				}
				
				total++;
				if(task.isCorrect(max.getKey()))
					correct++;
				
				String out = "<";
				if(!task.isCorrect(max.getKey()))
					out += "INCORRECT,";
				else
					out += "CORRECT,";
				out += max.getKey().getName() + ">,";
				
				output.set(i, out);
			}
		}
		
		Tuple<Double, String> ret = new Tuple<Double, String>((double)1.0*correct/total, output.toString());
		return ret;
	}
	
//	public void runExperiment(String logfile)
//	{
//		FileWriter fw = null;
//		try {
//			fw = new FileWriter(logfile);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		int correct = 0;
//		int total = 0;
//		int progress = 0;
//		
//		for(int fold = 0; fold < NUM_FOLDS; fold++)
//		{
//			System.out.println("Training weights for fold " + fold);
//			Map<ComparisonFunction, Double> weights = trainWeights(fold);
//			List<ComparisonFunction> weightedComparators = new ArrayList<ComparisonFunction>();
//			for(Entry<ComparisonFunction, Double> e : weights.entrySet())
//				weightedComparators.add(new WeightedComparisonFunction(e.getKey(), e.getValue()));
//			
//			for(int i = 0; i < tasks.size(); i++)
//			{
//				if(i % NUM_FOLDS != fold)
//					continue;
//				
//				MatrixCompletionTask task = tasks.get(i);
//				
//				Map<MatrixEntry, Double> results = solver.solveTask(task, weightedComparators);
//				
//				Entry<MatrixEntry, Double> max = null;
//				for(Entry<MatrixEntry, Double> e : results.entrySet())
//				{
//					if(max == null || e.getValue() > max.getValue())
//						max = e;
//				}
//				
//				total++;
//				if(task.isCorrect(max.getKey()))
//					correct++;
//				
//				try {
//					if(!task.isCorrect(max.getKey()))
//						fw.write("############## WRONG #################\n");
//					fw.write(task.toString() + "\n");
//					MatrixEntry correctChoice = null;
//					for(Entry<MatrixEntry, Double> e : results.entrySet())
//					{
//						fw.write(e.getKey().toString() + ":" + (100*e.getValue()) + "%\n");
//						if(task.isCorrect(e.getKey()))
//							correctChoice = e.getKey();
//					}
//					fw.write("Correct = " + correctChoice.toString() + "\n");
//					fw.write("==================================================================\n");
//					fw.flush();
//					
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//				
//				progress++;
//				System.out.println("Completed " + progress + " out of " + NUM_TASKS);
//			}
//		}
//		
//		System.out.println("correct  =" + correct);
//		System.out.println("incorrect=" + (total - correct));
//		System.out.println("total    =" + total);
//		
//		try {
//			fw.write("correct  =" + correct + "\n");
//			fw.write("incorrect=" + (total - correct) + "\n");
//			fw.write("total    =" + total + "\n");
//			fw.flush();
//			fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	private Map<ComparisonFunction, Double> trainWeights(int testFold, List<ComparisonFunction> comparators)
	{		
		Map<List<ComparisonFunction>, Double> bestSetPerformance = new HashMap<List<ComparisonFunction>, Double>();
		List<ComparisonFunction> bestSet = new ArrayList<ComparisonFunction>(comparators);
		Collections.sort(bestSet, new Comparator<ComparisonFunction>() {
			@Override
			public int compare(ComparisonFunction o1, ComparisonFunction o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
		bestSetPerformance.put(bestSet, computeAccuracy(testFold, bestSet));
		
		/*
		 * Perform gradient decent. Start with all the comparison functions (cf's) in use and
		 * evaluate its performance. Then iterate over each cf, remove it, and evaluate the
		 * performance of the remaining cf's. Permanently remove the cf that caused the biggest
		 * increase in performance over using all the cf's. Now repeat this entire process on
		 * the remaining cf's until all of them have been removed. Once this is done, go back
		 * and find the set that had the highest performance and set all the weights for the
		 * cf's in that set to 1.0 and the rest to 0.0.
		 */
		while(bestSet.size() > 1)
		{
			List<ComparisonFunction> bestSoFar = null;
			double bestPerformanceSoFar = 0.0;
			for(ComparisonFunction cf : bestSet)
			{
				List<ComparisonFunction> temp = new ArrayList<ComparisonFunction>(bestSet);
				temp.remove(cf);
				double accuracy = computeAccuracy(testFold, temp);
				
				if(accuracy >= bestPerformanceSoFar)
				{
					bestPerformanceSoFar = accuracy;
					bestSoFar = temp;
				}
			}
			
			bestSetPerformance.put(bestSoFar, bestPerformanceSoFar);
			bestSet = bestSoFar;
			
		}
		
		//now find the set that did the best
		List<Entry<List<ComparisonFunction>, Double>> list = 
				new ArrayList<Entry<List<ComparisonFunction>, Double>>(bestSetPerformance.entrySet());
		//this is sorted because converting the entry set to a list is not done deterministically by Java's hashmap
		Collections.sort(list, new Comparator<Entry<List<ComparisonFunction>, Double>>() {
			@Override
			public int compare(Entry<List<ComparisonFunction>, Double> o1,
					Entry<List<ComparisonFunction>, Double> o2) {
				return o1.getKey().toString().compareTo(o2.getKey().toString());
			}
		});
		Entry<List<ComparisonFunction>, Double> best = Utility.getMax(
				list, 
				new Comparator<Entry<List<ComparisonFunction>, Double>>() {
					@Override
					public int compare(Entry<List<ComparisonFunction>, Double> o1,
							Entry<List<ComparisonFunction>, Double> o2) {
						return Double.compare(o1.getValue(), o2.getValue());
					}
				}, 
				rand);
		
		//now set the weights
		Map<ComparisonFunction, Double> ret = new HashMap<ComparisonFunction, Double>();
		for(ComparisonFunction cf : comparators)
		{
			if(best.getKey().contains(cf))
				ret.put(cf, 1.0);
			else
				ret.put(cf, 0.0);
		}
		
		return ret;
	}

	//this is to cache the accuracies for faster lookup, the array is to reduce collisions
	//java wouldn't let me create an array with a generic type, so I had to leave off the generic
	@SuppressWarnings("rawtypes")
	private Map[] cachedAccuracies = new Map[100];
	@SuppressWarnings("unchecked")
	private double computeAccuracy(int testFold, List<ComparisonFunction> temp) {
		//first lets see if this value is cached
//		Tuple<List<ComparisonFunction>, Integer> pair = new Tuple<List<ComparisonFunction>, Integer>(temp, testFold);
//		int hash = Math.abs(pair.hashCode()%cachedAccuracies.length);
//		if(cachedAccuracies[hash] == null)
//			cachedAccuracies[hash] = new HashMap<Tuple<List<ComparisonFunction>, Integer>, Double>();
//		Map<Tuple<List<ComparisonFunction>, Integer>, Double> map = cachedAccuracies[hash];
//		synchronized(map) {
//			if(map.get(pair) != null)
//				return map.get(pair).doubleValue();
//		}
		
		//if it wasn't stored, let's compute it
		
		int correct = 0;
		int total = 0;
		for(int i = 0; i < tasks.size(); i++)
		{
			if(i % NUM_FOLDS == testFold)
				continue;
			
			MatrixCompletionTask task = tasks.get(i);
			
			
			Map<MatrixEntry, Double> results = solver.solveTask(task, temp);
			
			Entry<MatrixEntry, Double> max = null;
			for(Entry<MatrixEntry, Double> e : results.entrySet())
			{
				if(max == null || e.getValue() > max.getValue())
					max = e;
			}
			
			total++;
			if(task.isCorrect(max.getKey()))
				correct++;
		}
		double accuracy = (double)1.0*correct/total;
		
		//okay, now lets store it for later recall
//		synchronized(map) {
//			map.put(pair, accuracy);
//		}
		
		return accuracy;
	}	
	
	private void initializeObjects(String objectFilepath)
	{
		try {
			objects = MatrixEntry.loadMatrixEntryFile(objectFilepath);
			FeatureExtractionManager feManager = new FeatureExtractionManager(rand);
			feManager.assignFeatures(objects, getAllContexts());
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("The objects file was not found at " + objectFilepath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Set<Context> getAllContexts() {
//		Set<Context> contexts = new HashSet<Context>();
//		//add each context explicitly so we know which ones we're using
//		//audio contexts
//		contexts.add(new Context(Behavior.crush, Modality.audio));
//		contexts.add(new Context(Behavior.grasp, Modality.audio));
//		contexts.add(new Context(Behavior.high_velocity_shake, Modality.audio));
//		contexts.add(new Context(Behavior.hold, Modality.audio));
//		contexts.add(new Context(Behavior.lift_slow, Modality.audio));
//		contexts.add(new Context(Behavior.low_drop, Modality.audio));
//		contexts.add(new Context(Behavior.poke, Modality.audio));
//		contexts.add(new Context(Behavior.push, Modality.audio));
//		contexts.add(new Context(Behavior.shake, Modality.audio));
//		contexts.add(new Context(Behavior.tap, Modality.audio));
//		//proprioception contexts
//		contexts.add(new Context(Behavior.crush, Modality.proprioception));
//		contexts.add(new Context(Behavior.grasp, Modality.proprioception));
//		contexts.add(new Context(Behavior.high_velocity_shake, Modality.proprioception));
//		contexts.add(new Context(Behavior.hold, Modality.proprioception));
//		contexts.add(new Context(Behavior.lift_slow, Modality.proprioception));
//		contexts.add(new Context(Behavior.low_drop, Modality.proprioception));
//		contexts.add(new Context(Behavior.poke, Modality.proprioception));
//		contexts.add(new Context(Behavior.push, Modality.proprioception));
//		contexts.add(new Context(Behavior.shake, Modality.proprioception));
//		contexts.add(new Context(Behavior.tap, Modality.proprioception));
//		//color contexts
//		contexts.add(new Context(Behavior.look, Modality.color));
//		
//		return contexts;
		
		return allContexts;
	}

	private void initializeSolver()
	{
		solver = new ScoredChangeSolver();
	}
	
	private void initializeComparators()
	{
		allComparators = new HashMap<Context, ComparisonFunction>();
		
		for(Context c : getAllContexts())
		{
			Set<Context> temp = new HashSet<Context>();
			temp.add(c);
			allComparators.put(c, new ClusterDiffComparator(objects, temp));
		}
	}
	
	private void initializeTasks()
	{
		if(objects == null)
			throw new IllegalStateException("The matrix completion tasks cannot be initialized until after the objects are initialized");
		
		if(new File(TASK_CACHE_FILE).exists())
		{
			tasks = loadTasksFromFile();
		}
		else
		{
			tasks = generateTasks();
			writeTasksToFile(tasks);
		}
	}
	
	private void writeTasksToFile(List<MatrixCompletionTask> tasks)
	{
		try {
			FileWriter fw = new FileWriter(new File(TASK_CACHE_FILE));
			for(MatrixCompletionTask t : tasks)
				fw.write(t.toSerialString()  + "\n");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private List<MatrixCompletionTask> loadTasksFromFile()
	{
		List<MatrixCompletionTask> ret = new ArrayList<MatrixCompletionTask>();
		Scanner lines;
		try {
			lines = new Scanner(new File(TASK_CACHE_FILE));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		while(lines.hasNextLine())
		{
			ret.add(MatrixCompletionTask.constructTaskFromSerialString(lines.nextLine(), objects, rand));
		}
		
		return ret;
	}

	private List<MatrixCompletionTask> generateTasks() {
		Set<Matrix> matrices = MatrixGenerator.generateMatrix(objects, rowPatterns, colPatterns, validPatterns, NUM_TASKS, rand);
		List<MatrixCompletionTask> tasks = new ArrayList<MatrixCompletionTask>();
		for(Matrix m : matrices)
		{
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
		
		return tasks;
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
				Pattern xor = new XORMetaPattern(new DifferentPattern(property, new HashSet<MatrixEntry>(objects), rand), 
						new ORMetaPattern(dec, inc));
				
				rowPatterns.add(inc); rowPatterns.add(dec); rowPatterns.add(xor);
				colPatterns.add(inc); colPatterns.add(dec); colPatterns.add(xor);
				validPatterns.put(inc, true); validPatterns.put(dec, true); validPatterns.put(xor, false); 
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

	@Override
	public String name() {
		return "ClusterDiffGAscent";
	}

}
