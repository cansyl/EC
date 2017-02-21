package edu.metu.cytoscape.plugin.eclerize;

/*
 * #%L
 * Cytoscape Layout Algorithms Impl (layout-cytoscape-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator;

public class EClerizeContext implements TunableValidator {
	/**
	 * The average number of iterations per Node
	 */
	
	//biolayoutcontext
//	@ContainsTunables
//	public EdgeWeighter edgeWeighter = new EdgeWeighter();
	
	/**
	 * Whether or not to initialize by randomizing all points
	 */
	
	@Tunable(description="Intra-cluster attractive force constant",groups="EClerize settings")
	public double ecStrenghtConstant=1;
	
	@Tunable(description="Add Cluster Distances",groups="EClerize settings")
	public boolean addClusterDistance = false;	
	
	@Tunable(description="Inter-cluster repulsive constant",groups="EClerize settings")
	public double distanceFactor=0.1;	
		
	@Tunable(description="Randomize graph before layout", groups="Standard settings")
	public boolean randomize = true;
	
	@Tunable(description="Average number of iteratations for each node")
	public int averageIterationsPerNodeConstant = 40;
	
	@Tunable(description="Spring strength")
	public double springStrengthsConstant=15.0;
	
	@Tunable(description="Spring rest length")	
	public double restLengthConstant=45.0;
	
	@Tunable(description="Strength of a 'disconnected' spring")
	public double disconnectedSpringStrengthConstant=0.05;
	
	@Tunable(description="Rest length of a 'disconnected' spring")
	public double disconnectedSpringRestLengthConstant=2000;
	
	@Tunable(description="Strength to apply to avoid collisions")
	public double anticollisionSpringStrengthConstant=0.05;
	
	@Tunable(description="Number of layout passes")
	public int maxLayoutPass = 10;
	//@Tunable(description="Don't partition graph before layout", groups="Standard settings")
	public boolean singlePartition;
	//@Tunable(description="Use unweighted edges", groups="Standard settings")
	public boolean unweighted;
	
	@Override // TODO
	public ValidationState getValidationState(final Appendable errMsg) {
		return ValidationState.OK;
	}

}
