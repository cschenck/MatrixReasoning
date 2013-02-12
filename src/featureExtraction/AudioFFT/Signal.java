package featureExtraction.AudioFFT;



/**
 * Indicates events like beginning or end of data, data dropped,
 * quality changed, etc.. It implements the Data interface, and it will
 * pass between DataProcessors to inform them about the Data that is
 * passed between DataProcessors.
 *
 * @see Data
 * @see DataProcessor
 */
public class Signal implements Data {

    private long time;  // the time this Signal was issued

    /**
     * Constructs a Signal with the given name.
     *
     * @param time the time this Signal is created
     */
    protected Signal(long time) {
        this.time = time;
    }

    /**
     * Returns the time this Signal was created.
     *
     * @return the time this Signal was created
     */
    public long getTime() {
        return time;
    }
}
