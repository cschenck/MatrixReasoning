package matrices.patterns;

import java.util.List;
import java.util.Set;

import matrices.MatrixEntry;

public interface Pattern {
	
	/**
	 * Detects if the corresponding pattern is present in this ordered
	 * list of objects.
	 * 
	 * @param objects	The objects to detect the pattern in
	 * @return			True iff the pattern is present in objects.
	 */
	public boolean detectPattern(List<MatrixEntry> objects);
	
	/**
	 * Attempts to align the given list of objects to this pattern. It will
	 * replace objects in the given list with objects from the objectPool
	 * that are the closest match to the replaced object but  fit the pattern,
	 * e.g., if the pattern is all the same color, then it will replace the
	 * second through last objects in the given list with identical objects from
	 * the object pool except that they all have the same color as the first
	 * object in the list. The returned list is the aligned version of the
	 * given list. If it is not possible to align the given list from the given
	 * object pool, then the returned list is null.
	 * 
	 * @param toAlign		List of objects to align on.
	 * @param objectPool	Object pool to use to replace object in toAlign.
	 * @return				The aligned version of toAlign if it was able to be aligned; false otherwise.
	 */
	public List<MatrixEntry> align(List<MatrixEntry> toAlign, Set<MatrixEntry> objectPool);
	
	/**
	 * does the opposite of align
	 * 
	 * @param toAlter
	 * @param objectPool
	 * @return
	 */
	public List<MatrixEntry> disalign(List<MatrixEntry> toAlter, Set<MatrixEntry> objectPool);
	
	

}
