package com.bradsdavis.edi.transformer;

public class TransformerException extends Exception {

	public TransformerException(String message) {
		super(message);
	}
	
	public TransformerException(String message, Throwable t) {
		super(message, t);
	}
}
