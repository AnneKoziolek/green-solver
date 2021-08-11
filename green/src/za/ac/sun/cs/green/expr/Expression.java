package za.ac.sun.cs.green.expr;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class Expression implements Comparable<Expression>, Externalizable {
    public Serializable metadata;
    public int serializationMark;

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
		if (metadata instanceof HashSet) {
			out.writeBoolean(true);
			HashSet set = (HashSet) metadata;
			out.writeInt(set.size());
			for (Object each : set) {
				out.writeObject(each);
			}
		} else {
			out.writeBoolean(false);
			out.writeObject(metadata);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		if (in.readBoolean()) {
			int size = in.readInt();
			HashSet map = new HashSet(size);
			this.metadata = map;
			for(int i = 0; i < size; i++){
				map.add(in.readObject());
			}
		} else {
			metadata = (Serializable) in.readObject();
		}
	}
}
