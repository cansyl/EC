package edu.metu.cytoscape.plugin.eclerize;

import org.cytoscape.work.TaskMonitor;

public class MyMonitorManager {
	
	private final double percentCompletedBeforePasses = 5.0d;
	private final double percentCompletedAfterPass1 = 60.0d;
	private final double percentCompletedAfterFinalPass = 95.0d;
	private TaskMonitor taskMonitor;
	private int maxLayoutPass;	
	private int nodeCount;
	private double numOfTotalIterations;
	
	private double currentProgress;
	private double percentProgressPerIter;
	

	public MyMonitorManager(final TaskMonitor taskMonitor, final int maxLayoutPass,final int nodeCount,final double numOfTotalIterations) {
		currentProgress = percentCompletedBeforePasses;
		this.taskMonitor = taskMonitor;
		this.maxLayoutPass = maxLayoutPass;
		this.nodeCount = nodeCount;
		this.numOfTotalIterations = numOfTotalIterations;
	}
		
	public void startIteration(int layoutPassConstant) {
		percentProgressPerIter = 0;
		if (layoutPassConstant == 0) {
			percentProgressPerIter = (percentCompletedAfterPass1 - percentCompletedBeforePasses) / (double) (nodeCount
			                         + numOfTotalIterations);
		} else {
			percentProgressPerIter = (percentCompletedAfterFinalPass
			                         - percentCompletedAfterPass1) / (double) ((nodeCount
			                                                                   + numOfTotalIterations) * (maxLayoutPass
			                                                                                      - 1));
		}
		taskMonitor.setStatusMessage("Calculating partial derivatives -- pass " + (layoutPassConstant + 1)
                + " of " + maxLayoutPass);
	}
	
	public void progress() {
		taskMonitor.setProgress(currentProgress/100.0);
		currentProgress += percentProgressPerIter;
	}
	
	public void beginSpringLogic(int layoutPassConstant) {
		taskMonitor.setStatusMessage("Executing spring logic -- pass " + (layoutPassConstant + 1) + " of "
                + maxLayoutPass);
	}

	public void finish() {
		taskMonitor.setProgress(percentCompletedAfterFinalPass/100.0);
		taskMonitor.setStatusMessage("Updating display");		
	}
	
}
