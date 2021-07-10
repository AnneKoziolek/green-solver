package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BVVariable extends Variable {
	private static final long serialVersionUID = -2827655758420481178L;

	private int  size;

	public BVVariable(){
		super(null);
	}

	public BVVariable(String name, Integer size) {
		super(name);
		this.size = size;
	}

	public BVVariable(String name, Object original, Integer size) {
		super(name, original);
		this.size = size;
	}

	public Integer getSize() {
		return size;
	}

	@Override
	public void accept(Visitor visitor) throws VisitorException {
		visitor.preVisit(this);
		visitor.postVisit(this);
	}

	@Override
	public Expression copy(Copier c) {
		return c.copy(this);
	}

//	@Override
//	public int compareTo(Expression expression) {
//		IntVariable variable = (IntVariable) expression;
//		return getName().compareTo(variable.getName());
//	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof BVVariable) {
			BVVariable variable = (BVVariable) object;
			return getName().equals(variable.getName());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(this.size);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.size = in.readInt();
	}
}
