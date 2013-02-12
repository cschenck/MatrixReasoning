package taskSolver.comparisonFunctions;

import java.util.ArrayList;
import java.util.List;

import matrices.MatrixEntry;
import taskSolver.comparisonFunctions.DistanceComparator.DistanceFunction;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ClassifierComparator implements ComparisonFunction {
	
	private static Classifier instantiateClassifier()
	{
		return new SMO();
	}
	
	private Classifier classifier;
	private int testExecution;
	private ArrayList<Attribute> attributes;
	private List<DistanceComparator> distances;
	private String property;
	
	public ClassifierComparator(List<Integer> trainExecutions, int testExecution, 
			List<MatrixEntry> objects, List<DistanceComparator> distances, String property)
	{
		this.testExecution = testExecution;
		this.distances = distances;
		this.property = property;
		
		setUpClassifier(trainExecutions, objects);
	}
	
	private void setUpClassifier(List<Integer> trainExecutions, List<MatrixEntry> objects)
	{
		this.classifier = instantiateClassifier();
		
		attributes = new ArrayList<Attribute>();
		for(int i = 1; i <= distances.size(); i++)
		{
			Attribute attribute = new Attribute("" + i);
			attributes.add(attribute);
		}
		
		List<String> values = new ArrayList<String>();
		values.add("different");
		values.add("same");
		Attribute classAttribute = new Attribute("class", values);
		attributes.add(classAttribute);
		
		int capacity = (int) Math.pow(objects.size()*trainExecutions.size(), 2);
		
		Instances trainData = new Instances("data", attributes, capacity);
		trainData.setClassIndex(attributes.size() - 1);
		
		for(MatrixEntry object1 : objects)
		{
			for(Integer exec1 : trainExecutions)
			{
				for(MatrixEntry object2 : objects)
				{
					if(object1.equals(object2))
						continue;
					for(Integer exec2 : trainExecutions)
					{
						Instance dataPoint = new DenseInstance(attributes.size());
						for(int i = 0; i < distances.size(); i++)
							dataPoint.setValue(attributes.get(i), distances.get(i).compare(object1, exec1, object2, exec2));
						
						String s = "different";
						if(object1.getPropertyValue(property).equals(object2.getPropertyValue(property)))
							s = "same";
						dataPoint.setValue(attributes.get(attributes.size() - 1), s);
						dataPoint.setDataset(trainData);
						trainData.add(dataPoint);
					}
				}
			}
		}
		
		try {
			classifier.buildClassifier(trainData);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double compare(MatrixEntry obj1, MatrixEntry obj2) {
		Instance dataPoint = new DenseInstance(attributes.size());
		for(int i = 0; i < distances.size(); i++)
			dataPoint.setValue(attributes.get(i), distances.get(i).compare(obj1, testExecution, obj2, testExecution));
		
		Instances testData = new Instances("testData", attributes, 1);
		dataPoint.setDataset(testData);
		testData.add(dataPoint);
		
		try {
			double prob = classifier.distributionForInstance(dataPoint)[0];
			if(prob > 0.5)
				return 1.0;
			else
				return 0.0;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
