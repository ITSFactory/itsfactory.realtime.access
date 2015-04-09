package fi.itsfactory.realtime.access.siri.vm;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SiriRequestHandler extends DefaultHandler {
	private String vehicleMonitoringRef;
	private String lineRef;
	private String vehicleRef;
	
	private boolean vmRef;
	private boolean lRef;
	private boolean vRef;
	
	public SiriRequestHandler() {
		this.vehicleMonitoringRef = null;
		this.lineRef = null;
		this.vehicleRef = null;
	
		vmRef = false;
		lRef = false;
		vRef = false;
	}
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if("VehicleMonitoringRef".equals(localName)){
			vmRef = true;
		}else if("VehicleRef".equals(localName)){
			vRef = true;
		}else if("LineRef".equals(localName)){
			lRef = true;
		}
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		if(vmRef == true){
			vehicleMonitoringRef = new String(ch, start, length);
		}else if(vRef == true){
			vehicleRef = new String(ch, start, length);
		}else if(lRef == true){
			lineRef = new String(ch, start, length);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if("VehicleMonitoringRef".equals(localName)){
			vmRef = false;
		}else if("VehicleRef".equals(localName)){
			vRef = false;
		}else if("LineRef".equals(localName)){
			lRef = false;
		}	
	}
	
	public String getVehicleMonitoringRef() {
		return vehicleMonitoringRef;
	}

	public String getLineRef() {
		return lineRef;
	}

	public String getVehicleRef() {
		return vehicleRef;
	}
}
