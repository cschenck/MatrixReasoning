package taskSolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matrices.MatrixCompletionTask;
import matrices.MatrixEntry;
import taskSolver.comparisonFunctions.ComparisonFunction;
import utility.Utility;

public class CachedScoredChangeSolver implements TaskSolver {
	
	private ScoredChangeSolver solver;
	public Map<ComparisonFunction, Map<MatrixCompletionTask, Map<MatrixEntry, Double>>> cache;
	
	public CachedScoredChangeSolver(List<MatrixCompletionTask> tasks, 
			Set<ComparisonFunction> functions)
	{
		solver = new ScoredChangeSolver();
		
		Utility.debugPrintln("Computing cache for contexts");
		this.cache = new HashMap<ComparisonFunction, Map<MatrixCompletionTask,Map<MatrixEntry,Double>>>();
		for(ComparisonFunction cf : functions)
		{
			this.cache.put(cf, new HashMap<MatrixCompletionTask, Map<MatrixEntry,Double>>());
			for(MatrixCompletionTask task : tasks)
			{
				List<ComparisonFunction> temp = new ArrayList<ComparisonFunction>();
				temp.add(cf);
				Map<MatrixEntry, Double> temp2 = solver.solveTask(task, temp);
				this.cache.get(cf).put(task, solver.solveTask(task, temp));
			}
		}
		Utility.debugPrintln("done computing cache");
	}

	@Override
	public Map<MatrixEntry, Double> solveTask(MatrixCompletionTask task,
			List<ComparisonFunction> comparators) {
		
		Map<MatrixEntry, Double> ret = new HashMap<MatrixEntry, Double>();
		for(ComparisonFunction cf : comparators)
		{
			for(MatrixEntry obj : this.cache.get(cf).get(task).keySet())
			{
				if(ret.get(obj) == null)
					ret.put(obj, this.cache.get(cf).get(task).get(obj));
				else
					ret.put(obj, ret.get(obj).doubleValue() + this.cache.get(cf).get(task).get(obj).doubleValue());
			}
		}
		
		return ret;
	}

}
