package experiment;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import matrices.Matrix;
import matrices.MatrixEntry;
import matrices.MatrixGenerator;
import matrices.patterns.DecrementPattern;
import matrices.patterns.DifferentPattern;
import matrices.patterns.IncrementPattern;
import matrices.patterns.ORMetaPattern;
import matrices.patterns.OneSameOneDifferentPattern;
import matrices.patterns.Pattern;
import matrices.patterns.SamePattern;
import matrices.patterns.XORMetaPattern;
import taskSolver.comparisonFunctions.ComparisonFunction;
import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.MultiThreadRunner;
import utility.MultiThreadRunner.MultiThreadRunnable;
import utility.Utility;
import experiment.scoredChangeExps.ScoredChangeExp;
import experiment.scoredChangeExps.ScoredChangeExpClustering;
import experiment.scoredChangeExps.ScoredChangeExpGDescentClustering;
import experiment.scoredChangeExps.depricated.ScoredChangeExpWeightedClustering;
import featureExtraction.FeatureExtractionManager;
import featureExtraction.backgroundSubtraction.BackgroundSubtraction;

public class Main {

	private final static Map<String, List<String>> ORDERED_PROPERTIES = DEFINE_PROPERTIES(); 
	
	private static Map<String, List<String>> DEFINE_PROPERTIES()
	{
		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		
		//weight, with light, medium, heavy
		List<String> values = new ArrayList<String>();
		values.add("light"); values.add("medium"); values.add("heavy");
		ret.put("weight", values);
		
		return ret;
	}
	
	public static final String objectsFile = "objects.txt";
	public static final String logFile = "logs/log.txt";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		loadObjectsFile();
//		testBackgroundSubtraction();
		
//		ScoredChangeExp exp = new ScoredChangeExp(objectsFile);
//		ScoredChangeExpBoosting exp = new ScoredChangeExpBoosting(objectsFile);
//		ScoredChangeExpTaskEval exp = new ScoredChangeExpTaskEval(objectsFile);
//		ScoredChangeExpClassDiff exp = new ScoredChangeExpClassDiff(objectsFile);
//		ScoredChangeExpClustering exp = new ScoredChangeExpClustering(initializeObjects(objectsFile));
//		ScoredChangeExpWeightedClustering exp = new ScoredChangeExpWeightedClustering(objectsFile);
//		ScoredChangeExpTopKClustering exp = new ScoredChangeExpTopKClustering(objectsFile);
//		ScoredChangeExpBoostedClustering exp = new ScoredChangeExpBoostedClustering(objectsFile);
//		ScoredChangeExpGDescentClustering exp = 
//				new ScoredChangeExpGDescentClustering(initializeObjects(objectsFile, new Random(1)), getAllContexts());
//		System.out.println(exp.runExperiment(new ArrayList<Context>(getAllContexts())));
		
		runExperiments();
	}
	
	private static void runExperiments()
	{
		Random rand = new Random(1);
		List<MatrixEntry> objects = initializeObjects(objectsFile, rand);
//		List<Experiment> exps = new ArrayList<Experiment>();
//		exps.add(new ScoredChangeExp(objects, getAllContexts()));
//		exps.add(new ScoredChangeExpClustering(objects, getAllContexts()));
//		exps.add(new ScoredChangeExpGDescentClustering(objects, getAllContexts()));
//		
//		ExperimentController ec = new ExperimentController(exps, getAllContexts(), rand);
//		ec.runExperiments();
	}
	
	private static List<MatrixEntry> initializeObjects(String objectFilepath, Random rand)
	{
		try {
			List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile(objectFilepath);
			FeatureExtractionManager feManager = new FeatureExtractionManager(rand);
			feManager.assignFeatures(objects, getAllContexts());
			return objects;
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("The objects file was not found at " + objectFilepath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Set<Context> getAllContexts() {
		Set<Context> contexts = new HashSet<Context>();
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
	
	private static void loadObjectsFile() throws FileNotFoundException
	{
		List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile("objects.txt");
		
		for(MatrixEntry obj : objects)
			System.out.println(obj.toString());
		System.out.println(objects.size());
		
		Random rand = new Random(1);
		
		Set<Pattern> rowPatterns = new HashSet<Pattern>();
		Set<Pattern> colPatterns = new HashSet<Pattern>();
		Map<Pattern, Boolean> validPatterns = new HashMap<Pattern, Boolean>();
		
		intializePatterns(objects.get(0).getDefinedProperties(), new HashSet<MatrixEntry>(objects),
				rand, rowPatterns, colPatterns, validPatterns);
		
		List<Matrix> matrices = new ArrayList<Matrix>(MatrixGenerator.generateMatrix(objects, rowPatterns, colPatterns, validPatterns, 50, rand));
		
		Map<Class<? extends Pattern>, Integer> counts = new HashMap<Class<? extends Pattern>, Integer>();
		for(Pattern p : rowPatterns)
		{
			if(counts.get(p.getClass()) == null)
				counts.put(p.getClass(), 0);
		}
		for(Pattern p : colPatterns)
		{
			if(counts.get(p.getClass()) == null)
				counts.put(p.getClass(), 0);
		}
		
		
		for(Matrix m : matrices)
		{
			System.out.println(m.toString() + "\n" 
					+ "row[0]=" + m.findPatterns(m.getRow(0), rowPatterns) + "\n" 
					+ "row[1]=" + m.findPatterns(m.getRow(1), rowPatterns) + "\n"
					+ "row[2]=" + m.findPatterns(m.getRow(2), rowPatterns) + "\n"
					+ "col[0]=" + m.findPatterns(m.getCol(0), colPatterns) + "\n"
					+ "col[1]=" + m.findPatterns(m.getCol(1), colPatterns) + "\n"
					+ "col[2]=" + m.findPatterns(m.getCol(2), colPatterns) + "\n"
					+ "valid=" + m.isValidMatrix(rowPatterns, colPatterns, validPatterns) + "\n"
					+ "========================================================================");
			
			for(Pattern p : m.findPatterns(m.getRow(0), rowPatterns))
			{
				if(m.findPatterns(m.getRow(1), rowPatterns).contains(p) 
						&& m.findPatterns(m.getRow(2), rowPatterns).contains(p))
				{
					if(counts.get(p.getClass()) == null)
						counts.put(p.getClass(), 1);
					else
						counts.put(p.getClass(), counts.get(p.getClass()) + 1);
				}
			}
			
			for(Pattern p : m.findPatterns(m.getCol(0), rowPatterns))
			{
				if(m.findPatterns(m.getCol(1), colPatterns).contains(p) 
						&& m.findPatterns(m.getCol(2), colPatterns).contains(p))
				{
					if(counts.get(p.getClass()) == null)
						counts.put(p.getClass(), 1);
					else
						counts.put(p.getClass(), counts.get(p.getClass()) + 1);
				}
			}
		}
		
		for(Entry<Class<? extends Pattern>, Integer> e : counts.entrySet())
			System.out.println(e.getKey().getName() + " = " + e.getValue());
	}
	
	private static void intializePatterns(Set<String> properties, Set<MatrixEntry> objects, Random rand, 
			Set<Pattern> rowPatterns, Set<Pattern> colPatterns, Map<Pattern, Boolean> validPatterns) {
		
		for(String property: properties)
		{
			Pattern sp = new SamePattern(property, rand);
			Pattern osod = new OneSameOneDifferentPattern(property, rand);
			rowPatterns.add(sp); rowPatterns.add(osod);
			colPatterns.add(sp); colPatterns.add(osod);
			validPatterns.put(sp, true); validPatterns.put(osod, false);
			
			if(ORDERED_PROPERTIES.keySet().contains(property))
			{
				Pattern inc = new IncrementPattern(property, ORDERED_PROPERTIES.get(property), rand);
				Pattern dec = new DecrementPattern(property, ORDERED_PROPERTIES.get(property), rand);
				Pattern xor = new XORMetaPattern(new DifferentPattern(property, new HashSet<MatrixEntry>(objects), rand), 
						new ORMetaPattern(dec, inc));
				
				rowPatterns.add(inc); rowPatterns.add(dec); rowPatterns.add(xor);
				colPatterns.add(inc); colPatterns.add(dec); colPatterns.add(xor);
				validPatterns.put(inc, true); validPatterns.put(dec, true); validPatterns.put(xor, false); 
			}
			else
			{
				Pattern dp = new DifferentPattern(property, new HashSet<MatrixEntry>(objects), rand);
				rowPatterns.add(dp); 
				colPatterns.add(dp); 
				validPatterns.put(dp, true); 
			}
			
		}
	}

	private static void generateObjectsFile() throws IOException
	{
		List<String> orderedProps = new ArrayList<String>();
		Map<String, List<String>> properties = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		
		orderedProps.add("weight");
		values.add("heavy");
		values.add("light");
		values.add("medium");
		properties.put(orderedProps.get(orderedProps.size() - 1), values);
		
		values = new ArrayList<String>();
		orderedProps.add("color");
		values.add("blue");
		values.add("brown");
		values.add("green");
		properties.put(orderedProps.get(orderedProps.size() - 1), values);
		
		values = new ArrayList<String>();
		orderedProps.add("contents");
		values.add("beans");
		values.add("glass");
		values.add("rice");
		values.add("screws");
		properties.put(orderedProps.get(orderedProps.size() - 1), values);
		
		MatrixEntry.generateMatrixEntryFile(orderedProps, properties, "objects.txt");
	}
	
	private static void testBackgroundSubtraction() throws IOException
	{
		List<String> trainingImages = new ArrayList<String>();
		
		//this code uses all the images from no_object - look to train
//		String filepath = "F:/research/matrix_reasoning_corrupted_data/robot-deskbot/no_object/trial_1";
//		for(File f1 : new File(filepath).listFiles())
//		{
//			for(File f2 : new File(f1.getAbsolutePath() + "/look/vision").listFiles())
//			{
//				if(f2.getName().endsWith(".jpg"))
//					trainingImages.add(f2.getAbsolutePath());
//			}
//		}
		
		//this code randomly selects images to train
		System.out.println("generating training images");
		final String datapath = "H:/research/matrix_reasoning_corrupted_data";
		String filepath = datapath;
		while(trainingImages.size() < 200)
		{
			String[] files = new File(filepath).list();
			//String path = filepath + "/" + files[(int) (Math.random()*files.length)] + "/trial_1";
			String path = filepath + "/no_object/trial_1";
			
			files = new File(path).list();
			path += "/" + files[(int) (Math.random()*files.length)];
			
			files = new File(path).list();
			path += "/" + files[(int) (Math.random()*files.length)] + "/vision";
			
			files = new File(path).list();
			path += "/" + files[(int) (Math.random()*files.length)];
			
			if(!trainingImages.contains(path))
				trainingImages.add(path);
		}
		
		final BackgroundSubtraction subtractor = new BackgroundSubtraction(640, 480, 0);
		System.out.println("Training background");
		subtractor.trainBackgroundModel(trainingImages);

		List<MultiThreadRunnable> threads = new ArrayList<MultiThreadRunner.MultiThreadRunnable>();
		final int numTestObjects = 15;
		for(int i = 0; i < numTestObjects; i++)
		{
			threads.add(new MultiThreadRunnable() {
				
				private String title = "";
				private int count = 0;
				private int total = 1;
				
				@Override
				public void run() {
					String[] files = new File(datapath).list();
					String object = files[(int) (Math.random()*files.length)];
					while(object.equals("no_object"))
						object = files[(int) (Math.random()*files.length)];
					String path = datapath + "/" + object + "/trial_1";
					
					files = new File(path).list();
					String execution = files[(int) (Math.random()*files.length)];
					path += "/" + execution;
					
					files = new File(path).list();
					String behavior = files[(int) (Math.random()*files.length)];
					while(!behavior.equals("crush") && !behavior.equals("high_velocity_shake") && !behavior.equals("hold")
							&& !behavior.equals("poke") && !behavior.equals("shake"))
						behavior = files[(int) (Math.random()*files.length)];
					path += "/" + behavior + "/vision";
					
					title = object + ", " + execution + ","  + behavior;
					
					List<File> startFiles = Utility.convertToList(new File(path).listFiles());
					total = startFiles.size();
					Collections.sort(startFiles, new Comparator<File>() {
						@Override
						public int compare(File o1, File o2) {
							//remove "vision_" from the front of the file name
							String name1 = o1.getName().substring(7);
							String name2 = o2.getName().substring(7);
							//remove ".jpg" from the end
							name1 = name1.substring(0, name1.length() - 4);
							name2 = name2.substring(0, name2.length() - 4);
							
							return Integer.parseInt(name1) - Integer.parseInt(name2);
						}
					});
					
					if(!(new File("testImages/" + object + "_" + execution + "_" + behavior).exists()))
						new File("testImages/" + object + "_" + execution + "_" + behavior).mkdir();
					
					for(File f : startFiles)
					{
						BufferedImage image = null;
						try {
							image = ImageIO.read(f);
						} catch (IOException e) {
							e.printStackTrace();
						}
						BufferedImage alteredImage = subtractor.subtractBackground(image);
						try {
							ImageIO.write(alteredImage, "jpg", new File("testImages/" + object + "_" + execution + "_" + behavior + "/" + f.getName()));
						} catch (IOException e) {
							e.printStackTrace();
						}
						count++;
					}
					
				}
				
				@Override
				public String getTitle() {
					return title;
				}
				
				@Override
				public String getStatus() {
					return ((int)((double)1.0*count/total*100)) + "";
				}
			});
			
		}
		
		System.out.println("beginning background subtraction");
		MultiThreadRunner runner = new MultiThreadRunner(threads, 7);
		runner.startThreads();
		System.out.println("done");
		
//		System.out.println("displaying results");
//		
//		//borrowed this code from my VN game draw code, seems not very well optimized or written
//		JFrame frame = new JFrame("Background Subtraction");
//		
//		Canvas canvas = new Canvas();
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.getContentPane().add(canvas, BorderLayout.CENTER);
//		frame.pack();
//		frame.setMinimumSize(new Dimension(1280, 480));
//		frame.setVisible(true);
//		
//		canvas.setIgnoreRepaint(true);
//		canvas.requestFocus();
//		canvas.setFocusTraversalKeysEnabled(false);
//		canvas.createBufferStrategy(3);
//		BufferStrategy strategy = canvas.getBufferStrategy();
//		
//		frame.pack();
//		
//		frame.setVisible(true);
//		
//		for(int i = 0; i < images.size(); i++)
//		{
//			Graphics g = strategy.getDrawGraphics();
//			g.drawImage(images.get(i), 0, 0, null);
//			g.drawImage(alteredImages.get(i), 640, 0, null);
//			
//			g.dispose();
//			strategy.show();
//			
//			try {
//				Thread.sleep(200);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		
		
		
	}

}
