package org.deri.vocab;

import com.hp.hpl.jena.rdf.model.*;
 
public class SKOS {

    private static Model m_model = ModelFactory.createDefaultModel();
    
    public static final String NS = "http://www.w3.org/2004/02/skos/core#";
    
    public static String getURI() {return NS;}
    
    public static final Resource NAMESPACE = m_model.createResource( NS );

    public static final Property altLabel = m_model.createProperty( NS + "altLabel" );    
    public static final Property broadMatch = m_model.createProperty( NS + "broadMatch" );
    public static final Property broader = m_model.createProperty( NS + "broader" );
    public static final Property broaderTransitive = m_model.createProperty( NS + "broaderTransitive" );
    public static final Property changeNote = m_model.createProperty( NS + "changeNote" );
    public static final Property closeMatch = m_model.createProperty( NS + "closeMatch" );
    public static final Property definition = m_model.createProperty( NS + "definition" );
    public static final Property editorialNote = m_model.createProperty( NS + "editorialNote" );
    public static final Property exactMatch = m_model.createProperty( NS + "exactMatch" );
    public static final Property example = m_model.createProperty( NS + "example" );
    public static final Property hasTopConcept = m_model.createProperty( NS + "hasTopConcept" );
    public static final Property hiddenLabel = m_model.createProperty( NS + "hiddenLabel" );
    public static final Property historyNote = m_model.createProperty( NS + "historyNote" );
    public static final Property inScheme = m_model.createProperty( NS + "inScheme" );
    public static final Property mappingRelation = m_model.createProperty( NS + "mappingRelation" );
    public static final Property member = m_model.createProperty( NS + "member" );
    public static final Property memberList = m_model.createProperty( NS + "memberList" );
    public static final Property narrowMatch = m_model.createProperty( NS + "narrowMatch" );
    public static final Property narrower = m_model.createProperty( NS + "narrower" );
    public static final Property narrowerTransitive = m_model.createProperty( NS + "narrowerTransitive" );
    public static final Property notation = m_model.createProperty( NS + "notation" );
    public static final Property note = m_model.createProperty( NS + "note" );
    public static final Property prefLabel = m_model.createProperty( NS + "prefLabel" );
    public static final Property related = m_model.createProperty( NS + "related" );
    public static final Property relatedMatch = m_model.createProperty( NS + "relatedMatch" );
    public static final Property scopeNote = m_model.createProperty( NS + "scopeNote" );
    public static final Property semanticRelation = m_model.createProperty( NS + "semanticRelation" );
    public static final Property topConceptOf = m_model.createProperty( NS + "topConceptOf" );

    public static final Resource Collection = m_model.createResource( NS + "Collection" );
    public static final Resource Concept = m_model.createResource( NS + "Concept" );
    public static final Resource ConceptScheme = m_model.createResource( NS + "ConceptScheme" );
    public static final Resource OrderedCollection = m_model.createResource( NS + "OrderedCollection" );
    
}