package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ArrayVariable extends Variable {

	private static final long serialVersionUID = 9145722106088943318L;

	private Class<?> type;

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
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.type = (Class<?>) in.readObject();
	}
}
