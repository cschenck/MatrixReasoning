package experiment.scoredChangeExps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import taskSolver.comparisonFunctions.ClusterDiffComparator;
import taskSolver.comparisonFunctions.ComparisonFunction;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import taskSolver.comparisonFunctions.DistanceComparatorLogisticsNormalization;
import utility.Context;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import experiment.Experiment;
import featureExtraction.FeatureExtractionManager;

public class ScoredChangeExpClustering implements Experiment {
	
	private final static int NUM_TASKS = 500; 
	private final static int NUM_CHOICES = 5;
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
	
	private ROWS_COLS_VALUES rowsCols;
	private Map<MatrixCompletionTask, Set<String>> taskDifficulties = new HashMap<MatrixCompletionTask, Set<String>>();
	
	private Random rand;
	
	public ScoredChangeExpClustering(List<MatrixEntry> objects, Set<Context> allContexts, ROWS_COLS_VALUES rowsCols)
	{
		this.rowsCols = rowsCols;
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
		
		for(MatrixCompletionTask task : tasks)
		{
			this.taskDifficulties.put(task, new HashSet<String>());
			this.taskDifficulties.get(task).add("Averaged");
			Set<Pattern> patterns = getApplicablePatterns(task);
			if(patterns.size() == 1) //special case, this means there is the same pattern on the rows and columns
				this.taskDifficulties.get(task).add("2 patterns");
			else
				this.taskDifficulties.get(task).add(patterns.size() + " patterns");
			for(Pattern p : patterns)
			{
				this.taskDifficulties.get(task).add(p.toString());
				this.taskDifficulties.get(task).add(p.getRelavantProperties().toString());
				this.taskDifficulties.get(task).add(p.getPatternName());
			}
		}
	}
	
	private Set<Pattern> getApplicablePatterns(MatrixCompletionTask task)
	{
		Set<Pattern> ret = new HashSet<Pattern>();
		
		List<List<MatrixEntry>> rows = new ArrayList<List<MatrixEntry>>();
		for(int i = 0; i < task.getNumRows() - 1; i++)
			rows.add(task.getRow(i));
		
		for(Pattern p : rowPatterns)
		{
			boolean predicate = true;
			for(List<MatrixEntry> row : rows)
			{
				if(!p.detectPattern(row))
					predicate = false;
			}
			if(predicate && !ret.contains(p))
				ret.add(p);
		}
		
		//put the matrix into col format
		List<List<MatrixEntry>> cols = new ArrayList<List<MatrixEntry>>();
		for(int i = 0; i < task.getNumCols() - 1; i++)
			cols.add(task.getCol(i));
		
		for(Pattern p : colPatterns)
		{
			boolean predicate = true;
			for(List<MatrixEntry> col : cols)
			{
				if(!p.detectPattern(col))
					predicate = false;
			}
			if(predicate && !ret.contains(p))
				ret.add(p);
		}

		return ret;
	}
	
	public Instances runExperiment(List<Context> contexts, List<Integer> numCandidateObjects, 
			Map<ExperimentVariable, Attribute> attributes, ArrayList<Attribute> attributeList)
	{
		Map<Integer, Map<String, Integer>> correct = new HashMap<Integer, Map<String, Integer>>();
		Map<Integer, Map<String, Integer>> total = new HashMap<Integer, Map<String, Integer>>();
		
		for(int i : numCandidateObjects)
		{
			correct.put(i, new HashMap<String, Integer>());
			total.put(i, new HashMap<String, Integer>());
		}
		
		List<ComparisonFunction> comparators = new ArrayList<ComparisonFunction>();
		for(Context c : contexts)
			comparators.add(allComparators.get(c));
		
		for(MatrixCompletionTask task : tasks)
		{
			Map<MatrixEntry, Double> results = solver.solveTask(task, task.getMaxNumChoices(), comparators);
			
			for(int numChoices : numCandidateObjects)
			{
				MatrixEntry max = null;
				for(MatrixEntry obj : task.getChoicesForSize(numChoices))
				{
					if(max == null || results.get(obj).doubleValue() > results.get(max).doubleValue())
						max = obj;
				}
				
				for(String p : this.taskDifficulties.get(task))
				{
					if(total.get(numChoices).get(p) == null)
					{
						total.get(numChoices).put(p, 0);
						correct.get(numChoices).put(p, 0);
					}
					if(task.isCorrect(max))
						correct.get(numChoices).put(p, correct.get(numChoices).get(p) + 1);
					total.get(numChoices).put(p, total.get(numChoices).get(p) + 1);
				}
			}
		}
		
		Instances ret = new Instances("ret", attributeList, 1);
		for(Integer numChoices : total.keySet())
		{
			for(String p : total.get(numChoices).keySet())
			{
				Instance point = new DenseInstance(attributeList.size());
				point.setValue(attributes.get(ExperimentVariable.ROWS_COLS), rowsCols.toString());
				point.setValue(attributes.get(ExperimentVariable.FUNCTION), this.name());
				point.setValue(attributes.get(ExperimentVariable.NUM_CANDIDATES), numChoices);
				point.setValue(attributes.get(ExperimentVariable.NUM_CONTEXTS), contexts.size());
				point.setValue(attributes.get(ExperimentVariable.DIFFICULTY_TYPE), p);
				point.setValue(attributes.get(ExperimentVariable.ACCURACY), 
						(double)1.0*correct.get(numChoices).get(p)/total.get(numChoices).get(p));
				point.setValue(attributes.get(ExperimentVariable.STD_DEV), 0);
				point.setDataset(ret);
				ret.add(point);
			}
		}
		
		return ret;
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
		solver = new ScoredChangeSolver(rowsCols);
	}
	
	private void initializeComparators()
	{
		allComparators = new HashMap<Context, ComparisonFunction>();

		 /*
		//cheating comparators for testing purposes
		comparators.add(new CheatingComparator("weight", ORDERED_PROPERTIES.get("weight")));
		comparators.add(new CheatingComparator("color"));
		comparators.add(new CheatingComparator("contents"));
		
		/*/

		//distance function comparators

		for(Context c : getAllContexts())
		{
//			if(c.modality.equals(Modality.color))
//				comparators.add(new DistanceComparatorLogisticsNormalization(c, DistanceFunction.Euclidean, objects));
////				comparators.add(new CheatingComparator("color"));
//			else
//				comparators.add(new DistanceComparatorLogisticsNormalization(c, DistanceFunction.Euclidean, objects));
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
		return "ClusterDiff";
	}

	@Override
	public Set<Pattern> getValidPatterns() {
		Set<Pattern> ret = new HashSet<Pattern>();
		for(Pattern p : validPatterns.keySet())
		{
			if(validPatterns.get(p).booleanValue())
				ret.add(p);
		}
		return ret;
	}

}
