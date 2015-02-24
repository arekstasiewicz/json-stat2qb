package org.deri.jsonstat2qb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.Manifest;

import net.hamnaberg.funclite.Optional;

import org.apache.jena.iri.IRIFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.deri.jsonstat2qb.jsonstat.Dataset;
import org.deri.jsonstat2qb.jsonstat.Dimension;
import org.deri.jsonstat2qb.jsonstat.Helpers;
import org.deri.jsonstat2qb.jsonstat.Role;
import org.deri.jsonstat2qb.jsonstat.Stat;
import org.deri.jsonstat2qb.jsonstat.parser.JacksonStatParser;

import arq.cmdline.ArgDecl;
import arq.cmdline.CmdGeneral;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.sparql.util.Utils;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import org.deri.jsonstat2qb.jsonstat.Category;
import org.deri.jsonstat2qb.jsonstat.Data;
import org.deri.jsonstat2qb.jsonstat.table.CsvRenderer;
import org.deri.jsonstat2qb.jsonstat.table.Table;
import org.deri.vocab.DataCube;
import org.deri.vocab.JSONSTAT;
import org.deri.vocab.SKOS;

public class jsonstat2qb extends CmdGeneral {

    private final static Logger log = Logger.getLogger(jsonstat2qb.class);

    // --version info
    public static final String VERSION;
    public static final String BUILD_DATE;

    static {
        String version = "Unknown";
        String date = "Unknown";
        try {
            URL res = jsonstat2qb.class.getResource(jsonstat2qb.class.getSimpleName() + ".class");
            Manifest manifest = ((JarURLConnection) res.openConnection()).getManifest();
            version = (String) manifest.getMainAttributes().getValue("Implementation-Version");
            date = (String) manifest.getMainAttributes().getValue("Built-Date");
        } catch (Exception ex) {
            // do nothing
        }
        VERSION = version;
        BUILD_DATE = date;
    }

    public static void main(String[] args) {
        new jsonstat2qb(args).mainRun();
    }

    private String datasetUrl = null;
    private String encoding = null;
    private boolean writeNTriples = false;

    private final ArgDecl encodingArg = new ArgDecl(true, "encoding", "e");
    private final ArgDecl nTriplesArg = new ArgDecl(false, "ntriples");

    public jsonstat2qb(String[] args) {
        super(args);

        getUsage().startCategory("Options");
        add(encodingArg, "-e   --encoding", "Override source file encoding (e.g., utf-8 or latin-1)");
        add(nTriplesArg, "--ntriples", "Write N-Triples instead of Turtle");
        getUsage().startCategory("Main arguments");

        getUsage().addUsage("datasetUrl", "Link to the converted file.");

        modVersion.addClass(jsonstat2qb.class);

    }

    @Override
    protected String getSummary() {
        return getCommandName() + " [options] datasetUrl";
    }

    @Override
    protected void processModulesAndArgs() {

        if (getPositional().isEmpty()) {
            doHelp();
        }

        if (hasArg(encodingArg)) {
            encoding = getValue(encodingArg);
        }

        if (hasArg(nTriplesArg)) {
            writeNTriples = true;
        }

        datasetUrl = getPositionalArg(0);
        if (datasetUrl == null || datasetUrl.length() < 1) {
            cmdError("Value of datasetUrl must be valid URL");
        }

    }

    @Override
    protected void exec() {
        initLogging();
        try {

            if (encoding != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Encoding: " + encoding);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Dataset: " + datasetUrl);
            }

            InputStream input = open(datasetUrl);
            Stat stat = new JacksonStatParser().parse(input);

            // TODO - process multiple datasets?
            Optional<Dataset> dataset = stat.getDataset(0);

            for (Dataset ds : dataset) {

                if (log.isDebugEnabled()) {
                    System.out.println("ds.size() = " + ds.size());
                    List<Dimension> dimensions = ds.getDimensions();
                    for (Dimension dimension : dimensions) {
                        System.out.println("dimension label: " + dimension.getLabel().get());
                    }
                }

                processResults(dataset.get());

            }

        } catch (NotFoundException ex) {
            cmdError("Not found: " + ex.getMessage());
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private InputStream open(String datasetUrl) {
        InputStream in = FileManager.get().open(datasetUrl);
        if (in == null) {
            throw new NotFoundException(datasetUrl);
        }
        return in;
    }

    @Override
    protected String getCommandName() {
        return Utils.className(this);
    }

    private void initLogging() {
        if (isQuiet()) {
            Logger.getRootLogger().setLevel(Level.ERROR);
        }
        if (isVerbose()) {
            Logger.getLogger("org.deri.jsonstat2qb").setLevel(Level.INFO);
        }
        if (isDebug()) {
            Logger.getLogger("org.deri.jsonstat2qb").setLevel(Level.DEBUG);
        }
    }

    private void processResults(Dataset dataset) {

        Model model = ModelFactory.createDefaultModel();

        model.setNsPrefix("json-stat", JSONSTAT.getURI());
        model.setNsPrefix("qb", DataCube.getURI());

        // TODO keep separate tables for observations and measure
        // TODO order obs obs obs measure
        
        // TODO "source"
        // TODO "updated"

        // TODO add base prefix
        // TODO allow user input

        // Used in DS, DSD, dimensions etc.
        String datasetNameSpace = Helpers.makeSafeName( dataset.getLabel().get() );

        // DataSet
        Resource ds = model.createResource("dataset/" + datasetNameSpace);
        ds.addProperty(RDF.type, DataCube.DataSet);
        ds.addProperty(RDFS.label, model.createLiteral(dataset.getLabel().get()));

        // Data Structure Definition
        Resource dsd = model.createResource("structure/" + datasetNameSpace);
        dsd.addProperty(RDF.type, DataCube.DataStructureDefinition);

        // Define dimensions
        List<Dimension> dimensions = dataset.getDimensions();
        
        // Collect Categories
        // ArrayList <Category> categoriesIndex = new ArrayList<>();
        
        HashMap<String, Category> categoriesIndex = new HashMap<String, Category>();
        
        
        // Dimension order
        int index = 1;
        for (Dimension dm : dimensions) {
        	String dimUri = "structure/" + datasetNameSpace + "/component/" + index;
        	
        	
        	if (! dm.getRole().isNone()){
        		dimUri = dm.getRole().get() == Role.metric ? "structure/" + datasetNameSpace + "/measure" : "structure/" + datasetNameSpace + "/component/" + index;
        	}

        	Resource d = model.createResource( dimUri );
        	d.addProperty(RDF.type, DataCube.ComponentSpecification);
        	d.addProperty(DataCube.order, model.createTypedLiteral(index));

        	// Define the property
        	// TODO allow user to decide about type
        	// TODO try to guess type / hierarchy from JSON
        	// TODO codeList? <http://data.cso.ie/census-2011/property/have-a-personal-computer> <http://purl.org/linked-data/cube#codeList> <http://data.cso.ie/census-2011/classification/have-a-personal-computer> .
        	Resource prop = model.createResource( dimUri );
        	prop.addProperty(DataCube.dimension, d);

        	prop.addProperty(RDF.type, RDF.Property);
        	prop.addProperty(RDF.type, SKOS.Concept);
        	prop.addProperty(RDFS.range, DataCube.ComponentProperty);
        	prop.addProperty(RDFS.range, DataCube.DimensionProperty);

        	prop.addProperty(RDFS.label, model.createLiteral(dm.getLabel().get()));

        	// Add to dsd
        	dsd.addProperty(DataCube.component, d);

        	Category categories = dm.getCategory();
        	
        	// Get Categories (exclude "metric")
        	if (! dm.getRole().isNone() && dm.getRole().get() != Role.metric ){
        		categoriesIndex.put(dm.getId(), categories);
        	}

        	// tmp
        	categoriesIndex.put(dm.getId(), categories);

        	index++;         
        }
        
        
        
        int valueIndex = 0;
        int currentRow = categoriesIndex.size() - 1;
        
        Iterator entries = categoriesIndex.entrySet().iterator();

        while (entries.hasNext()) {
        	  Entry thisEntry = (Entry) entries.next();
        	  Object key = thisEntry.getKey();
        	  Category value = (Category) thisEntry.getValue();
        	  System.out.println( key + " " + value);
        	  
          	for (String v : value) {
        		valueIndex++;
        		System.out.println( key + " " + v);
        	}
          	
          	
        	}
        
        
        
        
//    	for (Dimension dm : dimensions) {
//        	for ( Category cat : categoriesIndex ){
//        		for (String value : cat) {
//        			System.out.println( dm.getLabel().get() + ": " + value );
//        		}
//        	}
//    	}
    	

        
        
        
        /* TODO Waqar */
        
        // generate list of categories for each value
        // Cartesian product
        
        
        
        
        // Observations
        List<Data> values = dataset.getValues();

        for (Data value : values) {
        	// System.out.println( value );
        }

//        rr:logicalTable <#TablesView>;
//
//        rr:subjectMap [
//            rr:template <dataset/{"DATASET"}/{"AREA_ID"};{"DIMENSION_IRI"}>; 
//            rr:class qb:Observation ;
//        ];
//
//        rr:predicateObjectMap [
//            rr:predicateMap [ rr:template <property/{"PROPERTY_1"}>; ];
//            rr:objectMap [ rr:template 'classification/{"PROPERTY_1"}/{"DIMENSION_IRI"}'; ];
//        ];
//
//        rr:predicateObjectMap [
//            rr:predicate sdmx-dimension:refArea;
//            rr:objectMap [ rr:template <classification/ST/{"AREA_ID"}>; ];
//        ];
//
//        rr:predicateObjectMap [
//            rr:predicate qb:dataSet;
//            rr:objectMap  [ rr:template <dataset/{"DATASET"}>] ;
//        ];
//
//        rr:predicateObjectMap [
//            rr:predicateMap [ rr:template <property/{"MEASURE"}> ];
//            rr:objectMap [ rr:column '"OBSERVATION_VALUE"'; rr:datatype xsd:int ];
//        ];

        // Link ds and dsd
        ds.addProperty(DataCube.structure, dsd);


        
        
        

//        for (Dimension dm : dim) {
//
//            Resource dimension = model.createResource();
//            dimension.addProperty(RDF.type, JSONSTAT.Dimension);
//            dimension.addProperty(RDFS.label, dm.getId());
//            
//            
//            Category cat = dm.getCategory();
//          
//            for (String value : cat) {
//                Resource index = model.createResource();
//                index.addProperty(RDF.type, JSONSTAT.KeyValue);
//                index.addProperty(JSONSTAT.key, model.createLiteral(value));
//                index.addProperty(JSONSTAT.value, model.createTypedLiteral(cat.getIndex(value)));
//                dimension.addProperty(JSONSTAT.indexs, index);
//                Resource label = model.createResource();
//                label.addProperty(RDF.type, JSONSTAT.KeyValue);
//                label.addProperty(JSONSTAT.key, model.createLiteral(value));
//                label.addProperty(JSONSTAT.value, model.createTypedLiteral(cat.getLabel(value).get()));
//                dimension.addProperty(JSONSTAT.labels, label);
//            }

           // resource.addProperty(JSONSTAT.dimensions, dimension);
//        }

        if (writeNTriples){
        //	model.write(System.out, "N-Triples");
        } else {
        //	model.write(System.out, "RDF/XML-ABBREV");
        }

    }

}
