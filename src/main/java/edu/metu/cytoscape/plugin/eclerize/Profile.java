package edu.metu.cytoscape.plugin.eclerize;

/**
 * This class is used to provide some simple profiling
 */
public class Profile {
	long startTime;
	long totalTime;

	/**
	 * Creates a new Profile object.
	 */
	public Profile() {
		this.startTime = 0;
		this.totalTime = 0;
	}

	/**
	 *  DOCUMENT ME!
	 */
	public void start() {
		this.startTime = System.currentTimeMillis();
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @return  DOCUMENT ME!
	 */
	public long checkpoint() {
		long runTime = System.currentTimeMillis() - this.startTime;
		this.totalTime += runTime;

		return runTime;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param message DOCUMENT ME!
	 */
	public void done(String message) {
		// Get our runtime
		checkpoint();

		System.out.println(message + this.totalTime + "ms");
		this.totalTime = 0;
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @return  DOCUMENT ME!
	 */
	public long getTotalTime() {
		return this.totalTime;
	}
}