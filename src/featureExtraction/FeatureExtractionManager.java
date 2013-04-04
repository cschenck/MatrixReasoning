package featureExtraction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import utility.Context;
import utility.Modality;
import utility.Utility;

import matrices.MatrixEntry;

public class FeatureExtractionManager {
	
	public final static String dataPath = "H:\\research\\matrix_reasoning";
	public final static String featurePath = "features";
	public final static int NUM_EXECUTIONS = 10;
	
	private Map<Modality, FeatureExtractor> extractors;
	private Random rand;
	
	public FeatureExtractionManager(Random rand)
	{
		this.rand = rand;
		extractors = initializeFeatureExtractors();
	}
	
	private Map<Modality, FeatureExtractor> initializeFeatureExtractors() {
		Map<Modality, FeatureExtractor> ret = new HashMap<Modality, FeatureExtractor>();
		ret.put(Modality.proprioception, new ProprioceptionFeatureExtractor(dataPath, featurePath));
		ret.put(Modality.audio, new AudioFeatureExtractor(dataPath, featurePath));
		ret.put(Modality.color, new ColorFeatureExtractor(dataPath, featurePath, rand));
		
		return ret;
	}

	public void assignFeatures(List<MatrixEntry> objects, Set<Context> contexts) throws IOException
	{
		Utility.debugPrintln("Beginning feature extraction");
		for(Context c : contexts)
		{
			if(extractors.get(c.modality) == null)
				Utility.debugPrintln("No feature extractor for " + c.modality.toString() + ", will skip " + c.toString());
		}
		
		for(FeatureExtractor extractor : extractors.values())
			extractor.assignFeatures(objects, contexts);
		
		Utility.debugPrintln("Finished feature extraction");
	}

}
