package utility;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GradientAscent<T> {
	
	public interface GradientAscentCallbacks<T> {
		public List<T> successors(T t);
		public double fitness(T t);
	}
	
	private GradientAscentCallbacks<T> callback;
	private Map<T, T> cache;
	
	public GradientAscent(GradientAscentCallbacks<T> callback)
	{
		this.callback = callback;
		this.cache = new ConcurrentHashMap<T, T>();
	}
	
	public T ascend(T start)
	{
		//first check to see if we have this computation cached
		if(this.cache.containsKey(start))
			return this.cache.get(start);
		
		//otherwise we need to compute it
		List<T> path = new LinkedList<T>();
		path.add(start);
		double bestScore = callback.fitness(start);
		T bestT = start;
		
		//now iterate until we stop improving
		while(true)
		{
			double maxScore = -Double.MAX_VALUE;
			T maxT = null;
			for(T potential : callback.successors(bestT))
			{
				double score = callback.fitness(potential);
				if(maxT == null || score > maxScore)
				{
					maxT = potential;
					maxScore = score;
				}
			}
			
			if(maxT == null) //if there were no successors, then we should stop
				break;
			
			//if the best successor is better than its parent, put it in the path and loop again
			if(maxScore > bestScore)
			{
				path.add(maxT);
				bestT = maxT;
				bestScore = maxScore;
			}
			else
				break;
		}
		
		//now make sure to add each of the things along the path to the cache for quick lookup
		for(T t : path)
			cache.put(t, bestT);
		
		return bestT;
	}

}








