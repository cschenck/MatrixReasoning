package taskSolver.comparisonFunctions;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matrices.MatrixEntry;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import utility.Context;
import utility.RunningMean;
import utility.SpectralClusterer;
import weka.clusterers.Clusterer;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.PerformanceStats;

public class ClusterDiffComparator implements ComparisonFunction {
	
	private static Clusterer instantiateClusterer(final List<MatrixEntry> objects, final Set<Context> contexts)
	{
		SpectralClusterer clusterer = new SpectralClusterer();
		clusterer.setDistanceFunctionIsSimilarityFunction(true);
		clusterer.setAlphaStar(0.95);
		final Map<Context, ComparisonFunction> comps = new HashMap<Context, ComparisonFunction>();
		for(Context c : contexts)
			comps.put(c, new DistanceComparatorLogisticsNormalization(c, DistanceFunction.Euclidean, objects));
		clusterer.setDistanceFunction(new weka.core.DistanceFunction() {
			private Instances data = null;
			public void setOptions(String[] arg0) throws Exception {throw new UnsupportedOperationException();}
			public Enumeration listOptions() {throw new UnsupportedOperationException();}
			public String[] getOptions() {throw new UnsupportedOperationException();}
			public void update(Instance arg0) {throw new UnsupportedOperationException();}
			public void setInvertSelection(boolean arg0) {throw new UnsupportedOperationException();}
			public void setInstances(Instances arg0) {data = arg0;}
			public void setAttributeIndices(String arg0) {throw new UnsupportedOperationException();}
			public void postProcessDistances(double[] arg0) {throw new UnsupportedOperationException();}
			public boolean getInvertSelection() {throw new UnsupportedOperationException();}
			public Instances getInstances() {return data;}
			public String getAttributeIndices() {throw new UnsupportedOperationException();}
			public double distance(Instance arg0, Instance arg1, double arg2,
					PerformanceStats arg3) {throw new UnsupportedOperationException();}
			public double distance(Instance arg0, Instance arg1, double arg2) {throw new UnsupportedOperationException();}
			public double distance(Instance arg0, Instance arg1, PerformanceStats arg2)
					throws Exception {throw new UnsupportedOperationException();}
			
			@Override
			public double distance(Instance arg0, Instance arg1) {
				MatrixEntry obj1 = objects.get((int) arg0.value(0));
				MatrixEntry obj2 = objects.get((int) arg1.value(0));
				RunningMean ret = new RunningMean();
				for(Context c : contexts)
					ret.addValue(comps.get(c).compare(obj1, obj2));
				if(ret.getMean() == 1.0) //this is to prevent 0's from causing exceptions when doing eigen value decomp
					return 0.00000001; //make it close to zero, but not exactly zero
				else
					return 1.0 - ret.getMean();
			}
		});
		return clusterer;
	}
	
	private Clusterer clusterer;
	private Map<MatrixEntry, Integer> clusterLabels;
	private Set<Context> contexts;
	private List<MatrixEntry> objects; //this is kept merely so that objects can be looked up by their index number for convenience
	private List<String> values = null;
	
	public ClusterDiffComparator(List<MatrixEntry> objects, Set<Context> contexts)
	{
		this.contexts = contexts;
		
		this.objects = objects;
		
		setUpClusterer();
		detectClusterOrdering();
	}
	
	private void detectClusterOrdering() {
		// TODO Auto-generated method stub
	}

	private void setUpClusterer()
	{
		this.clusterer = instantiateClusterer(objects, contexts);
		

		Attribute classAttribute = new Attribute("index");
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(classAttribute);
		
		
		int capacity = objects.size();
		
		Instances trainData = new Instances("data", attributes, capacity);
		
		for(MatrixEntry object : objects)
		{
			Instance dataPoint = new DenseInstance(attributes.size());
			dataPoint.setDataset(trainData);
			dataPoint.setValue(0, objects.indexOf(object));
			trainData.add(dataPoint);
		}
		
		try {
			clusterer.buildClusterer(trainData);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		this.clusterLabels = new HashMap<MatrixEntry, Integer>();
		for(MatrixEntry obj : objects)
		{
			this.clusterLabels.put(obj, this.computeCluster(obj));
		}
	}

	@Override
	public double compare(MatrixEntry obj1, MatrixEntry obj2) {		
		try {
			if(this.values == null)
			{
				int i1 = this.getCluster(obj1);
				int i2 = this.getCluster(obj2);
				if(i1 == i2)
					return 0.0;
				else
					return 1.0;
			}
			else
			{
				int i1 = this.getCluster(obj1);
				int i2 = this.getCluster(obj2);
				//we want the range to be [0,1], so first get it to be [-1,1] then shift up by 1 and divide by 2
				return (1.0 + 1.0*(i2 - i1)/(values.size() - 1))/2.0;
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String toString()
	{
		return contexts.toString();
	}
	
	public int getCluster(MatrixEntry obj)
	{
		return this.clusterLabels.get(obj);
	}
	
	private int computeCluster(MatrixEntry obj)
	{
		Instance dataPoint1 = new DenseInstance(1);
		
		Attribute classAttribute = new Attribute("index");
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(classAttribute);
		Instances testData = new Instances("testData", attributes, 1);
		dataPoint1.setDataset(testData);
		
		
		dataPoint1.setValue(0, objects.indexOf(obj));
		
		testData.add(dataPoint1);
		
		try {
			return clusterer.clusterInstance(dataPoint1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
