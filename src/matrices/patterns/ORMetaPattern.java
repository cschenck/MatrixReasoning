package matrices.patterns;

import java.util.List;
import java.util.Set;

import matrices.MatrixEntry;

public class ORMetaPattern implements Pattern {

	private Pattern p1;
	private Pattern p2;
	
	public ORMetaPattern(Pattern p1, Pattern p2)
	{
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public boolean detectPattern(List<MatrixEntry> objects) {
		return this.p1.detectPattern(objects) || this.p2.detectPattern(objects);
	}

	@Override
	public Set<String> getRelavantProperties() {
		Set<String> ret = this.p1.getRelavantProperties();
		ret.addAll(this.p2.getRelavantProperties());
		return ret;
	}

}
