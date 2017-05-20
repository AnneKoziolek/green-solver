package za.ac.sun.cs.green.expr;

public class ArrayVariable extends Variable {

	private static final long serialVersionUID = 9145722106088943318L;

	public ArrayVariable(String name) {
		super(name);
	}

	@Override
	public void accept(Visitor visitor) throws VisitorException {
		visitor.preVisit(this);
		visitor.postVisit(this);
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof ArrayVariable && ((ArrayVariable) object).getName().equals(getName());
	}

	@Override
	public String toString() {
		return getName();
	}
}
