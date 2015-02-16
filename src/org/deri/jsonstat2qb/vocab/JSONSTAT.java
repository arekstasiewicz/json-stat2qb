package org.deri.jsonstat2qb.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class JSONSTAT {

    public static String getURI() {
        return uri;
    }
    protected static final String uri = "http://json-states.org/json-stat#";
    private static Model m = ModelFactory.createDefaultModel();
    public static final Resource Dataset = m.createResource(uri + "Dataset");
    public static final Property dimensions = m.createProperty(uri + "dimensions");
    public static final Resource Dimension = m.createResource(uri + "Dimension");
    public static final Property indexs = m.createProperty(uri + "indexs");
    public static final Property labels = m.createProperty(uri + "labels");
    public static final Resource KeyValue= m.createResource(uri + "KeyValue");
    public static final Property key = m.createProperty(uri + "key");
    public static final Property value = m.createProperty(uri + "value");
}
