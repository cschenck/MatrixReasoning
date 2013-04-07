package utility;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import utility.MultiThreadRunner.MultiThreadRunnable;

public class MultiJobRunner<J,R> {

	public interface JobProcessor<J,R> {
		public R processJob(J job);
	}
	
	private JobProcessor<J, R> processor;
	private int numThreads;
	private boolean enableStatusWindow;
	
	public MultiJobRunner(JobProcessor<J,R> jobProcessor, int numThreads, boolean enableStatusWindow)
	{
		this.processor = jobProcessor;
		this.numThreads = numThreads;
		this.enableStatusWindow = enableStatusWindow;
	}
	
	public MultiJobRunner(JobProcessor<J,R> jobProcessor, int numThreads)
	{
		this(jobProcessor, numThreads, true);
	}
	
	public Map<J, R> processJobs(final Queue<J> jobs)
	{
		final Map<J, R> results = new HashMap<J, R>();
		
		List<MultiThreadRunnable> threads = new ArrayList<MultiThreadRunnable>();
		for(int i = 0; i < numThreads; i++)
		{
			threads.add(new MultiThreadRunnable() {
				
				private int remaining;
				private int initial;
				private J current;
				
				@Override
				public void run() {
					initial = jobs.size();	
					
					while(true)
					{
						synchronized(jobs) {
							if(jobs.isEmpty())
								break;
							else
							{
								current = jobs.poll();
								remaining = jobs.size();
							}
							
						}
						
						R result = processor.processJob(current);
						
						synchronized(results) {
							results.put(current, result);
						}
					}
					
				}
				
				@Override
				public String getTitle() {
					return remaining + "/" + initial;
				}
				
				@Override
				public String getStatus() {
					if(current == null)
						return "";
					else
						return current.toString();
				}
			});
		}
		
		final long startTime = System.currentTimeMillis();
		final int startSize = jobs.size();
		threads.add(new MultiThreadRunnable() {
			
			private String status = "";
			
			@Override
			public void run() {
				int currentSize = startSize;
				while(true)
				{
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					synchronized(jobs) {
						if(jobs.isEmpty())
							break;
						else
							currentSize = jobs.size();
					}
					long currentTime = System.currentTimeMillis();
					double msPerJob = (double)1.0*(currentTime - startTime)/(startSize - currentSize);
					long estMS = (long) (msPerJob*currentSize);
					
					long estHours = estMS/3600000;
					long estMins = (estMS % 3600000)/60000;
					long estSecs = ((estMS % 3600000) % 60000)/1000;
					
					String time = "";
					if(estHours > 0)
						time += estHours + " hours, ";
					if(estMins > 0)
						time += estMins + " minutes, ";
					time += estSecs + " seconds.";
					
					status = time;
					
					System.out.print(currentSize + "/" + startSize + ", est. time remaining: " + status + "\r");
				}
				System.out.println("Done");
			}
			
			@Override
			public String getTitle() {
				return "Est. time-to-completion";
			}
			
			@Override
			public String getStatus() {
				return status;
			}
		});
		
		MultiThreadRunner runner = new MultiThreadRunner(threads, Integer.MAX_VALUE, enableStatusWindow);
		runner.startThreads();
		
		return results;
	}
	
}
