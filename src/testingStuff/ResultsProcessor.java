package testingStuff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;

import experiment.Experiment.ROWS_COLS_VALUES;
import featureExtraction.FeatureExtractionManager;

import matrices.MatrixCompletionTask;
import matrices.MatrixEntry;
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
import taskSolver.comparisonFunctions.DistanceComparatorLogisticsNormalization;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.RunningMean;
import utility.Tuple;
import utility.Utility;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ResultsProcessor {
	
	private final static int NUM_FOLDS = 10;
	
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

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
//		aggregateResults("results/results.txt");
//		processResults("results/aggregateResults.txt");
		computeTables("results/aggregateResults.txt");
//		exampleTask();
	}
	
	private static void exampleTask() throws IOException
	{
		Random rand = new Random(1);
		List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
		FeatureExtractionManager feManager = new FeatureExtractionManager(rand);
		feManager.assignFeatures(objects, getAllContexts());
		List<MatrixCompletionTask> tasks = new ArrayList<MatrixCompletionTask>();
		Scanner lines = new Scanner(new File("cachedTasks.txt"));
		while(lines.hasNextLine())
			tasks.add(MatrixCompletionTask.constructTaskFromSerialString(lines.nextLine(), objects, rand));
		
		List<Pattern> rowPatterns = new ArrayList<Pattern>();
		List<Pattern> colPatterns = new ArrayList<Pattern>();
		Map<Pattern, Boolean> validPatterns = new HashMap<Pattern, Boolean>();
		
		intializePatterns(objects, rand, rowPatterns, colPatterns, validPatterns);
		
		MatrixCompletionTask chosen = null;
		for(MatrixCompletionTask task : tasks)
		{
			Set<Pattern> patterns = getApplicablePatterns(task, rowPatterns, colPatterns);
			boolean hasSameColor = false;
			boolean hasDifferentColor = false;
			boolean hasDecrementWeight = false;
			boolean hasSameWeight= false;
			//[Same:weight, Same:color, Different:color, DecrementPattern:weight]
			for(Pattern p : patterns)
			{
				if(p.toString().equalsIgnoreCase("Same:color"))
					hasSameColor = true;
				if(p.toString().equalsIgnoreCase("Different:color"))
					hasDifferentColor = true;
				if(p.toString().equalsIgnoreCase("DecrementPattern:weight"))
					hasDecrementWeight = true;
				if(p.toString().equalsIgnoreCase("Same:weight"))
					hasSameWeight = true;
			}
			if(hasSameColor && hasDifferentColor && hasDecrementWeight && hasSameWeight)
			{
				System.out.println(task.toString());
				System.out.println(task.getChoicesForSize(8));
				System.out.println("patterns=" + patterns.toString());
				System.out.println("Use this task?");
				Scanner in = new Scanner(System.in);
				if(in.next().startsWith("y"))
				{
					chosen = task;
					break;
				}
			}
		}
		
		ScoredChangeSolver solver = new ScoredChangeSolver(ROWS_COLS_VALUES.BOTH);
		Map<Context, ComparisonFunction> allComparators = new HashMap<Context, ComparisonFunction>();
		for(Context c : getAllContexts())
			allComparators.put(c, new DistanceComparatorLogisticsNormalization(c, DistanceFunction.Euclidean, objects, false));
		 
		Map<MatrixEntry, Double> rawSolution = Utility.normalize(solver.solveTask(chosen, 8, 
				 new ArrayList<ComparisonFunction>(allComparators.values())));
		 
		allComparators = new HashMap<Context, ComparisonFunction>();
		for(Context c : getAllContexts())
		{
			Set<Context> temp = new HashSet<Context>();
			temp.add(c);
			allComparators.put(c, new ClusterDiffComparator(objects, temp));
		}
		Map<MatrixEntry, Double> clusterSolution = Utility.normalize(solver.solveTask(chosen, 8, 
				 new ArrayList<ComparisonFunction>(allComparators.values())));
		
		int testFold = tasks.indexOf(chosen) % NUM_FOLDS;
		List<ComparisonFunction> comparators = prune(testFold, new ArrayList<ComparisonFunction>(allComparators.values()),
				8, rand, tasks, solver);
		Map<MatrixEntry, Double> prunedSolution = Utility.normalize(solver.solveTask(chosen, 8, comparators));
		
		System.out.println("Results of the example task:");
		System.out.println(chosen.toString());
		System.out.println(chosen.getChoicesForSize(8));
		System.out.println("raw=" + rawSolution.toString());
		System.out.println("clusterin=" + clusterSolution.toString());
		System.out.println("pruning chose " + comparators.toString());
		System.out.println("pruning=" + prunedSolution.toString());
		for(MatrixEntry obj : chosen.getChoices())
		{
			if(chosen.isCorrect(obj))
				System.out.println("correct answer:" + obj.getName());
		}
	}
	
	private static List<ComparisonFunction> prune(int testFold, List<ComparisonFunction> comparators, int numChoices, Random rand,
			List<MatrixCompletionTask> tasks, TaskSolver solver)
	{	
		Map<List<ComparisonFunction>, Double> bestSetPerformance = new HashMap<List<ComparisonFunction>, Double>();
		List<ComparisonFunction> bestSet = new ArrayList<ComparisonFunction>(comparators);
		Collections.sort(bestSet, new Comparator<ComparisonFunction>() {
			@Override
			public int compare(ComparisonFunction o1, ComparisonFunction o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
		bestSetPerformance.put(bestSet, computeAccuracy(testFold, bestSet, numChoices, tasks, solver));
		
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
				double accuracy = computeAccuracy(testFold, temp, numChoices, tasks, solver);
				
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
		List<ComparisonFunction> ret = new ArrayList<ComparisonFunction>();
		for(ComparisonFunction cf : comparators)
		{
			if(best.getKey().contains(cf))
				ret.add(cf);
		}
		return ret;
	}

	
	private static double computeAccuracy(int testFold, List<ComparisonFunction> temp, int numChoices, 
			List<MatrixCompletionTask> tasks, TaskSolver solver) {
		
		//if it wasn't stored, let's compute it
		
		int correct = 0;
		int total = 0;
		for(int i = 0; i < tasks.size(); i++)
		{
			if(i % NUM_FOLDS == testFold)
				continue;
			
			MatrixCompletionTask task = tasks.get(i);
			
			Map<MatrixEntry, Double> results = solver.solveTask(task, numChoices, temp);
			
			total++;
			MatrixEntry max = null;
			for(MatrixEntry obj : task.getChoicesForSize(numChoices))
			{
				if(max == null || results.get(obj).doubleValue() > results.get(max).doubleValue())
					max = obj;
			}
			
			if(task.isCorrect(max))
				correct++;
		}
		double accuracy = (double)1.0*correct/total;
		
		return accuracy;
	}	
	
	private static Set<Context> getAllContexts() {
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
	
	private static Set<Pattern> getApplicablePatterns(MatrixCompletionTask task, List<Pattern> rowPatterns, 
			List<Pattern> colPatterns)
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
	
	private static void intializePatterns(List<MatrixEntry> objects, Random rand, List<Pattern> rowPatterns,
			List<Pattern> colPatterns, Map<Pattern, Boolean> validPatterns) 
	{		
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
	
	public static void computeTables(String filename) throws FileNotFoundException, IOException
	{
		Instances data = new Instances(new FileReader(filename));
		
		ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
		Map<String, Attribute> attributes = new HashMap<String, Attribute>();
		for(int i = 0; i < data.numAttributes() - 2; i++) //exclude standard dev and accuracy from consideration
		{
			attributes.put(data.attribute(i).name(), data.attribute(i));
			attributeList.add(data.attribute(i));
		}
		
		//Function type v. # of contexts
		Attribute xAxis = attributes.get("NUM_CONTEXTS");
		Attribute zAxis = attributes.get("FUNCTION");
		Map<Attribute, Integer> values = new HashMap<Attribute, Integer>();
		values.put(attributes.get("ROWS_COLS"), 2);
		values.put(attributes.get("NUM_CANDIDATES"), 8);
		values.put(attributes.get("DIFFICULTY_TYPE"), 0);
		writeOutTable(new PrintStream(new File("results/funcVcontexts.txt")), 
				data, xAxis, 1, 21, zAxis, 0, 3, values, false, false);
		
		//# of candidate objects v. # of contexts
		xAxis = attributes.get("NUM_CONTEXTS");
		zAxis = attributes.get("NUM_CANDIDATES");
		values = new HashMap<Attribute, Integer>();
		values.put(attributes.get("ROWS_COLS"), 2);
		values.put(attributes.get("FUNCTION"), 2);
		values.put(attributes.get("DIFFICULTY_TYPE"), 0);
		writeOutTable(new PrintStream(new File("results/candidatesVcontexts.txt")), 
				data, xAxis, 1, 21, zAxis, 2, 10, values, false, true);
		
		//# of patterns v. # of contexts
		xAxis = attributes.get("NUM_CONTEXTS");
		zAxis = attributes.get("DIFFICULTY_TYPE");
		values = new HashMap<Attribute, Integer>();
		values.put(attributes.get("ROWS_COLS"), 2);
		values.put(attributes.get("FUNCTION"), 2);
		values.put(attributes.get("NUM_CANDIDATES"), 8);
//		values.put(attributes.get("DIFFICULTY_TYPE"), 0);
		writeOutTable(new PrintStream(new File("results/numPatternsVcontexts.txt")), 
				data, xAxis, 1, 21, zAxis, 18, 22, values, false, false);
		
		//# of patterns v. # of contexts
		xAxis = attributes.get("NUM_CONTEXTS");
		zAxis = attributes.get("DIFFICULTY_TYPE");
		values = new HashMap<Attribute, Integer>();
		values.put(attributes.get("ROWS_COLS"), 2);
		values.put(attributes.get("FUNCTION"), 0);
		values.put(attributes.get("NUM_CANDIDATES"), 8);
//		values.put(attributes.get("DIFFICULTY_TYPE"), 0);
		writeOutTable(new PrintStream(new File("results/numPatternsVcontextsRAW.txt")), 
				data, xAxis, 1, 21, zAxis, 18, 22, values, false, false);
		
		//rules v. # of contexts
		xAxis = attributes.get("NUM_CONTEXTS");
		zAxis = attributes.get("DIFFICULTY_TYPE");
		values = new HashMap<Attribute, Integer>();
		values.put(attributes.get("ROWS_COLS"), 2);
		values.put(attributes.get("FUNCTION"), 2);
		values.put(attributes.get("NUM_CANDIDATES"), 8);
//		values.put(attributes.get("DIFFICULTY_TYPE"), 0);
		writeOutTable(new PrintStream(new File("results/rulesVcontexts.txt")), 
				data, xAxis, 1, 21, zAxis, 1, 4, values, true, false);
		
		//pattern v. # of contexts
		xAxis = attributes.get("NUM_CONTEXTS");
		zAxis = attributes.get("DIFFICULTY_TYPE");
		values = new HashMap<Attribute, Integer>();
		values.put(attributes.get("ROWS_COLS"), 2);
		values.put(attributes.get("FUNCTION"), 2);
		values.put(attributes.get("NUM_CANDIDATES"), 8);
//		values.put(attributes.get("DIFFICULTY_TYPE"), 0);
		writeOutTable(new PrintStream(new File("results/patternVcontexts.txt")), 
				data, xAxis, 1, 21, zAxis, 11, 17, values, false, false);
		
		//property inclusion v. # of contexts
		xAxis = attributes.get("NUM_CONTEXTS");
		zAxis = attributes.get("DIFFICULTY_TYPE");
		values = new HashMap<Attribute, Integer>();
		values.put(attributes.get("ROWS_COLS"), 2);
		values.put(attributes.get("FUNCTION"), 2);
		values.put(attributes.get("NUM_CANDIDATES"), 8);
//		values.put(attributes.get("DIFFICULTY_TYPE"), 0);
		writeOutTable(new PrintStream(new File("results/propertyInclusionVcontexts.txt")), 
				data, xAxis, 1, 21, zAxis, 5, 7, values, true, false);
		
		//property exclusion v. # of contexts
		xAxis = attributes.get("NUM_CONTEXTS");
		zAxis = attributes.get("DIFFICULTY_TYPE");
		values = new HashMap<Attribute, Integer>();
		values.put(attributes.get("ROWS_COLS"), 2);
		values.put(attributes.get("FUNCTION"), 2);
		values.put(attributes.get("NUM_CANDIDATES"), 8);
//		values.put(attributes.get("DIFFICULTY_TYPE"), 0);
		writeOutTable(new PrintStream(new File("results/propertyExclusionVcontexts.txt")), 
				data, xAxis, 1, 21, zAxis, 8, 10, values, true, false);
		
		//rowsColsBoth v. # of contexts
		xAxis = attributes.get("NUM_CONTEXTS");
		zAxis = attributes.get("ROWS_COLS");
		values = new HashMap<Attribute, Integer>();
//		values.put(attributes.get("ROWS_COLS"), 2);
		values.put(attributes.get("FUNCTION"), 2);
		values.put(attributes.get("NUM_CANDIDATES"), 8);
		values.put(attributes.get("DIFFICULTY_TYPE"), 0);
		writeOutTable(new PrintStream(new File("results/rowsColsBothVcontexts.txt")), 
				data, xAxis, 1, 21, zAxis, 0, 2, values, true, false);
	}
	
	private static void writeOutTable(PrintStream out, Instances data, Attribute xAxis, int startX, int endX, 
			Attribute zAxis, int startZ, int endZ, Map<Attribute, Integer> values, boolean printStds, boolean kappa)
	{
		Attribute accuracy = null;
		Attribute stddev = null;
		Attribute candidates = null;
		for(int i = 0; i < data.numAttributes(); i++)
		{
			Attribute a = data.attribute(i);
			if(a.name().equalsIgnoreCase("ACCURACY"))
				accuracy = a;
			else if(a.name().equalsIgnoreCase("STD_DEV"))
				stddev = a;
			else if(a.name().equalsIgnoreCase("NUM_CANDIDATES"))
				candidates = a;
		}
		
		int xDim = endX - startX + 1;
		int zDim = (endZ - startZ + 1)*(printStds ? 3 : 1);
		
		double[][] table = new double[xDim][zDim];
		for(Instance inst : data)
		{
			//first determine whether or not this instance belongs in the table
			boolean check = true;
			for(Entry<Attribute, Integer> e : values.entrySet())
			{
				if(e.getValue().intValue() != ((int)inst.value(e.getKey())))
					check = false;
			}
			if(((int)inst.value(xAxis)) < startX || ((int)inst.value(xAxis)) > endX)
				check = false;
			if(((int)inst.value(zAxis)) < startZ || ((int)inst.value(zAxis)) > endZ)
				check = false;
			if(!check) //this datapoint is not part of the table
				continue;
			
			//ok, now that we know this datapoint belongs in the table, lets find its coords
			int xPos = ((int)inst.value(xAxis)) - startX;
			int zPos = ((int)inst.value(zAxis)) - startZ;
			if(printStds)
				zPos *= 3;
			
			double acc = inst.value(accuracy);
			double std = inst.value(stddev);
			if(kappa)
			{
				int numCandidates = ((int)inst.value(candidates));
				acc = (acc - 1.0/numCandidates)/(1.0 - 1.0/numCandidates);
				std = std/(1.0 - 1.0/numCandidates);
				table[xPos][zPos] = acc;
				if(printStds)
				{
					table[xPos][zPos+1] = Math.max(acc - std, -1.0);
					table[xPos][zPos+2] = Math.min(acc + std, 1.0);
				}
			}
			else
			{
				acc *= 100;
				std *= 100;
				table[xPos][zPos] = acc;
				if(printStds)
				{
					table[xPos][zPos+1] = Math.max(acc - std, 0.0);
					table[xPos][zPos+2] = Math.min(acc + std, 100.0);
				}
			}
		}
		
		List<String> rowHeaders = new ArrayList<String>();
		for(int i = startX; i <= endX; i++)
		{
			rowHeaders.add(i + "");
		}
		
		List<String> colHeaders = new ArrayList<String>();
		for(int i = startZ; i <= endZ; i++)
		{
			String s;
			if(zAxis.isNumeric())
				s = i + "";
			else
				s = zAxis.value(i);
			colHeaders.add(s);
			if(printStds)
			{
				colHeaders.add(s + "-std");
				colHeaders.add(s + "+std");
			}
		}
		
		PrintStream temp = System.out;
		System.setOut(out);
		Utility.printTable(colHeaders, rowHeaders, table, false);
		System.setOut(temp);
	}
	
	private static void processResults(String filename) throws FileNotFoundException, IOException
	{
		Instances data = new Instances(new FileReader(filename));
		Enumeration iter = data.enumerateAttributes();
		while(iter.hasMoreElements())
			System.out.println(iter.nextElement().toString());
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(int i = 0; i < data.numAttributes() - 2; i++) //exclude standard dev and accuracy from consideration
			attributes.add(data.attribute(i));
		
		Tuple<Attribute, Tuple<Integer, Integer>> xAxis = selectAxisAndRange("x", attributes, data);
		attributes.remove(xAxis.a);
		Tuple<Attribute, Tuple<Integer, Integer>> zAxis = selectAxisAndRange("z", attributes, data);
		attributes.remove(zAxis.a);
		
		Map<Attribute, Integer> values = new HashMap<Attribute, Integer>();
		for(Attribute att : attributes)
		{
			List<String> choices = new ArrayList<String>();
			if(!att.isNumeric())
			{
				for(int i = 0; i < att.numValues(); i++)
					choices.add(att.value(i));
				int choice = choices.indexOf(presentOptions("Please select a value for " + att.name() + ":", choices));
				values.put(att, choice);
			}
			else
			{
				double max = -Double.MAX_VALUE;
				double min = Double.MAX_VALUE;
				for(Instance inst : data)
				{
					double value = inst.value(att);
					if(value > max)
						max = value;
					if(value < min)
						min = value;
				}
				for(int i = (int) min; i <= max; i++)
					choices.add(i + "");
				int choice = (int) (choices.indexOf(presentOptions("Please select a value for " + att.name() + ":", choices)) + min);
				values.put(att, choice);
			}
			
		}
		
		boolean printStds = presentOptions("Print standard deviations?", 
				Utility.convertToList(new String[]{"No","Yes"})).equals("Yes");
		boolean kappa = presentOptions("Print accuracy or kappa?", 
				Utility.convertToList(new String[]{"Accuracy","Kappa"})).equals("Kappa");
		
		writeOutTable(System.out, data, xAxis.a, xAxis.b.a, xAxis.b.b, zAxis.a, zAxis.b.a, zAxis.b.b, values, printStds, kappa);
	}
	
	private static Tuple<Attribute, Tuple<Integer, Integer>> selectAxisAndRange(String axis, 
			List<Attribute> attributes, Instances data)
	{
		Attribute xAxis = presentOptions("Please select an attribute for the " + axis + "-axis:", attributes);
		attributes.remove(xAxis);
		int start, end;
		if(!xAxis.isNumeric())
		{
			List<String> values = new ArrayList<String>();
			for(int i = 0; i < xAxis.numValues(); i++)
				values.add(xAxis.value(i));
			String startValue = presentOptions("Which value for " + xAxis.name() + " would you like to start at?", 
					values.subList(0, values.size() - 1));
			String endValue = presentOptions("Which value would you like to end at?", 
					values.subList(values.indexOf(startValue) + 1, values.size()));
			start = values.indexOf(startValue);
			end = values.indexOf(endValue);
		}
		else
		{
			double max = -Double.MAX_VALUE;
			double min = Double.MAX_VALUE;
			for(Instance inst : data)
			{
				double value = inst.value(xAxis);
				if(value > max)
					max = value;
				if(value < min)
					min = value;
			}
			start = (int) min;
			end = (int) max;
		}
		
		return new Tuple<Attribute, Tuple<Integer,Integer>>(xAxis, new Tuple<Integer, Integer>(start, end));
	}
	
	private static <T> T presentOptions(String prompt, List<T> options)
	{
		System.out.println(prompt);
		
		for(int i = 1; i <= options.size(); i++)
			System.out.println("(" + i + ") " + options.get(i - 1).toString());
		System.out.print(">");
		
		Scanner scan = new Scanner(System.in);
		int choice;
		do
		{
			choice = scan.nextInt();
		} while(choice <= 0 && choice > options.size());
		return options.get(choice - 1);
	}
	
	private static void aggregateResults(String filename) throws FileNotFoundException, IOException
	{
		Instances allData = new Instances(new FileReader(filename));
		Map<HashArray, RunningMean> means = new HashMap<ResultsProcessor.HashArray, RunningMean>();
		int count = 0;
		System.out.println("Beginning aggregation");
		for(Instance inst : allData)
		{
			double[] dd = inst.toDoubleArray();
			HashArray ha = new HashArray(dd, dd.length - 2); //don't use the last two values
			if(means.get(ha) == null)
				means.put(ha, new RunningMean());
			means.get(ha).addValue(dd[dd.length - 2]);
			count++;
			if(count % 100000 == 0)
				System.out.println(count);
		}
		
		System.out.println("Aggregating results");
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(int i = 0; i < allData.numAttributes(); i++)
			attributes.add(allData.attribute(i));
		Instances aggregate = new Instances("aggregateResults", attributes, means.size());
		
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		int maxCount = 0;
		for(Entry<HashArray, RunningMean> e : means.entrySet())
		{
			Instance inst = new DenseInstance(attributes.size());
			inst.setDataset(aggregate);
			for(int i = 0; i < e.getKey().array.length; i++)
			{
				if(attributes.get(i).isNumeric())
					inst.setValue(attributes.get(i), e.getKey().array[i]);
				else
					inst.setValue(attributes.get(i), attributes.get(i).value((int) e.getKey().array[i]));
			}
			inst.setValue(attributes.get(attributes.size() - 2), e.getValue().getMean());
			inst.setValue(attributes.get(attributes.size() - 1), e.getValue().getStandardDeviation());
			aggregate.add(inst);
			if(counts.get(e.getValue().getN()) == null)
				counts.put(e.getValue().getN(), 1);
			else
				counts.put(e.getValue().getN(), counts.get(e.getValue().getN()) + 1);
			if(e.getValue().getN() > maxCount)
				maxCount = e.getValue().getN();
		}
		
		FileWriter fw = new FileWriter("results/aggregateResults.txt");
		fw.write(aggregate.toString());
		fw.flush();
		fw.close();
		System.out.println("Done");
		for(int i = 1; i <= maxCount; i++)
		{
			if(counts.get(i) != null)
				System.out.println(i + " => " + counts.get(i));
		}
	}
	
	private static class HashArray
	{
		public final double[] array;
		
		public HashArray(double[] d, int length)
		{
			double[] temp = new double[length];
			System.arraycopy(d, 0, temp, 0, length);
			array = temp;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if(!(obj instanceof HashArray))
				return false;
			HashArray ha = (HashArray) obj;
			
			if(ha.array.length != this.array.length)
				return false;
			
			for(int i = 0; i < this.array.length; i++)
			{
				if(ha.array[i] != this.array[i])
					return false;
			}
			
			return true;
		}
		
		@Override
		public int hashCode()
		{
			double ret = 1;
			for(double d : array)
				ret += d;
			return (int) ret;
		}
	}

}









