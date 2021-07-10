package za.ac.sun.cs.green.expr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class BinaryOperation extends Operation {
    private Expression left;
    private Expression right;

    public BinaryOperation(){
        super(null);
    }

    public BinaryOperation(final Operator operator, Expression left, Expression right) {
        super(operator);
        this.left = left;
        this.right = right;
    }

    public BinaryOperation(final Operator operator, int immediate, Expression left, Expression right) {
        super(operator, immediate);
        this.left = left;
        this.right = right;
    }

    public BinaryOperation(final Operator operator, int immediate1, int immediate2, Expression left, Expression right) {
        super(operator, immediate1, immediate2);
        this.left = left;
        this.right = right;
    }

    @Override
    public int getArity() {
        return 2;
    }

    @Override
    protected Expression doGetOperand(int index) {
        return (index == 0) ? left : right;
    }

    @Override
    public Iterable<Expression> getOperands() {
        return () -> {
            return new Iterator<Expression>() {
                private Expression next = left;

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public Expression next() {
                    if (next == left) {
                        next = right;
                        return left;
                    } else if (next == right) {
                        next = null;
                        return right;
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
        left.accept(visitor);
        right.accept(visitor);
        visitor.postVisit(this);
    }

    @Override
    public Expression copy(Copier c) {
        return c.copy(this);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Operation) {
            BinaryOperation operation = (BinaryOperation) object;
            if (operator != operation.operator) {
                return false;
            }
            if (!left.equals(operation.left))
                return false;
            if (!right.equals(operation.right))
                return false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = operator.hashCode();
        h ^= left.hashCode();
        h ^= right.hashCode();
        return h;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int arity = operator.getArity();
        Fix fix = operator.getFix();
        if (fix == Fix.INFIX) {
            if ((left instanceof Constant) || (left instanceof Variable)) {
                sb.append(left.toString());
            } else {
                sb.append('(');
                if(left == null)
                    sb.append("NULL!");
                else
                    sb.append(left.toString());
                sb.append(')');
            }
            sb.append(operator.toString());
            if ((right instanceof Constant) || (right instanceof Variable)) {
                sb.append(right.toString());
            } else {
                sb.append('(');
                if(right == null)
                    sb.append("NULL!");
                else
                    sb.append(right.toString());
                sb.append(')');
            }
        } else {
            sb.append(operator.toString());
            sb.append(" " + left.toString());
            sb.append(" " + right.toString());
        }
        return sb.toString();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.left = (Expression) in.readObject();
        this.right = (Expression) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(this.left);
        out.writeObject(this.right);
    }
}
