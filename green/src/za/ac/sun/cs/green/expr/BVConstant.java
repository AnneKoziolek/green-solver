package za.ac.sun.cs.green.expr;

public class BVConstant extends Constant {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3665626951329772927L;
	private final long value;
	private final int  size;

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

}
