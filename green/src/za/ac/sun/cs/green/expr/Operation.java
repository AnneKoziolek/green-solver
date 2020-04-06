package za.ac.sun.cs.green.expr;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class Operation extends Expression {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6628353039467454097L;

	public static enum Fix {
		PREFIX, INFIX, POSTFIX;
	}

	public static enum Operator {
		EQ("==", 2, Fix.INFIX),
		NE("!=", 2, Fix.INFIX),
		LT("<", 2, Fix.INFIX),
		LE("<=", 2, Fix.INFIX),
		GT(">", 2, Fix.INFIX),
		GE(">=", 2, Fix.INFIX),
		AND("&&", 2, Fix.INFIX),
		OR("||", 2, Fix.INFIX),
		IMPLIES("=>", 2, Fix.INFIX),
		ITE("ITE", 3),
		NOT("!", 1, Fix.INFIX),
		ADD("+", 2, Fix.INFIX),
		SUB("-", 2, Fix.INFIX),
		MUL("*", 2, Fix.INFIX),
		DIV("/", 2, Fix.INFIX),
		MOD("%", 2, Fix.INFIX),
		NEG("-", 1, Fix.INFIX),
		BIT_AND("&", 2, Fix.INFIX),
		BIT_OR("|", 2, Fix.INFIX),
		BIT_XOR("^", 2, Fix.INFIX),
		BIT_NOT("~", 1, Fix.INFIX),
		SHIFTL("<<", 2, Fix.INFIX),
		SHIFTR(">>", 2, Fix.INFIX),
		SHIFTUR(">>>", 2, Fix.INFIX),
		SIN("SIN", 1),
		COS("COS", 1),
		TAN("TAN", 1),
		ASIN("ASIN", 1),
		ACOS("ACOS", 1),
		ATAN("ATAN", 1),
		ATAN2("ATAN2", 2),
		ROUND("ROUND", 1),
		LOG("LOG", 1),
		EXP("EXP", 1),
		POWER("POWER", 1),
		SQRT("SQRT", 1),
		I2R("I2R", 1),
		R2I("R2I", 1),
		// BV operations
		EXTRACT("EXTRACT", 1),
		I2BV("I2BV", 1),
		BV2I("BV2I", 1),
		R2BV("R2BV", 1),
		BV2R("BV2R", 1),
		BIT_CONCAT("BIT_CONCAT", 2, Fix.PREFIX),
		SIGN_EXT("SIGN_EXTEND", 1, Fix.PREFIX),
		ZERO_EXT("ZERO_EXTEND", 1, Fix.PREFIX),
		// String Operations
		SUBSTRING("SUBSTRING", 3, Fix.POSTFIX),
		CONCAT("CONCAT", 2, Fix.POSTFIX),
		TRIM("TRIM", 1, Fix.POSTFIX), 
		REPLACE("REPLACE", 3, Fix.POSTFIX),
		REPLACEFIRST("REPLACEFIRST", 3, Fix.POSTFIX),  
		TOLOWERCASE("TOLOWERCASE", 2, Fix.POSTFIX),
		TOUPPERCASE("TOUPPERCASE", 2, Fix.POSTFIX), 
		VALUEOF("VALUEOF", 2, Fix.POSTFIX),
		// String Comparators
		NOTCONTAINS("NOTCONTAINS", 2, Fix.POSTFIX),
		CONTAINS("CONTAINS", 2, Fix.POSTFIX),
		LASTINDEXOFCHAR("LASTINDEXOFCHAR", 3, Fix.POSTFIX),
		LASTINDEXOFSTRING("LASTINDEXOFSTRING", 3, Fix.POSTFIX),
		STARTSWITH("STARTSWITH", 2, Fix.POSTFIX),
		NOTSTARTSWITH("NOTSTARTSWITH", 2, Fix.POSTFIX),
		ENDSWITH("ENDSWITH", 2, Fix.POSTFIX),
		NOTENDSWITH("NOTENDSWITH", 2, Fix.POSTFIX),
		EQUALS("EQUALS", 2, Fix.POSTFIX),
		NOTEQUALS("NOTEQUALS", 2, Fix.POSTFIX),
		EQUALSIGNORECASE("EQUALSIGNORECASE", 2, Fix.POSTFIX),
		NOTEQUALSIGNORECASE("NOTEQUALSIGNORECASE", 2, Fix.POSTFIX),
		EMPTY("EMPTY", 1, Fix.POSTFIX),
		NOTEMPTY("NOTEMPTY", 1, Fix.POSTFIX),
		ISINTEGER("ISINTEGER", 1, Fix.POSTFIX),
		NOTINTEGER("NOTINTEGER", 1, Fix.POSTFIX),
		ISFLOAT("ISFLOAT", 1, Fix.POSTFIX),
		NOTFLOAT("NOTFLOAT", 1, Fix.POSTFIX),
		ISLONG("ISLONG", 1, Fix.POSTFIX),
		NOTLONG("NOTLONG", 1, Fix.POSTFIX),
		ISDOUBLE("ISDOUBLE", 1, Fix.POSTFIX),
		NOTDOUBLE("NOTDOUBLE", 1, Fix.POSTFIX),
		ISBOOLEAN("ISBOOLEAN", 1, Fix.POSTFIX),
		NOTBOOLEAN("NOTBOOLEAN", 1, Fix.POSTFIX),
		REGIONMATCHES("REGIONMATCHES", 6, Fix.POSTFIX),
		NOTREGIONMATCHES("NOTREGIONMATCHES", 6, Fix.POSTFIX),
		LENGTH("LENGTH", 1, Fix.POSTFIX),
		CHARAT("CHARAT", 2, Fix.POSTFIX),
		//Array
		SELECT("SELECT",2,Fix.POSTFIX),
		STORE("STORE",2,Fix.POSTFIX);

		private final String string;

		private final int maxArity;

		private final Fix fix;

		Operator(String string, int maxArity) {
			this.string = string;
			this.maxArity = maxArity;
			fix = Fix.PREFIX;
		}

		Operator(String string, int maxArity, Fix fix) {
			this.string = string;
			this.maxArity = maxArity;
			this.fix = fix;
		}

		@Override
		public String toString() {
			return string;
		}

		public int getArity() {
			return maxArity;
		}

		public Fix getFix() {
			return fix;
		}

	}

	public static final IntConstant ZERO  = new IntConstant(0L);
	public static final IntConstant ONE   = new IntConstant(1L);
	public static final IntConstant TWO   = new IntConstant(1L);
	public static final IntConstant THREE = new IntConstant(1L);
	public static final IntConstant FOUR  = new IntConstant(1L);
	public static final IntConstant FIVE  = new IntConstant(1L);
	public static final IntConstant SIX   = new IntConstant(1L);
	public static final IntConstant SEVEN = new IntConstant(1L);
	public static final IntConstant EIGHT = new IntConstant(1L);
	public static final IntConstant NINE  = new IntConstant(1L);
	public static final IntConstant TEN   = new IntConstant(1L);

	public static final Expression FALSE = new BinaryOperation(Operation.Operator.EQ, ZERO, ONE);
	public static final Expression TRUE  = new BinaryOperation(Operation.Operator.EQ, ZERO, ZERO);

	protected final Operator operator;

	private final int immediate1, immediate2;

	public Operation(final Operator operator) {
		this.operator = operator;
		this.immediate1 = 0;
		this.immediate2 = 0;
	}

	public Operation(final Operator operator, int immediate) {
		this.operator = operator;
		this.immediate1 = immediate;
		this.immediate2 = 0;
	}

	public Operation(final Operator operator, int immediate1, int immediate2) {
		this.operator = operator;
		this.immediate1 = immediate1;
		this.immediate2 = immediate2;
	}

	public Operator getOperator() {
		return operator;
	}

	public abstract int getArity();

	public abstract Iterable<Expression> getOperands();

	public Expression getOperand(int index) {
		if ((index < 0) || (index >= this.getArity())) {
			return null;
		} else {
			return this.doGetOperand(index);
		}
	}

	protected abstract Expression doGetOperand(int index);

	public int getImmediate1() {
		return this.immediate1;
	}

	public int getImmediate2() {
		return this.immediate2;
	}

//	@Override
//	public int compareTo(Expression expression) {
//		Operation operation = (Operation) expression;
//		int result = operator.compareTo(operation.operator);
//		if (result != 0) {
//			return result;
//		}
//		if (operands.length < operation.operands.length) {
//			return -1;
//		} else if (operands.length > operation.operands.length) {
//			return 1;
//		}
//		for (int i = 0; i < operands.length; i++) {
//			result = operands[i].compareTo(operation.operands[i]);
//			if (result != 0) {
//				return result;
//			}
//		}
//		return 0;
//	}

	@Override
	public abstract boolean equals(Object object);

	@Override
	public abstract int hashCode();

	@Override
	public abstract String toString();

}
