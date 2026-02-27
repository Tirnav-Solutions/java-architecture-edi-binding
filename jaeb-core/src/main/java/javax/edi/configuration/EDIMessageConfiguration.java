package javax.edi.configuration;

public class EDIMessageConfiguration
{
    private char elementDelimiter;
    private char componentDelimiter;
    private char segmentDelimiter;
    
    private EDIMessageConfiguration() {
    }
    
    public char getElementDelimiter() {
        return this.elementDelimiter;
    }
    
    public char getComponentDelimiter() {
        return this.componentDelimiter;
    }
    
    public char getSegmentDelimiter() {
        return this.segmentDelimiter;
    }
    
    public EDIMessageConfiguration(final char elementDelimiter, final char componentDelimiter, final char segmentDelimiter) {
        this.elementDelimiter = elementDelimiter;
        this.componentDelimiter = componentDelimiter;
        this.segmentDelimiter = segmentDelimiter;
    }
}