package za.ac.sun.cs.green.expr;

public abstract class Copier {

	public Expression copy(Expression expression) throws VisitorException {
	    throw new Error("Not Implemented");
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
	    throw new Error("Not implemented");
	}

	public Expression copy(UnaryOperation operation) {
		return postCopy(operation, new UnaryOperation(operation.getOperator(), operation.getImmediate1(), operation.getImmediate2(), operation.getOperand(0)));
	}

	public Expression copy(BinaryOperation operation) {
		return postCopy(operation, new BinaryOperation(operation.getOperator(), operation.getImmediate1(), operation.getImmediate2(), operation.getOperand(0), operation.getOperand(1)));
	}

	public Expression copy(NaryOperation operation) {
		Expression[] operands = new Expression[operation.getArity()];

		for (int i = 0 ; i < operands.length ; i++)
			operands[i] = operation.getOperand(i).copy(this);

		return postCopy(operation, new NaryOperation(operation.getOperator(), operation.getImmediate1(), operation.getImmediate2(), operands));
	}

	public Expression copy(RealConstant realConstant) {
		throw new Error("Not implemented");
	}

	public Expression copy(RealVariable realVariable) {
		throw new Error("Not implemented");
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
			args[i] = function.getArguments()[i].copy(this);

		return postCopy(function, new FunctionCall(function.getName(), args));
	}

	public Expression copy(ArrayVariable variable) {
		return postCopy(variable, new ArrayVariable(variable.getName(), variable.getType()));
	}

}
