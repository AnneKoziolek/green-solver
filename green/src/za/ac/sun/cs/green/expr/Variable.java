package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

public abstract class Variable extends Expression {

	private static final long serialVersionUID = -1712398155778326862L;

	public String name;

	private Object original;

	public Variable(final String name) {
		this.name = name;
		this.original = null;
	}

	public Variable(final String name, final Object original) {
		this.name = name;
		this.original = original;
	}

	public final String getName() {
		return name;
	}

	public final Object getOriginal() {
		return original;
	}

	@Override
	public Object clone() {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Variable other = (Variable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeUTF(this.name);
		out.writeObject(this.original);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.name = in.readUTF();
		this.original = in.readObject();
	}
}
