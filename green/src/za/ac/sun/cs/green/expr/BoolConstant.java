package za.ac.sun.cs.green.expr;

public class BoolConstant extends Constant {

	/**
	 * 
	 */
	private static final long serialVersionUID = 219363665250466089L;
	private final boolean value;

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

}
