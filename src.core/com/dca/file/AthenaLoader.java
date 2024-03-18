package com.dca.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.dca.file.simplesax.SimpleSAX;

public class AthenaLoader extends AbstractAthenaLoader{

	@Override
	public void loadFromStream(DocumentLoadingContext context, InputStream source, String fileName) throws ASIFLoadException,
			IOException {
		System.out.println("Loading .athena file");
		
		InputSource xmlSource = new InputSource(source);
		ASIFHandler handler = new ASIFHandler(context);
		
		AthenaStandardInputDocument doc = context.getASIFDocument();
		
		try {
			SimpleSAX.readXML(xmlSource, handler);
		} catch (SAXException e) {
			System.out.println("Malformed XML in input");
			throw new ASIFLoadException("Malformed XML in input.", e);
		}
		
		// load the stage activeness
		for (FlightConfiguration config : doc.getRocket().getFlightConfigurations()) {
			config.applyPreloadedStageActiveness();
		}
		
		// If we saved data for a simulation before, we'll use that as our default option this time
		// Also, updaet all the sims' modIDs to agree with flight config
		for (Simulation s : doc.getSimulations()) {
			s.syncModID();		// The config's modID can be out of sync with the simulation's after the whole loading process
			if (s.getStatus() == Simulation.Status.EXTERNAL ||
					s.getStatus() == Simulation.Status.NOT_SIMULATED)
				continue;
			if (s.getSimulatedData() == null)
				continue;
			if (s.getSimulatedData().getBranchCount() == 0)
				continue;
			FlightDataBranch branch = s.getSimulatedData().getBranch(0);
			if (branch == null)
				continue;
			List<Double> list = branch.get(FlightDataType.TYPE_TIME);
			if (list == null)
				continue;

			doc.getDefaultStorageOptions().setSaveSimulationData(true);
		}

		doc.getDefaultStorageOptions().setExplicitlySet(false);
		doc.getDefaultStorageOptions().setFileType(FileType.OPENROCKET);
		
		// Call simulation extensions
		for (Simulation sim : doc.getSimulations()) {
			for (SimulationExtension ext : sim.getSimulationExtensions()) {
				ext.documentLoaded(doc, sim);
			}
		}
		
		
		doc.clearUndo();
		System.out.println("Loading done");
	}
}
