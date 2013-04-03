package taskSolver.comparisonFunctions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import featureExtraction.FeatureExtractionManager;

import utility.Context;
import utility.RunningMean;
import utility.UnorderedTuple;
import matrices.MatrixEntry;

public class DistanceComparatorLogisticsNormalization extends DistanceComparator {
	
	private static final double UPPER_VALUE = 0.9;
	private static final double LOWER_VALUE = 0.1;
	
	private double a;
	private double b;
	private Context context;
	
	private Map<UnorderedTuple<MatrixEntry>, Double> cachedDistances = null;
	
	public DistanceComparatorLogisticsNormalization(Context context, DistanceFunction func, 
			List<MatrixEntry> objects, boolean cacheComparisons)
	{
		super(context, func, objects);
		this.context = context;
		
		//compute the values at the 1st and 3rd quartiles
		List<Double> list = new ArrayList<Double>();
		for(MatrixEntry objectA : objects)
		{
			for(double[] a : objectA.getFeatures(context))
			{
				for(MatrixEntry objectB : objects)
				{
					if(objectA.equals(objectB))
						continue;
					
					for(double[] b : objectB.getFeatures(context))
					{
						double dis = this.computeDistance(a, b, func);
						list.add(dis);
					}
				}
			}
		}
		
		Collections.sort(list);
		
		double firstQuartile = list.get(list.size()/4);
		double thirdQuartile = list.get(list.size()*3/4);
		
		b = (firstQuartile/thirdQuartile*Math.log(1.0/UPPER_VALUE - 1) - Math.log(1.0/LOWER_VALUE - 1))
				/(1 - firstQuartile/thirdQuartile);
		a = -1.0/thirdQuartile*(Math.log(1.0/UPPER_VALUE - 1) + b);
		
		if(cacheComparisons) //lets cache those comparisons for faster lookup
		{
			System.out.println(this.toString() + ":caching comparisons");
			this.cachedDistances = new HashMap<UnorderedTuple<MatrixEntry>, Double>();
			for(MatrixEntry obj1 : objects)
			{
				for(MatrixEntry obj2 : objects)
				{
					UnorderedTuple<MatrixEntry> pair = new UnorderedTuple<MatrixEntry>(obj1, obj2);
					this.cachedDistances.put(pair, this.compare(obj1, obj2));
				}
			}
		}
	}

	@Override
	public double compare(MatrixEntry obj1, MatrixEntry obj2) {
		//first check to see if we cached comparisons
		if(this.cachedDistances != null)
		{
			UnorderedTuple<MatrixEntry> pair = new UnorderedTuple<MatrixEntry>(obj1, obj2);
			if(this.cachedDistances.get(pair) != null)
				return this.cachedDistances.get(pair).doubleValue();
		}
		
		//return the average
		RunningMean mean = new RunningMean();
		for(double[] a : obj1.getFeatures(context))
		{
			for(double[] b : obj2.getFeatures(context))
				mean.addValue(this.computeDistance(a, b, this.getFunction()));
		}
		
//		for(int i = 0; i < FeatureExtractionManager.NUM_EXECUTIONS; i++)
//		{
//			double[] a = obj1.getFeatures(context).get(i);
//			double[] b = obj2.getFeatures(context).get(i);
//			mean.addValue(this.computeDistance(a, b, this.getFunction()));
//		}
		
		return this.normalize(mean.getMean());
	}
	
	public double compare(MatrixEntry obj1, int exec1, MatrixEntry obj2, int exec2)
	{
		return this.normalize(this.computeDistance(obj1.getFeatures(context).get(exec1), 
				obj2.getFeatures(context).get(exec2), this.getFunction()));
	}
	
	@Override
	public String toString()
	{
		return "LogisticsNormalized" + super.toString();
	}
	
	private double normalize(double v)
	{
		return 1.0/(1 + Math.exp(-1.0*(a*v + b)));
	}

}
