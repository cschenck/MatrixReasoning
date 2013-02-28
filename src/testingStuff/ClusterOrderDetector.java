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
import taskSolver.comparisonFunctions.ComparisonFunction;
import taskSolver.comparisonFunctions.DistanceComparator;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.ObjectOrderer;
import utility.ObjectOrderer.OrderingDistanceFunction;
import utility.RunningMean;
import utility.Utility;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.KStar;
import weka.classifiers.trees.RandomForest;
import weka.clusterers.Clusterer;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.XMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.PerformanceStats;
import featureExtraction.ColorFeatureExtractor;
import featureExtraction.FeatureExtractionManager;
import featureExtraction.FeatureExtractor;

public class ClusterOrderDetector {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Random rand = new Random(1);
		FeatureExtractionManager manager = new FeatureExtractionManager(rand);
		
		Set<Context> contexts = getContexts();
		String property = "color";
		//String property = "weight";
//		String property = "contents";
		
		final List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
		manager.assignFeatures(objects, contexts);
		
		System.out.println("building instances");
		Instances data = buildInstances(objects, contexts);
		XMeans clusterer = new XMeans();
		clusterer.setMaxNumClusters(3);
		clusterer.setMinNumClusters(3);
		final Map<Context, ComparisonFunction> comps = new HashMap<Context, ComparisonFunction>();
		for(Context c : contexts)
			comps.put(c, new DistanceComparator(c, DistanceFunction.Euclidean, objects));
		clusterer.setDistanceF(new weka.core.DistanceFunction() {
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
				MatrixEntry obj1 = objects.get((int) arg0.value(0));
				MatrixEntry obj2 = objects.get((int) arg1.value(0));
				RunningMean ret = new RunningMean();
				for(Context c : getContexts())
					ret.addValue(comps.get(c).compare(obj1, obj2));
				return ret.getMean();
			}
		});
		System.out.println("building clusterer");
		clusterer.buildClusterer(data);
		
		System.out.println("clustering objects into " + clusterer.getMaxNumClusters() + " clusters");
		Map<Integer, List<MatrixEntry>> clusters = new HashMap<Integer, List<MatrixEntry>>();
		for(int i = 0; i < clusterer.getMaxNumClusters(); i++)
			clusters.put(i, new ArrayList<MatrixEntry>());
		for(MatrixEntry obj : objects)
		{
			
			Instance dataPoint = buildInstance(obj, objects.indexOf(obj));
			clusters.get(clusterer.clusterInstance(dataPoint)).add(obj);
			
//			System.out.println(obj.getName() + " : " + clusterer.clusterInstance(dataPoint));
			
		}
		
		@SuppressWarnings("unchecked")
		ObjectOrderer<MatrixEntry> orderer = 
				new ObjectOrderer<MatrixEntry>((OrderingDistanceFunction<MatrixEntry>) comps.values().iterator().next());
		Map<List<Integer>, Integer> orderCounts = new HashMap<List<Integer>, Integer>();
		for(int i = 0; i < 50; i++)
		{
			Set<MatrixEntry> toOrder = new HashSet<MatrixEntry>();
			for(List<MatrixEntry> cluster : clusters.values())
				toOrder.add(cluster.get(rand.nextInt(cluster.size())));
			List<MatrixEntry> order = orderer.findBestOrdering(toOrder);
			List<Integer> clusterOrder = new ArrayList<Integer>();
			for(MatrixEntry obj : order)
				clusterOrder.add(clusterer.clusterInstance(buildInstance(obj, objects.indexOf(obj))));
			//we don't care about the direction, so ensure a consistent order
			if(clusterOrder.get(0) > clusterOrder.get(clusterOrder.size() - 1))
				clusterOrder = Utility.reverseOrder(clusterOrder);
			
			if(orderCounts.get(clusterOrder) == null)
				orderCounts.put(clusterOrder, 1);
			else
				orderCounts.put(clusterOrder, orderCounts.get(clusterOrder) + 1);
		}
		
		for(Entry<List<Integer>, Integer> e : orderCounts.entrySet())
			System.out.println(e.getKey() + " - " + e.getValue());
		
	}
	
	private static Instance buildInstance(MatrixEntry object, int index)
	{
		Instance dataPoint = new DenseInstance(1);
		dataPoint.setValue(0, index);
		
		return dataPoint;
	}
	
	private static Instances buildInstances(List<MatrixEntry> objects, Set<Context> contexts)
	{
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("index"));
		
		int capacity = FeatureExtractionManager.NUM_EXECUTIONS*objects.size();
		
		Instances ret = new Instances("data", attributes, capacity);
		
		for(MatrixEntry object : objects)
		{
			Instance dataPoint = new DenseInstance(attributes.size());
			dataPoint.setDataset(ret);
			dataPoint.setValue(0, objects.indexOf(object));
			ret.add(dataPoint);
		}
		
		return ret;
	}
	
	private static double[] combineFeatures(MatrixEntry object, int exec, Set<Context> contexts)
	{
		List<Double> features = new ArrayList<Double>();
		for(Context c : contexts)
		{
			for(double d : object.getFeatures(c).get(exec))
				features.add(d);
		}
		
		double[] ret = new double[features.size()];
		for(int i = 0; i < ret.length; i++)
			ret[i] = features.get(i);
		
		return ret;
	}
	
	private static Set<Context> getContexts() {
		Set<Context> contexts = new HashSet<Context>();
		//add each context explicitly so we know which ones we're using
		//audio contexts
//		contexts.add(new Context(Behavior.crush, Modality.audio));
//		contexts.add(new Context(Behavior.grasp, Modality.audio));
//		contexts.add(new Context(Behavior.high_velocity_shake, Modality.audio));
//		contexts.add(new Context(Behavior.hold, Modality.audio));
//		contexts.add(new Context(Behavior.lift_slow, Modality.audio));
//		contexts.add(new Context(Behavior.low_drop, Modality.audio));
//		contexts.add(new Context(Behavior.poke, Modality.audio));
//		contexts.add(new Context(Behavior.push, Modality.audio));
//		contexts.add(new Context(Behavior.shake, Modality.audio));
//		contexts.add(new Context(Behavior.tap, Modality.audio));
		//proprioception contexts
//		contexts.add(new Context(Behavior.crush, Modality.proprioception));
//		contexts.add(new Context(Behavior.grasp, Modality.proprioception));
//		contexts.add(new Context(Behavior.high_velocity_shake, Modality.proprioception));
//		contexts.add(new Context(Behavior.hold, Modality.proprioception));
		contexts.add(new Context(Behavior.lift_slow, Modality.proprioception));
//		contexts.add(new Context(Behavior.low_drop, Modality.proprioception));
//		contexts.add(new Context(Behavior.poke, Modality.proprioception));
//		contexts.add(new Context(Behavior.push, Modality.proprioception));
//		contexts.add(new Context(Behavior.shake, Modality.proprioception));
//		contexts.add(new Context(Behavior.tap, Modality.proprioception));
		//color contexts	
//		contexts.add(new Context(Behavior.look, Modality.color));
		
		return contexts;
	}

}