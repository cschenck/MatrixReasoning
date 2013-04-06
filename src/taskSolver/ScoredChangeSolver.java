package taskSolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matrices.MatrixCompletionTask;
import matrices.MatrixEntry;
import taskSolver.comparisonFunctions.ComparisonFunction;
import utility.RunningMean;
import utility.Utility;

public class ScoredChangeSolver implements TaskSolver {
	
	private static final boolean USE_COLUMNS = false;
	private static final boolean USE_ROWS = true;

	@Override
	public Map<MatrixEntry, Double> solveTask(MatrixCompletionTask task, int numChoices, List<ComparisonFunction> comparators) {
		//first compute the score for each comparator for rows and columns
		List<List<MatrixEntry>> lists = new ArrayList<List<MatrixEntry>>();
		for(int i = 0; i < task.getNumRows() - 1; i++)
			lists.add(task.getRow(i));
		Map<ComparisonFunction, Double> rowScores = new HashMap<ComparisonFunction, Double>();
		if(USE_ROWS)
				rowScores = computeScores(lists, comparators);
		else
		{
			for(ComparisonFunction cf : comparators)
				rowScores.put(cf, 0.0);
		}
		
		lists.clear();
		for(int i = 0; i < task.getNumCols() - 1; i++)
			lists.add(task.getCol(i));
		Map<ComparisonFunction, Double> colScores = new HashMap<ComparisonFunction, Double>();
		if(USE_COLUMNS)
			colScores = computeScores(lists, comparators);
		else
		{
			for(ComparisonFunction cf : comparators)
				colScores.put(cf, 0.0);
		}
		
		Map<ComparisonFunction, List<Double>> rowExpectedValues = new HashMap<ComparisonFunction, List<Double>>();
		for(ComparisonFunction cf : comparators)
		{
			List<Double> values = new ArrayList<Double>();
			for(int col = 0; col < task.getNumCols() - 1; col++)
			{
				RunningMean mean = new RunningMean();
				for(int row = 0; row < task.getNumRows() - 1; row++)
					mean.addValue(cf.compare(task.getRow(row).get(col), task.getRow(row).get(task.getNumCols() - 1)));
				values.add(mean.getMean());
			}
			rowExpectedValues.put(cf, values);
		}
		
		Map<ComparisonFunction, List<Double>> colExpectedValues = new HashMap<ComparisonFunction, List<Double>>();
		for(ComparisonFunction cf : comparators)
		{
			List<Double> values = new ArrayList<Double>();
			for(int row = 0; row < task.getNumRows() - 1; row++)
			{
				RunningMean mean = new RunningMean();
				for(int col = 0; col < task.getNumCols() - 1; col++)
					mean.addValue(cf.compare(task.getCol(col).get(row), task.getCol(col).get(task.getNumRows() - 1)));
				values.add(mean.getMean());
			}
			colExpectedValues.put(cf, values);
		}
		
		Map<MatrixEntry, Double> ret = new HashMap<MatrixEntry, Double>();
		for(MatrixEntry obj : task.getChoicesForSize(numChoices))
		{
			double dis = computeDistanceFromExpectation(task, obj, rowScores, colScores, rowExpectedValues, colExpectedValues);
			ret.put(obj, -dis);
		}
		
		return ret;
//		return Utility.normalize(ret);
		
//		MatrixEntry bestObj = null;
//		double minValue = Double.MAX_VALUE;
//		
//		for(MatrixEntry obj : task.getChoices())
//		{
//			double dis = computeDistanceFromExpectation(task, obj, rowScores, colScores, rowExpectedValues, colExpectedValues);
//			if(dis < minValue)
//			{
//				minValue = dis;
//				bestObj = obj;
//			}
//		}
//		
//		Map<MatrixEntry, Double> ret = new HashMap<MatrixEntry, Double>();
//		for(MatrixEntry obj : task.getChoices())
//		{
//			if(obj.equals(bestObj))
//				ret.put(obj, 1.0);
//			else
//				ret.put(obj, 0.0);
//		}
//		
//		return ret;
	}

	private double computeDistanceFromExpectation(MatrixCompletionTask task,
			MatrixEntry obj, Map<ComparisonFunction, Double> rowScores,
			Map<ComparisonFunction, Double> colScores,
			Map<ComparisonFunction, List<Double>> rowExpectedValues,
			Map<ComparisonFunction, List<Double>> colExpectedValues) {
		
		//let's do weighted euclidean distance
		//first compute the sum of the weights so that we transform them to sum to 1
		double sum = 0;
		for(Double d : rowScores.values())
			sum += d;
		for(Double d : colScores.values())
			sum += d;
		
		if(sum == 0.0) //special case
			return 0.0;
		
		//next let's sum the weighted squared difference
		double squareDiff = 0;
		for(ComparisonFunction cf : rowScores.keySet())
		{
			List<Double> values = new ArrayList<Double>();
			//first fill the list with the values for this object
			for(int col = 0; col < task.getNumCols() - 1; col++)
				values.add(cf.compare(task.getRow(task.getNumRows() - 1).get(col), obj));
			
			//next compare those values to the expected values
			for(int i = 0; i < values.size(); i++)
//				squareDiff += rowScores.get(cf)/sum*Math.pow(values.get(i) - rowExpectedValues.get(cf).get(i), 2);
				squareDiff += rowScores.get(cf)*Math.pow(values.get(i) - rowExpectedValues.get(cf).get(i), 2);
			
		}
		
		for(ComparisonFunction cf : colScores.keySet())
		{
			List<Double> values = new ArrayList<Double>();
			//first fill the list with the values for this object
			for(int row = 0; row < task.getNumRows() - 1; row++)
				values.add(cf.compare(task.getCol(task.getNumCols() - 1).get(row), obj));
			
			//next compare those values to the expected values
			for(int i = 0; i < values.size(); i++)
//				squareDiff += colScores.get(cf)/sum*Math.pow(values.get(i) - colExpectedValues.get(cf).get(i), 2);
				squareDiff += colScores.get(cf)*Math.pow(values.get(i) - colExpectedValues.get(cf).get(i), 2);
			
		}
		
		return Math.sqrt(squareDiff);
	}

	private Map<ComparisonFunction, Double> computeScores(List<List<MatrixEntry>> lists, List<ComparisonFunction> comparators) {
		Map<ComparisonFunction, Double> ret = new HashMap<ComparisonFunction, Double>();
		for(ComparisonFunction cf : comparators)
		{
			ret.put(cf, 1.0);
			for(int rowI = 0; rowI < lists.size(); rowI++)
			{
				for(int rowJ = rowI + 1; rowJ < lists.size(); rowJ++)
				{
					for(int objI = 0; objI < lists.get(rowI).size(); objI++)
					{
						for(int objJ = objI + 1; objJ < lists.get(rowI).size(); objJ++)
						{
							double value = 1 - Math.log(Math.abs(
									cf.compare(lists.get(rowI).get(objI), lists.get(rowI).get(objJ)) 
									- cf.compare(lists.get(rowJ).get(objI), lists.get(rowJ).get(objJ)))
									+ 1)/Math.log(2);
							ret.put(cf, ret.get(cf)*value);
						}
					}
				}
			}
		}
		
		return ret;
	}

}
