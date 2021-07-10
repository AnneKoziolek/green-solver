package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BoolConstant extends Constant {

	/**
	 *
	 */
	private static final long serialVersionUID = 219363665250466089L;
	private boolean value;

	public BoolConstant(){

	}
	public BoolConstant(final boolean value) {
		this.value = value;
	}

	public final boolean getValue() {
		return value;
	}

	@Override
	public void accept(Visitor visitor) throws VisitorException {
		visitor.preVisit(this);
		visitor.postVisit(this);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof BoolConstant) {
			BoolConstant constant = (BoolConstant) object;
			return value == constant.value;
		} else {
			return false;
		}
	}

	@Override
	public Expression copy(Copier c) {
		return c.copy(this);
	}

	@Override
	public int hashCode() {
		return Boolean.hashCode(this.value);
	}

	@Override
	public String toString() {
		return Boolean.toString(this.value);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeBoolean(this.value);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.value = in.readBoolean();
	}
}
