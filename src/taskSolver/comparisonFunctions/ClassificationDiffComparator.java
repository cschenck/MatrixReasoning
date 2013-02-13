package taskSolver.comparisonFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import matrices.MatrixEntry;
import utility.Context;
import utility.Utility;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SMOreg;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ClassificationDiffComparator implements ComparisonFunction {
	
	private static double PCA_VARIANCE_COVERED = 0.95;
	private static double FEATURE_SELECTION_GAIN_THRESHOLD = 0.075;

	private static Classifier instantiateClassifier()
	{
		return new SMO();
//		return new J48();
//		AdaBoostM1 ret = new AdaBoostM1();
//		ret.setClassifier(new J48());
//		return ret;
	}
	
	private static Classifier instantiateRegressor()
	{
		return new SMOreg();
	}
	
	private Classifier classifier;
	private int testExecution;
	private ArrayList<Attribute> attributes;
	private String property;
	private Set<Context> contexts;
	private List<String> values = null;
	
	private AttributeSelection as;
	private PrincipalComponents pca;
	private boolean doFeatureSelection;
	private boolean doPCA;
	
	public ClassificationDiffComparator(List<Integer> trainExecutions, int testExecution, 
			List<MatrixEntry> objects, String property, Set<Context> contexts, 
			boolean doFeatureSelection, boolean doPCA)
	{
		this.testExecution = testExecution;
		this.property = property;
		this.contexts = contexts;
		this.doFeatureSelection = doFeatureSelection;
		this.doPCA = doPCA;
		
		setUpClassifier(trainExecutions, objects);
	}
	
	public ClassificationDiffComparator(List<Integer> trainExecutions, int testExecution, 
			List<MatrixEntry> objects, String property, List<String> values, Set<Context> contexts, 
			boolean doFeatureSelection, boolean doPCA)
	{
		this.testExecution = testExecution;
		this.property = property;
		this.contexts = contexts;
		this.values = values;
		this.doFeatureSelection = doFeatureSelection;
		this.doPCA = doPCA;
		
		setUpClassifier(trainExecutions, objects);
	}
	
	private void setUpClassifier(List<Integer> trainExecutions, List<MatrixEntry> objects)
	{
		if(values == null)
			this.classifier = instantiateClassifier();
		else
			this.classifier = instantiateRegressor();
		
		attributes = new ArrayList<Attribute>();
		int featureLength = this.combineFeatures(objects.get(0), 0, contexts).length;
		for(int i = 1; i <= featureLength; i++)
		{
			Attribute attribute = new Attribute("" + i);
			attributes.add(attribute);
		}
		
		if(this.values == null)
		{
			List<String> values = new ArrayList<String>();
			for(MatrixEntry object : objects)
			{
				if(!values.contains(object.getPropertyValue(property)))
					values.add(object.getPropertyValue(property));
			}
			Attribute classAttribute = new Attribute("class", values);
			attributes.add(classAttribute);
		}
		else
		{
			Attribute classAttribute = new Attribute("class");
			attributes.add(classAttribute);
		}
		
		int capacity = (int) Math.pow(objects.size()*trainExecutions.size(), 2);
		
		Instances trainData = new Instances("data", attributes, capacity);
		trainData.setClassIndex(attributes.size() - 1);
		
		for(MatrixEntry object : objects)
		{
			for(Integer exec : trainExecutions)
			{
				Instance dataPoint = new DenseInstance(attributes.size());
				double[] fs = this.combineFeatures(object, exec, contexts);
				for(int i = 0; i < fs.length; i++)
					dataPoint.setValue(attributes.get(i), fs[i]);
				
				if(this.values == null)
				{
					String s = object.getPropertyValue(property);
					dataPoint.setValue(attributes.get(attributes.size() - 1), s);
				}
				else
					dataPoint.setValue(attributes.get(attributes.size() - 1), this.values.indexOf(object.getPropertyValue(property)));
				
				dataPoint.setDataset(trainData);
				trainData.add(dataPoint);
			}
		}
		
		//now do PCA and/or feature selection (probably shouldn't do both)
		if(doPCA)
		{
			Utility.debugPrintln("computing Principal Components");
			pca = new PrincipalComponents();
			pca.setVarianceCovered(PCA_VARIANCE_COVERED);
			try {
				pca.buildEvaluator(trainData);
				trainData = pca.transformedData(trainData);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			Utility.debugPrintln("done computing Principal Components");
		}
		
		if(doFeatureSelection)
		{
			Utility.debugPrintln("computing Feature Selection");
			if(this.values == null)
			{
				as = ClassificationDiffComparator.doRankedAttributeSelection(trainData, 
						new GainRatioAttributeEval(), FEATURE_SELECTION_GAIN_THRESHOLD);
			}
			else
			{
				as = ClassificationDiffComparator.doRankedAttributeSelection(trainData, 
						new ReliefFAttributeEval(), FEATURE_SELECTION_GAIN_THRESHOLD);
			}
			
			try {
				trainData = as.reduceDimensionality(trainData);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			Utility.debugPrintln("done computing Features Selection");
		}
		
		try {
			classifier.buildClassifier(trainData);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private double[] combineFeatures(MatrixEntry object, int exec, Set<Context> contexts)
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

	@Override
	public double compare(MatrixEntry obj1, MatrixEntry obj2) {
		return this.compare(obj1, testExecution, obj2, testExecution);
	}
	
	public double compare(MatrixEntry obj1, int exec1, MatrixEntry obj2, int exec2) {
		Instance dataPoint1 = new DenseInstance(attributes.size());
		Instance dataPoint2 = new DenseInstance(attributes.size());
		
		double[] fs1 = this.combineFeatures(obj1, exec1, contexts);
		double[] fs2 = this.combineFeatures(obj2, exec2, contexts);
		
		
		for(int i = 0; i < fs1.length; i++)
		{
			dataPoint1.setValue(attributes.get(i), fs1[i]);
			dataPoint2.setValue(attributes.get(i), fs2[i]);
		}
		
		Instances testData = new Instances("testData", attributes, 2);
		dataPoint1.setDataset(testData);
		testData.add(dataPoint1);
		dataPoint2.setDataset(testData);
		testData.add(dataPoint2);
		testData.setClassIndex(this.attributes.size() - 1);
		
		if(doPCA)
		{
			try {
//				testData = pca.transformedData(testData);
				dataPoint1 = as.reduceDimensionality(dataPoint1);
				dataPoint2 = as.reduceDimensionality(dataPoint2);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		if(doFeatureSelection)
		{
			try {
//				testData = as.reduceDimensionality(testData);
				dataPoint1 = as.reduceDimensionality(dataPoint1);
				dataPoint2 = as.reduceDimensionality(dataPoint2);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		try {
			if(this.values == null)
			{
				double[] prob1 = classifier.distributionForInstance(dataPoint1);
				double[] prob2 = classifier.distributionForInstance(dataPoint2);
	
				int max1 = 0;
				int max2 = 0;
				for(int i = 0; i < prob1.length; i++)
				{
					if(prob1[i] > prob1[max1])
						max1 = i;
					if(prob2[i] > prob2[max2])
						max2 = i;
				}
				
				if(max1 == max2)
					return 0.0;
				else
					return 1.0;
			}
			else
			{
				double index1 = classifier.classifyInstance(dataPoint1);
				double index2 = classifier.classifyInstance(dataPoint2);
				//we want the range to be [0,1], so first get it to be [-1,1] then shift up by 1 and divide by 2
				return (1.0 + 1.0*(index2 - index1)/(values.size() - 1))/2.0;
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
	
	private static AttributeSelection doRankedAttributeSelection(Instances data, ASEvaluation eval, double threshold)
	{
            AttributeSelection attsel = new AttributeSelection();

            Ranker search = new Ranker();
            search.setThreshold(threshold);
            attsel.setRanking(true);

            //perform attribute selection
            attsel.setEvaluator(eval);
            attsel.setSearch(search);
            try {
				attsel.SelectAttributes(data);
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}

            return attsel;
    }

}
