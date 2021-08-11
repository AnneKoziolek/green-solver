package za.ac.sun.cs.green.expr;

public class StringVariable extends Variable {

	private static final long serialVersionUID = -4405246046006773012L;

	public int observedLength;

	public BinaryOperation andBV3265535;

	public StringVariable(){
		super(null);
	}

	public StringVariable(String name) {
		super(name);
	}

	public StringVariable(String name, Object original) {
		super(name, original);
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
//		RealVariable variable = (RealVariable) expression;
//		return getName().compareTo(variable.getName());
//	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof StringVariable) {
			StringVariable variable = (StringVariable) object;
			return getName().equals(variable.getName());
		} else {
			return false;
		}
	}

	int hashCode;

	@Override
	public int hashCode() {
		if (this.hashCode == 0)
			this.hashCode = getName().hashCode();
		return this.hashCode;
	}

	@Override
	public String toString() {
		return getName();
	}

}
