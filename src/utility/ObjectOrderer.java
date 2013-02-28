package utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ObjectOrderer<T> {
	
	public interface OrderingDistanceFunction<T> {
		public double distance(T obj1, T obj2);
	}
	
	private OrderingDistanceFunction<T> distance;
	
	public ObjectOrderer(OrderingDistanceFunction<T> distance)
	{
		this.distance = distance;
	}
	
	public List<T> findBestOrdering(Set<T> objects)
	{
		List<T> ret = null;
		double min = Double.MAX_VALUE;
		for(List<T> ordering : Utility.createAllPermutations(objects))
		{
			double error = this.computeError(ordering);
			if(error < min)
			{
				min = error;
				ret = ordering;
			}
		}
		
		if(ret == null)
			throw new IllegalArgumentException();
		
		return ret;
	}

	private double computeError(List<T> ordering) {
		double ret = 0;
		
		for(int i = 0; i < ordering.size(); i++)
		{
			for(int j = i + 1; j < ordering.size(); j++)
			{
				double est = this.estimateDistance(ordering, i, j);
				double act = this.distance.distance(ordering.get(i), ordering.get(j));
				ret += Math.pow(est - act, 2);
			}
		}
		
		return ret;
		
	}

	private double estimateDistance(List<T> ordering, int i, int j) {
		double ret = 0;
		
		//make i be smaller than j, this is possible because i and j are interchangable
		if(i > j)
		{
			int temp = i;
			i = j;
			j = temp;
		}
		
		for(int k = i; k < j; k++)
			ret += this.distance.distance(ordering.get(k), ordering.get(k + 1));
		
		return ret;
		
	}

}
