package com.dca.file;

public class ASIFDocumentFactory {

	public static AthenaStandardInputDocument createNewRocket() {
		Rocket rocket = new Rocket();
		AxialStage stage = new AxialStage();
		//// Sustainer
		stage.setName(trans.get("BasicFrame.StageName.Sustainer"));
		rocket.addChild(stage);
		rocket.getSelectedConfiguration().setAllStages();
		AthenaStandardInputDocument doc = new AthenaStandardInputDocument(rocket);
		doc.setSaved(true);
		return doc;
	}
	
	public static AthenaStandardInputDocument createDocumentFromRocket(Rocket r) {
		AthenaStandardInputDocument doc = new AthenaStandardInputDocument(r);
		return doc;
	}
	
	public static AthenaStandardInputDocument createEmptyRocket() {
		Rocket rocket = new Rocket();
		AthenaStandardInputDocument doc = new AthenaStandardInputDocument(rocket);
		return doc;
	}
}
