package fi.itsfactory.realtime.access.siri.vm;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * VMResponseFilter filters SIRI Vehicle Monitoring Delivery responses. This class extends DefaultHandler and such
 * is used with SAX parser. As it is case with SAX parsers, this class parses XML documents (SIRI Vehicle Monitoring
 * Delivery responses). This class allows setting of filters (in the class constructor) which are used to filter out 
 * Vehicle Activity elements from the SIRI Vehicle Monitoring Delivery responses. Filters match to elements inside 
 * Vehicle Activity XML elements, and a matching filter(s) causes that specific Vehicle Activity element, inside
 * which the filter(s) matched, to be filtered out from the final response. 
 * 
 * @author jlundan
 */
public class VMResponseFilter extends DefaultHandler {
	private Document document;
	private Node currentElement;
	private Element rootElement;
	
	private String filteringLineRef;
	private String filteringVehicleRef;
	private String journeyPatternRef;
	
	private boolean lineRefMatched;
	private boolean vehicleRefMatched;
	
	public VMResponseFilter(String lineRef, String vehicleRef) {
		this.filteringLineRef = lineRef;
		this.filteringVehicleRef = vehicleRef;
		
		currentElement = null;
		journeyPatternRef = null;
		lineRefMatched = false;
		vehicleRefMatched = false;
		
		try {	
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			document = docBuilder.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		Element element = document.createElementNS(uri, qName);
		
		int attrLength = attributes.getLength();
		for(int i=0; i<attrLength;i++){
			element.setAttributeNS(attributes.getURI(i),attributes.getQName(i),attributes.getValue(i));
		}
		
		if(rootElement == null){
			rootElement = element;
			currentElement = element;
		}else{
			currentElement.appendChild(element);
			currentElement = element;
		}
		
		if("VehicleActivity".equals(qName)){
			lineRefMatched = false;
			vehicleRefMatched = false;
		}
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		String contents = new String(ch, start, length).trim();
		currentElement.setTextContent(contents);
		if(filteringVehicleRef != null && "VehicleRef".equals(currentElement.getNodeName()) && filteringVehicleRef.equals(contents)){
			vehicleRefMatched = true;
		}else if(filteringLineRef != null && "JourneyPatternRef".equals(currentElement.getNodeName()) && filteringLineRef.equals(contents)){
			lineRefMatched = true;
		}else if(filteringLineRef != null && "LineRef".equals(currentElement.getNodeName()) && filteringLineRef.equals(contents)){
            lineRefMatched = true;
        }

		if("JourneyPatternRef".equals(currentElement.getNodeName())){
		    journeyPatternRef = contents;
		}else if("DatedVehicleJourneyRef".equals(currentElement.getNodeName())){
		    String parts[] = contents.split("_");
		    if(parts.length == 3){
		        currentElement.setTextContent(parts[parts.length - 1]);
		    }
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
	    Node parent = currentElement.getParentNode();
		if("VehicleActivity".equals(qName)){
	        /*
	         * If client has specified both lineRef and vehicleRef filters, an element must not match either in order
	         * to be incluced. If the client has specified only one of the filters, the element must not match that filter
	         */
	        if((filteringLineRef != null && filteringVehicleRef != null && lineRefMatched == false && vehicleRefMatched == false) ||
					(filteringLineRef != null && lineRefMatched == false) ||
					(filteringVehicleRef != null && vehicleRefMatched == false)){
			
				lineRefMatched = false;
				vehicleRefMatched = false;
				parent.removeChild(currentElement);
			}else{
			    if(currentElement != null && journeyPatternRef != null 
			            && ((Element)currentElement).getElementsByTagName("LineRef").getLength() > 0){
			        ((Element)currentElement).getElementsByTagName("LineRef").item(0).setTextContent(journeyPatternRef);
			        journeyPatternRef = null;
			    }
			}
		}else if("Extensions".equals(qName) || "OriginAimedDepartureTime".equals(qName)
		        || "DestinationShortName".equals(qName) || "OriginShortName".equals(qName) || "OnwardCalls".equals(qName)){
		    parent.removeChild(currentElement);
		}else if("JourneyPatternRef".equals(qName)){
            parent.removeChild(currentElement);
        }
		currentElement = parent;
	}
	
	public String getFilteredDocument() throws TransformerFactoryConfigurationError, TransformerException{
		document.appendChild(rootElement);
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter sw = new StringWriter();
		Result output = new StreamResult(sw);
		Source input = new DOMSource(document);

		transformer.transform(input, output);
		return sw.toString();
	}
	
}
