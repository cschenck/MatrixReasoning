package experiment;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import utility.Behavior;
import utility.Context;
import utility.Modality;
import utility.MultiJobRunner;
import utility.MultiThreadRunner;
import utility.MultiThreadRunner.MultiThreadRunnable;
import utility.RunningMean;
import utility.Tuple;
import utility.Utility;

public class ExperimentController {
	
	private static final int NUM_SAMPLES = 200;
	public static final int NUM_THREADS = 24;
	public final static int NUM_CHOICES = 10;
	private static final String RESULTS_PATH = "results";
	
	private List<Experiment> exps;
	private Random rand;
	private Set<Context> allContexts;
	
	private Queue<Job> jobs;
	
	private Map<Job, Map<Integer, Tuple<Double, String>>> results;
	
	public ExperimentController(List<Experiment> exps, Set<Context> allContexts, Random rand)
	{
		this.allContexts = allContexts;
		this.exps = exps;
		this.rand = rand;
	}
	
	private class Job
	{
		public final Experiment exp;
		public final List<Context> list;
		
		public Job(Experiment exp, List<Context> list)
		{
			this.exp = exp;
			this.list = list;
		}
		
		@Override
		public String toString()
		{
			return exp.name() + ":" + list.toString();
		}
	}
	
//	private class JobProcessor implements MultiThreadRunnable
//	{
//
//		private int remainingJobs = 0;
//		private int initialJobs = 0;
//		private Job currentJob = null;
//		
//		@Override
//		public void run() {
//			
//			while(true)
//			{
//				synchronized(jobs) {
//					if(jobs.isEmpty())
//						break;
//					else
//					{
//						if(initialJobs == 0)
//							initialJobs = jobs.size();
//						currentJob = jobs.poll();
//						remainingJobs = jobs.size();
//					}
//				}
//				
//				Tuple<Double, String> result = currentJob.exp.runExperiment(currentJob.list);
//				
//				synchronized(results) {
//					results.put(currentJob, result);
//				}
//			}
//			
//		}
//
//		@Override
//		public String getStatus() {
//			if(currentJob == null)
//				return "";
//			else
//				return currentJob.exp.name() + " : " + currentJob.list.toString();
//		}
//
//		@Override
//		public String getTitle() {
//			return remainingJobs + "/" + initialJobs;
//		}
//	}
	
	public void runExperiments()
	{
		long startTime = System.currentTimeMillis();
		System.out.println("Building job queue");
		//add all the jobs to the queue
		buildQueue();
		
		System.out.println("Running threads");
		final List<Integer> numChoices = new ArrayList<Integer>();
		for(int i = 2; i <= ExperimentController.NUM_CHOICES; i++)
			numChoices.add(i);
//		numChoices.add(5);
		//run the threads
		MultiJobRunner<Job, Map<Integer, Tuple<Double, String>>> runner = 
				new MultiJobRunner<Job, Map<Integer, Tuple<Double,String>>>(
					new MultiJobRunner.JobProcessor<Job, Map<Integer, Tuple<Double, String>>>() {
	
						@Override
						public Map<Integer, Tuple<Double, String>> processJob(Job job) {
							return job.exp.runExperiment(job.list, numChoices);
						}
					}, NUM_THREADS);
			results = runner.processJobs(jobs);
		
		System.out.println("Aggregating results");
		//aggregate the results
		try {
			aggregateResults();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		long processTime = System.currentTimeMillis() - startTime;
		long estHours = processTime/3600000;
		long estMins = (processTime % 3600000)/60000;
		long estSecs = ((processTime % 3600000) % 60000)/1000;
		
		String time = "";
		if(estHours > 0)
			time += estHours + " hours, ";
		if(estMins > 0)
			time += estMins + " minutes, ";
		time += estSecs + " seconds.";
		System.out.println("Took " + time);
	}
	
	private void aggregateResults() throws IOException
	{
		//first divide up the results by experiment
		Map<Experiment, Map<List<Context>, Map<Integer, Tuple<Double, String>>>> expRes = 
				new HashMap<Experiment, Map<List<Context>,Map<Integer,Tuple<Double,String>>>>();
		for(Entry<Job, Map<Integer,Tuple<Double, String>>> e : results.entrySet())
		{
			if(expRes.get(e.getKey().exp) == null)
				expRes.put(e.getKey().exp, new HashMap<List<Context>, Map<Integer,Tuple<Double,String>>>());
			expRes.get(e.getKey().exp).put(e.getKey().list, e.getValue());
		}
		
		//now go over each experiment and save the results to a file
		for(Experiment exp : expRes.keySet())
		{
			Map<Integer, List<RunningMean>> expMeans = new HashMap<Integer, List<RunningMean>>();
//			FileWriter fw = new FileWriter(RESULTS_PATH + "/" + exp.name() + "_dump.txt");
			for(int numCandidates = 2; numCandidates <= NUM_CHOICES; numCandidates++)
			{
				List<RunningMean> means = new ArrayList<RunningMean>();
				for(int i = 1; i <= getContexts().size(); i++)
					means.add(new RunningMean());
				
				for(Entry<List<Context>, Map<Integer,Tuple<Double, String>>> e : expRes.get(exp).entrySet())
				{
					//dump the raw results into a file
//					fw.write(e.getKey().toString() + " = " + e.getValue().get(numCandidates).a + " = " 
//							+ e.getValue().get(numCandidates).b + "\n");
//					fw.flush();
					
					//aggregate the results as well
					means.get(e.getKey().size() - 1).addValue(e.getValue().get(numCandidates).a);
				}
				expMeans.put(numCandidates, means);
			}
			
//			fw.close();
		
		
			//now lets save the means to a file
			List<String> rowHeaders = new ArrayList<String>();
			List<String> columnHeaders = new ArrayList<String>();
			for(Integer num : expMeans.keySet())
				columnHeaders.add(num.toString());
			for(int i = 1; i <= getContexts().size(); i++)
				rowHeaders.add(i + "");
			
			double[][] data = new double[getContexts().size()][expMeans.keySet().size()];
			double[][] dataStddev = new double[getContexts().size()][expMeans.keySet().size()];
			for(int i = 0; i < getContexts().size(); i++)
			{
				for(int j = 0; j < expMeans.keySet().size(); j++)
				{
					data[i][j] = expMeans.get(j + 2).get(i).getMean();
					dataStddev[i][j] = expMeans.get(j + 2).get(i).getStandardDeviation();
				}
			}
				
			PrintStream fos = new PrintStream(new FileOutputStream(RESULTS_PATH + "/" + exp.name() + "_averages.txt"));
			PrintStream fosStddev = new PrintStream(new FileOutputStream(RESULTS_PATH + "/" + exp.name() + "_stddevs.txt"));
			PrintStream temp = System.out;
			System.setOut(fos); //this is a hack because I, for some reason, made the print table function go to sysout
			Utility.printTable(columnHeaders, rowHeaders, data, false);
			System.setOut(fosStddev);
			Utility.printTable(columnHeaders, rowHeaders, dataStddev, false);
			fos.close();
			fosStddev.close();
			System.setOut(temp);
			
			//now we'll also print it out to standard out
			System.out.println("=============================" + exp.name() + "===============================");
			Utility.printTable(columnHeaders, rowHeaders, data, false);
			System.out.println();
		}
	}

	private void buildQueue() {
		List<Job> toBuild = new ArrayList<Job>();
		for(int num = 1; num <= getContexts().size(); num++)
		{
			for(List<Context> list : Utility.createRandomListsOfSize(new ArrayList<Context>(getContexts()), num, NUM_SAMPLES, false, rand))
			{
				for(Experiment exp : exps)
					toBuild.add(new Job(exp, list));
			}
		}
		
		//this is because the later things in the list tend to take the longest time
		//so this way when we're watching the jobs progress, we can get an accurate idea
		//of how long it will take by estimating based on the jobs/min
		this.jobs = new LinkedList<Job>(Utility.randomizeOrder(toBuild, rand));
	}
	
	private Set<Context> getContexts() {
//		Set<Context> contexts = new HashSet<Context>();
//		//add each context explicitly so we know which ones we're using
//		//audio contexts
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
//		//proprioception contexts
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
//		//color contexts
//		contexts.add(new Context(Behavior.look, Modality.color));
//		
//		return contexts;
		
		return allContexts;
	}

}
