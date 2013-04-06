package taskSolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import matrices.MatrixCompletionTask;
import matrices.MatrixEntry;

import org.apache.commons.math3.distribution.NormalDistribution;

import taskSolver.comparisonFunctions.ComparisonFunction;
import utility.RunningMean;

public class WeightedGaussianSolver implements TaskSolver {
	
	private Map<ComparisonFunction, Double> weights;
	private double weightSum;
	
	public WeightedGaussianSolver(Map<ComparisonFunction, Double> weights)
	{
		this.weights = weights;
		
		weightSum = 0;
		for(Double d : weights.values())
			weightSum += d;
	}

	@Override
	public Map<MatrixEntry, Double> solveTask(MatrixCompletionTask task,
			List<ComparisonFunction> comparators) {
		
		Map<ComparisonFunction, NormalDistribution> rowDistros = new HashMap<ComparisonFunction, NormalDistribution>();
		Map<ComparisonFunction, NormalDistribution> colDistros = new HashMap<ComparisonFunction, NormalDistribution>();
		
		//first build the distributions
		for(ComparisonFunction comp : comparators)
		{
			//build a distribution for the rows
			RunningMean rm = new RunningMean();
			for(int i = 0; i < task.getNumRows(); i++)
			{
				List<MatrixEntry> row = task.getRow(i);
				for(int j = 0; j < row.size(); j++)
				{
					for(int k = j + 1; k < row.size(); k++)
					{
						rm.addValue(comp.compare(row.get(j), row.get(k)));
					}
				}
			}
			double mean = rm.getMean();
			double var = rm.getVariance();
			if(var == 0) //this will screw things up, so lets make it not exactly 0, but very small
				var = 0.00000000000001;
			
			NormalDistribution dist = new NormalDistribution(mean, Math.sqrt(var));
			rowDistros.put(comp, dist);
			
			//now do the same for the columns
			rm = new RunningMean();
			for(int i = 0; i < task.getNumCols(); i++)
			{
				List<MatrixEntry> col = task.getCol(i);
				for(int j = 0; j < col.size(); j++)
				{
					for(int k = j + 1; k < col.size(); k++)
					{
						rm.addValue(comp.compare(col.get(j), col.get(k)));
					}
				}
			}
			mean = rm.getMean();
			var = rm.getVariance();
			if(var == 0) //this will screw things up, so lets make it not exactly 0, but very small
				var = 0.00000000000001;
			
			dist = new NormalDistribution(mean, Math.sqrt(var));
			colDistros.put(comp, dist);
		}
		
		//now that the distributions are built, lets evaluate each object
		Map<MatrixEntry, Double> ret = new HashMap<MatrixEntry, Double>();
		for(MatrixEntry object : task.getChoices())
		{
			RunningMean avg = new RunningMean();
			for(Entry<ComparisonFunction, NormalDistribution> e : rowDistros.entrySet())
			{
				for(MatrixEntry obj2 : task.getRow(task.getNumRows() - 1))
				{
					//prob *= e.getValue().density(e.getKey().compare(object, obj2));
					double dis = e.getKey().compare(object, obj2);
					double prob;
					if(dis > e.getValue().getMean())
						prob = 2*(1 - e.getValue().cumulativeProbability(dis));
					else
						prob = 2*e.getValue().cumulativeProbability(dis);
					avg.addValue(prob*this.weights.get(e.getKey()));
				}
				
			}
			
			for(Entry<ComparisonFunction, NormalDistribution> e : colDistros.entrySet())
			{
				for(MatrixEntry obj2 : task.getCol(task.getNumCols() - 1))
				{
					//prob *= e.getValue().density(e.getKey().compare(object, obj2));
					double dis = e.getKey().compare(object, obj2);
					double prob;
					if(dis > e.getValue().getMean())
						prob = 2*(1 - e.getValue().cumulativeProbability(dis));
					else
						prob = 2*e.getValue().cumulativeProbability(dis);
					avg.addValue(prob*this.weights.get(e.getKey()));
				}
			}
			double adjustedWeightSum = weightSum*(task.getRow(task.getNumRows() - 1).size() + task.getCol(task.getNumCols() - 1).size());
			ret.put(object, avg.getMean()/adjustedWeightSum);
		}
		
		
		return ret;
	}

}
