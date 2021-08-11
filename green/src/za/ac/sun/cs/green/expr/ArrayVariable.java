package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

public class ArrayVariable extends Variable {

	private static final long serialVersionUID = 9145722106088943318L;

	public Class<?> type;

	public ArrayVariable(){
		super(null);
	}

	public ArrayVariable(String name, Class<?> type) {
		super(name);
		this.type = type;
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
	public String toString() {
		return getName();
	}

	public Class<?> getType() {
		return type;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(type);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		ArrayVariable that = (ArrayVariable) o;

		return type != null ? type.equals(that.type) : that.type == null;
	}

	int hashCode;
	@Override
	public int hashCode() {
	    if(this.hashCode == 0) {
			int result = super.hashCode();
			result = 31 * result + (type != null ? type.hashCode() : 0);
			this.hashCode = result;
		}
	    return this.hashCode;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.type = (Class<?>) in.readObject();
	}
}
