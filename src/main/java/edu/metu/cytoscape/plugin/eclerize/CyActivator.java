package edu.metu.cytoscape.plugin.eclerize;

import java.util.Locale;
import java.util.Properties;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;
import org.cytoscape.work.ServiceProperties;


public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		UndoSupport undoSupportServiceRef = getService(bc,UndoSupport.class);
		CyEventHelper cyEventHelperServiceRef = getService(bc,CyEventHelper.class);
		
		Properties modifiedKKAlgorithmProps = new Properties();
		modifiedKKAlgorithmProps.setProperty(ServiceProperties.PREFERRED_MENU, "Apps");
		modifiedKKAlgorithmProps.setProperty("preferredTaskManager", "Menu");
		modifiedKKAlgorithmProps.setProperty(ServiceProperties.TITLE,"EClerize");
		modifiedKKAlgorithmProps.setProperty(ServiceProperties.MENU_GRAVITY,"10.88");		
		
		EClerizeAlgorithm modifiedKKAlgorithm = new EClerizeAlgorithm(undoSupportServiceRef, cyEventHelperServiceRef);
		registerService(bc, modifiedKKAlgorithm, CyLayoutAlgorithm.class, modifiedKKAlgorithmProps);
		
		System.out.println("locale " + Locale.getDefault());
	}
}
