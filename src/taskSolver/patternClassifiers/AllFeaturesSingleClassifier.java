package taskSolver.patternClassifiers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import matrices.MatrixEntry;
import taskSolver.comparisonFunctions.ClassificationDiffComparator;
import utility.Context;
import utility.Utility;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class AllFeaturesSingleClassifier implements PatternClassifier {
	
	private static boolean DO_FEATURE_SELECTION = false;
	private static boolean DO_PCA = false;
	private static double PCA_VARIANCE_COVERED = 0.95;
	private static double FEATURE_SELECTION_GAIN_THRESHOLD = 0.075;
	
	private static Classifier instantiateClassifier()
	{
//		return new J48();
		return new SMO();
	}
	
	private int testExecution;
	private List<Integer> trainExecutions;
	private Set<Context> contexts;
	private Random rand;
	
	private Classifier classifier;
	private ArrayList<Attribute> attributes;
	private AttributeSelection as;
	private PrincipalComponents pca;
	
	public AllFeaturesSingleClassifier(List<Integer> trainExecutions, int testExecution, Set<Context> contexts, Random rand)
	{
		this.trainExecutions = trainExecutions;
		this.testExecution = testExecution;
		this.contexts = contexts;
		this.rand = rand;
	}
	
	private double[] combineFeatures(List<MatrixEntry> list, int exec, Set<Context> contexts)
	{
		List<Double> features = new ArrayList<Double>();
		for(Context c : contexts)
		{
			for(MatrixEntry obj : list)
			{
				for(double d : obj.getFeatures(c).get(exec))
					features.add(d);
			}
		}
		
		double[] ret = new double[features.size()];
		for(int i = 0; i < ret.length; i++)
			ret[i] = features.get(i);
		
		return ret;
	}

	@Override
	public void trainClassifier(Set<List<MatrixEntry>> positiveExamples, Set<List<MatrixEntry>> negativeExamples) {
		this.classifier = instantiateClassifier();
		
		attributes = new ArrayList<Attribute>();
		int featureLength = this.combineFeatures(positiveExamples.iterator().next(), 0, contexts).length;
		for(int i = 1; i <= featureLength; i++)
		{
			Attribute attribute = new Attribute("" + i);
			attributes.add(attribute);
		}
		
		List<String> values = new ArrayList<String>();
		values.add("positive");
		values.add("negative");
		Attribute classAttribute = new Attribute("class", values);
		attributes.add(classAttribute);
		
		int capacity = (positiveExamples.size() + negativeExamples.size())*trainExecutions.size();
		
		Instances trainData = new Instances("data", attributes, capacity);
		trainData.setClassIndex(attributes.size() - 1);
		
		Iterator<List<MatrixEntry>> positive = positiveExamples.iterator();
		Iterator<List<MatrixEntry>> negative = negativeExamples.iterator();
		while(positive.hasNext() || negative.hasNext())
		{
			List<MatrixEntry> list = null;
			String value = "";
			if(!positive.hasNext()) //if there aren't any more positive examples, get a negative one
			{
				list = negative.next();
				value = "negative";
			}
			else if(!negative.hasNext())//if there aren't any more negative examples, get a positive one
			{
				list = positive.next();
				value = "positive";
			}
			else if(rand.nextBoolean()) //if there are both, flip a coin and pick one
			{
				list = positive.next();
				value = "positive";
			}
			else
			{
				list = negative.next();
				value = "negative";
			}
				
			for(Integer exec : trainExecutions)
			{
				Instance dataPoint = new DenseInstance(attributes.size());
				double[] fs = this.combineFeatures(list, exec, contexts);
				for(int i = 0; i < fs.length; i++)
					dataPoint.setValue(attributes.get(i), fs[i]);
				
				dataPoint.setValue(attributes.get(attributes.size() - 1), value);
				dataPoint.setDataset(trainData);
				trainData.add(dataPoint);
			}
		}
		
		//now do PCA and/or feature selection (probably shouldn't do both)
		if(DO_PCA)
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
		
		if(DO_FEATURE_SELECTION)
		{
			Utility.debugPrintln("computing Feature Selection");
			as = AllFeaturesSingleClassifier.doRankedAttributeSelection(trainData, 
					new GainRatioAttributeEval(), FEATURE_SELECTION_GAIN_THRESHOLD);
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

	@Override
	public double classifyPattern(List<MatrixEntry> list) {
		Instance dataPoint = new DenseInstance(attributes.size());
		
		double[] fs = this.combineFeatures(list, testExecution, contexts);
		
		for(int i = 0; i < fs.length; i++)
		{
			dataPoint.setValue(attributes.get(i), fs[i]);
		}
		
		Instances testData = new Instances("testData", attributes, 1);
		dataPoint.setDataset(testData);
		testData.add(dataPoint);
		testData.setClassIndex(this.attributes.size() - 1);
		
		if(DO_PCA)
		{
			try {
				testData = pca.transformedData(testData);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		if(DO_FEATURE_SELECTION)
		{
			try {
				testData = as.reduceDimensionality(testData);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		try {
			return this.classifier.distributionForInstance(dataPoint)[0];			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset() {
		classifier = null;
		attributes = null;
		as = null;
		pca = null;
	}
	
	private static AttributeSelection doRankedAttributeSelection(Instances data, GainRatioAttributeEval eval, double threshold)
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
