package edu.metu.cytoscape.plugin.eclerize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.AbstractPartitionLayoutTask;
import org.cytoscape.view.layout.LayoutEdge;
import org.cytoscape.view.layout.LayoutNode;
import org.cytoscape.view.layout.LayoutPartition;
import org.cytoscape.view.layout.LayoutPoint;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.undo.UndoSupport;

public class EClerizeAlgorithmTask extends AbstractPartitionLayoutTask {

	/**
	 * A small value used to avoid division by zero
	   */
	protected static double EPSILON = 0.0000001D;

	/**
	 * The total number of layout passes
	 */
	private int maxLayoutPass = 10;

	/**
	 * The average number of iterations per Node
	 */
	private int averageIterationsPerNodeConstant = 40;
	
	/**
	 * Spring strength
	 */
	private double springStrengthsConstant=15.0;
	
	/** 
	 * Spring rest length"
	 */
	private double restLengthConstant=45.0;
	
	private double[] m_nodeDistanceSpringScalars;
	
	/**
	 * Strength of a 'disconnected' spring
	 */
	private double disconnectedSpringStrengthConstant=0.05;
	
	/**
	 * Rest length of a 'disconnected' spring"
	 */
	private double disconnectedSpringRestLengthConstant=2000.0;
	
	/**
	 * Strength to apply to avoid collisions
	 */
	private double anticollisionSpringStrengthConstant;
	
	private double[] anticollisionSpringScalars;

	/**
	 * Data arrays
	 */
	private double[][] restLengths;
	private double[][] springStrengths;
	

	/**
	 * Current layout pass
	 */
	private int layoutPassCounter = 0;

	/**
	 * The number of nodes
	 */
	private int nodeCount;

	/**
	 * The Partition
	 */
	private LayoutPartition partition;

	/**
	 * Profile data
	 */
	private EClerizeContext context;
	private CyNetworkView networkView;
	private CyTable nodeTable;
	private CyTable edgeTable;
	
	private List<LayoutEdge> partitionEdgeList;
	private List<LayoutNode> partitionNodeList;
	
	
	private CyEventHelper cyEventHelperServiceRef;
	
	private double ecStrenghtConstant;
	public double distanceFactor;
	private boolean addClusterDistance;
	
	/**
	 * This is the constructor for the bioLayout algorithm.
	 * @param supportEdgeWeights a boolean to indicate whether we should
	 *                                                  behave as if we support weights
	 */
	public EClerizeAlgorithmTask(final String displayName, CyNetworkView networkView, 
			Set<View<CyNode>> nodesToLayOut, final EClerizeContext context, 
			String attrName, UndoSupport undo,
			CyEventHelper cyEventHelperServiceRef) {
		
		super(displayName, context.singlePartition, networkView, nodesToLayOut,attrName, undo);
		this.networkView = networkView;
		this.nodeTable = networkView.getModel().getDefaultNodeTable();
		this.edgeTable = networkView.getModel().getDefaultEdgeTable();
		this.context = context;
		this.maxLayoutPass = context.maxLayoutPass;
		//this.edgeWeighter = context.edgeWeighter;
		//this.edgeWeighter.setWeightAttribute(layoutAttribute);
		this.averageIterationsPerNodeConstant = context.averageIterationsPerNodeConstant;
		this.springStrengthsConstant=context.springStrengthsConstant;
		this.restLengthConstant=context.restLengthConstant;
		this.disconnectedSpringStrengthConstant=context.disconnectedSpringStrengthConstant;
		this.disconnectedSpringRestLengthConstant=context.disconnectedSpringRestLengthConstant;
		this.anticollisionSpringStrengthConstant = context.anticollisionSpringStrengthConstant;
		this.cyEventHelperServiceRef = cyEventHelperServiceRef;
		
		this.ecStrenghtConstant = context.ecStrenghtConstant;
		this.distanceFactor = context.distanceFactor;
		
		this.addClusterDistance = context.addClusterDistance;
	}
	
	/**
	 * Perform a layout
	 */
	
	HashSet<LayoutNode> furtherIgnoreList = new HashSet<LayoutNode>();	
	
	public void layoutPartition(LayoutPartition partition) {
		
		List<LayoutEdge> originalEdgeList = new ArrayList<>();
		originalEdgeList.addAll( partition.getEdgeList() );
		
		initVariables(partition);
		ecTree = new ECTree();
		ecTree.setEcColumnNameAndType(nodeTable);
		
		ecTree.addVirtualEdges(nodeTable, edgeTable, partitionNodeList, partitionEdgeList,ecStrenghtConstant);

		LayoutPoint initialLocation = partition.getAverageLocation();

		if (context.randomize){
			partition.randomizeLocations();
		}
		
		if (cancelled) {
			return;
		}

		int numOfTotalIterations = (int) ((nodeCount * averageIterationsPerNodeConstant) / maxLayoutPass);	
		MyMonitorManager taskMonitorManager = new MyMonitorManager(taskMonitor,maxLayoutPass,nodeCount,numOfTotalIterations);
		
		int[][] nodeDistances = calculateNodeDistances();
				
		if (cancelled) {
			return;
		}
		
		calculateSpringData(nodeDistances);
		ecTree.changeECEdgesSpringData(springStrengths);
		
		runAlgorithm(partition, numOfTotalIterations, taskMonitorManager);
		
		if(addClusterDistance) {
			if(ecTree.getEstimatedEcColumnName() != null) {
				furtherIgnoreList = ecTree.getAllECNodes();
				ecTree.addDistancesToClusters(partition,distanceFactor);	
				runAlgorithm(partition, numOfTotalIterations, taskMonitorManager);
				
				System.err.println("EC MODE ON");
			}else {
				System.out.println("EC MODE OFF");
			}
		}	
		
		arrangePoisitons(partition, initialLocation);
		taskMonitorManager.finish();
		Colorize.colorize(cyEventHelperServiceRef, networkView, partition, nodeTable,ecTree.getEstimatedEcColumnName(),ecTree.isEcColumnListType());
	}

	private void arrangePoisitons(LayoutPartition partition,
			LayoutPoint initialLocation) {

		double xDelta = 0.0;
		double yDelta = 0.0;
		LayoutPoint finalLocation = partition.getAverageLocation();
		xDelta = finalLocation.getX() - initialLocation.getX();
		yDelta = finalLocation.getY() - initialLocation.getY();
		partition.resetNodes();
		for (LayoutNode v: partitionNodeList) {
			if (!v.isLocked()) {
				v.decrement(xDelta, yDelta);
				partition.moveNodeToLocation(v);
			}
		}
	}

	private void runAlgorithm(LayoutPartition partition,
			int numOfTotalIterations, MyMonitorManager taskMonitorManager) {
		List <PartialDerivatives> partialsList = new ArrayList<PartialDerivatives>();
		double[] potentialEnergy = new double[1];
		potentialEnergy[0] = 0.0;
		PartialDerivatives partials;
		PartialDerivatives furthestNodePartials = null;
		double euclideanDistanceThreshold = (nodeCount + partition.edgeCount()) / 2;

		for (layoutPassCounter = 0; layoutPassCounter < maxLayoutPass; layoutPassCounter++) {
			taskMonitorManager.startIteration(layoutPassCounter);			
			
			potentialEnergy[0] = 0.0;
			partialsList.clear();
			furthestNodePartials = null;

			for (LayoutNode v: partitionNodeList) {

				if (cancelled) {
					return;
				}

				if (v.isLocked()) {
					continue;
				}

				partials = new PartialDerivatives(v);
				calculatePartials(partials, potentialEnergy);
				partialsList.add(partials);
				
				if ((furthestNodePartials == null)
				    || (partials.euclideanDistance > furthestNodePartials.euclideanDistance)) {
					
					if(!furtherIgnoreList.contains(partials.node)) {
						furthestNodePartials = partials;
					}
				}	
				
				taskMonitorManager.progress();				
			}
						
			taskMonitorManager.beginSpringLogic(layoutPassCounter);
			
			int iterations_i;			
			
			for (iterations_i = 0;
			     (iterations_i < numOfTotalIterations)
			     && (furthestNodePartials.euclideanDistance >= euclideanDistanceThreshold);
			     iterations_i++) {
				if (cancelled) {
					return;
				}
				furthestNodePartials = moveNode(furthestNodePartials, partialsList, potentialEnergy);
				taskMonitorManager.progress();
			}
		}		
	}
	
	private void calculateSpringData(int[][] nodeDistances) {
		for (int node_i = 0; node_i < nodeCount; node_i++) {
			Arrays.fill(restLengths[node_i],
			            disconnectedSpringRestLengthConstant);
			Arrays.fill(springStrengths[node_i],
			            disconnectedSpringStrengthConstant);
		}

		for (LayoutEdge edge: partitionEdgeList) {
						 
			int node_i = edge.getSource().getIndex();
			int node_j = edge.getTarget().getIndex();
			
			double weight = 0.5;
			
			if (nodeDistances[node_i][node_j] != Integer.MAX_VALUE) {
				restLengths[node_i][node_j] = (restLengthConstant * nodeDistances[node_i][node_j]) / (weight);
				restLengths[node_j][node_i] = restLengths[node_i][node_j];

				springStrengths[node_i][node_j] = springStrengthsConstant / (nodeDistances[node_i][node_j] * nodeDistances[node_i][node_j]);
				springStrengths[node_j][node_i] = springStrengths[node_i][node_j];
			}
		}
	}
	
	private PartialDerivatives moveNode(PartialDerivatives furthestNodePartial, List<PartialDerivatives> partialsList,
			double[] potentialEnergy) {
		PartialDerivatives copy = new PartialDerivatives(furthestNodePartial);
		calculatePartialsWithExtraction(furthestNodePartial, partialsList, potentialEnergy);

		try {
			simpleMoveNode(copy);
		} catch (Exception e) {
			System.out.println(e);
		}

		return calculatePartialsWithInsertionAndReturnFurthest(furthestNodePartial, partialsList, potentialEnergy);
	}

	private void simpleMoveNode(PartialDerivatives partials) {
		LayoutNode node = partials.node;

		if (node.isLocked()) {
			return;
		}

		double denominator = ((partials.xx * partials.yy) - (partials.xy * partials.xy));

		if (((float) denominator) == 0.0) {
			System.out.println("denominator too close to 0 for node "+partials.node.getIndex());
			return;			
		}

		double deltaX = (((-partials.x * partials.yy) - (-partials.y * partials.xy)) / denominator);
		double deltaY = (((-partials.y * partials.xx) - (-partials.x * partials.xy)) / denominator);
		
		node.setLocation(node.getX() + deltaX, node.getY() + deltaY);		
	}	
	

	/**
	 * Here is the code for the partial derivative solver.  Note that for clarity,
	 * it has been devided into four parts:
	 *    calculatePartials -- main algorithm, calls the other three parts
	 *    calculateSpringPartial -- computes the first part of the spring partial (partial.x, partial.y)
	 *    calculateSpringPartial3 -- computes the second part of the partial (partial.xx, partial.yy)
	 *    calculateSpringPartialCross -- computes the final part of the partial (partial.xy)
	 *    calculatePE -- computes the potential energy
	 */
	private void calculatePartials(PartialDerivatives partials, double[] potentialEnergy) {
		partials.reset();

		LayoutNode node = partials.node;

		double nodeRadius = node.getWidth() / 2;
		double nodeX = node.getX();
		double nodeY = node.getY();
		LayoutNode otherNode;
		double otherNodeRadius;
				
		@SuppressWarnings("rawtypes")
		Iterator iterator =	partition.nodeIterator();		

		double deltaX;
		double deltaY;
		double otherNodeX;
		double otherNodeY;
		double euclideanDistance;
		double euclideanDistanceCubed;
		double distanceFromRest;
		double distanceFromTouching;		
		double[] xTable = { .01, .01, -.01, -.01 };
		double[] yTable = { .01, -.01, .01, -.01 };
		int offsetTable = 0;
		int nodeIndex = node.getIndex();

		while (iterator.hasNext()) {
			otherNode = (LayoutNode) iterator.next();
			
			if (node == otherNode) {
				continue;
			}
			if(node.isLocked() || otherNode.isLocked()) {
				continue;
			}

			otherNodeRadius = otherNode.getWidth() / 2;
			otherNodeX = otherNode.getX();
			otherNodeY = otherNode.getY();

			deltaX = nodeX - otherNodeX;
			deltaY = nodeY - otherNodeY;
			euclideanDistance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));

			if (((float) euclideanDistance) < 0.0001) {
				otherNodeX = otherNodeX + xTable[offsetTable];
				otherNodeY = otherNodeY + yTable[offsetTable++];

				if (offsetTable > 3)
					offsetTable = 0;

				otherNode.setX(otherNodeX);
				otherNode.setY(otherNodeY);
				euclideanDistance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
			}

			int otherNodeIndex = otherNode.getIndex();
			double radius = nodeRadius + otherNodeRadius;

			euclideanDistanceCubed = euclideanDistance * euclideanDistance * euclideanDistance;
			distanceFromTouching = euclideanDistance - (nodeRadius + otherNodeRadius);
			distanceFromRest = (euclideanDistance - restLengths[nodeIndex][otherNodeIndex]);

			
			partials.x += calculateSpringPartial(layoutPassCounter, distanceFromTouching, nodeIndex,
					otherNodeIndex, euclideanDistance, deltaX,
					radius);
			partials.y += calculateSpringPartial(layoutPassCounter, distanceFromTouching, nodeIndex,
					otherNodeIndex, euclideanDistance, deltaY,
					radius);
			partials.xx += calculateSpringPartial3(layoutPassCounter, distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed, deltaY * deltaY,
					radius);
			partials.yy += calculateSpringPartial3(layoutPassCounter, distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed, deltaX * deltaX,
					radius);
			partials.xy += calculateSpringPartialCross(layoutPassCounter, distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed, deltaX * deltaY,
					radius);

			potentialEnergy[0] += calculatePE(layoutPassCounter, distanceFromRest,
					distanceFromTouching, nodeIndex, otherNodeIndex);			

		} 
		
		partials.euclideanDistance = Math.sqrt((partials.x * partials.x)
					+ (partials.y * partials.y));		

		return;
	}

	private void calculatePartialsWithExtraction(PartialDerivatives partials, List<PartialDerivatives> partialsList,
			double[] potentialEnergy) {
		partials.reset();

		LayoutNode node = partials.node;

		double nodeRadius = node.getWidth() / 2;
		double nodeX = node.getX();
		double nodeY = node.getY();
		PartialDerivatives otherPartials = null;
		LayoutNode otherNode;
		double otherNodeRadius;
		@SuppressWarnings("rawtypes")
		Iterator iterator = partialsList.iterator();

		double deltaX;
		double deltaY;
		double otherNodeX;
		double otherNodeY;
		double euclideanDistance;
		double euclideanDistanceCubed;
		double distanceFromRest;
		double distanceFromTouching;		
		double[] xTable = { .01, .01, -.01, -.01 };
		double[] yTable = { .01, -.01, .01, -.01 };
		int offsetTable = 0;
		int nodeIndex = node.getIndex();

		while (iterator.hasNext()) {
			otherPartials = (PartialDerivatives) iterator.next();
			otherNode = otherPartials.node;
			
			if (node == otherNode) {
				continue;
			}
			if(node.isLocked() || otherNode.isLocked()) {
				continue;
			}

			otherNodeRadius = otherNode.getWidth() / 2;
			otherNodeX = otherNode.getX();
			otherNodeY = otherNode.getY();

			deltaX = nodeX - otherNodeX;
			deltaY = nodeY - otherNodeY;
			euclideanDistance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));

			if (((float) euclideanDistance) < 0.0001) {
				otherNodeX = otherNodeX + xTable[offsetTable];
				otherNodeY = otherNodeY + yTable[offsetTable++];

				if (offsetTable > 3)
					offsetTable = 0;

				otherNode.setX(otherNodeX);
				otherNode.setY(otherNodeY);
				euclideanDistance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
			}

			int otherNodeIndex = otherNode.getIndex();
			double radius = nodeRadius + otherNodeRadius;

			euclideanDistanceCubed = euclideanDistance * euclideanDistance * euclideanDistance;
			distanceFromTouching = euclideanDistance - (nodeRadius + otherNodeRadius);
			distanceFromRest = (euclideanDistance - restLengths[nodeIndex][otherNodeIndex]);

			otherPartials.x -= calculateSpringPartial(layoutPassCounter, distanceFromTouching,
					otherNodeIndex, nodeIndex,
					euclideanDistance, -deltaX, radius);
			otherPartials.y -= calculateSpringPartial(layoutPassCounter, distanceFromTouching,
					otherNodeIndex, nodeIndex,
					euclideanDistance, -deltaY, radius);
			otherPartials.xx -= calculateSpringPartial3(layoutPassCounter, distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed,
					deltaY * deltaY, radius);
			otherPartials.yy -= calculateSpringPartial3(layoutPassCounter, distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed,
					deltaX * deltaX, radius);
			otherPartials.xy -= calculateSpringPartialCross(layoutPassCounter,
					distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed,
					deltaX * deltaY, radius);
			potentialEnergy[0] -= calculatePE(layoutPassCounter, distanceFromRest,
					distanceFromTouching, nodeIndex,
					otherNodeIndex);					

			otherPartials.euclideanDistance = Math.sqrt((otherPartials.x * otherPartials.x)
					+ (otherPartials.y * otherPartials.y));


		} 

		return;
	}

	private PartialDerivatives calculatePartialsWithInsertionAndReturnFurthest(PartialDerivatives partials, List<PartialDerivatives> partialsList,double[] potentialEnergy) {
		partials.reset();

		LayoutNode node = partials.node;

		double nodeRadius = node.getWidth() / 2;
		double nodeX = node.getX();
		double nodeY = node.getY();
		PartialDerivatives otherPartials = null;
		LayoutNode otherNode;
		double otherNodeRadius;
		PartialDerivatives furthestPartials = null;
		@SuppressWarnings("rawtypes")
		Iterator iterator = partialsList.iterator();

		double deltaX;
		double deltaY;
		double otherNodeX;
		double otherNodeY;
		double euclideanDistance;
		double euclideanDistanceCubed;
		double distanceFromRest;
		double distanceFromTouching;		
		double[] xTable = { .01, .01, -.01, -.01 };
		double[] yTable = { .01, -.01, .01, -.01 };
		int offsetTable = 0;
		int nodeIndex = node.getIndex();

		while (iterator.hasNext()) {
			otherPartials = (PartialDerivatives) iterator.next();
			otherNode = otherPartials.node;
			
			if (node == otherNode) {
				continue;
			}
			if(node.isLocked() || otherNode.isLocked()) {
				continue;
			}

			otherNodeRadius = otherNode.getWidth() / 2;
			otherNodeX = otherNode.getX();
			otherNodeY = otherNode.getY();

			deltaX = nodeX - otherNodeX;
			deltaY = nodeY - otherNodeY;
			euclideanDistance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));

			if (((float) euclideanDistance) < 0.0001) {
				otherNodeX = otherNodeX + xTable[offsetTable];
				otherNodeY = otherNodeY + yTable[offsetTable++];

				if (offsetTable > 3)
					offsetTable = 0;

				otherNode.setX(otherNodeX);
				otherNode.setY(otherNodeY);
				euclideanDistance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
			}

			int otherNodeIndex = otherNode.getIndex();
			double radius = nodeRadius + otherNodeRadius;

			euclideanDistanceCubed = euclideanDistance * euclideanDistance * euclideanDistance;
			distanceFromTouching = euclideanDistance - (nodeRadius + otherNodeRadius);
			distanceFromRest = (euclideanDistance - restLengths[nodeIndex][otherNodeIndex]);

			partials.x += calculateSpringPartial(layoutPassCounter, distanceFromTouching, nodeIndex,
					otherNodeIndex, euclideanDistance, deltaX,
					radius);
			partials.y += calculateSpringPartial(layoutPassCounter, distanceFromTouching, nodeIndex,
					otherNodeIndex, euclideanDistance, deltaY,
					radius);
			partials.xx += calculateSpringPartial3(layoutPassCounter, distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed, deltaY * deltaY,
					radius);
			partials.yy += calculateSpringPartial3(layoutPassCounter, distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed, deltaX * deltaX,
					radius);
			partials.xy += calculateSpringPartialCross(layoutPassCounter, distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed, deltaX * deltaY,
					radius);

			potentialEnergy[0] += calculatePE(layoutPassCounter, distanceFromRest,
					distanceFromTouching, nodeIndex, otherNodeIndex);

			otherPartials.x += calculateSpringPartial(layoutPassCounter, distanceFromTouching,
					otherNodeIndex, nodeIndex,
					euclideanDistance, -deltaX, radius);
			otherPartials.y += calculateSpringPartial(layoutPassCounter, distanceFromTouching,
					otherNodeIndex, nodeIndex,
					euclideanDistance, -deltaY, radius);
			otherPartials.xx += calculateSpringPartial3(layoutPassCounter, distanceFromTouching,
					otherNodeIndex, nodeIndex,
					euclideanDistanceCubed,
					deltaY * deltaY, radius);
			otherPartials.yy += calculateSpringPartial3(layoutPassCounter, distanceFromTouching,
					otherNodeIndex, nodeIndex,
					euclideanDistanceCubed,
					deltaX * deltaX, radius);
			otherPartials.xy += calculateSpringPartialCross(layoutPassCounter,
					distanceFromTouching,
					nodeIndex, otherNodeIndex,
					euclideanDistanceCubed,
					deltaX * deltaY, radius);
			potentialEnergy[0] += calculatePE(layoutPassCounter, distanceFromRest,
					distanceFromTouching, nodeIndex,
					otherNodeIndex);					

			otherPartials.euclideanDistance = Math.sqrt((otherPartials.x * otherPartials.x)
					+ (otherPartials.y * otherPartials.y));

			if ((furthestPartials == null)
					|| (otherPartials.euclideanDistance > furthestPartials.euclideanDistance)) {
				
				if(!furtherIgnoreList.contains(otherPartials.node)) {
					furthestPartials = otherPartials;
				}
			}

		} 
		
		partials.euclideanDistance = Math.sqrt((partials.x * partials.x)
					+ (partials.y * partials.y));
		
		if ((furthestPartials == null)
				|| (partials.euclideanDistance > furthestPartials.euclideanDistance)) {
			if(!furtherIgnoreList.contains(partials.node)) {
				furthestPartials = partials;
			}
		}

		return furthestPartials;
	}
	
	private double calculateSpringPartial(int pass, double distToTouch, int nodeIndex,
			int otherNodeIndex, double eucDist, double delta,
			double radius) {
		double incrementalChange = 
				(m_nodeDistanceSpringScalars[pass] * (
						springStrengths[nodeIndex][otherNodeIndex] 
						* (delta - ((restLengths[nodeIndex][otherNodeIndex] * delta) / eucDist))));

		if (distToTouch < 0.0) {
			incrementalChange += (anticollisionSpringScalars[pass] * (anticollisionSpringStrengthConstant * (delta
					- ((radius * delta) / eucDist))));
		}

		return incrementalChange;
	}

	private double calculateSpringPartial3(int pass, double distToTouch, int nodeIndex,
	                                       int otherNodeIndex, double eucDist3, double value,
	                                       double radius) {
		double incrementalChange = (m_nodeDistanceSpringScalars[pass] * (springStrengths[nodeIndex][otherNodeIndex] * (1.0
		                                                                                                                            - ((restLengths[nodeIndex][otherNodeIndex] * value) / eucDist3))));

		if (distToTouch < 0.0) {
			incrementalChange += (anticollisionSpringScalars[layoutPassCounter] * (anticollisionSpringStrengthConstant * (1.0
			                                                                                                    - ((radius * value) / eucDist3))));
		}

		return incrementalChange;
	}

	private double calculateSpringPartialCross(int pass, double distToTouch, int nodeIndex,
	                                           int otherNodeIndex, double eucDist3, double value,
	                                           double radius) {
		double incrementalChange = (m_nodeDistanceSpringScalars[pass] * (springStrengths[nodeIndex][otherNodeIndex] * ((restLengths[nodeIndex][otherNodeIndex] * value) / eucDist3)));

		if (distToTouch < 0.0) {
			incrementalChange += ((anticollisionSpringScalars[layoutPassCounter] * (anticollisionSpringStrengthConstant * radius * value)) / eucDist3);
		}

		return incrementalChange;
	}

	private double calculatePE(int pass, double distToRest, double distToTouch, int nodeIndex,
	                           int otherNodeIndex) {
		double incrementalChange = (m_nodeDistanceSpringScalars[pass] * ((springStrengths[nodeIndex][otherNodeIndex] * (distToRest * distToRest)) / 2));

		if (distToTouch < 0.0) {
			incrementalChange += (anticollisionSpringScalars[pass] * ((anticollisionSpringStrengthConstant * (distToTouch * distToTouch)) / 2));
		}

		return incrementalChange;
	}
	
	private ECTree ecTree;
	
	private void initVariables(LayoutPartition partition) {
		this.partition = partition;
		
		nodeCount = partition.nodeCount();

		m_nodeDistanceSpringScalars = new double[maxLayoutPass];

		for (int i = 0; i < maxLayoutPass; i++) {
			m_nodeDistanceSpringScalars[i] = 1.0;
		}

		anticollisionSpringScalars = new double[maxLayoutPass];
		anticollisionSpringScalars[0] = 0.0;

		for (int i = 1; i < maxLayoutPass; i++) {
			anticollisionSpringScalars[i] = 1.0;
		}
		
		restLengths = new double[nodeCount][nodeCount];
		springStrengths = new double[nodeCount][nodeCount];
		
		partitionEdgeList = partition.getEdgeList();	
		partitionNodeList = partition.getNodeList();		
	}

	private int[][] calculateNodeDistances() {
		int[][] distances = new int[nodeCount][];
		LinkedList<Integer> queue = new LinkedList<Integer>();
		boolean[] completedNodes = new boolean[nodeCount];
		int fromNode;
		int neighbor;
		int toNodeDistance;
		int neighborDistance;

		for (LayoutNode v: partitionNodeList) {
			fromNode = v.getIndex();

			if (distances[fromNode] == null)
				distances[fromNode] = new int[nodeCount];

			Arrays.fill(distances[fromNode], Integer.MAX_VALUE);
			distances[fromNode][fromNode] = 0;
			Arrays.fill(completedNodes, false);
			queue.add(Integer.valueOf(fromNode));

			while (!(queue.isEmpty())) {
				int index = ((Integer) queue.removeFirst()).intValue();

				if (completedNodes[index])
					continue;

				completedNodes[index] = true;
				toNodeDistance = distances[fromNode][index];

				if (index < fromNode) {
					int distanceThroughToNode;

					for (int i = 0; i < nodeCount; i++) {
						if (distances[index][i] == Integer.MAX_VALUE)
							continue;

						distanceThroughToNode = toNodeDistance + distances[index][i];

						if (distanceThroughToNode <= distances[fromNode][i]) {
							if (distances[index][i] == 1)
								completedNodes[i] = true;

							distances[fromNode][i] = distanceThroughToNode;
						}
					}

					continue;
				}

				List<LayoutNode> neighborList = v.getNeighbors();
				for (LayoutNode neighbor_v: neighborList) {
					neighbor = neighbor_v.getIndex();

					if (completedNodes[neighbor])
						continue;

					neighborDistance = distances[fromNode][neighbor];

					if ((toNodeDistance != Integer.MAX_VALUE)
					    && (neighborDistance > (toNodeDistance + 1))) {
						distances[fromNode][neighbor] = toNodeDistance + 1;
						queue.addLast(Integer.valueOf(neighbor));
					}
				}
			}
		}

		return distances;
	}
	
	/**
	 * Overrides for LayoutAlgorithm support
	 */
	public String getName() {
		return "EClerize";
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @return  DOCUMENT ME!
	 */
	public String toString() {
		return "EClerize";
	}

	/**
	 * Sets the number of iterations
	 *
	 * @param value the number of iterations
	 */
	public void setNumberOfIterationsPerNode(int value) {
		averageIterationsPerNodeConstant = value;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param value DOCUMENT ME!
	 */
	public void setNumberOfIterationsPerNode(String value) {
		Integer val = Integer.valueOf(value);
		averageIterationsPerNodeConstant = val.intValue();
	}

	/**
	 * Sets the number of layout passes
	 *
	 * @param value the number of layout passes
	 */
	public void setNumberOfLayoutPasses(int value) {
		maxLayoutPass = value;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param value DOCUMENT ME!
	 */
	public void setNumberOfLayoutPasses(String value) {
		Integer val = Integer.valueOf(value);
		maxLayoutPass = val.intValue();
	}

	/**
	 * Sets the distance spring strength contant
	 *
	 * @param value the distance spring strength contant
	 */
	public void setDistanceSpringStrength(double value) {
		springStrengthsConstant = value;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param value DOCUMENT ME!
	 */
	public void setDistanceSpringStrength(String value) {
		Double val = new Double(value);
		springStrengthsConstant = val.doubleValue();
	}

	/**
	 * Sets the rest length constant
	 *
	 * @param value the rest length constant
	 */
	public void setDistanceRestLength(double value) {
		restLengthConstant = value;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param value DOCUMENT ME!
	 */
	public void setDistanceRestLength(String value) {
		Double val = new Double(value);
		restLengthConstant = val.doubleValue();
	}

	/**
	 * Sets the disconnected node distance spring strength
	 *
	 * @param value the disconnected node distance spring strength
	 */
	public void setDisconnectedSpringStrength(double value) {
		disconnectedSpringStrengthConstant = value;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param value DOCUMENT ME!
	 */
	public void setDisconnectedSpringStrength(String value) {
		Double val = new Double(value);
		disconnectedSpringStrengthConstant = val.doubleValue();
	}

	/**
	 * Sets the disconnected node sprint rest length
	 *
	 * @param value the disconnected node sprint rest length
	 */
	public void setDisconnectedRestLength(double value) {
		disconnectedSpringRestLengthConstant = value;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param value DOCUMENT ME!
	 */
	public void setDisconnectedRestLength(String value) {
		Double val = new Double(value);
		disconnectedSpringRestLengthConstant = val.doubleValue();
	}

	/**
	 * Sets the anticollision spring strength
	 *
	 * @param value the anticollision spring strength
	 */
	public void setAnticollisionSpringStrength(double value) {
		anticollisionSpringStrengthConstant = value;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param value DOCUMENT ME!
	 */
	public void setAnticollisionSpringStrength(String value) {
		Double val = new Double(value);
		anticollisionSpringStrengthConstant = val.doubleValue();
	}

}