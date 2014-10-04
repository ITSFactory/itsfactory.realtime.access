package fi.itsfactory.realtime.access;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class APIHelper {
	public static String createXmlError(String message, String details){
		try {	
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document document = docBuilder.newDocument();
			
			Element rootElement = document.createElement("error");
			
			Element messageElement = document.createElement("message");
			if(message != null){
				messageElement.setTextContent(message);
			}else{
				messageElement.setTextContent("internal error");
			}
			rootElement.appendChild(messageElement);
						
			if(details != null){
				Element detailsElement = document.createElement("error-details");
				detailsElement.setTextContent(details);				
				rootElement.appendChild(detailsElement);
			}
			
			document.appendChild(rootElement);
			
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StringWriter sw = new StringWriter();
			Result output = new StreamResult(sw);
			Source input = new DOMSource(document);

			transformer.transform(input, output);
			
			return sw.toString();
		} catch (Exception e) {
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>internal error</message></error>";
		} catch (Error e) {
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><error><message>internal error</message></error>";
		} 
	}
	public static String createJsonError(String message, String details){
		return "{\"error\":\""+message+"\",\"details\":\""+details+"\"}";
	}
}
