package fi.itsfactory.realtime.access;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import uk.org.siri.siri.Siri;

public class SiriJsonBuilder {
    private Siri siri;
    
    public SiriJsonBuilder(Siri siri) {
        this.siri = siri;
    }
    
    public String buildJson(String indent) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("deprecation")
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
        mapper.setAnnotationIntrospector(introspector);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        if (indent != null && indent.equals("yes")) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return mapper.writeValueAsString(siri);
    }
}
