package javax.edi.bind.test;

import java.io.StringWriter;

import javax.edi.bind.EDIMarshaller;

import org.junit.Test;

public class TestEDIMarshaller extends EDITestBase {

	@Test
	public void testParser() throws Exception {
		StringWriter sw = new StringWriter();
		System.out.println("Print Java Version: "+System.getProperty("java.version"));
		EDIMarshaller.marshal(exampleMessage, sw);
		System.out.println(sw.toString());
	}
	
}
