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

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;

import taskSolver.comparisonFunctions.ComparisonFunction;
import taskSolver.comparisonFunctions.DistanceComparator;
import taskSolver.comparisonFunctions.WeightedGaussianSolver;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import taskSolver.GaussianSolver;
import taskSolver.TaskSolver;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.RunningMean;
import utility.Utility;
import featureExtraction.FeatureExtractionManager;




public class MatrixCompletionExperimentGeneticAlgorithm {
	
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
	
	public MatrixCompletionExperimentGeneticAlgorithm(String objectFilepath)
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
		WeightEvolver evolver = new WeightEvolver(comparators, new HashSet<MatrixCompletionTask>(trainTasks), rand);
		try {
			this.functionWeights = evolver.evolveWeights();
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}
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
	
	public class WeightEvolver extends FitnessFunction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5857987569321477526L;
		private static final int NUM_EVOLUTIONS = 50;
		private static final int CUT_OFF_AFTER_WITH_NO_CHANGES = 50;
		private static final int CANDIDATE_POOL_SIZE = 10;
		
		private List<ComparisonFunction> functions;
		private Set<MatrixCompletionTask> tasks;
		private Random rand;
		
		public WeightEvolver(List<ComparisonFunction> functions, Set<MatrixCompletionTask> tasks, Random rand)
		{
			this.functions = functions;
			this.tasks = tasks;
			this.rand = rand;
		}
		
		public Map<ComparisonFunction, Double> evolveWeights() throws InvalidConfigurationException
		{
			//SPECIAL CASE: if there is only one function, then we don't need to evolve anything
			//this is done for efficiency purposes
			if(functions.size() <= 1)
			{
				Map<ComparisonFunction, Double> ret = new HashMap<ComparisonFunction, Double>();
				ret.put(functions.get(0), 1.0);
				return ret;
			}
			
			//Configuration.reset();
			Configuration conf = new DefaultConfiguration(functions.toString() + tasks.toString(), "no name");
			conf.setFitnessFunction(this);
			
			conf.setRandomGenerator(new org.jgap.RandomGenerator() {
				private static final long serialVersionUID = -5739626465827283110L;
				public long nextLong() {return rand.nextLong();}
				public int nextInt(int arg0) {return rand.nextInt(arg0);}
				public int nextInt() {return rand.nextInt();}
				public float nextFloat() {return rand.nextFloat();}
				public double nextDouble() {return rand.nextDouble();}
				public boolean nextBoolean() {return rand.nextBoolean();}
			});
			
			Gene[] genes = new Gene[functions.size()];
			for(int i = 0; i < functions.size(); i++)
				genes[i] = new DoubleGene(conf, 0.0, 1.0);
			
			Chromosome sampleChromosone = new Chromosome(conf, genes);
			conf.setSampleChromosome(sampleChromosone);
			
			conf.setPopulationSize(CANDIDATE_POOL_SIZE);
			Genotype population = Genotype.randomInitialGenotype(conf);
			
			int lastChange = 0;
			double lastScore = -1;
			for(int i = 0; i < NUM_EVOLUTIONS; i++)
			{
				Utility.debugPrintln("processing evolution " + i);
				population.evolve();
				IChromosome s = population.getFittestChromosome();
				double score = this.evaluate(s);
				
				//TODO debug
				Utility.debugPrintln("best = " + score);
				
				//if we've found one that does perfect, stop evolving
				if(score == 1.0)
					break;
				
				//check to see if the score has changed, discarding changes downward, which I'm pretty sure can't happen
				if(score > lastScore)
				{
					lastScore = score;
					lastChange = i;
				}
				else if(i - lastChange > CUT_OFF_AFTER_WITH_NO_CHANGES) //if there haven't been any changes in score for awhile
					break;
			}
			
			IChromosome solution = population.getFittestChromosome();
			Map<ComparisonFunction, Double> weights = new HashMap<ComparisonFunction, Double>();
			for(int i = 0; i < functions.size(); i++)
				weights.put(functions.get(i), (Double)solution.getGene(i).getAllele());
			
			return weights;
		}

		@Override
		protected double evaluate(IChromosome solution) {
			
			int numCorrect = 0;
			int numTotal = 0;
			
			Map<ComparisonFunction, Double> weights = new HashMap<ComparisonFunction, Double>();
			for(ComparisonFunction cf : functions)
				weights.put(cf, (Double)solution.getGene(functions.indexOf(cf)).getAllele());
			TaskSolver solver = new WeightedGaussianSolver(weights);
			
			for(MatrixCompletionTask task : tasks)
			{
				MatrixEntry prediction = Utility.getMax(
						new ArrayList<Entry<MatrixEntry, Double>>(solver.solveTask(task, functions).entrySet()), 
						new Comparator<Entry<MatrixEntry, Double>>() {
							@Override
							public int compare(Entry<MatrixEntry, Double> o1,
									Entry<MatrixEntry, Double> o2) {
								if(o1.getValue() > o2.getValue())
									return 1;
								else if(o1.getValue() < o2.getValue())
									return -1;
								else
									return 0;
							}
						}, rand).getKey();
				
				if(task.isCorrect(prediction))
					numCorrect++;
				
				numTotal++;
			}
			
			RunningMean avgWeight = new RunningMean();
			for(ComparisonFunction cf : functions)
				avgWeight.addValue((Double)solution.getGene(functions.indexOf(cf)).getAllele());
			
//			double ret = (double)numCorrect/numTotal - Math.pow(avgWeight.getMean(), 2); 
			double ret = (double)numCorrect/numTotal;
			
			return (ret > 0 ? ret : 0.0);
			
		}

	}

}
