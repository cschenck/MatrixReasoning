package featureExtraction.backgroundSubtraction;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;

public class ClassifierBackgroundSubtraction extends BackgroundSubtraction {
	
	private int width;
	private int height;
	
	private Classifier[][] classifiers;

	public ClassifierBackgroundSubtraction(int width, int height) {
		super(width, height, 0);
		
		this.width = width;
		this.height = height;
		
		classifiers = new Classifier[width][height];
		for(int i = 0; i < width; i++)
		{
			for(int j = 0; j < height; j++)
			{
				classifiers[i][j] = initializeClassifier(i, j);
			}
		}
	}
	
	private Classifier initializeClassifier(int i, int j) {
		return new SMO();
	}

	private Set<Pixel> getClassificationNeighborhood(Pixel p)
	{
		int neighborhoodSize = 5; //must be an odd number, covers a size x size square
		
		Set<Pixel> ret = new HashSet<BackgroundSubtraction.Pixel>();
		for(int i = p.i - (neighborhoodSize - 1)/2; i < p.i + (neighborhoodSize - 1)/2; i++)
		{
			for(int j = p.j - (neighborhoodSize - 1)/2; j < p.j + (neighborhoodSize - 1)/2; j++)
			{
				if(i >= 0 && i < width && j >= 0 && j < height) //if this neighbor is in the bounds of the image
					ret.add(this.pixels[i][j]);
			}
		}
		
		return ret;
	}
	
	@Override
	public void trainBackgroundModel(List<String> imagePaths) throws IOException
	{
		super.trainBackgroundModel(imagePaths);
		
		//this is going to be annoying and take forever
		//MultiThreadRunner to the RESCUE!!!
		//Here's the problem: the training data for the classifiers is all of the pixels
		//in all of the training images. We don't have enough memory to keep all of the
		//training images loaded at once, so we'll have to load them one at a time,
		//extract the training data, and then unload that image and move to the next. We
		//can't extract all the training data, though, because that would be the entire
		//image, so we can only extract some of it. After we finish with all the images, we
		//can then train the classifiers we extracted the data for. We'll then have to
		//repeat this entire process until we have each classifier trained. This method
		//will not work for lazy classifiers (e.g., classifiers that retain more than a
		//constant amount of data with respect to the number of training points).
		//The main bottleneck to this is the File I/O, but training the classifier itself
		//takes a non-trivial amount of time. There's nothing we can do about the File
		//I/O, but we can multithread the training of the classifiers. 
		//
		//Here's the plan: Make one thread called "File I/O" that will load each training
		//image in order, extract the training data for X classifiers, and when it is done
		//it will create X Instances objects (from Weka) and put them in a queue. There
		//will be X "classifier training" threads that will then each pull one Instances
		//object from the queue and train the corresponding classifier. The "classifier
		//training" threads will wait until the queue has objects and then process them,
		//waiting once again, etc. until all classifiers are trained. The "File I/O"
		//thread will wait while the queue has >= X objects in it (just in case the training
		//takes longer than than the File I/O, which it almost certainly won't) and
		//then load X Instances objects when the wait stop condition is met. It will
		//also terminate when all classifiers have been trained. This will require at most
		//O(X*M) space where X is the number of classifiers and M is the number of training
		//images.
	}
	

}
