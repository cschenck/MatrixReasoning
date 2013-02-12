package featureExtraction.AudioFFT;

import java.io.File;
import java.io.PrintStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class TestAudio {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = "/home/users/jsinapov/robot/robotSoundData/soundData/o0/soundData_o0_b0_t0.wav";
		String outFileName = "01.txt";
		File inputFile = new File(filename);
		File outFile = new File(outFileName);
		PrintStream fileOut = new PrintStream(System.out);
		AudioInputStream inStream = null;
		try{
			inStream = AudioSystem.getAudioInputStream(inputFile);
			outFile.createNewFile();
			fileOut = new PrintStream(outFile);
		}
		catch(Exception e){
			System.exit(-1);
		}
		DiscreteFourierTransform fft = new DiscreteFourierTransform();
		fft.initialize();
		StreamDataSource file = new StreamDataSource();
		file.setInputStream(inStream, "music2");
		file.initialize();
		RaisedCosineWindower windower = new RaisedCosineWindower();
		windower.initialize();
		windower.setPredecessor(file);
		fft.setPredecessor(windower);
		
		System.out.println("Initialized");
		try{
			System.out.println(fft.getData().toString());
			Data dat = fft.getData();
			while(dat instanceof DoubleData){
				fileOut.print(((DoubleData)dat).getFirstSampleNumber());
				double[] values = ((DoubleData)dat).getValues();
				for(int i = 0; i < values.length; ++i){
					fileOut.print("\t" + Math.log(values[i]));
				}
				fileOut.println("");
				dat = fft.getData();
			}
			/*for(int i = 0; i < 50; ++i){
				System.out.println(fft.getData().toString());
			}
			//DoubleData dat = (DoubleData)fft.getData();
			//System.out.println(dat.getFirstSampleNumber() + " " + dat.getCollectTime());
			double[] values1 = ((DoubleData)fft.getData()).getValues();
			double[] values2 = ((DoubleData)fft.getData()).getValues();
			double[] values3 = ((DoubleData)fft.getData()).getValues();
			
			for(int i = 0; i < values1.length; ++i){
				System.out.println(Math.log(values1[i]) + "\t" + Math.log(values2[i])+ "\t" + Math.log(values3[i]));
			}*/
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println("getData failed");
			System.exit(-1);
		}
	}

}
