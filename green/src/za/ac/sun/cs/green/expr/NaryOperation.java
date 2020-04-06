package za.ac.sun.cs.green.expr;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class NaryOperation extends Operation {
    private final Expression[] operands;

    public NaryOperation(final Operator operator, Expression... operands) {
        super(operator);
        this.operands = operands;
    }

    public NaryOperation(final Operator operator, int immediate, Expression... operands) {
        super(operator, immediate);
        this.operands = operands;
    }

    public NaryOperation(final Operator operator, int immediate1, int immediate2, Expression... operands) {
        super(operator, immediate1, immediate2);
        this.operands = operands;
    }


    @Override
    public int getArity() {
        return operands.length;
    }

    @Override
    protected Expression doGetOperand(int index) {
        return operands[index];
    }

    public Iterable<Expression> getOperands() {
        return new Iterable<Expression>() {
            @Override
            public Iterator<Expression> iterator() {
                return new Iterator<Expression>() {
                    private int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < getArity();
                    }

                    @Override
                    public Expression next() {
                        if (index < getArity()) {
                            return doGetOperand(index++);
                        } else {
                            throw new NoSuchElementException();
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }


    @Override
    public void accept(Visitor visitor) throws VisitorException {
        visitor.preVisit(this);
        for (Expression o : operands)
            o.accept(visitor);
        visitor.postVisit(this);
    }

    @Override
    public Expression copy(Copier c) {
        return c.copy(this);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof NaryOperation) {
            NaryOperation operation = (NaryOperation) object;
            if (operator != operation.operator) {
                return false;
            }
            if (operands.length != operation.operands.length) {
                return false;
            }
            for (int i = 0; i < operands.length; i++) {
                if (!operands[i].equals(operation.operands[i])) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = operator.hashCode();
        for (Expression o : operands) {
            h ^= o.hashCode();
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int arity = operator.getArity();
        Fix fix = operator.getFix();
        if (arity == 2 && fix == Fix.INFIX) {
            if ((operands[0] instanceof Constant) || (operands[0] instanceof Variable)) {
                sb.append(operands[0].toString());
            } else {
                sb.append('(');
                if(operands[0] == null)
                    sb.append("NULL!");
                else
                    sb.append(operands[0].toString());
                sb.append(')');
            }
            sb.append(operator.toString());
            if ((operands[1] instanceof Constant) || (operands[1] instanceof Variable)) {
                sb.append(operands[1].toString());
            } else {
                sb.append('(');
                if(operands[1] == null)
                    sb.append("NULL!");
                else
                    sb.append(operands[1].toString());
                sb.append(')');
            }
        } else if (arity == 1 && fix == Fix.INFIX) {
            sb.append(operator.toString());
            if ((operands[0] instanceof Constant) || (operands[0] instanceof Variable)) {
                sb.append(operands[0].toString());
            } else {
                sb.append('(');
                sb.append(operands[0].toString());
                sb.append(')');
            }
        } else if (fix == Fix.POSTFIX) {
            sb.append(operands[0].toString());
            sb.append('.');
            sb.append(operator.toString());
            sb.append('(');
            if (operands.length > 1) {
                sb.append(operands[1].toString());
                for (int i = 2; i < operands.length; i++) {
                    sb.append(',');
                    sb.append(operands[i].toString());
                }
            }
            sb.append(')');
        } else if (operands.length > 0) {
            sb.append(operator.toString());
            sb.append('(');
            sb.append(operands[0].toString());
            for (int i = 1; i < operands.length; i++) {
                sb.append(',');
                sb.append(operands[i].toString());
            }
            sb.append(')');
        } else {
            sb.append(operator.toString());
        }
        return sb.toString();
    }
}
