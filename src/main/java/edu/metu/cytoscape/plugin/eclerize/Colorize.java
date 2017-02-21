package edu.metu.cytoscape.plugin.eclerize;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.LayoutNode;
import org.cytoscape.view.layout.LayoutPartition;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

public class Colorize {
	
	public static void colorize(CyEventHelper cyEventHelperServiceRef, 
			CyNetworkView networkView,LayoutPartition partition,
			CyTable nodeTable, String ecColumn,boolean isEcColumnListType) {

		ArrayList<DelayedVizProp> vizList = new ArrayList<DelayedVizProp>();
		
		List<LayoutNode> nodes = partition.getNodeList();
		for(LayoutNode n : nodes) {
			
			if(ecColumn!=null) {
				String s = getEc(nodeTable, n.getNode(), ecColumn, isEcColumnListType);
				
				ColorCode c = ColorCode.getCode(s);
				DelayedVizProp d = new DelayedVizProp(n.getNode(), BasicVisualLexicon.NODE_FILL_COLOR, c.getColor(), false);
				vizList.add(d);		
			}						
		}
		cyEventHelperServiceRef.flushPayloadEvents();
		DelayedVizProp.applyAll(networkView, vizList);
	}
	
	private static String getEc(CyTable nodeTable, CyNode node,String estimatedEcColumnName,boolean isEcColumnListType) {
		CyRow row = nodeTable.getRow(node.getSUID());
		
		if(isEcColumnListType) {
			List<String> list = row.getList(estimatedEcColumnName, String.class);
			if(list != null && list.size() > 0 ) {
				return list.get(0);
			}
		}else {
			return row.get(estimatedEcColumnName, String.class);
		}		
		return "";
	}
	
}
