package testingStuff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import matrices.MatrixEntry;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.Utility;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.KStar;
import weka.classifiers.trees.RandomForest;
import weka.clusterers.Clusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import featureExtraction.ColorFeatureExtractor;
import featureExtraction.FeatureExtractionManager;
import featureExtraction.FeatureExtractor;

public class ColorClustering {

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
		
		List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
		manager.assignFeatures(objects, contexts);
		
		System.out.println("building instances");
		Instances data = buildInstances(objects, contexts);
		SimpleKMeans clusterer = new SimpleKMeans();
		clusterer.setNumClusters(3);
		System.out.println("building clusterer");
		clusterer.buildClusterer(data);
		
		System.out.println("clustering objects into " + clusterer.getNumClusters() + " clusters");
		Map<Integer, List<MatrixEntry>> clusters = new HashMap<Integer, List<MatrixEntry>>();
		for(int i = 0; i < clusterer.getNumClusters(); i++)
			clusters.put(i, new ArrayList<MatrixEntry>());
		for(MatrixEntry obj : objects)
		{
			int[] votes = new int[clusterer.getNumClusters()];
			for(int i = 0; i < FeatureExtractionManager.NUM_EXECUTIONS; i++)
			{
				Instance dataPoint = buildInstance(obj, i, contexts);
				votes[clusterer.clusterInstance(dataPoint)]++;
			}
			System.out.println(obj.getName() + " : " + Utility.convertToList(votes).toString());
			int cluster = Utility.getMax(votes);
			clusters.get(cluster).add(obj);
		}
		for(List<MatrixEntry> cluster : clusters.values())
			System.out.println(cluster);
		
	}
	
	private static Instance buildInstance(MatrixEntry object, int exec, Set<Context> contexts)
	{
		double[] dd = combineFeatures(object, exec, contexts);
		Instance dataPoint = new DenseInstance(dd.length);
		for(int i = 0; i < dd.length; i++)
			dataPoint.setValue(i, dd[i]);
		
		return dataPoint;
	}
	
	private static Instances buildInstances(List<MatrixEntry> objects, Set<Context> contexts)
	{
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		double[] fs = combineFeatures(objects.get(0), 0, contexts);
		for(int i = 1; i <= fs.length; i++)
		{
			Attribute attribute = new Attribute("" + i);
			attributes.add(attribute);
		}
		
		int capacity = FeatureExtractionManager.NUM_EXECUTIONS*objects.size();
		
		Instances ret = new Instances("data", attributes, capacity);
		
		for(MatrixEntry object : objects)
		{
			for(int exec = 0; exec < FeatureExtractionManager.NUM_EXECUTIONS; exec++)
			{
				double[] dd = combineFeatures(object, exec, contexts);
				Instance dataPoint = new DenseInstance(attributes.size());
				for(int i = 0; i < dd.length; i++)
					dataPoint.setValue(attributes.get(i), dd[i]);
				
				dataPoint.setDataset(ret);
				ret.add(dataPoint);
			}
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
//		contexts.add(new Context(Behavior.lift_slow, Modality.proprioception));
//		contexts.add(new Context(Behavior.low_drop, Modality.proprioception));
//		contexts.add(new Context(Behavior.poke, Modality.proprioception));
//		contexts.add(new Context(Behavior.push, Modality.proprioception));
//		contexts.add(new Context(Behavior.shake, Modality.proprioception));
//		contexts.add(new Context(Behavior.tap, Modality.proprioception));
		//color contexts
		contexts.add(new Context(Behavior.look, Modality.color));
		
		return contexts;
	}

}
