package utility;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Context implements Serializable, Comparable<Context> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4290488221704382808L;
	
	public final Behavior behavior;
	public final Modality modality;
	public Context(Behavior b, Modality m) {
		behavior = b;
		modality = m;
	}
	public int hashCode() {
		return behavior.hashCode()*modality.hashCode();
	}
	public boolean equals(Object o) {
		return (o instanceof Context) && ((Context)o).behavior.equals(behavior) && ((Context)o).modality.equals(modality);
	}
	public String toString() {
		return modality.toString() + "_" + behavior.toString();
	}
	public static List<Context> values() {
		List<Context> ret = new ArrayList<Context>();
		for(Modality m : Modality.values())
		{
			for(Behavior b : Behavior.values())
				ret.add(new Context(b, m));
		}
		return ret;
	}
	@Override
	public int compareTo(Context o) {
		//sort alphabeticaly
		return this.toString().compareTo(o.toString());
	}
}