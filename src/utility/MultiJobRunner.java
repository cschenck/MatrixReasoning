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
	
	public MultiJobRunner(JobProcessor<J,R> jobProcessor, int numThreads)
	{
		this.processor = jobProcessor;
		this.numThreads = numThreads;
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
		
		MultiThreadRunner runner = new MultiThreadRunner(threads, numThreads);
		runner.startThreads();
		
		return results;
	}
	
}
