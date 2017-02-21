package edu.metu.cytoscape.plugin.eclerize;

import org.cytoscape.view.layout.LayoutNode;

public class PartialDerivatives {
	final LayoutNode node;
	double x;
	double y;
	double xx;
	double yy;
	double xy;
	double euclideanDistance;

	PartialDerivatives(LayoutNode node) {
		this.node = node;
	}

	PartialDerivatives(PartialDerivatives copyFrom) {
		this.node = copyFrom.node;
		copyFrom(copyFrom);
	}

	String printPartial() {
		String retVal = "Partials for node " + node.getIndex() + " are: " + x + "," + y + ","
		                + xx + "," + yy + "," + xy + " dist = " + euclideanDistance;

		return retVal;
	}

	void reset() {
		x = 0.0;
		y = 0.0;
		xx = 0.0;
		yy = 0.0;
		xy = 0.0;
		euclideanDistance = 0.0;
	}

	void copyFrom(PartialDerivatives otherPartialDerivatives) {
		x = otherPartialDerivatives.x;
		y = otherPartialDerivatives.y;
		xx = otherPartialDerivatives.xx;
		yy = otherPartialDerivatives.yy;
		xy = otherPartialDerivatives.xy;
		euclideanDistance = otherPartialDerivatives.euclideanDistance;
	}
}
