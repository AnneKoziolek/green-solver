package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BVConstant extends Constant {

	/**
	 *
	 */
	private static final long serialVersionUID = 3665626951329772927L;
	private long value;
	private int  size;

	public BVConstant(){

	}
	public BVConstant(final long value, final int size) {
		this.value = value;
		this.size  = size;
	}

	public final long getValue() {
		return value;
	}

	public final int getSize() {
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

	@Override
	public boolean equals(Object object) {
		if (object instanceof BVConstant) {
			BVConstant constant = (BVConstant) object;
			return value == constant.value && size == constant.size;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return ((int) (value^(value>>>32))) ^ size;
	}

	@Override
	public String toString() {
		return "BV" + size + "-" + Long.toString(value);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(this.size);
		out.writeLong(this.value);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.size = in.readInt();
		this.value = in.readLong();
	}
}
