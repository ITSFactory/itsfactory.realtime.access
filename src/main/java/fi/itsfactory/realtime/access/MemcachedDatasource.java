package fi.itsfactory.realtime.access;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import fi.itsfactory.realtime.access.siri.vm.VMResponseFilter;

public class MemcachedDatasource implements SiriDatasource {
	private static final Logger logger = LoggerFactory.getLogger(MemcachedDatasource.class);
	
	private String cacheUrl;
	private String vmKey;
	private String gmKey;
	private SAXParser responseSAXParser;
	
	public MemcachedDatasource(String cacheUrl, String vmKey, String gmKey) {
		this.cacheUrl = cacheUrl;
		this.vmKey = vmKey;
		this.gmKey = gmKey;
		
		responseSAXParser = null;
	}
	
	@Override
	public synchronized String getVehicleMonitoringData(String lineRef, String vehicleRef) {
        try {
            /*
             * Fetch the cached response. A backend process updates the (mem)cache(d) periodically (once per
             * second). cacheUrl and vmKey(the key the backend process uses to store the response) are
             * injected into this class in the constructor.
             */
            MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(cacheUrl));
            MemcachedClient memcachedClient = builder.build();
            memcachedClient.setTranscoder(new SerializingTranscoder(100 * 1024 * 1024));
            String latestVM = memcachedClient.get(vmKey);

            memcachedClient.shutdown();

            if (responseSAXParser != null) { // init failed, don't even try
                /*
                 * If the client has specified either lineRef or vehicleRef, we have to filter the response.
                 * Since the backend system pushes one single document to the cache, which contains data for
                 * all vehicles, we have to parse the XML and filter our during parsing.
                 */
                try {
                    VMResponseFilter responseFilter = new VMResponseFilter(lineRef, vehicleRef);
                    responseSAXParser.parse(new ByteArrayInputStream(latestVM.getBytes()), responseFilter);
                    latestVM = responseFilter.getFilteredDocument();
                    return latestVM;
                } catch (SAXException e) {
                    return null;
                } catch (Throwable t) {
                    return null;
                }
            } else {
                logger.error("Response parser not initialized properly, cannot process request since client requests filtering. "
                        + "Check the logs for startup exceptions.");
                return null;
            }
        } catch (IOException e) {
            logger.error("Cannot process request", e);
            return null;
        } catch (TransformerFactoryConfigurationError e) {
            logger.error("Cannot process request", e);
            return null;
        } catch (TimeoutException e) {
            logger.error("Cannot process request", e);
            return null;
        } catch (InterruptedException e) {
            logger.error("Cannot process request", e);
            return null;
        } catch (MemcachedException e) {
            logger.error("Cannot process request", e);
            return null;
        }
	}

	    public String getGeneralMessageData() {
	        try {
	            /*
	             * Fetch the cached response. A backend process updates the
	             * (mem)cache(d) periodically (once per second). cacheUrl and gmKey(the
	             * key the backend process uses to store the response) are injected into
	             * this class in the constructor.
	             */
	            MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(cacheUrl));
	            MemcachedClient memcachedClient = builder.build();
	            String latestVM = memcachedClient.get(gmKey);
	            memcachedClient.shutdown();

	            return latestVM;
	        } catch (IOException e) {
	            logger.error("Cannot process request", e);
	            return null;
	        } catch (TimeoutException e) {
                logger.error("Cannot process request", e);
                return null;
            } catch (InterruptedException e) {
                logger.error("Cannot process request", e);
                return null;
            } catch (MemcachedException e) {
                logger.error("Cannot process request", e);
                return null;
            }
	    }
	
	@Override
	public void initialize() {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			responseSAXParser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			logger.error("Initialization failed", e);
		} catch (SAXException e) {
			logger.error("Initialization failed", e);
		}
	}

	@Override
	public void shutdown() {
		//No persistent resoures -nothing to do here.
	}

}
