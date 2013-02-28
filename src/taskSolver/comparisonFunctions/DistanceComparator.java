package taskSolver.comparisonFunctions;

import java.util.List;

import utility.Context;
import utility.ObjectOrderer.OrderingDistanceFunction;
import utility.RunningMean;
import matrices.MatrixEntry;

public class DistanceComparator implements ComparisonFunction, OrderingDistanceFunction<MatrixEntry> {
	
	public enum DistanceFunction {
		Euclidean, Manhatten
	}
	
	private DistanceFunction func;
	private double min;
	private double max;
	private Context context;
	
	public DistanceComparator(Context context, DistanceFunction func, List<MatrixEntry> objects)
	{
		this.context = context;
		this.func = func;
		
		min = Double.MAX_VALUE;
		max = -Double.MAX_VALUE;
		
		//compute the max and the min so we can normalize the output
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
						if(dis > max)
							max = dis;
						if(dis < min)
							min = dis;
					}
				}
			}
		}
	}

	@Override
	public double compare(MatrixEntry obj1, MatrixEntry obj2) {
		//return the average
		RunningMean mean = new RunningMean();
		for(double[] a : obj1.getFeatures(context))
		{
			for(double[] b : obj2.getFeatures(context))
				mean.addValue(this.computeDistance(a, b, func));
		}
		
		return (mean.getMean() - min)/(max - min);
	}
	
	public double compare(MatrixEntry obj1, int exec1, MatrixEntry obj2, int exec2)
	{
		return (this.computeDistance(obj1.getFeatures(context).get(exec1), obj2.getFeatures(context).get(exec2), this.func)
				- min)/(max - min);
	}
	
	@Override
	public String toString()
	{
		return "ComparisonFunction:" + context.toString();
	}
	
	protected DistanceFunction getFunction()
	{
		return this.func;
	}
	
	protected double computeDistance(double[] a, double[] b, DistanceFunction func)
	{
		if(func.equals(DistanceFunction.Euclidean))
		{
			double ret = 0;
			for(int i = 0; i < a.length; i++)
				ret += Math.pow(a[i] - b[i], 2);
			return Math.sqrt(ret);
		}
		else if(func.equals(DistanceFunction.Manhatten))
		{
			double ret = 0;
			for(int i = 0; i < a.length; i++)
				ret += Math.abs(a[i] - b[i]);
			return ret;
		}
		else
			throw new IllegalStateException("Forgot to implement the new distance function");
	}

	@Override
	public double distance(MatrixEntry obj1, MatrixEntry obj2) {
		return this.compare(obj1, obj2);
	}

}
