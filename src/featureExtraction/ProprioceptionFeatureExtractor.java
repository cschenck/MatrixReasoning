package featureExtraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import utility.Context;
import utility.Modality;
import utility.Utility;

public class ProprioceptionFeatureExtractor extends FeatureExtractor {
	
	private final static BinningFeatureExtractor binner = new BinningFeatureExtractor(10, 7);

	public ProprioceptionFeatureExtractor(String dataPath, String featurePath) {
		super(dataPath, featurePath);
	}

	@Override
	protected Modality getModality() {
		return Modality.proprioception;
	}

	@Override
	protected Map<String, List<double[]>> generateFeatures(Context c) {
		Map<String, List<double[]>> ret = new HashMap<String, List<double[]>>();
		for(File object : new File(this.getDataPath()).listFiles())
		{
			Utility.debugPrint("Processing " + c.toString() + " for " + object.getName() + "... ");
			List<double[]> features = new ArrayList<double[]>();
			for(File execution : new File(object.getAbsolutePath() + "/trial_1").listFiles())
			{
				Scanner file = null;
				try {
					file = new Scanner(new File(execution.getAbsolutePath() + "/" 
							+ c.behavior.toString() + "/" + c.modality.toString() + "/jtrq0.txt"));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				
				List<double[]> rawFeatures = new ArrayList<double[]>();
				while(file.hasNextLine())
				{
					List<Double> fs = new ArrayList<Double>(); //sooo many lists
					
					Scanner line = new Scanner(file.nextLine());
					//scan off the tick number
					line.next();
					while(line.hasNextDouble())
						fs.add(line.nextDouble());
					double[] d = new double[fs.size()];
					for(int i = 0; i < fs.size(); i++)
						d[i] = fs.get(i);
					
					rawFeatures.add(d);
				}
				
				double[][] dRawFeatures = rawFeatures.toArray(new double[rawFeatures.size()][]);
				features.add(binner.bin(dRawFeatures));
			}
			ret.put(object.getName(), features);
			System.out.println("done.");
		}
		
		return ret;
	}

}
