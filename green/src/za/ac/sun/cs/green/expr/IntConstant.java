package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class IntConstant extends Constant {

	/**
	 *
	 */
	private static final long serialVersionUID = 1218090840820645022L;
	private long value;

	public IntConstant(){

	}
	public IntConstant(final int value) {
		this.value = (long) value;
	}

	public IntConstant(final long value) {
		this.value = value;
	}

	public final int getValue() {
		throw new UnsupportedOperationException();
	}

	public final long getValueLong() {
		return value;
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
//		IntConstant constant = (IntConstant) expression;
//		if (value < constant.value) {
//			return -1;
//		} else if (value > constant.value) {
//			return 1;
//		} else {
//			return 1;
//		}
//	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntConstant) {
			IntConstant constant = (IntConstant) object;
			return value == constant.value;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (int) (value^(value>>>32));
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.value = in.readLong();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeLong(this.value);
	}
}
