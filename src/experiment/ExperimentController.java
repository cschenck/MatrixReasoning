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
import utility.MultiThreadRunner;
import utility.MultiThreadRunner.MultiThreadRunnable;
import utility.RunningMean;
import utility.Tuple;
import utility.Utility;

public class ExperimentController {
	
	private static final int NUM_SAMPLES = 200;
	private static final int NUM_THREADS = 8;
	private static final String RESULTS_PATH = "results";
	
	private List<Experiment> exps;
	private Random rand;
	private Set<Context> allContexts;
	
	private Queue<Job> jobs = new LinkedList<Job>();
	
	private Map<Job, Tuple<Double, String>> results = new HashMap<Job, Tuple<Double, String>>();
	
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
	}
	
	private class JobProcessor implements MultiThreadRunnable
	{

		private int remainingJobs = 0;
		private int initialJobs = 0;
		private Job currentJob = null;
		
		@Override
		public void run() {
			
			while(true)
			{
				synchronized(jobs) {
					if(jobs.isEmpty())
						break;
					else
					{
						if(initialJobs == 0)
							initialJobs = jobs.size();
						currentJob = jobs.poll();
						remainingJobs = jobs.size();
					}
				}
				
				Tuple<Double, String> result = currentJob.exp.runExperiment(currentJob.list);
				
				synchronized(results) {
					results.put(currentJob, result);
				}
			}
			
		}

		@Override
		public String getStatus() {
			if(currentJob == null)
				return "";
			else
				return currentJob.exp.name() + " : " + currentJob.list.toString();
		}

		@Override
		public String getTitle() {
			return remainingJobs + "/" + initialJobs;
		}
	}
	
	public void runExperiments()
	{
		System.out.println("Building job queue");
		//add all the jobs to the queue
		buildQueue();
		
		System.out.println("Running threads");
		//run the threads
		List<MultiThreadRunnable> threads = new ArrayList<MultiThreadRunnable>();
		for(int i = 0; i < NUM_THREADS; i++)
			threads.add(new JobProcessor());
		MultiThreadRunner runner = new MultiThreadRunner(threads, NUM_THREADS);
		runner.startThreads();
		
		System.out.println("Aggregating results");
		//aggregate the results
		try {
			aggregateResults();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void aggregateResults() throws IOException
	{
		//first divide up the results by experiment
		Map<Experiment, Map<List<Context>, Tuple<Double, String>>> expRes = new HashMap<Experiment, Map<List<Context>,Tuple<Double,String>>>();
		for(Entry<Job, Tuple<Double, String>> e : results.entrySet())
		{
			if(expRes.get(e.getKey().exp) == null)
				expRes.put(e.getKey().exp, new HashMap<List<Context>, Tuple<Double,String>>());
			expRes.get(e.getKey().exp).put(e.getKey().list, e.getValue());
		}
		
		//now go over each experiment and save the results to a file
		Map<Experiment, List<RunningMean>> expMeans = new HashMap<Experiment, List<RunningMean>>();
		for(Experiment exp : expRes.keySet())
		{
			List<RunningMean> means = new ArrayList<RunningMean>();
			for(int i = 1; i <= getContexts().size(); i++)
				means.add(new RunningMean());
			
			FileWriter fw = new FileWriter(RESULTS_PATH + "/" + exp.name() + "_dump.txt");
			for(Entry<List<Context>, Tuple<Double, String>> e : expRes.get(exp).entrySet())
			{
				//dump the raw results into a file
				fw.write(e.getKey().toString() + " = " + e.getValue().a + " = " + e.getValue().b + "\n");
				fw.flush();
				
				//aggregate the results as well
				means.get(e.getKey().size() - 1).addValue(e.getValue().a);
			}
			
			fw.close();
			expMeans.put(exp, means);
		}
		
		//now lets save the means to a file
		List<String> rowHeaders = new ArrayList<String>();
		List<String> columnHeaders = new ArrayList<String>();
		for(Experiment exp : this.exps)
			columnHeaders.add(exp.name());
		for(int i = 1; i <= getContexts().size(); i++)
			rowHeaders.add(i + "");
		
		double[][] data = new double[getContexts().size()][exps.size()];
		for(int i = 0; i < getContexts().size(); i++)
		{
			for(int j = 0; j < exps.size(); j++)
				data[i][j] = expMeans.get(exps.get(j)).get(i).getMean();
		}
			
		PrintStream fos = new PrintStream(new FileOutputStream(RESULTS_PATH + "/average_per_num_contexts.txt"));
		PrintStream temp = System.out;
		System.setOut(fos); //this is a hack because I, for some reason, made the print table function go to sysout
		Utility.printTable(columnHeaders, rowHeaders, data, false);
		fos.close();
		System.setOut(temp);
		
		//now we'll also print it out to standard out
		Utility.printTable(columnHeaders, rowHeaders, data, false);
	}

	private void buildQueue() {
		for(int num = 1; num <= getContexts().size(); num++)
		{
			for(List<Context> list : Utility.createRandomListsOfSize(new ArrayList<Context>(getContexts()), num, NUM_SAMPLES, false, rand))
			{
				for(Experiment exp : exps)
					jobs.add(new Job(exp, list));
			}
		}
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
