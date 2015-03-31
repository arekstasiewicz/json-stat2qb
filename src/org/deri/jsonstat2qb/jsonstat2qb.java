package org.deri.jsonstat2qb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import net.hamnaberg.funclite.Optional;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.deri.jsonstat2qb.jsonstat.Category;
import org.deri.jsonstat2qb.jsonstat.Data;
import org.deri.jsonstat2qb.jsonstat.Dataset;
import org.deri.jsonstat2qb.jsonstat.Dimension;
import org.deri.jsonstat2qb.jsonstat.Helpers;
import org.deri.jsonstat2qb.jsonstat.Role;
import org.deri.jsonstat2qb.jsonstat.Stat;
import org.deri.jsonstat2qb.jsonstat.parser.JacksonStatParser;
import org.deri.vocab.DataCube;
import org.deri.vocab.JSONSTAT;
import org.deri.vocab.SKOS;
import arq.cmdline.ArgDecl;
import arq.cmdline.CmdGeneral;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.sparql.util.Utils;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class jsonstat2qb extends CmdGeneral {

    private final static Logger log = Logger.getLogger(jsonstat2qb.class);

    // --version info
    public static final String VERSION;
    public static final String BUILD_DATE;

    static {
        String version = "1.0";
        String date = "30/03/2015";
        try {
            URL res = jsonstat2qb.class.getResource(jsonstat2qb.class.getSimpleName() + ".class");
            Manifest manifest = ((JarURLConnection) res.openConnection()).getManifest();
            version = (String) manifest.getMainAttributes().getValue("Implementation-Version");
            date = (String) manifest.getMainAttributes().getValue("Built-Date");
        } catch (Exception ex) {
            System.err.println(ex);
        }
        VERSION = version;
        BUILD_DATE = date;
    }

    public static void main(String[] args) {
        new jsonstat2qb(args).mainRun();
    }

    private String datasetUrl = null;
    private String baseUri = null;
    private String encoding = null;
    private boolean writeNTriples = false;
    private boolean validateCube = false;
    private final ArgDecl encodingArg = new ArgDecl(true, "encoding", "e");
    private final ArgDecl nTriplesArg = new ArgDecl(false, "ntriples");
    private final ArgDecl validateCubeArg = new ArgDecl(false, "validate");
    private final ArgDecl baseUriArg = new ArgDecl(true, "baseURI", "b");
    public jsonstat2qb(String[] args) {
        super(args);

        getUsage().startCategory("Options");
        add(encodingArg, "-e   --encoding", "Override source file encoding (e.g., utf-8 or latin-1)");
        add(nTriplesArg, "--ntriples", "Write N-Triples instead of Turtle");
        add(validateCubeArg, "--validate", "Test output data against DataCube queries");
        add(baseUriArg, "-b   --baseuri", "baseuri for uri e.g. http://example./ ");
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

        if (hasArg(validateCubeArg)) {
            validateCube = true;
        }

        if (hasArg(baseUriArg)) {	
           baseUri = getValue(baseUriArg);
        }else {
           baseUri = "http://example/";
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
                log.debug("NTriples output: " + writeNTriples);
                log.debug("Do the validation: " + validateCube);
            }

            InputStream input = open(datasetUrl);
            Stat stat = new JacksonStatParser().parse(input);

            // TODO - process multiple datasets?
            Optional<Dataset> dataset = stat.getDataset(0);
            Model model = ModelFactory.createDefaultModel();
            model.setNsPrefix("json-stat", JSONSTAT.getURI());
            model.setNsPrefix("qb", DataCube.getURI());
            model.setNsPrefix("xsd", XSD.getURI());
            
            int index = 0; 
            for (Dataset ds : dataset) {
                String datasetId = "ds-"+ index;
                if (log.isDebugEnabled()) {
                    System.out.println("ds.size() = " + ds.size());
                    List<Dimension> dimensions = ds.getDimensions();
                    for (Dimension dimension : dimensions) {
                        System.out.println("dimension label: " + dimension.getLabel().get());
                    }
                }
                model.setNsPrefix(datasetId , baseUri + datasetId + "/");
                processResults(model, dataset.get(), baseUri + datasetId + "/");
                index++;

            }
            model.write(System.out, "TTL");
        } catch (NotFoundException ex) {
            cmdError("Not found: " + ex.getMessage());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
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

    private void processResults(Model model, Dataset dataset, String datasetNameSpace) {

        // TODO keep separate tables for observations and measure
        // TODO order obs obs obs measure
        // TODO "source"
        // TODO "updated"
        // TODO add base prefix
        // TODO allow user input
        // Used in DS, DSD, dimensions etc.
     
        // DataSet
        Resource ds = model.createResource(datasetNameSpace);
        ds.addProperty(RDF.type, DataCube.DataSet);
        ds.addProperty(RDFS.label, model.createLiteral(dataset.getLabel().get()));

        // Data Structure Definition
        Resource dsd = model.createResource(datasetNameSpace + "structure");
        dsd.addProperty(RDF.type, DataCube.DataStructureDefinition);

        // Define dimensions
        List<Dimension> dimensions = dataset.getDimensions();

        // Collect Categories
        // ArrayList <Category> categoriesIndex = new ArrayList<>();
        HashMap<String, Category> categoriesIndex = new HashMap<String, Category>();
        HashMap<Integer, String> measuresIndex = new HashMap<Integer, String>();

        // Dimension order
        int index = 1;
        for (Dimension dm : dimensions) {
            Resource comSpec = model.createResource(datasetNameSpace  + "components/" + Helpers.makeSafeName(dm.getId()));
            comSpec.addProperty(RDF.type, DataCube.ComponentSpecification);
            comSpec.addProperty(DataCube.order, model.createTypedLiteral(index));

            // Define the property
            // TODO allow user to decide about type
            // TODO try to guess type / hierarchy from JSON
            // TODO codeList? <http://data.cso.ie/census-2011/property/have-a-personal-computer> <http://purl.org/linked-data/cube#codeList> <http://data.cso.ie/census-2011/classification/have-a-personal-computer> .
            //Resource dim = model.createResource(datasetNameSpace+ "components/" + Helpers.makeSafeName(dm.getId()));
          
               // dim.addProperty(DataCube.measure, comSpec);
           
            comSpec.addProperty(DataCube.dimension, model.createResource(datasetNameSpace  + Helpers.makeSafeName(dm.getId())));
            comSpec.addProperty(RDF.type, RDF.Property);
            comSpec.addProperty(RDF.type, SKOS.Concept);
            comSpec.addProperty(RDFS.range, DataCube.ComponentProperty);
            comSpec.addProperty(RDFS.range, DataCube.DimensionProperty);
            comSpec.addProperty(RDFS.label, model.createLiteral(dm.getLabel().get()));

            // Add to dsd
            dsd.addProperty(DataCube.component, comSpec);

    
            index++;
        }

        
         Resource comSpec = model.createResource(datasetNameSpace  + "components/value");
         comSpec.addProperty(RDF.type, DataCube.ComponentSpecification);
         comSpec.addProperty(DataCube.order, model.createTypedLiteral(index));
         comSpec.addProperty(DataCube.measure, model.createResource(datasetNameSpace  + "value"));
         comSpec.addProperty(RDF.type, RDF.Property);
         comSpec.addProperty(RDF.type, SKOS.Concept);
         comSpec.addProperty(RDFS.range, DataCube.ComponentProperty);
         comSpec.addProperty(RDFS.range, DataCube.DimensionProperty);
         comSpec.addProperty(RDFS.label, model.createLiteral("Value"));
        dsd.addProperty(DataCube.component, comSpec);

        int valueIndex = 0;
        int currentRow = categoriesIndex.size() - 1;

        Iterator entries = categoriesIndex.entrySet().iterator();

        while (entries.hasNext()) {
            Entry thisEntry = (Entry) entries.next();
            Object key = thisEntry.getKey();
            Category value = (Category) thisEntry.getValue();
            // System.out.println(key + " " + value);

            for (String v : value) {
                valueIndex++;
                // System.out.println(key + " " + v);
            }

        }


        // Cartesian product for the dimensions and measures
        LinkedHashMap<String, List<String>> dataList = new LinkedHashMap<String, List<String>>();;

        for (Dimension dm : dimensions) {
            Category cat = dm.getCategory();
            List<String> list = new ArrayList<String>();
            for (String value : cat) {
                list.add(Helpers.makeSafeName(cat.getLabel(value).get().toString()));
            }
            dataList.put(dm.getId(), list);
        }

        List<List<String>> product = new ArrayList<List<String>>();
        product.add(new ArrayList<String>());
        for (List<String> x : dataList.values()) {
            List<List<String>> t1 = new ArrayList<List<String>>();
            for (List<String> z : product) {
                for (String y : x) {
                    List<String> t2 = new ArrayList<String>(z);
                    t2.add(y);
                    t1.add(t2);
                }
            }
            product = t1;
        }

        int m = product.size() + 1;
        int n = dataList.keySet().size();

        String[][] combinations = new String[m][n];

        combinations[0] = dataList.keySet().toArray(new String[n]);

        for (int i = 1; i < m; i++) {
            for (int j = 0; j < n; j++) {
                combinations[i][j] = product.get(i - 1).get(j);
            }
        }

        int count = 0;

        String[] header = null;

        // Observations
        for (String[] combination : combinations) {
            count++;
            if (header == null) {
                header = combination;
                continue;
            }

            // generate unique Observation URI
            String obsURI =  datasetNameSpace + "observation-" + count;

           // Add Observations
            Resource obs = model.createResource(obsURI);
            obs.addProperty(RDF.type, DataCube.Observation);
            obs.addProperty(DataCube.dataSet, ds);
            int k = 0;
            for (k = 0; k < combination.length ; k++) {
                String dimUri = datasetNameSpace + Helpers.makeSafeName(combinations[0][k]);
                obs.addProperty(model.createProperty(dimUri), model.createResource(datasetNameSpace+"v/" + Helpers.makeSafeName(combination[k])));
            }     
            // TODO measure type as parameter
            try {
                obs.addProperty(model.createProperty( datasetNameSpace  + "value"), model.createTypedLiteral(Double.parseDouble(dataset.getValue(count - 1).toString())));
            } catch (Exception e) {
                //missing value
                obs.addProperty(model.createProperty( datasetNameSpace  + "value"), model.createTypedLiteral(0.0));
            }
            obs.addProperty(DataCube.dataSet, XSD.integer);
            count++;
        }

    }

}
