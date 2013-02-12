package testingStuff;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import featureExtraction.FeatureExtractionManager;

import matrices.MatrixEntry;
import matrices.MatrixGenerator;
import matrices.patterns.DifferentPattern;
import matrices.patterns.OneSameOneDifferentPattern;
import matrices.patterns.Pattern;
import matrices.patterns.SamePattern;

import taskSolver.PatternTrainerSolver;
import taskSolver.comparisonFunctions.ClassificationDiffComparator;
import taskSolver.comparisonFunctions.ComparisonFunction;
import taskSolver.patternClassifiers.AdaBoostContextClassifiers;
import taskSolver.patternClassifiers.AllFeaturesSingleClassifier;
import taskSolver.patternClassifiers.PatternClassifier;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.RunningMean;
import utility.Utility;

public class PatternClassificationTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		testPatternClassification();
	}
	
	private static void testPatternClassification() throws IOException
	{
		Random rand = new Random(1);
		FeatureExtractionManager manager = new FeatureExtractionManager(rand);
		
//		String property = "color";
//		String property = "weight";
		String property = "contents";
		
		Set<Context> contexts = getContexts();
		
		List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
		manager.assignFeatures(objects, contexts);
		Pattern pattern = getPattern(property, objects, rand);
		
		Set<List<MatrixEntry>> positiveExamples = new HashSet<List<MatrixEntry>>();
		Set<List<MatrixEntry>> negativeExamples = new HashSet<List<MatrixEntry>>();
		generateTrainingExamples(pattern, new HashSet<MatrixEntry>(objects), rand, positiveExamples, negativeExamples);
		
		RunningMean pos = new RunningMean();
		RunningMean neg = new RunningMean();
		for(int testTrial = 0; testTrial < FeatureExtractionManager.NUM_EXECUTIONS; testTrial++)
		{
			Utility.debugPrintln("Beggining fold " + testTrial);
			List<Integer> trainTrials = new ArrayList<Integer>();
			for(int i = 0; i < FeatureExtractionManager.NUM_EXECUTIONS; i++)
			{
				if(i != testTrial)
					trainTrials.add(i);
			}
			
			Utility.debugPrintln("Training classifier");
//			PatternClassifier classifier = new AllFeaturesSingleClassifier(trainTrials, testTrial, getContexts(), rand);
			PatternClassifier classifier = new AdaBoostContextClassifiers(trainTrials, testTrial, getContexts(), rand);
			classifier.trainClassifier(positiveExamples, negativeExamples);
			
			Utility.debugPrintln("Evaluating classifier");
			for(List<MatrixEntry> list : positiveExamples)
				pos.addValue(classifier.classifyPattern(list));
			
			for(List<MatrixEntry> list : negativeExamples)
				neg.addValue(classifier.classifyPattern(list));
			
			Utility.debugPrintln("done");
		}
		
		System.out.println("Positive examples: mean=" + pos.getMean() + ", stddev=" + pos.getStandardDeviation() + ", n=" + pos.getN());
		System.out.println("Negative examples: mean=" + neg.getMean() + ", stddev=" + neg.getStandardDeviation() + ", n=" + neg.getN());
	}
	
	private static Set<Context> getContexts() {
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
	
	private static void generateTrainingExamples(Pattern p, Set<MatrixEntry> objects, Random rand,
			Set<List<MatrixEntry>> positiveExamples, Set<List<MatrixEntry>> negativeExamples)
	{
		MatrixEntry[] objectArray = objects.toArray(new MatrixEntry[objects.size()]);
		
		//add in the negative examples
		negativeExamples.clear();
		while(negativeExamples.size() < PatternTrainerSolver.NUM_TRAINING_EXAMPLES/2)
		{
			List<MatrixEntry> example = new ArrayList<MatrixEntry>();
			while(example.size() < MatrixGenerator.NUM_ROWS)
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
		while(positiveExamples.size() < PatternTrainerSolver.NUM_TRAINING_EXAMPLES/2)
		{
			List<MatrixEntry> example = new ArrayList<MatrixEntry>();
			while(example.size() < MatrixGenerator.NUM_ROWS)
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
	
	private static Pattern getPattern(String property, List<MatrixEntry> objects, Random rand) {
		
		SamePattern ret = new SamePattern(property);
//		DifferentPattern ret = new DifferentPattern(property, new HashSet<MatrixEntry>(objects), rand);
//		OneSameOneDifferentPattern ret = new OneSameOneDifferentPattern(sproperty);
		
		return ret;
		
	}

}
