package experiment.comparisonFunctionExps;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
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
import matrices.patterns.DifferentPattern;
import matrices.patterns.OneSameOneDifferentPattern;
import matrices.patterns.Pattern;
import matrices.patterns.SamePattern;
import taskSolver.GaussianSolver;
import taskSolver.comparisonFunctions.ComparisonFunction;
import taskSolver.comparisonFunctions.DistanceComparator;
import taskSolver.comparisonFunctions.WeightedGaussianSolver;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import taskSolver.TaskSolver;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.Utility;
import featureExtraction.FeatureExtractionManager;



public class MatrixCompletionExperimentAdaBoost {
	
	private final static int NUM_TASKS = 100; 
	private final static int NUM_FOLDS = 5;
	private final static int NUM_CHOICES = 5;
	private final static long RANDOM_SEED = 1;
	
	private TaskSolver solver;
	private List<ComparisonFunction> comparators;
	private List<MatrixCompletionTask> tasks;
	private List<MatrixEntry> objects;
	private Map<ComparisonFunction, Double> functionWeights;
	
	private Set<Pattern> rowPatterns = new HashSet<Pattern>();
	private Set<Pattern> colPatterns = new HashSet<Pattern>();
	private Map<Pattern, Boolean> validPatterns = new HashMap<Pattern, Boolean>();
	
	private Random rand;
	
	public MatrixCompletionExperimentAdaBoost(String objectFilepath)
	{
		rand = new Random(RANDOM_SEED);
		System.out.println("loading objects");
		this.initializeObjects(objectFilepath);
		System.out.println("initalizing patterns");
		this.intializePatterns();
		System.out.println("generating tasks");
		this.initializeTasks();
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
		
		for(int fold = 0; fold < NUM_FOLDS; fold++)
		{
			System.out.println("Beginning training fold " + fold);
			//first seperate out the folds
			List<MatrixCompletionTask> trainTasks = new ArrayList<MatrixCompletionTask>();
			List<MatrixCompletionTask> testTasks = new ArrayList<MatrixCompletionTask>();
			for(int i = 0; i < tasks.size(); i++)
			{
				if(i % NUM_FOLDS == fold)
					testTasks.add(tasks.get(i));
				else
					trainTasks.add(tasks.get(i));
			}
			//now initialize the comparators
			this.initializeComparators(trainTasks);
			//now that the comparators are initialized (and the weights computed), intialize the task solver
			this.initializeSolver();
			System.out.println("Finished computing weights, beginning testing");
			try {
				fw.write("==================================================================\n");
				fw.write("===================Fold " + fold + "====================================\n");
				for(Entry<ComparisonFunction, Double> e : this.functionWeights.entrySet())
					fw.write(e.getKey().toString() + " = " + e.getValue() + "\n");
				fw.write("==================================================================\n");
				fw.flush();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			
			int progress = 0;
			for(MatrixCompletionTask task : testTasks)
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
				System.out.println("Completed " + progress + " out of " + testTasks.size());
			}
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
		solver = new WeightedGaussianSolver(this.functionWeights);
	}
	
	private void initializeComparators(List<MatrixCompletionTask> trainTasks)
	{
		comparators = new ArrayList<ComparisonFunction>();
		
		 //distance function comparators
		for(Context c : getContexts())
		{
			if(c.modality.equals(Modality.color))
				comparators.add(new DistanceComparator(c, DistanceFunction.Euclidean, objects));
			else
				comparators.add(new DistanceComparator(c, DistanceFunction.Euclidean, objects));
		}
		
		//now compute the weight for each comparator
		Map<MatrixCompletionTask, Map<ComparisonFunction, MatrixEntry>> predictions = new HashMap<MatrixCompletionTask, Map<ComparisonFunction,MatrixEntry>>();
		TaskSolver solver = new GaussianSolver();
		//the first thing to do is compute the predictions for each individual comparator
		for(MatrixCompletionTask task : trainTasks)
		{
			predictions.put(task, new HashMap<ComparisonFunction, MatrixEntry>());
			for(ComparisonFunction cf : comparators)
			{
				List<ComparisonFunction> singleton = new ArrayList<ComparisonFunction>();
				singleton.add(cf);
				//this next line of code, oh man, sooooo complex
				//essentially it just gets the MatrixEntry that cf decides best completes the matrix
				MatrixEntry prediction = Utility.getMax(new ArrayList<Entry<MatrixEntry, Double>>(
						solver.solveTask(task, singleton).entrySet()), 
						new Comparator<Entry<MatrixEntry, Double>>() {
							@Override
							public int compare(Entry<MatrixEntry, Double> arg0,
									Entry<MatrixEntry, Double> arg1) {
								if(arg0.getValue() > arg1.getValue())
									return 1;
								else if(arg0.getValue() < arg1.getValue())
									return -1;
								else
									return 0;
							}
				}, rand).getKey();
				predictions.get(task).put(cf, prediction);
			}
		}
		
		//next initialize the AdaBooster and compute the weights
		AdaBooster booster = new AdaBooster(comparators, predictions, new HashSet<MatrixCompletionTask>(trainTasks), rand);
		this.functionWeights = booster.generateWeights();
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
			SamePattern sp = new SamePattern(property);
			DifferentPattern dp = new DifferentPattern(property, new HashSet<MatrixEntry>(objects), rand);
			OneSameOneDifferentPattern osod = new OneSameOneDifferentPattern(property);
			
			rowPatterns.add(sp); rowPatterns.add(dp); rowPatterns.add(osod);
			colPatterns.add(sp); colPatterns.add(dp); colPatterns.add(osod);
			validPatterns.put(sp, true); validPatterns.put(dp, true); validPatterns.put(osod, false);
		}
	}
	
	private class AdaBooster {
		
		private static final int NUM_ITERATIONS = 50;
		
		private List<ComparisonFunction> functions;
		private Map<MatrixCompletionTask, Map<ComparisonFunction, MatrixEntry>> predictions;
		private Set<MatrixCompletionTask> tasks;
		private Random rand;
		
		public AdaBooster(List<ComparisonFunction> functions, Map<MatrixCompletionTask, Map<ComparisonFunction, MatrixEntry>> predictions, 
				Set<MatrixCompletionTask> tasks, Random rand)
		{
			this.functions = functions;
			this.predictions = predictions;
			this.tasks = tasks;
			this.rand = rand;
		}
		
		public Map<ComparisonFunction, Double> generateWeights()
		{
			Map<ComparisonFunction, Double> ret = new HashMap<ComparisonFunction, Double>();
			for(ComparisonFunction cf : functions)
				ret.put(cf, 0.0);
			
			Map<MatrixCompletionTask, Double> taskWeights = new HashMap<MatrixCompletionTask, Double>();
			for(MatrixCompletionTask task : tasks)
				taskWeights.put(task, 1.0/tasks.size());
			
			for(int iteration = 0; iteration < NUM_ITERATIONS; iteration++)
			{
				//first we need to calculate the error for each comparison function
				Map<ComparisonFunction, Double> functionErrors = new HashMap<ComparisonFunction, Double>();
				for(ComparisonFunction cf : functions)
					functionErrors.put(cf, computeError(cf, taskWeights));
				
				//now find the one that maximizes |0.5 - e| where e is the error
				Entry<ComparisonFunction, Double> best = Utility.getMax(new ArrayList<Entry<ComparisonFunction, Double>>(functionErrors.entrySet()), 
						new Comparator<Entry<ComparisonFunction, Double>>() {
							public int compare(Entry<ComparisonFunction, Double> o1, Entry<ComparisonFunction, Double> o2) {
								double e1 = -o1.getValue();
								double e2 = -o2.getValue();
								if(e1 > e2)
									return 1;
								else if(e1 < e2)
									return -1;
								else
									return 0;
							}
						}, rand);
				
				//now compute the weight for this context
				double alpha = 0.5*Math.log((1.0 - best.getValue())/best.getValue());
				
				//add the weight to the weight for that classifier (this is equivalent to the normal way in which adaboost is done)
				ret.put(best.getKey(), ret.get(best.getKey()) + alpha);
				
				//now update the task weights
				for(MatrixCompletionTask task : tasks)
				{
					double old = taskWeights.get(task);
					double w = 0.0;
					if(task.isCorrect(predictions.get(task).get(best.getKey())))
						w = old*Math.exp(-1.0*alpha);
					else
						w = old*Math.exp(alpha);
					taskWeights.put(task, w);
				}
				
				//normalize the task weights
				double sum = 0;
				for(Double d : taskWeights.values())
					sum += d;
				
				for(MatrixCompletionTask task : tasks)
					taskWeights.put(task, taskWeights.get(task)/sum);
				
			}
			
			return ret;
		}

		private Double computeError(ComparisonFunction cf, Map<MatrixCompletionTask, Double> taskWeights) {
			double error = 0;
			for(MatrixCompletionTask task : tasks)
			{
				if(!task.isCorrect(predictions.get(task).get(cf)))
					error += taskWeights.get(task);
			}
			
			//the error is supposed to be between 0 and 1, but if it is either of those, we will get errors,
			//so move it a very small amount away
			if(error == 1.0)
				error = 0.9999999;
			else if(error == 0.0)
				error = 0.0000001;
			
			return error;
		}

	}

}
