package za.ac.sun.cs.green.expr;

import java.util.Arrays;

public class FunctionCall extends Expression {

    private String name;
    private Expression[] arguments;

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
}
