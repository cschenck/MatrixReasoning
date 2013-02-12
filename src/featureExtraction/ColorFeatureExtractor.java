package featureExtraction;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import featureExtraction.backgroundSubtraction.BackgroundSubtraction;
import featureExtraction.backgroundSubtraction.ROIBackgroundSubtraction;

import matrices.MatrixEntry;

import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.MultiThreadRunner;
import utility.RunningMean;
import utility.MultiThreadRunner.MultiThreadRunnable;
import utility.Utility;

public class ColorFeatureExtractor extends FeatureExtractor {
	
	private static final int NUM_TRAIN_IMAGES = 200;
	private static final int NUM_HEIGHT_BINS = 8;
	private static final int NUM_WIDTH_BINS = 8;
	private static final int NUM_TIME_BINS = 1;
	private static final int NUM_THREADS = 7; //my computer has 8 cores, leave 1 for garbage collector and other stuff

	private BinningFeatureExtractor binner = new BinningFeatureExtractor(NUM_HEIGHT_BINS, NUM_WIDTH_BINS);
	private Map<Integer, BackgroundSubtraction> subtractor = null;
	private Random rand;
	
	public ColorFeatureExtractor(String dataPath, String featurePath, Random rand) {
		super(dataPath, featurePath);
		
		this.rand = rand;
	}
	
	private void initializeBackgroundSubtractor(Behavior b) throws IOException
	{
		//TODO debug select one
//		trainOnRandomImages();
		trainOnRandomNoObjectImages();
//		trainOnRandomBehaviorSpecificImages(b);
//		trainOnNoObjectBehaviorSpecificImages(b);
//		trainOnNoObjectBehaviorExecutionSeperatedImages(b);
	}
	
	private BackgroundSubtraction initializeBackgroundSubtractor(int width, int height, int id)
	{
		//return new BackgroundSubtraction(width, height, id);
		return new ROIBackgroundSubtraction(width, height, id);
	}

	private void trainOnNoObjectBehaviorExecutionSeperatedImages(Behavior b)
			throws IOException {
		Utility.debugPrintln("Selecting training images");
		//get all images for no_object for this behavior and train only on images for one execution
		Map<Integer, List<String>> trainImages = new HashMap<Integer, List<String>>();
		for(File execution : new File(this.getDataPath() + "/no_object/trial_1").listFiles())
		{
			//parse out the execution number
			int execNum = Integer.parseInt(execution.getName().substring(execution.getName().lastIndexOf("_") + 1)) - 1;
			if(trainImages.get(execNum) == null)
				trainImages.put(execNum, new ArrayList<String>());
			
			for(File image : new File(execution.getAbsolutePath() + "/" + b.toString() + "/vision").listFiles())
				trainImages.get(execNum).add(image.getAbsolutePath());
		}
		
		Utility.debugPrintln("Training background model");
		Image img = ImageIO.read(new File(trainImages.values().iterator().next().get(0))); //this, never do this
		this.subtractor = new HashMap<Integer, BackgroundSubtraction>();
		for(Entry<Integer, List<String>> e : trainImages.entrySet())
		{
			this.subtractor.put(e.getKey(), initializeBackgroundSubtractor(img.getWidth(null), img.getHeight(null), e.getKey()));
			this.subtractor.get(e.getKey()).trainBackgroundModel(e.getValue());
		}
		Utility.debugPrintln("Done training background model");
	}
	
	private void trainOnNoObjectBehaviorSpecificImages(Behavior b)
			throws IOException {
		Utility.debugPrintln("Selecting training images");
		//get all images for no_object for this behavior and train only on images for one execution
		List<String> trainImages = new ArrayList<String>();
		for(File execution : new File(this.getDataPath() + "/no_object/trial_1").listFiles())
		{
			for(File image : new File(execution.getAbsolutePath() + "/" + b.toString() + "/vision").listFiles())
				trainImages.add(image.getAbsolutePath());
		}
		
		Utility.debugPrintln("Training background model");
		Image img = ImageIO.read(new File(trainImages.get(0))); //this, never do this
		this.subtractor = new HashMap<Integer, BackgroundSubtraction>();
		BackgroundSubtraction sub = initializeBackgroundSubtractor(img.getWidth(null), img.getHeight(null), 0);
		sub.trainBackgroundModel(trainImages);
		for(int i = 0; i < 100; i++)
		{
			this.subtractor.put(i, sub);
		}
		Utility.debugPrintln("Done training background model");
	}

	private void trainOnRandomImages() throws IOException {
		Utility.debugPrintln("Selecting training images");
		//randomly select images
		List<String> trainImages = new ArrayList<String>();
		while(trainImages.size() < NUM_TRAIN_IMAGES)
		{
			String[] files = new File(this.getDataPath()).list();
			String path = this.getDataPath() + "/" + files[(int) (Math.random()*files.length)] + "/trial_1";
			//String path = this.getDataPath() + "/no_object/trial_1";
			
			files = new File(path).list();
			path += "/" + files[(int) (rand.nextDouble()*files.length)];
			
			files = new File(path).list();
			path += "/" + files[(int) (rand.nextDouble()*files.length)] + "/vision";
			//path += "/" + b.toString() + "/vision";
			
			files = new File(path).list();
			path += "/" + files[(int) (rand.nextDouble()*files.length)];
			
			if(!trainImages.contains(path))
				trainImages.add(path);
		}
		Utility.debugPrintln("Training background model");
		Image img = ImageIO.read(new File(trainImages.get(0)));
		BackgroundSubtraction sub = initializeBackgroundSubtractor(img.getWidth(null), img.getHeight(null), 0);
		sub.trainBackgroundModel(trainImages);
		
		this.subtractor = new HashMap<Integer, BackgroundSubtraction>();
		for(int i = 0; i < 100; i++)
			this.subtractor.put(i, sub); //just use the same background subtractor for all
		
		Utility.debugPrintln("Done training background model");
	}
	
	private void trainOnRandomNoObjectImages() throws IOException {
		Utility.debugPrintln("Selecting training images");
		//randomly select images
		List<String> trainImages = new ArrayList<String>();
		while(trainImages.size() < NUM_TRAIN_IMAGES)
		{
			String[] files = new File(this.getDataPath()).list();
			//String path = this.getDataPath() + "/" + files[(int) (Math.random()*files.length)] + "/trial_1";
			String path = this.getDataPath() + "/no_object/trial_1";
			
			files = new File(path).list();
			path += "/" + files[(int) (rand.nextDouble()*files.length)];
			
			files = new File(path).list();
			path += "/" + files[(int) (rand.nextDouble()*files.length)] + "/vision";
			//path += "/" + b.toString() + "/vision";
			
			files = new File(path).list();
			path += "/" + files[(int) (rand.nextDouble()*files.length)];
			
			if(!trainImages.contains(path))
				trainImages.add(path);
		}
		Utility.debugPrintln("Training background model");
		Image img = ImageIO.read(new File(trainImages.get(0)));
		BackgroundSubtraction sub = initializeBackgroundSubtractor(img.getWidth(null), img.getHeight(null), 0);
		sub.trainBackgroundModel(trainImages);
		
		this.subtractor = new HashMap<Integer, BackgroundSubtraction>();
		for(int i = 0; i < 100; i++)
			this.subtractor.put(i, sub); //just use the same background subtractor for all
		
		Utility.debugPrintln("Done training background model");
	}
	
	private void trainOnRandomBehaviorSpecificImages(Behavior b) throws IOException {
		Utility.debugPrintln("Selecting training images");
		//randomly select images
		List<String> trainImages = new ArrayList<String>();
		while(trainImages.size() < NUM_TRAIN_IMAGES)
		{
			String[] files = new File(this.getDataPath()).list();
			String path = this.getDataPath() + "/" + files[(int) (Math.random()*files.length)] + "/trial_1";
			//String path = this.getDataPath() + "/no_object/trial_1";
			
			files = new File(path).list();
			path += "/" + files[(int) (rand.nextDouble()*files.length)];
			
			files = new File(path).list();
			//path += "/" + files[(int) (rand.nextDouble()*files.length)] + "/vision";
			path += "/" + b.toString() + "/vision";
			
			files = new File(path).list();
			path += "/" + files[(int) (rand.nextDouble()*files.length)];
			
			if(!trainImages.contains(path))
				trainImages.add(path);
		}
		Utility.debugPrintln("Training background model");
		Image img = ImageIO.read(new File(trainImages.get(0)));
		BackgroundSubtraction sub = initializeBackgroundSubtractor(img.getWidth(null), img.getHeight(null), 0);
		sub.trainBackgroundModel(trainImages);
		
		this.subtractor = new HashMap<Integer, BackgroundSubtraction>();
		for(int i = 0; i < 100; i++)
			this.subtractor.put(i, sub); //just use the same background subtractor for all
		
		Utility.debugPrintln("Done training background model");
	}

	@Override
	protected Modality getModality() {
		return Modality.color;
	}

	@Override
	protected Map<String, List<double[]>> generateFeatures(Context c) throws IOException {
		//initialize the background subtractor if it isn't already
//		if(subtractor == null)
//			initializeBackgroundSubtractor();
		initializeBackgroundSubtractor(c.behavior);
		
		//next build the list of jobs
		Utility.debugPrintln("Building list of jobs for " + c.toString());
		
		//why use a set instead of a queue? so that the jobs aren't processed in the order they are placed in the set in
		//which should help prevent thread collisions. Why not just randomize the order of the list? Hashsets are a bit
		//more deterministic than that, but still effectively random
		final Set<ImageProcessingJob> jobSet = new HashSet<ColorFeatureExtractor.ImageProcessingJob>();
		
		//while we're at it, make a spot for the results so that we can store them faster
		Map<String, List<double[]>> results = new HashMap<String, List<double[]>>();
		
		for(File object : new File(this.getDataPath()).listFiles())
		{
			results.put(object.getName(), new ArrayList<double[]>());
			
			for(File execution : new File(object.getAbsolutePath() + "/trial_1").listFiles())
			{
				//read the execution number off the execution file name, and subtract 1 to make it 0-based
				int execNum = Integer.parseInt(execution.getName().substring(execution.getName().lastIndexOf("_") + 1)) - 1;
				
				//make a spot in the results for this data
				while(results.get(object.getName()).size() <= execNum)
					results.get(object.getName()).add(null);
				results.get(object.getName()).set(execNum, new double[NUM_HEIGHT_BINS*NUM_TIME_BINS*NUM_WIDTH_BINS*3]);
				
				//make sure there's a place to save the background subtracted images if we choose to
				if(!(new File(execution.getAbsolutePath() + "/" + c.behavior.toString() + "/background_subtracted").exists()))
					new File(execution.getAbsolutePath() + "/" + c.behavior.toString() + "/background_subtracted").mkdir();
				
				File dataFolder = new File(execution.getAbsolutePath() + "/" + c.behavior.toString() + "/vision");
				int totalNumImages = dataFolder.list().length;
				for(File image : dataFolder.listFiles())
				{
					//just like above, read the image number off the image name, but it is already 0-based, and don't forget to remove '.jpg'
					int imageNum = Integer.parseInt(image.getName().substring(
							image.getName().lastIndexOf("_") + 1, image.getName().lastIndexOf(".jpg")));
					
					//now make the job and put it in the queue
					ImageProcessingJob job = new ImageProcessingJob(object.getName(), execNum, c.behavior, imageNum, totalNumImages);
					jobSet.add(job);
				}
			}
		}
		
		//this is just a wrapper class around the hash set to provide queue functionality
		Queue<ImageProcessingJob> jobs = new Queue<ColorFeatureExtractor.ImageProcessingJob>() {
			public <T> T[] toArray(T[] arg0) {throw new UnsupportedOperationException();}
			public Object[] toArray() {throw new UnsupportedOperationException();}
			
			public int size() {return jobSet.size();}
			
			public boolean retainAll(Collection<?> arg0) {throw new UnsupportedOperationException();}
			public boolean removeAll(Collection<?> arg0) {throw new UnsupportedOperationException();}
			public boolean remove(Object arg0) {throw new UnsupportedOperationException();}
			public Iterator<ImageProcessingJob> iterator() {throw new UnsupportedOperationException();}
			
			public boolean isEmpty() {return jobSet.isEmpty();}
			
			public boolean containsAll(Collection<?> arg0) {throw new UnsupportedOperationException();}
			public boolean contains(Object arg0) {throw new UnsupportedOperationException();}
			
			public void clear() {jobSet.clear();}
			
			public boolean addAll(Collection<? extends ImageProcessingJob> arg0) {throw new UnsupportedOperationException();}
			public ImageProcessingJob remove() {throw new UnsupportedOperationException();}
			
			@Override
			public ImageProcessingJob poll() {
				if(!this.isEmpty())
				{
					ImageProcessingJob ret = jobSet.iterator().next();
					jobSet.remove(ret);
					return ret;
				}
				else
					return null;
			}
			
			public ImageProcessingJob peek() {throw new UnsupportedOperationException();}
			public boolean offer(ImageProcessingJob arg0) {throw new UnsupportedOperationException();}
			public ImageProcessingJob element() {throw new UnsupportedOperationException();}
			public boolean add(ImageProcessingJob arg0) {throw new UnsupportedOperationException();}
		};
		
		//now set up the image processors, one for each thread
		List<MultiThreadRunnable> processors = new ArrayList<MultiThreadRunnable>();
		for(int i = 0; i < NUM_THREADS; i++)
			processors.add(new ImageProcessor(jobs, results));
		
		//now lets run the processors and process the jobs
		MultiThreadRunner runner = new MultiThreadRunner(processors, NUM_THREADS);
		runner.startThreads();
		
		Utility.debugPrintln("Removing identical features");
		//let's do some post-processing, if there are any features that are identical for all objects
		//across all trials, let's remove that feature. This is to get rid of image patches over all
		//background (all black)
		Map<String, List<List<Double>>> tempResults = new HashMap<String, List<List<Double>>>();
		int featureLength = results.values().iterator().next().get(0).length;
		for(int i = 0; i < featureLength; i++)
		{
			//first check to see if the i'th feature is identical over all objects/exeuctions
			double value = results.values().iterator().next().get(0)[i];
			boolean foundDiff = false;
			for(List<double[]> list : results.values())
			{
				for(double[] dd : list)
				{
					if(dd[i] != value)
					{
						foundDiff = true;
						break;
					}
				}
				if(foundDiff)
					break;
			}
			
			//if a difference was found, keep the feature
			if(foundDiff)
			{
				for(Entry<String, List<double[]>> e : results.entrySet())
				{
					if(tempResults.get(e.getKey()) == null)
						tempResults.put(e.getKey(), new ArrayList<List<Double>>());
					for(int j = 0; j < e.getValue().size(); j++)
					{
						while(tempResults.get(e.getKey()).size() <= j)
							tempResults.get(e.getKey()).add(new ArrayList<Double>());
						double d = e.getValue().get(j)[i];
						tempResults.get(e.getKey()).get(j).add(d);
					}
				}
			}
		}
		
		//now put the features back in the results map
		results.clear();
		for(Entry<String, List<List<Double>>> e : tempResults.entrySet())
		{
			List<double[]> list = new ArrayList<double[]>();
			for(List<Double> dList : e.getValue())
			{
				double[] dd = new double[dList.size()];
				for(int i = 0; i < dd.length; i++)
					dd[i] = dList.get(i);
				list.add(dd);
			}
			results.put(e.getKey(), list);
		}
		
		Utility.debugPrintln("Done generating color features for " + c.toString());
		
		return results;
	}
	
	private class ImageProcessingJob
	{
		public final String objectName;
		public final int execution;
		public final Behavior behavior;
		public final int imageNum;
		public final int totalNumImages;
		
		public ImageProcessingJob(String objectName, int execution, Behavior behavior, int imageNum, int totalNumImages)
		{
			this.objectName = objectName;
			this.execution = execution;
			this.behavior = behavior;
			this.imageNum = imageNum;
			this.totalNumImages = totalNumImages;
		}
		
		public String getFilePath()
		{
			//construct the file path from what we know about this image job and the data file structure
			return ColorFeatureExtractor.this.getDataPath() + "/" + this.objectName + "/trial_1/exec_" 
				+ (this.execution + 1) + "/" + this.behavior.toString() + "/vision/vision_" + this.imageNum
				+ ".jpg";
		}

		public String getSavePath() {
			return ColorFeatureExtractor.this.getDataPath() + "/" + this.objectName + "/trial_1/exec_" 
					+ (this.execution + 1) + "/" + this.behavior.toString() + "/background_subtracted/vision_" + this.imageNum
					+ ".jpg";
		}
	}
	
	private class ImageProcessor implements MultiThreadRunnable
	{
		private Queue<ImageProcessingJob> jobs;
		private Map<String, List<double[]>> results;
		private ImageProcessingJob currentJob;
		private int initialQueueSize;
		private int currentQueueSize;
		
		public ImageProcessor(Queue<ImageProcessingJob> jobs, Map<String, List<double[]>> results)
		{
			this.jobs = jobs;
			this.results = results;
			initialQueueSize = this.jobs.size();
		}
		
		@Override
		public void run() {
			while(true)
			{
				//pop the next job off the queue
				synchronized(jobs) {
					if(jobs.isEmpty())
						break;
					else
						currentJob = jobs.poll();
					currentQueueSize = jobs.size();
				}
				
				//TODO debug
//				if(!debugObjectNames.contains(currentJob.objectName) || currentJob.imageNum != 0)
//					continue;
				
				//now process the job
				//first thing to do is load it from a file
				BufferedImage image = null;
				try {
					image = ImageIO.read(new File(currentJob.getFilePath()));
				} catch (IOException e) {
					e.printStackTrace();
					Utility.debugPrintln("Unable to load " + currentJob.getFilePath());
					Utility.debugPrintln("Unrecoverable error, exiting");
					jobs.clear();
					break;
				}
				
				//next subtract the background
//				BufferedImage image2 = image; //TODO debug
				image = subtractor.get(currentJob.execution).subtractBackground(image);
				
//				//TODO debug save the image
//				if(debugObjectNames.contains(currentJob.objectName))
//				{
//					try {
//						//ImageIO.write(image, "jpg", new File(currentJob.getSavePath()));
//						ImageIO.write(image, "jpg", new File("backgroundSubtractedImages/" + currentJob.objectName 
//								+ "_" + currentJob.behavior.toString() + "_exec_" + (currentJob.execution + 1) + ".jpg"));
//						//now make a heat map of this image
//						BufferedImage heatMap = new BufferedImage(image.getWidth()*4, image.getHeight(), image.getType());
//						for(int color = 0; color < 4; color++)
//						{
//							for(int i = 0; i < image.getWidth(); i++)
//							{
//								for(int j = 0; j < image.getHeight(); j++)
//								{
//									double diff = 0;
//									if(color == 0)
//									{
//										diff = Math.abs(subtractor.get(currentJob.execution).getAverageRedValue(i, j) - 
//												new Color(image2.getRGB(i, j)).getRed())
//												/subtractor.get(currentJob.execution).getRedStdDev(i, j);
//									}
//									else if(color == 1)
//									{
//										diff = Math.abs(subtractor.get(currentJob.execution).getAverageGreenValue(i, j) - 
//												new Color(image2.getRGB(i, j)).getGreen())
//												/subtractor.get(currentJob.execution).getGreenStdDev(i, j);
//									}
//									else if(color == 2)
//									{
//										diff = Math.abs(subtractor.get(currentJob.execution).getAverageBlueValue(i, j) - 
//												new Color(image2.getRGB(i, j)).getBlue())
//												/subtractor.get(currentJob.execution).getBlueStdDev(i, j);
//									}
//									else
//									{
//										double redDiff = Math.abs(subtractor.get(currentJob.execution).getAverageRedValue(i, j) - 
//												new Color(image2.getRGB(i, j)).getRed())
//												/subtractor.get(currentJob.execution).getRedStdDev(i, j);
//										
//										double greenDiff = Math.abs(subtractor.get(currentJob.execution).getAverageGreenValue(i, j) - 
//												new Color(image2.getRGB(i, j)).getGreen())
//												/subtractor.get(currentJob.execution).getGreenStdDev(i, j);
//										
//										double blueDiff = Math.abs(subtractor.get(currentJob.execution).getAverageBlueValue(i, j) - 
//												new Color(image2.getRGB(i, j)).getBlue())
//												/subtractor.get(currentJob.execution).getBlueStdDev(i, j);
//										diff = Math.max(Math.max(redDiff, greenDiff), blueDiff);
//									}
//									diff = diff*150;
//									if(diff > 255)
//										diff = 255;
//									heatMap.setRGB(color*image.getWidth() + i, j, new Color((int)diff, (int)diff, (int)diff).getRGB());
//								}
//							}
//						}
//						
//						ImageIO.write(heatMap, "jpg", new File("backgroundSubtractedImages/" + currentJob.objectName 
//								+ "_" + currentJob.behavior.toString() + "_exec_" + (currentJob.execution + 1) + "_heatMap.jpg"));
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
				
				//next extract the histogram features
				double[] features = extractFeatures(image);
				
				//finally add the features to the feature vector for this object/execution/behavior
				double[] dd = this.results.get(currentJob.objectName).get(currentJob.execution);
				synchronized(dd) { //gotta be thread safe
					dd = addFeatures(dd, features, currentJob);
					this.results.get(currentJob.objectName).set(currentJob.execution, dd);
				}
			}
			currentQueueSize = 0;
		}

		private double[] addFeatures(double[] dd, double[] features, ImageProcessingJob job) {
			//the color features (dd) will be layed out as follows:
			//the top level division will be based on color channel, i.e., [red][green][blue]
			//within each color channel the layout will be time-wise, i.e., [first time section]...[last time section] 
			//within each time section the layout will be image row-wise, i.e., [top-left section]...[bottom-right section]
			//The input feature vector to be added (features) will be layed out the same, but since it is from
			//a single frame, there will be only 1 time section
			
			for(int i = 0; i < 3; i++) //cycle through the colors
			{
				//compute the start position based on the color
				int startDD = dd.length/3*i;
				int startFeatures = features.length/3*i;
				
				//next shift the dd start position off by the time section
				//         (length of a time bin     )*(the time bin this image is in                  )
				startDD += (dd.length/3)/NUM_TIME_BINS*(job.imageNum/(job.totalNumImages/NUM_TIME_BINS));
				
				//now add the values from features to the overall feature vector
				for(int j = 0; j < features.length/3; j++)
					dd[startDD+j] += features[startFeatures+j];
			}
			
			
			return dd;
		}

		//this is so that the method below is not constantly allocating and deleting this array
		private double[][] values = null;
		private Lock valuesLock = new ReentrantLock(); //this is to detect multi-threading errors
		
		private double[] extractFeatures(BufferedImage image) {
			
			double[] ret = new double[3*NUM_HEIGHT_BINS*NUM_WIDTH_BINS];
			
			if(valuesLock.tryLock()) //this is so that we don't accidentally have multiple threads running one instance of this class at once
			{					    //which should never happen, but just in case
				if(values == null || values.length != image.getHeight() || values[0].length != image.getWidth())
					values = new double[image.getHeight()][image.getWidth()];
				
				for(int i = 0; i < 3; i++) //iterate over the colors
				{
					//let's iterate over the image and put all the values for this
					//color into the array
					for(int y = 0; y < values.length; y++)
					{
						for(int x = 0; x < values[0].length; x++)
						{
							 /*
							//standard RGB
							if(i == 0)
								values[y][x] = new Color(image.getRGB(x, y)).getRed();
							else if(i == 1)
								values[y][x] = new Color(image.getRGB(x, y)).getGreen();
							else
								values[y][x] = new Color(image.getRGB(x, y)).getBlue();

							/*/
							
							//let's try HSV
							double r =  new Color(image.getRGB(x, y)).getRed()/255.0;
							double g =  new Color(image.getRGB(x, y)).getGreen()/255.0;
							double b =  new Color(image.getRGB(x, y)).getBlue()/255.0;
							double max = Math.max(Math.max(r, g), b);
							double min = Math.min(Math.min(r, g), b);
							if(i == 0)
							{
								if(max == 0)
									values[y][x] = 0;
								else if(max - min == 0)
									values[y][x] = 0;
								else if(r == max)
									values[y][x] = (g - b)/(max - min);
								else if(g == max)
									values[y][x] = 2 + (b - r)/(max - min);
								else
									values[y][x] = 4 + (r - g)/(max - min);
								values[y][x] *= 60;
								if(values[y][x] < 0)
									values[y][x] += 360;
							}
							else if(i == 1)
								values[y][x] = (max == 0 ? 0 : (max -  min)/max)*360;
							else
								values[y][x] = max*360;
							//*/
						}
					}
					
					//now use the binner to extract the features
					double[] features = binner.bin(values);
					
					//now put the features in the return vector
					int start = ret.length/3*i;
					for(int j = 0; j < features.length; j++)
						ret[start+j] = features[j];
					
				}
				valuesLock.unlock();
			}
			else //if we do have another thread running this class instance, then throw an exception
				throw new IllegalStateException("There are multiple threads running this same class instance");
			
			return ret;
		}

		@Override
		public String getStatus() {
			if(currentJob == null)
				return "";
			else
				return currentJob.objectName + " - " + currentJob.execution + " - " + currentJob.behavior.toString()
					+ " - " + currentJob.imageNum + "/" + currentJob.totalNumImages;
		}

		@Override
		public String getTitle() {
			return "(" + currentQueueSize + "/" + initialQueueSize + ")";
		}
		
	}
	
	private static List<String> debugObjectNames = new ArrayList<String>();
	
	public static void main(String[] args) throws IOException {
		//let's test this class
		Random rand = new Random(1);
		FeatureExtractor fe = new ColorFeatureExtractor(FeatureExtractionManager.dataPath, FeatureExtractionManager.featurePath, rand);
		
		Set<Context> contexts = new HashSet<Context>();
		contexts.add(new Context(Behavior.look, Modality.color));
		
		//remove some objects to save time
		List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
//		while(objects.size() > 10)
//			objects.remove(rand.nextInt(objects.size()));
		
//		for(MatrixEntry object : objects)
//			debugObjectNames.add(object.getName());
		
		fe.assignFeatures(objects, contexts);
		
		//now print out the average red, green, and blue value for each object as a sanity check
		for(MatrixEntry object : objects)
		{
			List<double[]> features = object.getFeatures(new Context(Behavior.look, Modality.color));
			RunningMean[] avgs = new RunningMean[3];
			for(int i = 0; i < avgs.length; i++)
				avgs[i] = new RunningMean();
			
			for(double[] d : features)
			{
				for(int i = 0; i < avgs.length; i++)
				{
					int start = d.length/avgs.length*i;
					for(int j = 0; j < d.length/avgs.length; j++)
						avgs[i].addValue(d[start+j]);
				}
			}
			
			System.out.println(String.format("%20s = %15sR, %15sG, %15sB", 
						object.getName(), 
						Utility.doubleToStringWithCommas((int)avgs[0].getMean()),
						Utility.doubleToStringWithCommas((int)avgs[1].getMean()),
						Utility.doubleToStringWithCommas((int)avgs[2].getMean())));
		}
		
//		new File(FeatureExtractionManager.featurePath + "/" + new Context(Behavior.look, Modality.color).toString() + ".txt").delete();
	}

}
