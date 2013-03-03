package testingStuff;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import matrices.MatrixEntry;
import taskSolver.comparisonFunctions.ClusterDiffComparator;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import weka.clusterers.XMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.PerformanceStats;
import featureExtraction.FeatureExtractionManager;

public class ClustererAgreementTest {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		measureClustererAgreement();
	}
	

	private static void measureClustererAgreement() throws Exception {
		
		Random rand = new Random(1);
		FeatureExtractionManager manager = new FeatureExtractionManager(rand);
		
		final List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
		manager.assignFeatures(objects, new HashSet<Context>(getContexts()));
		
		final Map<Context, ClusterDiffComparator> clusterers = new HashMap<Context, ClusterDiffComparator>();
		for(Context c : getContexts())
		{
			System.out.println("building clusterer for " + c.toString());
			Set<Context> temp = new HashSet<Context>();
			temp.add(c);
			clusterers.put(c, new ClusterDiffComparator(objects, temp));
		}
		System.out.println("computing distances");
		final Map<Set<Context>, Double> distances = computeDistances(objects, clusterers);
		
		XMeans xmeans = new XMeans();
		xmeans.setMaxNumClusters(3);
		xmeans.setMinNumClusters(3);
		xmeans.setDistanceF(new weka.core.DistanceFunction() {
			public void setOptions(String[] arg0) throws Exception {throw new UnsupportedOperationException();}
			public Enumeration listOptions() {throw new UnsupportedOperationException();}
			public String[] getOptions() {throw new UnsupportedOperationException();}
			public void update(Instance arg0) {throw new UnsupportedOperationException();}
			public void setInvertSelection(boolean arg0) {throw new UnsupportedOperationException();}
			public void setInstances(Instances arg0) {}
			public void setAttributeIndices(String arg0) {throw new UnsupportedOperationException();}
			public void postProcessDistances(double[] arg0) {throw new UnsupportedOperationException();}
			public boolean getInvertSelection() {throw new UnsupportedOperationException();}
			public Instances getInstances() {throw new UnsupportedOperationException();}
			public String getAttributeIndices() {throw new UnsupportedOperationException();}
			public double distance(Instance arg0, Instance arg1, double arg2,
					PerformanceStats arg3) {throw new UnsupportedOperationException();}
			public double distance(Instance arg0, Instance arg1, double arg2) {throw new UnsupportedOperationException();}
			public double distance(Instance arg0, Instance arg1, PerformanceStats arg2)
					throws Exception {throw new UnsupportedOperationException();}
			
			@Override
			public double distance(Instance arg0, Instance arg1) {
				Context c1 = getContexts().get((int) arg0.value(0));
				Context c2 = getContexts().get((int) arg1.value(0));
				Set<Context> pair = new HashSet<Context>();
				pair.add(c1);
				pair.add(c2);
				return distances.get(pair);
			}
		});
		
		Instances data = buildInstances(getContexts());
		System.out.println("clustering contexts");
		xmeans.buildClusterer(data);
		
		Map<Integer, List<Context>> clusters = new HashMap<Integer, List<Context>>();
		for(int i = 0; i < xmeans.numberOfClusters(); i++)
			clusters.put(i, new ArrayList<Context>());
		for(Context c : getContexts())
			clusters.get(xmeans.clusterInstance(buildInstance(c))).add(c);
		for(int i = 0; i < xmeans.numberOfClusters(); i++)
			System.out.println(clusters.get(i));
		
	}
	
	private static Instance buildInstance(Context c)
	{
		Instance dataPoint = new DenseInstance(1);
		dataPoint.setValue(0, getContexts().indexOf(c));
		
		return dataPoint;
	}
	
	private static Instances buildInstances(List<Context> contexts)
	{
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("hash"));
		
		int capacity = contexts.size();
		
		Instances ret = new Instances("data", attributes, capacity);
		
		for(Context c : contexts)
		{
			Instance dataPoint = buildInstance(c);
			dataPoint.setDataset(ret);
			ret.add(dataPoint);
		}
		
		return ret;
	}
	
	private static List<Context> getContexts() {
		List<Context> contexts = new ArrayList<Context>();
		//add each context explicitly so we know which ones we're using
		//audio contexts
		contexts.add(new Context(Behavior.crush, Modality.audio));
		contexts.add(new Context(Behavior.grasp, Modality.audio));
		contexts.add(new Context(Behavior.high_velocity_shake, Modality.audio));
		contexts.add(new Context(Behavior.hold, Modality.audio));
		contexts.add(new Context(Behavior.lift_slow, Modality.audio));
		contexts.add(new Context(Behavior.low_drop, Modality.audio));
		contexts.add(new Context(Behavior.poke, Modality.audio));
		contexts.add(new Context(Behavior.push, Modality.audio));
		contexts.add(new Context(Behavior.shake, Modality.audio));
		contexts.add(new Context(Behavior.tap, Modality.audio));
		//proprioception contexts
		contexts.add(new Context(Behavior.crush, Modality.proprioception));
		contexts.add(new Context(Behavior.grasp, Modality.proprioception));
		contexts.add(new Context(Behavior.high_velocity_shake, Modality.proprioception));
		contexts.add(new Context(Behavior.hold, Modality.proprioception));
		contexts.add(new Context(Behavior.lift_slow, Modality.proprioception));
		contexts.add(new Context(Behavior.low_drop, Modality.proprioception));
		contexts.add(new Context(Behavior.poke, Modality.proprioception));
		contexts.add(new Context(Behavior.push, Modality.proprioception));
		contexts.add(new Context(Behavior.shake, Modality.proprioception));
		contexts.add(new Context(Behavior.tap, Modality.proprioception));
		//color contexts	
		contexts.add(new Context(Behavior.look, Modality.color));
		
		return contexts;
	}
	
	private static Map<Set<Context>, Double> computeDistances(List<MatrixEntry> objects, Map<Context, ClusterDiffComparator> clusterers)
	{
		Map<Context, Map<MatrixEntry, Integer>> clusters = new HashMap<Context, Map<MatrixEntry,Integer>>();
		for(Entry<Context, ClusterDiffComparator> e : clusterers.entrySet())
		{
			clusters.put(e.getKey(), new HashMap<MatrixEntry, Integer>());
			for(MatrixEntry obj : objects)
				clusters.get(e.getKey()).put(obj, e.getValue().getCluster(obj));
		}
		
		Map<Set<Context>, Double> ret = new HashMap<Set<Context>, Double>();
		for(Entry<Context, ClusterDiffComparator> e1 : clusterers.entrySet())
		{
			for(Entry<Context, ClusterDiffComparator> e2 : clusterers.entrySet())
			{
				Set<Context> pair = new HashSet<Context>();
				pair.add(e1.getKey());
				pair.add(e2.getKey());
				if(ret.get(pair) != null)
					continue;
				else if(e1.getKey().equals(e2.getKey()))
				{
					ret.put(pair, 0.0);
					continue;
				}
				
				int disagreed = 0;
				int total = 0;
				for(MatrixEntry obj1 : objects)
				{
					for(MatrixEntry obj2 : objects)
					{
						if(obj1.equals(obj2))
							continue;
						boolean same1 = (e1.getValue().getCluster(obj1) == e1.getValue().getCluster(obj2));
						boolean same2 = (e2.getValue().getCluster(obj1) == e2.getValue().getCluster(obj2));
						//do the two clusterers disagree on whether or not obj1 and obj2 belong in the same cluster?
						if(same1 != same2)
							disagreed++;
						total++;
					}
				}
				ret.put(pair, 1.0*disagreed/total);
			}
		}
		
		return ret;
	}

}












