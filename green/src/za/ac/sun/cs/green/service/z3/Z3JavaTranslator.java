package za.ac.sun.cs.green.service.z3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import za.ac.sun.cs.green.expr.ArrayVariable;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.RealConstant;
import za.ac.sun.cs.green.expr.RealVariable;
import za.ac.sun.cs.green.expr.StringConstant;
import za.ac.sun.cs.green.expr.StringVariable;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.SeqExpr;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Z3Exception;

class Z3JavaTranslator extends Visitor {
	
	private Context context = null;

	private Stack<Expr> stack = null;
	
	private List<BoolExpr> domains = null;

	private Map<Variable, Expr> v2e = null;

	public Z3JavaTranslator(Context c) {
		this.context = c;
		stack = new Stack<Expr>();
		v2e = new HashMap<Variable, Expr>();
		domains = new LinkedList<BoolExpr>();
	}

	public BoolExpr getTranslation() {
		BoolExpr result = (BoolExpr)stack.pop();
		/* not required due to Bounder being used */
		/* not sure why this was commented out, it is clearly wrong, with or without bounder */
		for (BoolExpr expr : domains) {
			try {
				result = context.mkAnd(result,expr);
			} catch (Z3Exception e) {
				e.printStackTrace();
			}
		}
		/* was end of old comment */
		return result;
	}
	
	public Map<Variable, Expr> getVariableMap() {
		return v2e;
	}

	@Override
	public void postVisit(StringConstant stringConstant) throws VisitorException {
		stack.push(context.mkString(stringConstant.getValue()));
	}
	
	@Override
	public void postVisit(IntConstant constant) {			
		try {
			stack.push(context.mkInt(constant.getValue()));
		} catch (Z3Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postVisit(RealConstant constant) {
		try {
			stack.push(context.mkReal(Double.toString(constant.getValue())));
		} catch (Z3Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postVisit(StringVariable stringVariable) throws VisitorException {
		Expr v = v2e.get(stringVariable);
		if(v == null)
		{
			v = context.mkConst(stringVariable.getName(), context.getStringSort());
			v2e.put(stringVariable, v);
		}
		stack.push(v);
	}
	
	@Override
	public void postVisit(Variable variable) throws VisitorException {
		if(variable instanceof ArrayVariable)
		{
			Expr v = v2e.get(variable);
			if(v == null)
			{
				v = context.mkConst(variable.getName(), context.mkArraySort(context.getIntSort(), context.getIntSort()));
				v2e.put(variable, v);
			}
			stack.push(v);
		}
	}
	@Override
	public void postVisit(IntVariable variable) {
		Expr v = v2e.get(variable);
		if (v == null) {
			Integer lower = variable.getLowerBound();
			Integer upper = variable.getUpperBound();
			try {
				v = context.mkIntConst(variable.getName());
				// now add bounds
				BoolExpr low  = context.mkGe((ArithExpr)v,(ArithExpr)context.mkInt(lower));
				BoolExpr high = context.mkLe((ArithExpr)v,(ArithExpr)context.mkInt(upper));
				domains.add(context.mkAnd(low,high));
			} catch (Z3Exception e) {
				e.printStackTrace();
			}
			v2e.put(variable, v);
		}
		stack.push(v);
	}

	@Override
	public void postVisit(RealVariable variable) {
		Expr v = v2e.get(variable);
		if (v == null) {
			int lower = (int) (double) variable.getLowerBound();
			int upper = (int) (double) variable.getUpperBound();
			try {
				v = context.mkRealConst(variable.getName());
				// now add bounds
				BoolExpr low  = context.mkGe((ArithExpr)v,(ArithExpr)context.mkReal(lower));
				BoolExpr high = context.mkLe((ArithExpr)v,(ArithExpr)context.mkReal(upper));
				domains.add(context.mkAnd(low,high));
			} catch (Z3Exception e) {
				e.printStackTrace();
			}
			v2e.put(variable, v);
		}
		stack.push(v);
	}

	@Override
	public void postVisit(Operation operation) throws VisitorException {
		Expr l = null;
		Expr r = null;
		Expr o = null;
		int arity = operation.getOperator().getArity();
		if (arity == 2) {
			if (!stack.isEmpty()) {
				r = stack.pop();
			}
			if (!stack.isEmpty()) {
				l = stack.pop();
			}
		} else if (arity == 1) {
			if (!stack.isEmpty()) {
				l = stack.pop();
			}
		} else if(arity == 3)
		{
			if (!stack.isEmpty()) {
				o = stack.pop();
			}
			if (!stack.isEmpty()) {
				r = stack.pop();
			}
			if (!stack.isEmpty()) {
				l = stack.pop();
			}
		}
		try {
			switch (operation.getOperator()) {
			case EQ:
				if(l instanceof SeqExpr && r instanceof IntNum)
				{
					//comparing a string to a single char
					stack.push(context.mkEq(l, context.mkString(new String(new char[]{(char) ((IntNum)r).getInt()}))));
				}
				else
					stack.push(context.mkEq(l, r));
				break;
			case NE:
				stack.push(context.mkNot(context.mkEq(l, r)));
				break;
			case LT:
				stack.push(context.mkLt((ArithExpr) l, (ArithExpr) r));
				break;
			case LE:
				stack.push(context.mkLe((ArithExpr) l, (ArithExpr) r));
				break;
			case GT:
				stack.push(context.mkGt((ArithExpr) l, (ArithExpr) r));
				break;
			case GE:
				stack.push(context.mkGe((ArithExpr) l, (ArithExpr) r));
				break;
			case AND:
				stack.push(context.mkAnd((BoolExpr) l, (BoolExpr) r));
				break;
			case OR:
				stack.push(context.mkOr((BoolExpr) l, (BoolExpr) r));
				break;
			case IMPLIES:
				stack.push(context.mkImplies((BoolExpr) l, (BoolExpr) r));
				break;
			case ADD:
				stack.push(context.mkAdd((ArithExpr) l, (ArithExpr) r));
				break;
			case SUB:
				stack.push(context.mkSub((ArithExpr) l, (ArithExpr) r));
				break;
			case MUL:
				stack.push(context.mkMul((ArithExpr) l, (ArithExpr) r));
				break;
			case DIV:
				stack.push(context.mkDiv((ArithExpr) l, (ArithExpr) r));
				break;
			case MOD:
				stack.push(context.mkMod((IntExpr) l, (IntExpr) r));
				break;
			case SHIFTL:
				stack.push(context.mkBV2Int(context.mkBVSHL(context.mkInt2BV(32, (IntExpr)l), context.mkInt2BV(32, (IntExpr)r)), true));
				break;
			case SHIFTR:
				stack.push(context.mkBV2Int(context.mkBVASHR(context.mkInt2BV(32, (IntExpr)l), context.mkInt2BV(32, (IntExpr)r)), true));
				break;
			case SHIFTUR:
				stack.push(context.mkBV2Int(context.mkBVLSHR(context.mkInt2BV(32, (IntExpr)l), context.mkInt2BV(32, (IntExpr)r)), true));
				break;
			case ENDSWITH:
				stack.push(context.mkSuffixOf((SeqExpr) l, (SeqExpr) r));
				break;
			case STARTSWITH:
				stack.push(context.mkPrefixOf((SeqExpr) l, (SeqExpr) r));
				break;
			case LENGTH:
				stack.push(context.mkLength((SeqExpr) l));
				break;
			case SELECT:
				stack.push(context.mkSelect((ArrayExpr) l, r));
				break;
			case EQUALS:
				stack.push(context.mkEq(l, r));
				break;
			case CONCAT:
				stack.push(context.mkConcat((SeqExpr) l,(SeqExpr) r));
				break;
			case SUBSTRING:
				stack.push(context.mkExtract((SeqExpr)l, (IntExpr) r, (IntExpr) o));
				break;
			case CHARAT:
				stack.push(context.mkAt((SeqExpr)l, (IntExpr) r));
				break;
			case BIT_OR:
				stack.push(context.mkBV2Int(context.mkBVOR(context.mkInt2BV(32, (IntExpr)l), context.mkInt2BV(32, (IntExpr)r)), true));
				break;
			case BIT_AND:
				stack.push(context.mkBV2Int(context.mkBVAND(context.mkInt2BV(32, (IntExpr)l), context.mkInt2BV(32, (IntExpr)r)), true));
				break;
			case BIT_NOT:
				stack.push(context.mkBV2Int(context.mkBVNot(context.mkInt2BV(32, (IntExpr)l)), true));
				break;
			case BIT_XOR:
				stack.push(context.mkBV2Int(context.mkBVXOR(context.mkInt2BV(32, (IntExpr)l), context.mkInt2BV(32, (IntExpr)r)), true));
				break;
			default:
				throw new TranslatorUnsupportedOperation(
						"unsupported operation " + operation.getOperator());
			}
		} catch (Z3Exception e) {
			e.printStackTrace();
		}
	}
}