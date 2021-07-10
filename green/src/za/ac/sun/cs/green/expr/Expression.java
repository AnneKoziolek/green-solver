package za.ac.sun.cs.green.expr;

import java.io.*;

public abstract class Expression implements Comparable<Expression>, Externalizable {
    public Serializable metadata;

	/**
	 *
	 */
	private static final long serialVersionUID = 7300060803953598321L;

	public abstract void accept(Visitor visitor) throws VisitorException;

	public abstract Expression copy(Copier copier);

	@Override
	public final int compareTo(Expression expression) {
		return toString().compareTo(expression.toString());
	}

	@Override
	public abstract boolean equals(Object object);

	@Override
	public abstract String toString();

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(metadata);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	    metadata = (Serializable) in.readObject();
	}
}
