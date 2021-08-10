package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class IntVariable extends Variable {

	private static final long serialVersionUID = 8942503924718973792L;

	public Integer lowerBound;

	public Integer upperBound;

	public IntVariable(){
		super(null);
	}

	public IntVariable(String name, Integer lowerBound, Integer upperBound) {
		super(name);
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public IntVariable(String name, Object original, Integer lowerBound, Integer upperBound) {
		super(name, original);
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public Integer getLowerBound() {
		return lowerBound;
	}

	public Integer getUpperBound() {
		return upperBound;
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
		if (object instanceof IntVariable) {
			IntVariable variable = (IntVariable) object;
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
		out.writeInt(this.lowerBound);
		out.writeInt(this.upperBound);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.lowerBound = in.readInt();
		this.upperBound = in.readInt();
	}
}
