package fi.itsfactory.realtime.access;

import java.io.OutputStream;
import java.io.StringReader;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.xml.sax.SAXException;

import uk.org.siri.siri.Siri;
import fi.itsfactory.realtime.access.gtfsrt.vp.GtfsRtBuilder;
import fi.itsfactory.realtime.access.siri.vm.SiriRequestHandler;

@Controller
public class RealtimeApiController {
	private static final Logger logger = LoggerFactory.getLogger(RealtimeApiController.class);

	private SAXParser requestSAXParser = null;
	private JAXBContext jaxbContext = null;

	private SiriDatasource datasource;
	private SiriDatasource datasourceV2;

	/*
	 * We dont want to create SAX parser and JAXBContext every time when the message arrives, so we create
	 * those on Servlet (this class) startup and use those from there on.
	 */
	public RealtimeApiController(SiriDatasource datasource, SiriDatasource datasourceV2) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			requestSAXParser = factory.newSAXParser();

			jaxbContext = JAXBContext.newInstance(Siri.class);

			this.datasource = datasource;
			this.datasource.initialize();
			
			this.datasourceV2 = datasourceV2;
			this.datasourceV2.initialize();
			
			logger.info("A SIRI VM StoreController started up successfully.");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}

	}

	/**
	 * getSiriVM retrieves SIRI Vehicle Monitoring deliveries as XML documents. Charset is UTF-8.
	 * 
	 * @return Vehicle Monitoring deliveries as XML string
	 */
	@RequestMapping(value = "/vm/siri", method = RequestMethod.POST, produces = "application/xml; charset=utf-8")
	public @ResponseBody
	String getSiriVM() {
		if (requestSAXParser != null && datasource != null) {
			try {
				HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
						.currentRequestAttributes()).getRequest();

				SiriRequestHandler requestHandler = new SiriRequestHandler();
				requestSAXParser.parse(request.getInputStream(), requestHandler);

				return datasource.getVehicleMonitoringData(requestHandler.getLineRef(),
						requestHandler.getVehicleRef());
			} catch (Exception e) {
				logger.error("Cannot process SIRI request", e);
				return APIHelper.createXmlError("Internal server error", e.getMessage());
			} catch (Error e) {
				logger.error("Cannot process SIRI request", e);
				return APIHelper.createXmlError("Internal server error", e.getMessage());
			}
		} else {
			logger.error("Request parser did not initialize properly, cannot process request. Check the logs for startup exceptions.");
			return APIHelper.createXmlError("Internal server error",
					"Request parser did not initialize properly, cannot process request.");
		}
	}
	
	   /**
     * getSiriVM retrieves SIRI Vehicle Monitoring deliveries as XML documents. Charset is UTF-8.
     * 
     * @return Vehicle Monitoring deliveries as XML string
     */
    @RequestMapping(value = "/vm/siri/2", method = RequestMethod.POST, produces = "application/xml; charset=utf-8")
    public @ResponseBody
    String getSiriVM2() {
        if (requestSAXParser != null && datasource != null) {
            try {
                HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                        .currentRequestAttributes()).getRequest();

                SiriRequestHandler requestHandler = new SiriRequestHandler();
                requestSAXParser.parse(request.getInputStream(), requestHandler);

                return datasourceV2.getVehicleMonitoringData(requestHandler.getLineRef(),
                        requestHandler.getVehicleRef());
            } catch (Exception e) {
                logger.error("Cannot process SIRI request", e);
                return APIHelper.createXmlError("Internal server error", e.getMessage());
            } catch (Error e) {
                logger.error("Cannot process SIRI request", e);
                return APIHelper.createXmlError("Internal server error", e.getMessage());
            }
        } else {
            logger.error("Request parser did not initialize properly, cannot process request. Check the logs for startup exceptions.");
            return APIHelper.createXmlError("Internal server error",
                    "Request parser did not initialize properly, cannot process request.");
        }
    }

	/**
	 * getJsonVM retrieves SIRI Vehicle Monitoring deliveries as JSON documents. Charset is UTF-8.
	 * 
	 * @return Vehicle Monitoring deliveries as JSON string
	 */
	@RequestMapping(value = "/vm/rest", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
	public @ResponseBody
	String getJsonVM(@RequestParam(value = "lineRef", required = false) String lineRef,
			@RequestParam(value = "vehicleRef", required = false) String vehicleRef,
			@RequestParam(value = "indent", required = false) String indent) {
		if (datasource != null) {
            try {
                String xmlResponse = datasource.getVehicleMonitoringData(lineRef, vehicleRef);

                if (xmlResponse != null && !xmlResponse.startsWith("<!DOCTYPE html")) {
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    Siri siri = (Siri) unmarshaller.unmarshal(new StringReader(xmlResponse));
                    SiriJsonBuilder jsonBuilder = new SiriJsonBuilder(siri);
                    return jsonBuilder.buildJson(indent);
                } else {
                    return APIHelper.createJsonError("Internal server error", "backend error");
                }
            } catch (Exception e) {
                logger.error("Cannot process SIRI request", e);
                return APIHelper.createJsonError("Internal server error", e.getMessage());
            } catch (Error e) {
                logger.error("Cannot process SIRI request", e);
                return APIHelper.createJsonError("Internal server error", e.getMessage());
            }
		} else {
			logger.error("Request parser did not initialize properly, cannot process request. Check the logs for startup exceptions.");
			return APIHelper.createJsonError("Internal server error",
					"Request parser did not initialize properly, cannot process request.");
		}
	}

	/**
	 * getGtfsRt retrieves SIRI Vehicle Monitoring deliveries as GTFS-RT.
	 * 
	 * @return Vehicle Monitoring deliveries as GTFS-RT protobuf
	 */
	@RequestMapping(value = "/gtfs-rt/vehicle-positions", method = RequestMethod.GET, produces = "application/octet-stream")
	public void getGtfsRt(@RequestParam(value = "lineRef", required = false) String lineRef,
			@RequestParam(value = "vehicleRef", required = false) String vehicleRef, OutputStream response) {
		if (datasource != null) {
			try {
				String xmlResponse = datasource.getVehicleMonitoringData(lineRef, vehicleRef);

				if (xmlResponse != null) {
					StringReader reader = new StringReader(xmlResponse);
					Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

					Siri siri = (Siri) unmarshaller.unmarshal(reader);

					GtfsRtBuilder builder = new GtfsRtBuilder(siri);
					builder.writeToOutputStream(response);
				}
			} catch (Exception e) {
				logger.error("Cannot process GTFS-RT request", e);
			} catch (Error e) {
				logger.error("Cannot process GTFS-RT request", e);
			}
		} else {
			logger.error("Request parser did not initialize properly, cannot process request. Check the logs for startup exceptions.");
		}
	}

	/**
	 * getGtfsRt retrieves SIRI Vehicle Monitoring deliveries as GTFS-RT JSON documents. Charset is UTF-8.
	 * 
	 * @return Vehicle Monitoring deliveries as GTFS-RT json
	 */
	@RequestMapping(value = "/gtfs-rt/vehicle-positions/json", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
	public @ResponseBody
	String getGtfsRtJson(@RequestParam(value = "lineRef", required = false) String lineRef,
			@RequestParam(value = "vehicleRef", required = false) String vehicleRef) {
		if (datasource != null) {
			try {
				String xmlResponse = datasource.getVehicleMonitoringData(lineRef, vehicleRef);
				if (xmlResponse != null) {
					StringReader reader = new StringReader(xmlResponse);
					Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

					Siri siri = (Siri) unmarshaller.unmarshal(reader);

					GtfsRtBuilder builder = new GtfsRtBuilder(siri);
					return builder.buildJson();
				} else {
					return "";
				}

			} catch (Exception e) {
				logger.error("Cannot process GTFS-RT request", e);
				return APIHelper.createJsonError("Internal server error", e.getMessage());
			} catch (Error e) {
				logger.error("Cannot process GTFS-RT request", e);
				return APIHelper.createJsonError("Internal server error", e.getMessage());
			}
		} else {
			logger.error("Request parser did not initialize properly, cannot process request. Check the logs for startup exceptions.");
			return APIHelper.createJsonError("Internal server error",
					"Request parser did not initialize properly, cannot process request.");
		}
	}
	
	   /**
     * getSiriGM retrieves SIRI General Message deliveries as XML documents. Charset is UTF-8.
     * 
     * @return General Message deliveries as XML string
     */
    @RequestMapping(value = "/gm/siri", method = RequestMethod.POST, produces = "application/xml; charset=utf-8")
    public @ResponseBody
    String getSiriGM() {
        if (requestSAXParser != null && datasource != null) {
            try {  
                return datasource.getGeneralMessageData();
            } catch (Exception e) {
                logger.error("Cannot process SIRI request", e);
                return APIHelper.createXmlError("Internal server error", e.getMessage());
            } catch (Error e) {
                logger.error("Cannot process SIRI request", e);
                return APIHelper.createXmlError("Internal server error", e.getMessage());
            }
        } else {
            logger.error("Request parser did not initialize properly, cannot process request. Check the logs for startup exceptions.");
            return APIHelper.createXmlError("Internal server error",
                    "Request parser did not initialize properly, cannot process request.");
        }
    }

    /**
     * getJsonGM retrieves SIRI General Message deliveries as JSON documents. Charset is UTF-8.
     * 
     * @return General Message deliveries as JSON string
     */
    @RequestMapping(value = "/gm/json", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public @ResponseBody
    String getJsonGM(
            @RequestParam(value = "indent", required = false) String indent) {
        if (datasource != null) {
            try {
                String xmlResponse = datasource.getGeneralMessageData();
                if (xmlResponse != null) {
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    Siri siri = (Siri) unmarshaller.unmarshal(new StringReader(xmlResponse));                    
                    SiriJsonBuilder jsonBuilder = new SiriJsonBuilder(siri);
                    return jsonBuilder.buildJson(indent);
                } else {
                    return "";
                }

            } catch (Exception e) {
                logger.error("Cannot process SIRI request", e);
                return APIHelper.createJsonError("Internal server error", e.getMessage());
            } catch (Error e) {
                logger.error("Cannot process SIRI request", e);
                return APIHelper.createJsonError("Internal server error", e.getMessage());
            }
        } else {
            logger.error("Request parser did not initialize properly, cannot process request. Check the logs for startup exceptions.");
            return APIHelper.createJsonError("Internal server error",
                    "Request parser did not initialize properly, cannot process request.");
        }
    }
}
