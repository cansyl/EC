package edu.metu.cytoscape.plugin.eclerize;

import java.util.HashSet;
import java.util.Set;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.AbstractLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TunableValidator;
import org.cytoscape.work.TunableValidator.ValidationState;
import org.cytoscape.work.undo.UndoSupport;

public class EClerizeAlgorithm  extends AbstractLayoutAlgorithm {
	
	public CyEventHelper cyEventHelperServiceRef;
	public static final String UNWEIGHTEDATTRIBUTE = "(unweighted)";
		
	public EClerizeAlgorithm(UndoSupport undo,CyEventHelper cyEventHelperServiceRef) {
		super("EClerize","EClerize",undo);
		this.cyEventHelperServiceRef = cyEventHelperServiceRef;
	}
	
	public TaskIterator createTaskIterator(CyNetworkView networkView, Object context, Set<View<CyNode>> nodesToLayOut, String attrName) {
		return new TaskIterator(
			new EClerizeAlgorithmTask(toString(), networkView, 
										nodesToLayOut, (EClerizeContext)context, 
										attrName, undoSupport,
										cyEventHelperServiceRef));
										
	}
	
	@Override
	public boolean isReady(CyNetworkView view, Object tunableContext, Set<View<CyNode>> nodesToLayout, String attributeName) {
		if (view == null || nodesToLayout == null)
			return false;
		
		if (nodesToLayout.size() == 0 && view.getNodeViews().size() == 0)
			return false;
		
		if (tunableContext instanceof TunableValidator) {
			StringBuilder errors = new StringBuilder();
			return ((TunableValidator) tunableContext).getValidationState(errors) == ValidationState.OK;
		}
		return true;
	}
	
	@Override
	public EClerizeContext createLayoutContext() {
		return new EClerizeContext();
	}
	
	@Override
	public Set<Class<?>> getSupportedEdgeAttributeTypes() {
		Set<Class<?>> ret = new HashSet<Class<?>>();

		ret.add( Integer.class );
		ret.add( Double.class );

		return ret;
	}

	@Override
	public boolean getSupportsSelectedOnly() {
		return true;
	}
}