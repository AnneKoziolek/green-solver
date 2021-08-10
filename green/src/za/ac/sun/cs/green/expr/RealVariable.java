package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RealVariable extends Variable {

	private static final long serialVersionUID = -8815803703741978839L;

	public Double lowerBound;

	public Double upperBound;

	public RealVariable(){
		super(null);
	}

	public RealVariable(String name, Object original, Double lowerBound, Double upperBound) {
		super(name, original);
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public RealVariable(String name, Double lowerBound, Double upperBound) {
		super(name);
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public RealVariable(String name) {
		super(name);
		this.lowerBound = Double.NEGATIVE_INFINITY;
		this.upperBound = Double.POSITIVE_INFINITY;
	}

	public Double getLowerBound() {
		return lowerBound;
	}

	public Double getUpperBound() {
		return upperBound;
	}

	@Override
	public void accept(Visitor visitor) throws VisitorException {
		visitor.preVisit(this);
		visitor.postVisit(this);
	}

//	@Override
//	public int compareTo(Expression expression) {
//		RealVariable variable = (RealVariable) expression;
//		return getName().compareTo(variable.getName());
//	}

	@Override
	public Expression copy(Copier c) {
		return c.copy(this);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof RealVariable) {
			RealVariable variable = (RealVariable) object;
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
		out.writeDouble(this.lowerBound.doubleValue());
		out.writeDouble(this.upperBound.doubleValue());
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.lowerBound = in.readDouble();
		this.upperBound = in.readDouble();
	}
}
