package edu.metu.cytoscape.plugin.eclerize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.LayoutEdge;
import org.cytoscape.view.layout.LayoutNode;
import org.cytoscape.view.layout.LayoutPartition;

public class ECTree {

	private ArrayList<VirtualEdge> virtualEdges = new ArrayList<VirtualEdge>();
	private String estimatedEcColumnName;
	private boolean isEcColumnListType;	

	private HashSet<LayoutNode> first = new HashSet<LayoutNode>();
	private HashSet<LayoutNode> second = new HashSet<LayoutNode>();
	private HashSet<LayoutNode> third = new HashSet<LayoutNode>();
	private HashSet<LayoutNode> fourth = new HashSet<LayoutNode>();
	private HashSet<LayoutNode> fifth = new HashSet<LayoutNode>();
	private HashSet<LayoutNode> sixth = new HashSet<LayoutNode>();
	
	public String setEcColumnNameAndType(CyTable nodeTable) {
		Collection<CyColumn> columns = nodeTable.getColumns();
		for(CyColumn c : columns) {
			if(c.getName().equalsIgnoreCase("ec") || c.getName().equalsIgnoreCase("ecnumber") || c.getName().equalsIgnoreCase("ec number")) {
				setEcColumnListType(c.getType().equals(java.util.List.class));			
				setEstimatedEcColumnName(c.getName());
			}
		}	
		return null;
	}

	public void addVirtualEdges(CyTable nodeTable,
			CyTable edgeTable,
			List<LayoutNode> partitionNodeList,
			List<LayoutEdge> partitionEdgeList,
			double ecStrenghtConstant) {

		if(getEstimatedEcColumnName()==null) {
			return;
		}		

		String firstEc = "";
		String secondEc = "";
		LayoutNode firstNode;
		LayoutNode secondNode;

		StringBuilder s = new StringBuilder();
		
		for(int i = 0 ; i < partitionNodeList.size() ; i++) {

			firstNode = partitionNodeList.get(i);
			firstEc = getEc(nodeTable,firstNode.getNode());

			if( firstEc != null && firstEc.trim().length() > 0 ) {

				for(int j = i+1; j < partitionNodeList.size() ; j ++) {

					secondNode = partitionNodeList.get(j);
					secondEc = getEc(nodeTable,secondNode.getNode());

					nodeTable.getRow(secondNode.getNode().getSUID()).getAllValues();

					if(secondEc != null && secondEc.trim().length() > 0) {

						double m = getMultiplier(firstEc, secondEc, ecStrenghtConstant);

						if(m > 0) {
							partitionEdgeList.add(new LayoutEdge(null,firstNode,secondNode,null));							
							virtualEdges.add(new VirtualEdge(firstNode,secondNode,m));							
							s.append("" + firstNode.getNode().getSUID() + "\t"
									+ secondNode.getNode().getSUID() + "\t"									
									+ nodeTable.getRow(firstNode.getNode().getSUID()).get("name",String.class) + "\t"									
									+ nodeTable.getRow(secondNode.getNode().getSUID()).get("name",String.class) + "\t" 
									+ i + "\t"
									+ j + "\t"
									+ m + "\t"
									+ firstEc + "\t"
									+ secondEc + "\n");
							
							if(firstEc.startsWith("1")) {
								first.add(firstNode);
								first.add(secondNode);
							}else if(firstEc.startsWith("2")) {
								second.add(firstNode);
								second.add(secondNode);
							}else if(firstEc.startsWith("3")) {
								third.add(firstNode);
								third.add(secondNode);
							}else if(firstEc.startsWith("4")) {
								fourth.add(firstNode);
								fourth.add(secondNode);
							}else if(firstEc.startsWith("5")) {
								fifth.add(firstNode);
								fifth.add(secondNode);
							}else if(firstEc.startsWith("6")) {
								sixth.add(firstNode);
								sixth.add(secondNode);
							}
						}
					}
				}
			}
		}
		
		//Test.writeToFile(s.toString(), "virtualEdges");
	}

	private String getEc(CyTable nodeTable,CyNode node) {
		CyRow row = nodeTable.getRow(node.getSUID());

		if(isEcColumnListType()) {
			List<String> list = row.getList(getEstimatedEcColumnName(), String.class);
			if(list != null && list.size() > 0 ) {
				return list.get(0);
			}
		}else {
			return row.get(getEstimatedEcColumnName(), String.class);
		}		
		return "";
	}

	public double getMultiplier(String firstEc,String secondEc, double ecStrenghtConstant) {
		double k = ecStrenghtConstant;

		String[] first = firstEc.split("\\.");
		String[] second = secondEc.split("\\.");


		String firstClass = first.length > 0 ? first[0] : null;
		String firstSubClass = first.length > 1 ? first[1] : null;
		String firstSubSubClass = first.length > 2 ? first[2] : null;
		String firstId = first.length > 3 ? first[3] : null;

		String secondClass = second.length > 0 ? second[0] : null;
		String secondSubClass = second.length > 1 ? second[1] : null;
		String secondSubSubClass = second.length > 2 ? second[2] : null;
		String secondId = second.length > 3 ? second[3] : null;

		if(firstClass!=null && 
				secondClass!=null &&
				firstClass.equals(secondClass)) {
			if(firstSubClass!=null &&
					secondSubClass!=null &&
					firstSubClass.equals(secondSubClass) ) {
				if(firstSubSubClass!=null &&
						secondSubSubClass!=null &&
						firstSubSubClass.equals(secondSubSubClass)) {
					if(firstId!=null &&
							secondId!=null &&
							firstId.equals(secondId)) {
						return 4 * k * 4;
					}
					return 3 * k * 3;
				}				
				return 2 * k * 2;
			}			
			return k;
		}

		return 0.0;		
	}


	public void changeECEdgesSpringData(double[][] springStrengths) {
		if(virtualEdges == null || virtualEdges.size() < 1) {
			return;
		}

		StringBuilder s = new StringBuilder();		
		
		for(VirtualEdge e : virtualEdges) {
			double d = springStrengths[e.to.getIndex()][e.from.getIndex()];
			double rate = d * e.multiplier;
			
			springStrengths[e.to.getIndex()][e.from.getIndex()] = rate;
			springStrengths[e.from.getIndex()][e.to.getIndex()] = rate;
		}
		
		for(int i = 0; i<springStrengths.length ; i++) {
			double d[] = springStrengths[i];
			for(int j = 0 ; j < d.length ; j++) {
				if(d[j]!=0.05 && d[j]!=15.0) {
					s.append(""+d[j] + "\t");
				}
			}
			s.append("\n");
		}
	
		//Test.writeToFile(s.toString(),"changeEcSpringData");
	}

	public String getEstimatedEcColumnName() {
		return estimatedEcColumnName;
	}

	public void setEstimatedEcColumnName(String estimatedEcColumnName) {
		this.estimatedEcColumnName = estimatedEcColumnName;
	}

	public boolean isEcColumnListType() {
		return isEcColumnListType;
	}

	public void setEcColumnListType(boolean isEcColumnListType) {
		this.isEcColumnListType = isEcColumnListType;
	}

	public HashSet<LayoutNode> getAllECNodes()  {
		HashSet<LayoutNode> list = new HashSet<LayoutNode>();
		list.addAll(first);
		list.addAll(second);
		list.addAll(third);
		list.addAll(fourth);
		list.addAll(fifth);
		list.addAll(sixth);		

		return list;
	}

	public void addDistancesToClusters(LayoutPartition partition, double distanceFactor) {

		Double[] centerOfClusters=new Double[2];
		centerOfClusters[0]=centerOfClusters[1]=0.0d;
		
		int totalNode=0;
		
		Double[] centerOfFirst = null;
		Double[] centerOfSecond = null;
		Double[] centerOfThird = null;
		Double[] centerOfFourth = null;
		Double[] centerOfFifth = null;
		Double[] centerOfSixth = null;

		if(first.size()>0) {
			centerOfFirst=getCenter(first);
			int size = first.size();
			totalNode+=size;
			
			centerOfClusters[0] += centerOfFirst[0] * size;
			centerOfClusters[1] += centerOfFirst[1] * size;
		}if(second.size()>0) {
			centerOfSecond=getCenter(second);
			int size = second.size();
			totalNode+=size;
			
			centerOfClusters[0] += centerOfSecond[0] * size;
			centerOfClusters[1] += centerOfSecond[1] * size;
		}if(third.size()>0) {
			centerOfThird=getCenter(third);
			int size = third.size();
			totalNode+=size;
			
			centerOfClusters[0] += centerOfThird[0] * size;
			centerOfClusters[1] += centerOfThird[1] * size;
		}if(fourth.size()>0) {
			centerOfFourth=getCenter(fourth);
			int size = fourth.size();
			totalNode+=size;
					
			centerOfClusters[0] += centerOfFourth[0] * size;
			centerOfClusters[1] += centerOfFourth[1] * size;
		}if(fifth.size()>0) {
			centerOfFifth=getCenter(fifth);
			int size = fifth.size();
			totalNode+=size;
			
			centerOfClusters[0] += centerOfFifth[0] * size;
			centerOfClusters[1] += centerOfFifth[1] * size;
		}if(sixth.size()>0) {
			centerOfSixth=getCenter(sixth);
			int size = sixth.size();
			totalNode+=size;
			
			centerOfClusters[0] += centerOfSixth[0] * size;
			centerOfClusters[1] += centerOfSixth[1] * size;
		}

		centerOfClusters[0] /= totalNode;
		centerOfClusters[1] /= totalNode;

		Double[] diff = new Double[2];
		diff[0] = diff [1] = 0.0d;
		if(first.size()>0) {
			diff[0] = centerOfFirst[0] - centerOfClusters[0];
			diff[1] = centerOfFirst[1] - centerOfClusters[1];			
			increaseAllNodesPos(partition,first,diff,distanceFactor);
		}if(second.size()>0) {
			diff[0] = centerOfSecond[0] - centerOfClusters[0];
			diff[1] = centerOfSecond[1] - centerOfClusters[1];
			increaseAllNodesPos(partition,second,diff,distanceFactor);
		}if(third.size()>0) {
			diff[0] = centerOfThird[0] - centerOfClusters[0];
			diff[1] = centerOfThird[1] - centerOfClusters[1];
			increaseAllNodesPos(partition,third,diff,distanceFactor);
		}if(fourth.size()>0) {
			diff[0] = centerOfFourth[0] - centerOfClusters[0];
			diff[1] = centerOfFourth[1] - centerOfClusters[1];
			increaseAllNodesPos(partition,fourth,diff,distanceFactor);
		}if(fifth.size()>0) {
			diff[0] = centerOfFifth[0] - centerOfClusters[0];
			diff[1] = centerOfFifth[1] - centerOfClusters[1];
			increaseAllNodesPos(partition,fifth,diff,distanceFactor);
		}if(sixth.size()>0) {
			diff[0] = centerOfSixth[0] - centerOfClusters[0];
			diff[1] = centerOfSixth[1] - centerOfClusters[1];
			increaseAllNodesPos(partition,sixth,diff,distanceFactor);
		}
	}

	private void increaseAllNodesPos(LayoutPartition partition, HashSet<LayoutNode> list, Double[] diff,double distanceFactor) {
		Iterator<LayoutNode> it = list.iterator();
		while(it.hasNext()) {
			LayoutNode ln = it.next();
			ln.increment(diff[0] * distanceFactor, diff[1] * distanceFactor);
			partition.moveNodeToLocation(ln);
		}
	}

	public Double[] getCenter(HashSet<LayoutNode> list) {
		if(list==null || list.size()<1) {
			return new Double[2];
		}
		Double[] xy = new Double[2];
		xy[0] = xy[1] = 0.0d;

		Iterator<LayoutNode> it = list.iterator();
		while(it.hasNext()) {
			LayoutNode ln = it.next();
			xy[0] += ln.getX();
			xy[1] += ln.getY();
		}
		xy[0] = xy[0] / list.size();
		xy[1] = xy[1] / list.size();

		return xy;
	}



}
