package utility;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MultiThreadRunner {
	
	public interface MultiThreadRunnable extends Runnable{
		public String getStatus();
		public String getTitle();
	}
	
	private class MultiThreadRunnableWrapper
	{
		private MultiThreadRunnable runnable;
		private Thread t;
		private int index;
		private boolean foundDead = false; //a flag that can be used to check to see if it has already been found dead
		
		public MultiThreadRunnableWrapper(MultiThreadRunnable r, Thread t, int i)
		{
			runnable = r;
			this.t = t;
			index = i;
		}
		
		@Override
		public int hashCode()
		{
			return index;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			return (obj instanceof MultiThreadRunnableWrapper) &&
					((MultiThreadRunnableWrapper)obj).runnable == runnable;
		}
	}
	
	private generatorStatus status;
	
	private List<MultiThreadRunnableWrapper> running;
	private Thread myThread;
	private boolean going = false;
	private int maxConcurrentThreads;
	
	public MultiThreadRunner(List<MultiThreadRunnable> runnable, int maxConcurrentThreads)
	{
		status = new generatorStatus();
		this.maxConcurrentThreads = maxConcurrentThreads;
		
		running = new LinkedList<MultiThreadRunner.MultiThreadRunnableWrapper>();
		for(MultiThreadRunnable r : runnable)
		{
			running.add(new MultiThreadRunnableWrapper(r, new Thread(r), status.addStatus()));
		}
	}
	
	public void startThreads()
	{
		final Queue<MultiThreadRunnableWrapper> threads = new LinkedList<MultiThreadRunner.MultiThreadRunnableWrapper>();
		for(MultiThreadRunnableWrapper r : running)
			threads.add(r);
		
		int numRunning = 0;
		
		myThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				going = true;
				while(going)
				{
					for(MultiThreadRunnableWrapper r : running)
					{
						if(threads.contains(r))
							status.updateStatus(r.runnable.getTitle() + ": Queued", r.index);
						else if(!r.t.isAlive())
							status.updateStatus(r.runnable.getTitle() + ": Done", r.index);
						else
							status.updateStatus(r.runnable.getTitle() + ":" + r.runnable.getStatus(), r.index);
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		myThread.start();
		
		while(threads.size() > 0)
		{
			while(numRunning < maxConcurrentThreads && !threads.isEmpty())
			{
				MultiThreadRunnableWrapper r = threads.poll();
				r.t.start();
				numRunning++;
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			for(MultiThreadRunnableWrapper r : running)
			{
				//if the thread has been started and has died
				if(!threads.contains(r) && !r.t.isAlive() && !r.foundDead)
				{
					numRunning--;
					r.foundDead = true;
				}
			}
		}
		
		//now wait for all threads to finish
		for(MultiThreadRunnableWrapper r : running)
			try {
				r.t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		going = false;
		try {
			myThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		status.dispose();
	}
	
	private class generatorStatus {
		
		private List<String> statuses = new ArrayList<String>();
		private List<JLabel> labels = new ArrayList<JLabel>();
		private JPanel panel;
		private JFrame frame;
		
		public generatorStatus() {
			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			
			frame = new JFrame("Running Threads");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setContentPane(panel);
			frame.setVisible(true);
		}
		
		public int addStatus() {
			synchronized(this)
			{
				statuses.add("");
				JLabel l = new JLabel("");
				labels.add(l);
				panel.add(l);
				panel.validate();
				panel.updateUI();
				frame.pack();
				return statuses.size() - 1;
			}
		}
		
		public void updateStatus(String newStatus, int index)
		{
			synchronized(this) {
				statuses.set(index, newStatus);
				labels.get(index).setText(newStatus);
				panel.validate();
				panel.updateUI();
				frame.pack();
			}
		}
		
		public String[] getStatuses() {
			synchronized(this) {
				return statuses.toArray(new String[0]);
			}
		}
		
		public void dispose() {
			frame.dispose();
		}
		
	}

}
