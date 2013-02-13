package taskSolver.comparisonFunctions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import matrices.MatrixEntry;
import utility.AdaBoost;
import utility.Context;
import utility.Utility;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;

public class ClassificationDiffAdaBoost implements ComparisonFunction {

	private Map<Context, ClassificationDiffComparator> classifiers;
	private Map<ClassificationDiffComparator, Double> classifierWeights;
	private int testExecution;
	private String property;
	private Set<Context> contexts;
	private Random rand;
	private List<String> values = null;
	
	public ClassificationDiffAdaBoost(List<Integer> trainExecutions, int testExecution, 
			List<MatrixEntry> objects, String property, Set<Context> contexts, Random rand)
	{
		this.testExecution = testExecution;
		this.property = property;
		this.contexts = contexts;
		this.rand = rand;
		
		setUpClassifiers(trainExecutions, objects);
	}
	
	public ClassificationDiffAdaBoost(List<Integer> trainExecutions, int testExecution, 
			List<MatrixEntry> objects, String property, List<String> values, Set<Context> contexts, Random rand)
	{
		this.testExecution = testExecution;
		this.property = property;
		this.contexts = contexts;
		this.rand = rand;
		this.values = values;
		
		setUpClassifiers(trainExecutions, objects);
	}
	
	private void setUpClassifiers(List<Integer> trainExecutions, List<MatrixEntry> objects)
	{
		Utility.debugPrintln("training classifiers");
		this.classifiers = new HashMap<Context, ClassificationDiffComparator>();
		//iterate over each context and make a classifications diff comparator for each
		for(Context c : contexts)
		{
			Set<Context> singleContext = new HashSet<Context>();
			singleContext.add(c);
			this.classifiers.put(c, new ClassificationDiffComparator(trainExecutions, testExecution, objects, property, values, singleContext));
		}
		
		//in order to run AdaBoost, we have to give it a set of interactions and what the correct predictions for each are
		//one interaction is comparing a pair of trials, from different objects, 
		Set<Interaction> interactions = new HashSet<ClassificationDiffAdaBoost.Interaction>();
		Map<ClassificationDiffComparator, Map<Interaction, Boolean>> predictions = 
				new HashMap<ClassificationDiffComparator, Map<Interaction,Boolean>>();
		//iterate over every pair of trials between pairs of objects
		for(MatrixEntry obj1 : objects)
		{
			for(int exec1 : trainExecutions)
			{
				for(MatrixEntry obj2 : objects)
				{
					if(obj1.equals(obj2)) //no need to compare the same object to itself
						continue;
					
					for(int exec2 : trainExecutions)
					{
						//set up the interaction
						Interaction interaction = new Interaction(obj1, exec1, obj2, exec2);
						interactions.add(interaction); //that was the easy part
						
						//now we need to find out if each classificationDiffComparator predicts this interaction correctly or not
						for(ClassificationDiffComparator cdc : classifiers.values())
						{
							if(predictions.get(cdc) == null)
								predictions.put(cdc, new HashMap<ClassificationDiffAdaBoost.Interaction, Boolean>());
							//get the predicted value
							double diff = cdc.compare(interaction.object1, interaction.execution1, interaction.object2, interaction.execution2);
							if(this.values == null) //if its predicting a categorical property, then it should only predict same or different
							{
								boolean equal = interaction.object1.getPropertyValue(property).equals(interaction.object2.getPropertyValue(property));
								if((diff < 0.5 && equal) || (diff > 0.5 && !equal))
									predictions.get(cdc).put(interaction, true);
								else
									predictions.get(cdc).put(interaction, false);
							}
							else //if its predicting an ordered property, then we need to make sure it got the distance correct
							{
								int index1 = values.indexOf(obj1.getPropertyValue(property));
								int index2 = values.indexOf(obj2.getPropertyValue(property));
								int expected = index2 - index1;
								//computing the predicted difference just undoes the adjustment done by classificationDiffComparator
								int predicted = (int) Math.round((diff*2.0 - 1.0)*(values.size() - 1));
								if(expected == predicted)
									predictions.get(cdc).put(interaction, true);
								else
									predictions.get(cdc).put(interaction, false);
							}
						}
					}
				}
			}
		}
		
		Utility.debugPrintln("boosting weights");
		AdaBoost<ClassificationDiffComparator, Interaction> booster =  
				new AdaBoost<ClassificationDiffComparator, Interaction>(new ArrayList<ClassificationDiffComparator>(classifiers.values()), 
				predictions, interactions, rand);
		classifierWeights = booster.generateWeights();
		Utility.debugPrintln("done");
	}

	@Override
	public double compare(MatrixEntry obj1, MatrixEntry obj2) {
		if(this.values == null)
		{
			double diffVotes = 0.0;
			double sameVotes = 0.0;
			
			for(Entry<ClassificationDiffComparator, Double> e : classifierWeights.entrySet())
			{
				if(e.getKey().compare(obj1, obj2) < 0.5)
					sameVotes += e.getValue();
				else
					diffVotes += e.getValue();
			}
			
			if(sameVotes > diffVotes)
				return 0.0;
			else
				return 1.0;
		}
		else
		{
			//weighted average
			double mean = 0;
			double sum = 0;
			
			for(Entry<ClassificationDiffComparator, Double> e : classifierWeights.entrySet())
			{
				mean += e.getValue()*e.getKey().compare(obj1, obj2);
				sum += e.getValue();
			}
			
			return mean/sum;
		}
	}
	
	private class Interaction {
		public final MatrixEntry object1;
		public final int execution1;
		public final MatrixEntry object2;
		public final int execution2;
		
		public Interaction(MatrixEntry object1, int execution1, MatrixEntry object2, int execution2)
		{
			this.object1 = object1;
			this.execution1 = execution1;
			this.object2 = object2;
			this.execution2 = execution2;
		}
	}
	
//	private class AdaBooster {
//		
//		private static final int NUM_ITERATIONS = 50;
//		
//		private List<ClassificationDiffComparator> classifiers;
//		//classifier -> object -> execution = prediction of that classifier on that object/execution
//		private Map<ClassificationDiffComparator, Map<Interaction, Double>> predictions;
//		private Set<Interaction> interactions;
//		private Random rand;
//		
//		public AdaBooster(List<ClassificationDiffComparator> classifiers, 
//				Map<ClassificationDiffComparator, Map<Interaction, Double>> predictions, 
//				Set<Interaction> interactions, Random rand)
//		{
//			this.classifiers = classifiers;
//			this.predictions = predictions;
//			this.interactions = interactions;
//			this.rand = rand;
//		}
//		
//		public Map<ClassificationDiffComparator, Double> generateWeights()
//		{
//			Map<ClassificationDiffComparator, Double> ret = new HashMap<ClassificationDiffComparator, Double>();
//			for(ClassificationDiffComparator cdc : classifiers)
//				ret.put(cdc, 0.0);
//			
//			Map<Interaction, Double> interactionWeights = new HashMap<Interaction, Double>();
//			for(Interaction interaction : interactions)
//				interactionWeights.put(interaction, 1.0/interactions.size());
//			
//			for(int iteration = 0; iteration < NUM_ITERATIONS; iteration++)
//			{
//				//first we need to calculate the error for each comparison function
//				Map<ClassificationDiffComparator, Double> functionErrors = new HashMap<ClassificationDiffComparator, Double>();
//				for(ClassificationDiffComparator cdc : classifiers)
//					functionErrors.put(cdc, computeError(cdc, interactionWeights));
//				
//				//now find the one that maximizes |0.5 - e| where e is the error
//				Entry<ClassificationDiffComparator, Double> best = Utility.getMax(new ArrayList<Entry<ClassificationDiffComparator, Double>>(functionErrors.entrySet()), 
//						new Comparator<Entry<ClassificationDiffComparator, Double>>() {
//							public int compare(Entry<ClassificationDiffComparator, Double> o1, Entry<ClassificationDiffComparator, Double> o2) {
//								double e1 = -o1.getValue();
//								double e2 = -o2.getValue();
//								if(e1 > e2)
//									return 1;
//								else if(e1 < e2)
//									return -1;
//								else
//									return 0;
//							}
//						}, rand);
//				
//				//now compute the weight for this context
//				double alpha = 0.5*Math.log((1.0 - best.getValue())/best.getValue());
//				
//				//add the weight to the weight for that classifier (this is equivalent to the normal way in which adaboost is done)
//				ret.put(best.getKey(), ret.get(best.getKey()) + alpha);
//				
//				//now update the task weights
//				for(Interaction interaction: interactions)
//				{
//					double old = interactionWeights.get(interaction);
//					double w = 0.0;
//					
//					double diff = best.getKey().compare(interaction.object1, interaction.execution1, interaction.object2, interaction.execution2);
//					boolean equal = interaction.object1.getPropertyValue(property).equals(interaction.object2.getPropertyValue(property));
//					if((diff < 0.5 && equal) || (diff > 0.5 && !equal))
//						w = old*Math.exp(-1.0*alpha);
//					else
//						w = old*Math.exp(alpha);
//					interactionWeights.put(interaction, w);
//				}
//				
//				//normalize the task weights
//				double sum = 0;
//				for(Double d : interactionWeights.values())
//					sum += d;
//				
//				for(Interaction interaction : interactions)
//					interactionWeights.put(interaction, interactionWeights.get(interaction)/sum);
//				
//			}
//			
//			return ret;
//		}
//
//		private Double computeError(ClassificationDiffComparator cdc, Map<Interaction, Double> interactionWeights) {
//			double error = 0;
//			for(Interaction interaction : interactions)
//			{
//				double diff = predictions.get(cdc).get(interaction);
//				boolean equal = interaction.object1.getPropertyValue(property).equals(interaction.object2.getPropertyValue(property));
//				if((diff < 0.5 && !equal) || (diff > 0.5 && equal))
//					error += interactionWeights.get(interaction);
//			}
//			
//			//the error is supposed to be between 0 and 1, but if it is either of those, we will get errors,
//			//so move it a very small amount away
//			if(error == 1.0)
//				error = 0.9999999;
//			else if(error == 0.0)
//				error = 0.0000001;
//			
//			return error;
//		}
//
//	}


}
