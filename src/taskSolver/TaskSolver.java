package taskSolver;

import java.util.List;
import java.util.Map;

import taskSolver.comparisonFunctions.ComparisonFunction;

import matrices.MatrixCompletionTask;
import matrices.MatrixEntry;

public interface TaskSolver {
	
	/**
	 * Takes in a matrix completion task and a list of comparison functions between pairs
	 * of objects and returns a probability distribution over the choices to complete
	 * the matrix.
	 * 
	 * @param task the task to solve
	 * @param comparators the comparison functions to use to solve the task
	 * @return a probability distribution over the choices for the task
	 */
	public Map<MatrixEntry, Double> solveTask(MatrixCompletionTask task, int numChoices, List<ComparisonFunction> comparators);

}
