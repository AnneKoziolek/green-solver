package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class UnaryOperation extends Operation {
    private static final long serialVersionUID = -8138087857364194252L;
    public Expression operand;

    public UnaryOperation() {
        super(null);
    }

    public UnaryOperation(final Operator operator, Expression operand) {
        super(operator);
        this.operand = operand;
    }

    public UnaryOperation(final Operator operator, int immediate, Expression operand) {
        super(operator, immediate);
        this.operand = operand;
    }

    public UnaryOperation(final Operator operator, int immediate1, int immediate2, Expression operand) {
        super(operator, immediate1, immediate2);
        this.operand = operand;
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected Expression doGetOperand(int index) {
        return operand;
    }

    @Override
    public Iterable<Expression> getOperands() {
        return () -> {
            return new Iterator<Expression>() {
                private Expression next = operand;

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public Expression next() {
                    if (next != null) {
                        next = null;
                        return operand;
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        };
    }


    @Override
    public void accept(Visitor visitor) throws VisitorException {
        visitor.preVisit(this);
        operand.accept(visitor);
        visitor.postVisit(this);
    }

    @Override
    public Expression copy(Copier c) {
        return c.copy(this);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof UnaryOperation) {
            UnaryOperation operation = (UnaryOperation) object;
            if (operator != operation.operator)
                return false;
            if (!operand.equals(operation.operand))
                return false;
            return true;
        } else {
            return false;
        }
    }

    int hashCode;
    @Override
    public int hashCode() {
        if(this.hashCode == 0) {
            int h = operator.hashCode();
            h ^= operand.hashCode();
            this.hashCode = h;
        }
        return this.hashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int arity = operator.getArity();
        Fix fix = operator.getFix();
        if (fix == Fix.INFIX) {
            sb.append(operator.toString());
            if ((operand instanceof Constant) || (operand instanceof Variable)) {
                sb.append(operand.toString());
            } else {
                sb.append('(');
                sb.append(operand.toString());
                sb.append(')');
            }
        } else if (fix == Fix.POSTFIX) {
            sb.append(operand.toString());
            sb.append('.');
            sb.append(operator.toString());
            sb.append('(');
            sb.append(')');
        } else {
            sb.append(operator.toString());
            sb.append(operand.toString());
        }
        return sb.toString();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.operand = (Expression) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(this.operand);
    }
}
