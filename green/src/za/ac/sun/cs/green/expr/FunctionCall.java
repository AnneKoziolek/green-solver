package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

public class FunctionCall extends Expression {

    public String name;
    public Expression[] arguments;

    public FunctionCall(){

    }

    public FunctionCall(String name, Expression[] arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public void accept(Visitor visitor) throws VisitorException {
        visitor.preVisit(this);
        for (Expression argument : arguments) {
            argument.accept(visitor);
        }
        visitor.postVisit(this);
    }

    @Override
    public Expression copy(Copier c) {
        return c.copy(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionCall that = (FunctionCall) o;
        return name.equals(that.name) &&
                Arrays.equals(arguments, that.arguments);
    }

    @Override
    public String toString() {
        return name + '(' + Arrays.toString(arguments) + ')';
    }

    public String getName() {
        return name;
    }

    public Expression[] getArguments() {
        return arguments;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.name = in.readUTF();
        int arguments = in.readInt();
        if (arguments == -1) {
            this.arguments = null;
        } else {
            this.arguments = new Expression[arguments];
            for (int i = 0; i < arguments; i++) {
                this.arguments[i] = (Expression) in.readObject();
            }
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(this.name);
        if (this.arguments == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(this.arguments.length);
            for (Expression arg : this.arguments) {
                out.writeObject(arg);
            }
        }
    }
}
