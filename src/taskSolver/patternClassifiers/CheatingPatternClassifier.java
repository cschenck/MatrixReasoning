package taskSolver.patternClassifiers;

import java.util.List;
import java.util.Set;

import matrices.MatrixEntry;
import matrices.patterns.Pattern;

public class CheatingPatternClassifier implements PatternClassifier {
	
	private Pattern pattern;
	
	public CheatingPatternClassifier(Pattern pattern)
	{
		this.pattern = pattern;
	}

	@Override
	public void trainClassifier(Set<List<MatrixEntry>> positiveExamples,
			Set<List<MatrixEntry>> negativeExamples) {
		// no need for training since this guy cheats
	}

	@Override
	public double classifyPattern(List<MatrixEntry> list) {
		if(pattern.detectPattern(list))
			return 1.0;
		else
			return 0.0;
	}

	@Override
	public void reset() {
		
	}
	
	@Override
	public String toString()
	{
		return "CheatingPatternClassifier:" + pattern.toString();
	}

}
