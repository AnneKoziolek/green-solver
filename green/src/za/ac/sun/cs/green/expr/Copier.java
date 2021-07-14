package za.ac.sun.cs.green.expr;

public abstract class Copier {

	public Expression copy(Expression expression) {
		if (expression instanceof IntConstant) {
			return copy((IntConstant) expression);
		} else if (expression instanceof BoolConstant) {
			return copy((BoolConstant) expression);
		} else if (expression instanceof BVConstant) {
			return copy((BVConstant) expression);
		} else if (expression instanceof RealConstant) {
			return copy((RealConstant) expression);
		} else if (expression instanceof StringConstant) {
			return copy((StringConstant) expression);
		} else if (expression instanceof UnaryOperation) {
			return copy((UnaryOperation) expression);
		} else if (expression instanceof NaryOperation) {
			return copy((NaryOperation) expression);
		} else if (expression instanceof BinaryOperation) {
			return copy((BinaryOperation) expression);
		} else if (expression instanceof StringVariable) {
			return copy((StringVariable) expression);
		} else if (expression instanceof RealVariable) {
			return copy((RealVariable) expression);
		} else if (expression instanceof IntVariable) {
			return copy((IntVariable) expression);
		} else if (expression instanceof ArrayVariable) {
			return copy((ArrayVariable) expression);
		} else if (expression instanceof BVVariable) {
			return copy((BVVariable) expression);
		} else if (expression instanceof FunctionCall) {
			return copy((FunctionCall) expression);
		}
		throw new Error("Not Implemented: " + expression.getClass());
	}

	protected Expression postCopy(Expression src, Expression dst) {
		dst.metadata = src.metadata;
		return dst;
	}

	public Expression copy(IntConstant intConstant) {
		return postCopy(intConstant, new IntConstant(intConstant.getValueLong()));
	}

	public Expression copy(BVConstant bvConstant) {
		return postCopy(bvConstant, new BVConstant(bvConstant.getValue(), bvConstant.getSize()));
	}

	public Expression copy(BVVariable bvVariable) {
		return postCopy(bvVariable, new BVVariable(bvVariable.getName(), bvVariable.getSize()));
	}

	public Expression copy(IntVariable intVariable) {
	    return postCopy(intVariable, new IntVariable(intVariable.getName(), intVariable.getLowerBound(), intVariable.getUpperBound()));
	}

	public Expression copy(UnaryOperation operation) {
		return postCopy(operation, new UnaryOperation(operation.getOperator(), operation.getImmediate1(), operation.getImmediate2(), copy(operation.getOperand(0))));
	}

	public Expression copy(BinaryOperation operation) {
		return postCopy(operation, new BinaryOperation(operation.getOperator(), operation.getImmediate1(), operation.getImmediate2(), copy(operation.getOperand(0)), copy(operation.getOperand(1))));
	}

	public Expression copy(NaryOperation operation) {
		Expression[] operands = new Expression[operation.getArity()];

		for (int i = 0 ; i < operands.length ; i++)
			operands[i] = copy(operation.getOperand(i));

		return postCopy(operation, new NaryOperation(operation.getOperator(), operation.getImmediate1(), operation.getImmediate2(), operands));
	}

	public Expression copy(RealConstant realConstant) {
	    return postCopy(realConstant, new RealConstant(realConstant.getValue()));
	}

	public Expression copy(RealVariable realVariable) {
	    return postCopy(realVariable, new RealVariable(realVariable.getName(), realVariable.getLowerBound(), realVariable.getUpperBound()));
	}

	public Expression copy(StringConstant stringConstant) {
		return postCopy(stringConstant, new StringConstant(stringConstant.getValue()));
	}

	public Expression copy(StringVariable stringVariable) {
		return postCopy(stringVariable, new StringConstant(stringVariable.getName()));
	}

	public Expression copy(BoolConstant constant) {
		return postCopy(constant, new BoolConstant(constant.getValue()));
	}

	public Expression copy(FunctionCall function) {
		Expression[] args = new Expression[function.getArguments().length];

		for (int i = 0 ; i < args.length ; i++)
			args[i] = copy(function.getArguments()[i]);

		return postCopy(function, new FunctionCall(function.getName(), args));
	}

	public Expression copy(ArrayVariable variable) {
		return postCopy(variable, new ArrayVariable(variable.getName(), variable.getType()));
	}

}
