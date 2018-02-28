package za.ac.sun.cs.green.util;

public class NotSatException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4606648765708961052L;

	public NotSatException() {
		super("The specified constraints are trivially non-sat");
	}
	
}