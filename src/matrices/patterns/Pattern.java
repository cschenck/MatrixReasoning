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
	 * Returns the set of properties that are relavant to this pattern. 
	 * 
	 * @return
	 */
	public Set<String> getRelavantProperties();
	

}
