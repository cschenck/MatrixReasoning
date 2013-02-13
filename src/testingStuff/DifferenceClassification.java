package testingStuff;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import matrices.MatrixEntry;
import taskSolver.comparisonFunctions.ClassificationDiffAdaBoost;
import taskSolver.comparisonFunctions.ClassificationDiffComparator;
import taskSolver.comparisonFunctions.ComparisonFunction;
import taskSolver.comparisonFunctions.DistanceComparator;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.Utility;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import featureExtraction.FeatureExtractionManager;

public class DifferenceClassification {

	public static void main(String[] args) throws Exception {
//		testDistanceBased();
		testClassificationDiff();
	}
	
	private static void testClassificationDiff() throws IOException
	{
		Random rand = new Random(1);
		FeatureExtractionManager manager = new FeatureExtractionManager(rand);
		
		String property = "color";
//		String property = "weight";
//		String property = "contents";
		
		Set<Context> contexts = getContexts();
		
		List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
		manager.assignFeatures(objects, contexts);
		
		int correct = 0;
		int total = 0;
		for(int testTrial = 0; testTrial < FeatureExtractionManager.NUM_EXECUTIONS; testTrial++)
		{
			Utility.debugPrintln("Beggining fold " + testTrial);
			List<Integer> trainTrials = new ArrayList<Integer>();
			for(int i = 0; i < FeatureExtractionManager.NUM_EXECUTIONS; i++)
			{
				if(i != testTrial)
					trainTrials.add(i);
			}
			ComparisonFunction comparator = new ClassificationDiffComparator(trainTrials, testTrial, objects, property, contexts, false, false);
//			ComparisonFunction comparator = new ClassificationDiffAdaBoost(trainTrials, testTrial, objects, property, contexts, rand);
			
			for(MatrixEntry obj1 : objects)
			{
				for(MatrixEntry obj2 : objects)
				{
					if(obj1.equals(obj2))
						continue;
					
					double diff = comparator.compare(obj1, obj2);
					if((diff < 0.5 && obj1.getPropertyValue(property).equals(obj2.getPropertyValue(property)))
							|| (diff > 0.5 && !obj1.getPropertyValue(property).equals(obj2.getPropertyValue(property))))
							correct++;
					total++;
				}
			}
			
			Utility.debugPrintln("done");
		}
		
		System.out.println("Correct   = " + correct);
		System.out.println("Incorrect = " + (total - correct));
		System.out.println("Total     = " + total);
	}

	private static void testDistanceBased() throws FileNotFoundException,
			IOException, Exception {
		Random rand = new Random(1);
		FeatureExtractionManager manager = new FeatureExtractionManager(rand);
		
		String property = "color";
//		String property = "weight";
//		String property = "contents";
		
		Set<Context> contexts = getContexts();
		
		List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
		manager.assignFeatures(objects, contexts);
		
		List<DistanceComparator> comparators = new ArrayList<DistanceComparator>();
		
		//distance function comparators
		for(Context c : contexts)
		{
			if(c.modality.equals(Modality.color))
				comparators.add(new DistanceComparator(c, DistanceFunction.Euclidean, objects));
			else
				comparators.add(new DistanceComparator(c, DistanceFunction.Euclidean, objects));
		}
		
		Instances data = buildInstances(objects, comparators, property);
		Evaluation eval = new Evaluation(data);
		Classifier classifier = new SMO();
//		Classifier classifier = new RandomForest();
		eval.crossValidateModel(classifier, data, 4, rand);
		System.out.println(eval.toSummaryString());
	}
	
	private static Instances buildInstances(List<MatrixEntry> objects, List<DistanceComparator> comparators, String property)
	{
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(int i = 1; i <= comparators.size(); i++)
		{
			Attribute attribute = new Attribute("" + i);
			attributes.add(attribute);
		}
		
		List<String> values = new ArrayList<String>();
		values.add("different");
		values.add("same");
		Attribute classAttribute = new Attribute("class", values);
		attributes.add(classAttribute);
		
		int capacity = (int) Math.pow(objects.size()*4, 2);
		
		Instances ret = new Instances("data", attributes, capacity);
		ret.setClassIndex(attributes.size() - 1);
		
		for(MatrixEntry object1 : objects)
		{
			for(int exec1 = 0; exec1 < 4; exec1++)
			{
				for(MatrixEntry object2 : objects)
				{
					if(object1.equals(object2))
						continue;
					for(int exec2 = 0; exec2 < 4; exec2++)
					{
						Instance dataPoint = new DenseInstance(attributes.size());
						for(int i = 0; i < comparators.size(); i++)
							dataPoint.setValue(attributes.get(i), comparators.get(i).compare(object1, exec1, object2, exec2));
						
						String s = "different";
						if(object1.getPropertyValue(property).equals(object2.getPropertyValue(property)))
							s = "same";
						dataPoint.setValue(attributes.get(attributes.size() - 1), s);
						dataPoint.setDataset(ret);
						ret.add(dataPoint);
					}
				}
			}
		}
		
		return ret;
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

}
