package taskSolver.comparisonFunctions;

import matrices.MatrixEntry;

public interface ComparisonFunction {
	
	/**
	 * Takes two objects and returns a value between 0 and 1 that represents
	 * a comparison between the two objects in some domain
	 * 
	 * @param obj1 the first object
	 * @param obj2 the second object
	 * @return a value between 0 and 1 that represents a comparison between
	 * 			the two objects
	 */
	public double compare(MatrixEntry obj1, MatrixEntry obj2);

}
