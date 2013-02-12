package utility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This class takes in in its constructor a list of classifiers to generate weights for,
 * a mapping of (classifiers x datapoints) -> whether or not that classifier correctly classified
 * that datapoint, a set of datapoints, and a random object. Calling the generateWeights
 * method will compute and return a map of classifiers to weights for each classifier.
 * 
 * @author cschenck
 *
 * @param <C> The class of the classifier
 * @param <T> The class of the datapoint
 */
public class AdaBoost<C, T> {
	
	private static final int NUM_ITERATIONS = 50;
	
	private List<C> classifiers;
	//classifier -> object -> execution = prediction of that classifier on that object/execution
	private Map<C, Map<T, Boolean>> predictions;
	private Set<T> datapoints;
	private Random rand;
	
	public AdaBoost(List<C> classifiers, Map<C, Map<T, Boolean>> predictions, Set<T> datapoints, Random rand)
	{
		this.classifiers = classifiers;
		this.predictions = predictions;
		this.datapoints = datapoints;
		this.rand = rand;
	}
	
	public Map<C, Double> generateWeights()
	{
		Map<C, Double> ret = new HashMap<C, Double>();
		for(C cdc : classifiers)
			ret.put(cdc, 0.0);
		
		Map<T, Double> dpWeights = new HashMap<T, Double>();
		for(T dp : datapoints)
			dpWeights.put(dp, 1.0/datapoints.size());
		
		for(int iteration = 0; iteration < NUM_ITERATIONS; iteration++)
		{
			//first we need to calculate the error for each comparison function
			Map<C, Double> functionErrors = new HashMap<C, Double>();
			for(C cdc : classifiers)
				functionErrors.put(cdc, computeError(cdc, dpWeights));
			
			//now find the one that maximizes |0.5 - e| where e is the error
			Entry<C, Double> best = Utility.getMax(new ArrayList<Entry<C, Double>>(functionErrors.entrySet()), 
					new Comparator<Entry<C, Double>>() {
						public int compare(Entry<C, Double> o1, Entry<C, Double> o2) {
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
			for(T dp: datapoints)
			{
				double old = dpWeights.get(dp);
				double w = 0.0;
				
				if(predictions.get(best.getKey()).get(dp))
					w = old*Math.exp(-1.0*alpha);
				else
					w = old*Math.exp(alpha);
				dpWeights.put(dp, w);
			}
			
			//normalize the task weights
			double sum = 0;
			for(Double d : dpWeights.values())
				sum += d;
			
			for(T dp : datapoints)
				dpWeights.put(dp, dpWeights.get(dp)/sum);
			
		}
		
		return ret;
	}

	private Double computeError(C cdc, Map<T, Double> dpWeights) {
		double error = 0;
		for(T dp : datapoints)
		{
			if(!predictions.get(cdc).get(dp))
				error += dpWeights.get(dp);
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
