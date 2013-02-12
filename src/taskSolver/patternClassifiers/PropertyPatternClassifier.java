package taskSolver.patternClassifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import matrices.MatrixEntry;
import utility.Context;
import utility.Utility;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class PropertyPatternClassifier implements PatternClassifier {
	
	private static Classifier instantiateClassifier()
	{
		return new SMO();
	}
	
	private static Classifier instantiateMetaClassifier()
	{
		return new J48();
	}
	
	private Set<Context> contexts;
	private List<Integer> trainExecutions;
	private int testExecution;
	private Set<String> properties;
	private Set<MatrixEntry> objects;
	
	private Map<String, List<String>> valueMap;
	private Map<String, Map<Context, Classifier>> classifiers;
	private Map<String, Map<Context, ArrayList<Attribute>>> attributes;
	private Classifier metaClassifier;
	private ArrayList<Attribute> metaAttributes;
	
	//TODO redo this entire class, make a PropertyClassifier class and take a set of those as inputs
	public PropertyPatternClassifier(Set<MatrixEntry> objects, List<Integer> trainExecutions, int testExecution, 
			Set<String> properties, Set<Context> contexts)
	{
		this.objects = objects;
		this.contexts = contexts;
		this.trainExecutions = trainExecutions;
		this.testExecution = testExecution;
		this.properties = properties;
	}

	@Override
	public void trainClassifier(Set<List<MatrixEntry>> positiveExamples, Set<List<MatrixEntry>> negativeExamples) {
		
		Utility.debugPrint("training context classifiers...");
		
		this.valueMap = new HashMap<String, List<String>>();
		for(String property : this.properties)
		{
			this.valueMap.put(property, new ArrayList<String>());
			for(MatrixEntry object : objects)
			{
				if(!this.valueMap.get(property).contains(object.getPropertyValue(property)))
					this.valueMap.get(property).add(object.getPropertyValue(property));
			}
		}
		
		this.classifiers = new HashMap<Context, Classifier>();
		this.attributes = new HashMap<Context, ArrayList<Attribute>>();
		for(Context c : contexts)
		{
			Classifier classifier = instantiateClassifier();
			ArrayList<Attribute> as = new ArrayList<Attribute>();
			int featureLength = objects.iterator().next().getFeatures(c).get(0).length;
			for(int i = 1; i <= featureLength; i++)
			{
				Attribute attribute = new Attribute("" + i);
				as.add(attribute);
			}
			
			Attribute classAttribute = new Attribute("class", values);
			as.add(classAttribute);
			
			int capacity = objects.size()*trainExecutions.size();
			
			Instances trainData = new Instances("data", as, capacity);
			trainData.setClassIndex(as.size() - 1);
			
			for(MatrixEntry object : objects)
			{
				for(Integer exec : trainExecutions)
				{
					Instance dataPoint = new DenseInstance(as.size());
					double[] fs = object.getFeatures(c).get(exec);
					for(int i = 0; i < fs.length; i++)
						dataPoint.setValue(as.get(i), fs[i]);
					
					String s = object.getPropertyValue(property);
					dataPoint.setValue(as.get(as.size() - 1), s);
					dataPoint.setDataset(trainData);
					trainData.add(dataPoint);
				}
			}
			
			try {
				classifier.buildClassifier(trainData);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			this.classifiers.put(c, classifier);
			this.attributes.put(c, as);
		}
		
		System.err.print("training meta classifier...");
		metaClassifier = instantiateMetaClassifier();
		this.metaAttributes = new ArrayList<Attribute>();
		int patternSize = positiveExamples.iterator().next().size();
		for(int i = 0; i < patternSize*contexts.size(); i++)
			new Attribute("meta" + i, values);
		
		List<String> metaValues = new ArrayList<String>();
		metaValues.add("positive");
		metaValues.add("negative");
		this.metaAttributes.add(new Attribute("metaClass", metaValues));
		
		int capacity = trainExecutions.size()*(positiveExamples.size() + negativeExamples.size());
		Instances trainData = new Instances("data", this.metaAttributes, capacity);
		trainData.setClassIndex(this.metaAttributes.size() - 1);
		for(List<MatrixEntry> list : positiveExamples)
		{
			for(Integer exec : this.trainExecutions)
			{
				List<String> features = new ArrayList<String>();
				for(Context c : contexts)
				{
					for(MatrixEntry obj : list)
						features.add(classifyInstance(c, obj, exec));
				}
				
				Instance dataPoint = new DenseInstance(this.metaAttributes.size());
				for(int i = 0; i < features.size(); i++)
					dataPoint.setValue(this.metaAttributes.get(i), features.get(i));
				
				dataPoint.setValue(this.metaAttributes.get(this.metaAttributes.size() - 1), metaValues.get(0));
				dataPoint.setDataset(trainData);
				trainData.add(dataPoint);
			}
		}
		
		for(List<MatrixEntry> list : negativeExamples)
		{
			for(Integer exec : this.trainExecutions)
			{
				List<String> features = new ArrayList<String>();
				for(Context c : contexts)
				{
					for(MatrixEntry obj : list)
						features.add(classifyInstance(c, obj, exec));
				}
				
				Instance dataPoint = new DenseInstance(this.metaAttributes.size());
				for(int i = 0; i < features.size(); i++)
					dataPoint.setValue(this.metaAttributes.get(i), features.get(i));
				
				dataPoint.setValue(this.metaAttributes.get(this.metaAttributes.size() - 1), metaValues.get(1));
				dataPoint.setDataset(trainData);
				trainData.add(dataPoint);
			}
		}
		
		try {
			this.metaClassifier.buildClassifier(trainData);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		System.err.println("done");
	}

	private String classifyInstance(Context c, MatrixEntry obj, Integer exec) {
		Instance dataPoint = new DenseInstance(attributes.get(c).size());
		
		double[] fs = obj.getFeatures(c).get(exec);
		
		for(int i = 0; i < fs.length; i++)
		{
			dataPoint.setValue(attributes.get(c).get(i), fs[i]);
		}
		
		Instances testData = new Instances("testData", attributes.get(c), 1);
		dataPoint.setDataset(testData);
		testData.add(dataPoint);
		testData.setClassIndex(this.attributes.get(c).size() - 1);
		
		double[] results = null;
		try {
			results = this.classifiers.get(c).distributionForInstance(dataPoint);			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		int max = 0;
		for(int i = 1; i < results.length; i++)
		{
			if(results[i] > results[max])
				max = i;
		}
		
		return this.values.get(max);
	}

	@Override
	public double classifyPattern(List<MatrixEntry> list) {
		List<String> features = new ArrayList<String>();
		for(Context c : contexts)
		{
			for(MatrixEntry obj : list)
				features.add(this.classifyInstance(c, obj, this.testExecution));
		}
		
		Instance dataPoint = new DenseInstance(this.metaAttributes.size());
		for(int i = 0; i < features.size(); i++)
			dataPoint.setValue(this.metaAttributes.get(i), features.get(i));
		
		Instances testData = new Instances("data", this.metaAttributes, 1);
		testData.setClassIndex(this.metaAttributes.size() - 1);
		dataPoint.setDataset(testData);
		testData.add(dataPoint);
		
		try {
			return this.metaClassifier.distributionForInstance(dataPoint)[0];
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset() {
		this.classifiers = null;
		this.attributes = null;
		this.metaAttributes = null;
		this.metaClassifier = null;
	}

}
