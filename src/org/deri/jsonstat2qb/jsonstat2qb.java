package org.deri.jsonstat2qb;

import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

import arq.cmdline.ArgDecl;
import arq.cmdline.CmdGeneral;

public class jsonstat2qb extends CmdGeneral {
	
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

	public static void main(String... args) {
		new jsonstat2qb(args).mainRun();
	}

	private String datasetUrl;
	private String encoding = null;
	private boolean writeNTriples = false;
		
	private final ArgDecl encodingArg = new ArgDecl(true, "encoding", "e");
	private final ArgDecl nTriplesArg = new ArgDecl(false, "ntriples");


	public jsonstat2qb(String[] args) {
		super(args);
		getUsage().startCategory("Options");
		add(encodingArg,      "-e   --encoding", "Override source file encoding (e.g., utf-8 or latin-1)");
		add(nTriplesArg,      "--ntriples", "Write N-Triples instead of Turtle");
		getUsage().startCategory("Main arguments");
		getUsage().addUsage("datasetUrl", "Link to the converted file.");
		modVersion.addClass(jsonstat2qb.class);

	}

	@Override
	protected String getSummary() {
		// TODO Auto-generated method stub
		return null;
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

		if (hasArg(datasetUrl)) {
			datasetUrl = getValue(datasetUrl);
			if (datasetUrl == null || datasetUrl.length() < 1) {
				cmdError("Value of datasetUrl must be valid URL");
			}
		}
	}

	@Override
	protected void exec() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String getCommandName() {
		// TODO Auto-generated method stub
		return null;
	}
	
//    public static void main(String[] args) {
//    	System.out.println("Hello World!"); 
//    	System.out.println(VERSION); 
//    	System.out.println(BUILD_DATE); 
//    }
}
