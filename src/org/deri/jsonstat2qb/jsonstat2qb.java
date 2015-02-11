package org.deri.jsonstat2qb;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Manifest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import arq.cmdline.ArgDecl;
import arq.cmdline.CmdGeneral;

import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.sparql.util.Utils;

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
		add(encodingArg,      "-e   --encoding", "Override source file encoding (e.g., utf-8 or latin-1)");
		add(nTriplesArg,      "--ntriples", "Write N-Triples instead of Turtle");
		getUsage().startCategory("Main arguments");

		getUsage().addUsage("datasetUrl", "Link to the converted file.");

		modVersion.addClass(jsonstat2qb.class);

	}

	@Override
	protected String getSummary() {
		return getCommandName() + " datasetUrl [options]";
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
				System.out.println(encoding);
			}
		} catch (NotFoundException ex) {
			cmdError("Not found: " + ex.getMessage());
		}
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

}
