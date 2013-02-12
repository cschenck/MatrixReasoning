package featureExtraction.AudioFFT;



/**
 * Thrown to indicate that a DataProcessor has problems processing
 * incoming Data objects.
 */
public class DataProcessingException extends Exception {

    /**
     * Constructs a DataProcessingException with no detailed message.
     */
    public DataProcessingException() {
        super();
    }

    /**
     * Constructs a DataProcessingException with the specified
     * detailed message.
     *
     * @param message the detailed message
     */
    public DataProcessingException(String message) {
        super(message);
    }
}
