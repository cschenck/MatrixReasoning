package featureExtraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import featureExtraction.AudioFFT.SoundFFT;

import utility.Context;
import utility.Modality;
import utility.Utility;

public class AudioFeatureExtractor extends FeatureExtractor {
	
	private final static BinningFeatureExtractor binner = new BinningFeatureExtractor(10, 10);
	private final static int NUM_BINS = 33;

	public AudioFeatureExtractor(String dataPath, String featurePath) {
		super(dataPath, featurePath);
	}

	@Override
	protected Modality getModality() {
		return Modality.audio;
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
				if(!(new File(execution.getAbsolutePath() + "/" + c.behavior.toString() 
						+ "/hearing/spectrogram.txt").exists()))
				{
					//find the wave file in the folder and compute the spectrogram
					for(File wav : new File(execution.getAbsolutePath() + "/" + c.behavior.toString() + "/hearing").listFiles())
					{
						if(wav.getName().endsWith(".wav"))
							computeSpectrogram(wav.getAbsolutePath(), execution.getAbsolutePath() + "/" 
									+ c.behavior.toString() + "/hearing/spectrogram.txt");
					}
				}
				
				Scanner file = null;
				try {
					file = new Scanner(new File(execution.getAbsolutePath() + "/" 
							+ c.behavior.toString() + "/hearing/spectrogram.txt"));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				
				List<double[]> rawFeatures = new ArrayList<double[]>();
				while(file.hasNextLine())
				{
					List<Double> fs = new ArrayList<Double>(); //sooo many lists
					
					Scanner line = new Scanner(file.nextLine());
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

	private void computeSpectrogram(String input, String output) 
	{
		SoundFFT fft = new SoundFFT();
		double[][] rawData = fft.getMatrixFFT(input, NUM_BINS);
		
		FileWriter fw;
		try {
			fw = new FileWriter(output);
			for(double[] col : rawData)
			{
				for(double d : col)
					fw.write(" " + d);
				fw.write("\n");
				fw.flush();
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
