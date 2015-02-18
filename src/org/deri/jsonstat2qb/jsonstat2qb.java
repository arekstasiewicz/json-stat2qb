package org.deri.jsonstat2qb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.List;
import java.util.jar.Manifest;

import net.hamnaberg.funclite.Optional;

import org.apache.jena.iri.IRIFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.deri.jsonstat2qb.jsonstat.Dataset;
import org.deri.jsonstat2qb.jsonstat.Dimension;
import org.deri.jsonstat2qb.jsonstat.Helpers;
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

        int index = 0;
        for (Dimension dm : dimensions) {
        	System.out.println("Current index is: " + ++index);
        }


        // Define measure
        
        // Link ds and dsd
        ds.addProperty(DataCube.structure, dsd.getURI());
        


        
        
        
//        
//
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
        	model.write(System.out, "N-Triples");
        } else {
        	model.write(System.out, "RDF/XML-ABBREV");
        }

    }

}
