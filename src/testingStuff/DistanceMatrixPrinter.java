package testingStuff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import taskSolver.comparisonFunctions.DistanceComparatorLogisticsNormalization;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.RunningMean;
import utility.SpectralClusterer;
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

public class DistanceMatrixPrinter {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Random rand = new Random(1);
		FeatureExtractionManager manager = new FeatureExtractionManager(rand);
		
		Context c = new Context(Behavior.look, Modality.color);
		Set<Context> set = new HashSet<Context>();
		set.add(c);
		
		final List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
		manager.assignFeatures(objects, set);
		
		Collections.sort(objects, new MatrixEntrySorter("color", "weight", "contents"));
		
		ComparisonFunction cf;
		if(c.modality.equals(Modality.color))
		{
			cf = new DistanceComparator(c, DistanceFunction.Euclidean, objects);
			((DistanceComparator)cf).enableNormalization(false);
		}
		else
			cf = new DistanceComparatorLogisticsNormalization(c, DistanceFunction.Euclidean, objects, false);
		
		List<String> headers = new ArrayList<String>();
		for(MatrixEntry obj : objects)
			headers.add(obj.getName());
		
		double[][] data = new double[objects.size()][objects.size()];
		for(int i = 0; i < objects.size(); i++)
		{
			for(int j = 0; j < objects.size(); j++)
			{
				data[i][j] = Math.exp(0.1*cf.compare(objects.get(i), objects.get(j)));
			}
		}
		
		Utility.printTable(headers, headers, data, false);
		
	}
	
	private static class MatrixEntrySorter implements Comparator<MatrixEntry>
	{
		private List<String> propertyPriority;
		
		public MatrixEntrySorter(String ... properties)
		{
			propertyPriority = Utility.convertToList(properties);
		}

		@Override
		public int compare(MatrixEntry o1, MatrixEntry o2) {
			for(String property : this.propertyPriority)
			{
				int res = o1.getPropertyValue(property).compareTo(o2.getPropertyValue(property));
				if(res != 0)
					return res;
			}
			
			return 0;
		}
	}
	

}
