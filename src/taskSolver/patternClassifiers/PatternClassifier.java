package taskSolver.patternClassifiers;

import java.util.List;
import java.util.Set;

import matrices.MatrixEntry;

public interface PatternClassifier {
	
	/**
	 * Takes in two sets of lists: The first set is a set of positive
	 * examples of the pattern that this classifier is to learn. The
	 * second set is a set of negative examples. The classifier then
	 * trains on these sets.
	 * 
	 * @param positiveExamples The set of positive examples
	 * @param negativeExamples	The set of negative examples
	 */
	public void trainClassifier(Set<List<MatrixEntry>> positiveExamples, Set<List<MatrixEntry>> negativeExamples);
	
	/**
	 * Takes in an ordered list and returns the probability that that
	 * list adheres to the pattern this classifier was trained on.
	 * 
	 * @param list
	 * @return
	 */
	public double classifyPattern(List<MatrixEntry> list);
	
	/**
	 * Resets the training of this classifier
	 */
	public void reset();

}
