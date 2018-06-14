package za.ac.sun.cs.green.service.z3;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import za.ac.sun.cs.green.expr.ArrayVariable;
import za.ac.sun.cs.green.expr.BVConstant;
import za.ac.sun.cs.green.expr.BVVariable;
import za.ac.sun.cs.green.expr.BoolConstant;
import za.ac.sun.cs.green.expr.Expression;
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
import za.ac.sun.cs.green.expr.Operation.Operator;
import za.ac.sun.cs.green.util.NotSatException;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.SeqExpr;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Z3Exception;

public class Z3JavaTranslator extends Visitor {
	
	private Context context = null;

	private Stack<Expr> stack = null;
	
	private List<BoolExpr> domains = null;

	private List<BoolExpr> charAtHacks = null;
	
	private Map<Variable, Expr> v2e = null;

	private Map<String, Expr> charAts = null;
	
	public Z3JavaTranslator(Context c) {
		this.context = c;
		stack = new Stack<Expr>();
		v2e = new HashMap<Variable, Expr>();
		domains = new LinkedList<BoolExpr>();
		charAts = new HashMap<String, Expr>();
		charAtHacks = new LinkedList<BoolExpr>();
	}
	public Z3GreenBridge getTranslationInternal() {
		Z3GreenBridge ret = new Z3GreenBridge();
		ret.constraints_int = (BoolExpr)stack.pop();
		ret.domains = domains;
		ret.varNames = new HashSet<String>();
		for(Variable v : v2e.keySet())
			ret.varNames.add(v.getName());
		ret.charAts = charAts.keySet();
		return ret;
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
		for (BoolExpr expr : charAtHacks) {
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

		SeqExpr expr = context.mkEmptySeq(context.mkSeqSort(context.mkBitVecSort(32)));
		for (int i = 0 ; i < stringConstant.getValue().length() ; i++)
			expr = context.mkConcat(expr, context.mkUnit(context.mkBV(stringConstant.getValue().charAt(i), 32)));

		stack.push(expr);
//		stack.push(context.mkString(stringConstant.getValue()));
	}
	
	@Override
	public void postVisit(IntConstant constant) {			
		try {
			stack.push(context.mkInt(constant.getValueLong()));
		} catch (Z3Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postVisit(BVConstant constant) {			
		try {
			BitVecNum bv = context.mkBV(constant.getValue(), constant.getSize());
			stack.push(bv);
		} catch (Z3Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postVisit(BoolConstant constant) {			
		try {
			stack.push(context.mkBool(constant.getValue()));
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
			v = context.mkEmptySeq(context.mkSeqSort(context.mkBitVecSort(32)));
//			v = context.mkConst(stringVariable.getName(), context.getStringSort());
//			if(stringVariable.observedLength > 0)
//				domains.add(context.mkLt(context.mkInt(stringVariable.observedLength), context.mkLength((SeqExpr) v)));
			v2e.put(stringVariable, v);
		}
		stack.push(v);
	}
	
	@Override
	public void postVisit(Variable variable) throws VisitorException {
		if(variable instanceof ArrayVariable)
		{
			throw new Error();
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
	public void postVisit(BVVariable variable) {
		Expr v = v2e.get(variable);
		if (v == null) {
			Integer size = variable.getSize();
			try {
				v = context.mkBVConst(variable.getName(), size);
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
//			long lower = (int) (double) variable.getLowerBound();
//			long upper = (int) (double) variable.getUpperBound();
			try {
				v = context.mkRealConst(variable.getName());
				// now add bounds
//				BoolExpr low  = context.mkGe((ArithExpr)v,(ArithExpr)context.mkReal(lower));
//				BoolExpr high = context.mkLe((ArithExpr)v,(ArithExpr)context.mkReal(upper));
//				domains.add(context.mkAnd(low,high));
			} catch (Z3Exception e) {
				e.printStackTrace();
			}
			v2e.put(variable, v);
		}
		stack.push(v);
	}
	
	@Override
	public void postVisit(ArrayVariable variable) throws VisitorException {
		Expr v = v2e.get(variable);
		if (v == null)
		{
			try {
				Sort range;
				String s = variable.getType().getName();
				switch (s) {
					case "boolean":
						range = context.mkBoolSort();
						break;
					case "byte":
						range = context.mkBitVecSort(32);
						break;
					case "short":
					case "char":
						range = context.mkBitVecSort(32);
						break;
					case "int":
						range = context.mkBitVecSort(32);
						break;
					case "long":
						range = context.mkBitVecSort(64);
						break;
					default:
						throw new Error("Not implemented");
				}
				v = context.mkArrayConst(variable.getName(), context.mkBitVecSort(32), range);
			} catch (Z3Exception e) {
				e.printStackTrace();
				throw e;
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
			case NOT:
				stack.push(context.mkNot((BoolExpr) l));
				break;
			case EQ:
				if(l instanceof SeqExpr && r instanceof IntNum)
				{
					//comparing a string to a single char
					stack.push(context.mkEq(l, context.mkString(new String(new char[]{(char) ((IntNum)r).getInt()}))));
				}
				else if(r instanceof SeqExpr && l instanceof IntNum)
				{
					//comparing a string to a single char
					stack.push(context.mkEq(r, context.mkString(new String(new char[]{(char) ((IntNum)l).getInt()}))));
				}
				else if(l instanceof BitVecExpr && r instanceof IntNum)
				{
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					stack.push(context.mkEq(l, r));
				}
				else if(r instanceof BitVecExpr && l instanceof IntNum)
				{
					l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
					stack.push(context.mkEq(l, r));
				}
				else if(l instanceof BoolExpr && r instanceof IntNum)
				{
					stack.push(context.mkEq(l, context.mkBool(((IntNum)r).getInt64() != 0)));
				}
				else if(r instanceof BoolExpr && l instanceof IntNum)
				{
					stack.push(context.mkEq(context.mkBool(((IntNum)l).getInt64() != 0), r));
				}
				else
					stack.push(context.mkEq(l, r));
				break;
			case NE:
				if(l instanceof SeqExpr && r instanceof IntNum)
				{
					//comparing a string to a single char
					stack.push(context.mkNot(context.mkEq(l, context.mkString(new String(new char[]{(char) ((IntNum)r).getInt()})))));
				}
				else if(r instanceof SeqExpr && l instanceof IntNum)
				{
					//comparing a string to a single char
					stack.push(context.mkNot(context.mkEq(r, context.mkString(new String(new char[]{(char) ((IntNum)l).getInt()})))));
				}
				else if(l instanceof BitVecExpr && r instanceof IntNum)
				{
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					stack.push(context.mkNot(context.mkEq(l, r)));
				}
				else if(r instanceof BitVecExpr && l instanceof IntNum)
				{
					l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
					stack.push(context.mkNot(context.mkEq(l, r)));
				}
				else if(l instanceof BoolExpr && r instanceof IntNum)
				{
					stack.push(context.mkNot(context.mkEq(l, context.mkNot(context.mkEq(r, context.mkInt(0))))));
				}
				else
					stack.push(context.mkNot(context.mkEq(l, r)));
				break;
			case LT:
				if(r instanceof SeqExpr && l instanceof IntNum)
				{
					int v = ((IntNum)l).getInt();
					BoolExpr exp = null;
					for(char i = (char) (v +1); i <= 127; i++)
					{
						if(exp == null)
							exp = context.mkEq(l, context.mkUnit(context.mkBV(i, 32)));
						else
							exp = context.mkOr(exp,context.mkEq(l, context.mkUnit(context.mkBV(i, 32))));
					}
					if(exp == null)
						throw new NotSatException();
					stack.push(exp);
				}
				else if(r instanceof IntNum && l instanceof SeqExpr)
				{
					int v = ((IntNum)r).getInt();
					BoolExpr exp = null;
					for(char i = 0; i < v; i++)
					{
						if(exp == null)
							exp = context.mkEq(l, context.mkUnit(context.mkBV(i, 32)));
						else
							exp = context.mkOr(exp,context.mkEq(l, context.mkUnit(context.mkBV(i, 32))));
					}
					if(exp == null)
						throw new NotSatException();
					stack.push(exp);
				}
				else if(l instanceof BitVecExpr && r instanceof IntNum)
				{
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					stack.push(context.mkBVSLT((BitVecExpr) l, (BitVecExpr) r));
				}
				else if(r instanceof BitVecExpr && l instanceof IntNum)
				{
					l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
					stack.push(context.mkBVSLT((BitVecExpr) l, (BitVecExpr) r));
				}
				else if(l instanceof BitVecExpr && r instanceof BitVecExpr)
				{
					stack.push(context.mkBVSLT((BitVecExpr) l, (BitVecExpr) r));
				}
				else
					stack.push(context.mkLt((ArithExpr) l, (ArithExpr) r));
				break;
			case LE:
				if(r instanceof SeqExpr && l instanceof IntNum)
				{
					int v = ((IntNum)l).getInt();
					BoolExpr exp = null;
					for(char i = (char) (v); i <= 127; i++)
					{
						if(exp == null)
							exp = context.mkEq(l, context.mkUnit(context.mkBV(i, 32)));
						else
							exp = context.mkOr(exp,context.mkEq(l, context.mkUnit(context.mkBV(i, 32))));
					}
					if(exp == null)
						throw new NotSatException();
					stack.push(exp);
				}
				else if(r instanceof IntNum && l instanceof SeqExpr)
				{
					int v = ((IntNum)r).getInt();
					BoolExpr exp = null;
					for(char i = 0; i <= v; i++)
					{
						if(exp == null)
							exp = context.mkEq(l, context.mkUnit(context.mkBV(i, 32)));
						else
							exp = context.mkOr(exp,context.mkEq(l, context.mkUnit(context.mkBV(i, 32))));
					}
					if(exp == null)
						throw new NotSatException();
					stack.push(exp);
				}
				else if(l instanceof BitVecExpr && r instanceof IntNum)
				{
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					stack.push(context.mkBVSLE((BitVecExpr) l, (BitVecExpr) r));
				}
				else if(l instanceof BitVecExpr && r instanceof BitVecExpr)
				{
					stack.push(context.mkBVSLE((BitVecExpr) l, (BitVecExpr) r));
				}
				else
					stack.push(context.mkLe((ArithExpr) l, (ArithExpr) r));
				break;
			case GT:
				if(r instanceof SeqExpr && l instanceof IntNum)
				{
					int v = ((IntNum)l).getInt();
					BoolExpr exp = null;
					for(char i = 0; i < v; i++)
					{
						if(exp == null)
							exp = context.mkEq(r, context.mkUnit(context.mkBV(i, 32)));
						else
							exp = context.mkOr(exp,context.mkEq(r, context.mkUnit(context.mkBV(i, 32))));
					}
					if(exp == null)
						throw new NotSatException();
					stack.push(exp);
				}
				else if(r instanceof IntNum && l instanceof SeqExpr)
				{
					int v = ((IntNum)r).getInt();
					BoolExpr exp = null;
					for(char i = (char) (v+1); i <= 127; i++)
					{
						if(exp == null)
							exp = context.mkEq(l, context.mkString(Character.toString(i)));
						else
							exp = context.mkOr(exp,context.mkEq(l, context.mkString(Character.toString(i))));
					}
					if(exp == null)
						throw new NotSatException();
					stack.push(exp);
				}
				else if(l instanceof BitVecExpr && r instanceof IntNum)
				{
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					stack.push(context.mkBVSGT((BitVecExpr) l, (BitVecExpr) r));
				}
				else if(r instanceof BitVecExpr && l instanceof IntNum)
				{
					int sortSize = ((BitVecExpr)r).getSortSize();
					long val = ((IntNum)l).getInt64();
					l = context.mkBV(val, sortSize);
					stack.push(context.mkBVSGT((BitVecExpr) l, (BitVecExpr) r));
				}
				else if(r instanceof BitVecExpr && l instanceof BitVecExpr)
					stack.push(context.mkBVSGT((BitVecExpr) l, (BitVecExpr) r));
				else
					stack.push(context.mkGt((ArithExpr) l, (ArithExpr) r));
				break;
			case GE:
				if(r instanceof SeqExpr && l instanceof IntNum)
				{
					int v = ((IntNum)l).getInt();
					BoolExpr exp = null;
					for(char i = 0; i <= v; i++)
					{
						if(exp == null)
							exp = context.mkEq(l, context.mkUnit(context.mkBV(i, 32)));
						else
							exp = context.mkOr(exp,context.mkEq(l, context.mkUnit(context.mkBV(i, 32))));
					}
					if(exp == null)
						throw new NotSatException();
					stack.push(exp);
				}
				else if(r instanceof IntNum && l instanceof SeqExpr)
				{
					int v = ((IntNum)r).getInt();
					BoolExpr exp = null;
					for(char i = (char) v; i <= 127; i++)
						if(exp == null)
							exp = context.mkEq(l, context.mkUnit(context.mkBV(i, 32)));
						else
							exp = context.mkOr(exp,context.mkEq(l, context.mkUnit(context.mkBV(i, 32))));
					if(exp == null)
						throw new NotSatException();
					stack.push(exp);
				}
				else if(l instanceof BitVecExpr && r instanceof IntNum)
				{
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					stack.push(context.mkBVSGE((BitVecExpr) l, (BitVecExpr) r));
				}
				else if(r instanceof BitVecExpr && l instanceof IntNum)
				{
					l = context.mkBV(((IntNum)l).getInt64(), ((BitVecExpr)r).getSortSize());
					stack.push(context.mkBVSGE((BitVecExpr) l, (BitVecExpr) r));
				}
				else if (l instanceof BitVecExpr && r instanceof BitVecExpr)
					stack.push(context.mkBVSGE((BitVecExpr) l, (BitVecExpr) r));
				else
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
			case ITE:
				stack.push(context.mkITE((BoolExpr) l, r, o));
				break;
			case ADD:
				if (l.isBV() || r.isBV()) {
					if (l instanceof IntNum)
						l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
					else if (r instanceof IntNum)
						r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					stack.push(context.mkBVAdd((BitVecExpr) l, (BitVecExpr) r));
				} else {
					stack.push(context.mkAdd((ArithExpr) l, (ArithExpr) r));
				}
				break;
			case SUB:
				if (l.isBV() || r.isBV()) {
					if (r instanceof IntNum)
						r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					else if (l instanceof IntNum)
						l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());

					stack.push(context.mkBVSub((BitVecExpr) l, (BitVecExpr) r));
				} else {
					stack.push(context.mkSub((ArithExpr) l, (ArithExpr) r));
				}
				break;
			case MUL:
				if (l.isBV() || r.isBV()) {
					if (l instanceof IntNum)
						l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
					else if (r instanceof IntNum)
						r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());

					stack.push(context.mkBVMul((BitVecExpr) l, (BitVecExpr) r));
				} else {
					stack.push(context.mkMul((ArithExpr) l, (ArithExpr) r));
				}
				break;
			case DIV:
				if (l.isBV() || r.isBV()) {
					if (l instanceof IntNum)
						l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
					else if (r instanceof IntNum)
						r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					
					stack.push(context.mkBVSDiv((BitVecExpr) l, (BitVecExpr) r));
				} else
					stack.push(context.mkDiv((ArithExpr) l, (ArithExpr) r));
				break;
			case MOD:
				if (l.isBV() || r.isBV()) {
					if (l instanceof IntNum)
						l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
					else if (r instanceof IntNum)
						r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
					
					stack.push(context.mkBVSMod((BitVecExpr)l, (BitVecExpr)r));
				} else
					stack.push(context.mkMod((IntExpr) l, (IntExpr) r));
				break;
			case SHIFTL:
				if (r instanceof BitVecExpr && l instanceof IntNum)
					l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
				else if (l instanceof BitVecExpr || r instanceof BitVecExpr) {

					if (l instanceof IntNum)
						l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
					else if (r instanceof IntNum)
						r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());

					BitVecExpr bvl = (BitVecExpr)l;
					BitVecExpr bvr = (BitVecExpr)r;

					if (bvl.getSortSize() == 64 && bvr.getSortSize() < 64)
						r = context.mkSignExt(bvl.getSortSize() - bvr.getSortSize(), bvr);
				}

				{
					// Adjust for Java shift-left semantics
					// See https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.19
					BitVecExpr bvl = (BitVecExpr)l;
					BitVecExpr bvr = (BitVecExpr)r;

					switch (bvl.getSortSize()) {
						case 64:
							r = context.mkBVAND(bvr, context.mkBV(0x3f, 64));
							break;
						case 32:
							r = context.mkBVAND(bvr, context.mkBV(0x3f, 32));
							break;
						default:
							throw new Error("SHL on less than int");
					}
				}
				
				stack.push(context.mkBVSHL((BitVecExpr)l, (BitVecExpr)r));
				break;
			case SHIFTR:
				if (l instanceof BitVecExpr && r instanceof IntNum)
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
				BitVecExpr bl = (BitVecExpr) l;
				BitVecExpr br = (BitVecExpr) r;
				
				br = context.mkExtract(5, 0, br);
				br = context.mkZeroExt(bl.getSortSize() - 6, br);
				stack.push(context.mkBVASHR(bl, br));
				break;
			case SHIFTUR:
				if (l instanceof BitVecExpr && r instanceof IntNum)
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
				bl = (BitVecExpr) l;
				br = (BitVecExpr) r;
				
				br = context.mkExtract(5, 0, br);
				br = context.mkZeroExt(bl.getSortSize() - 6, br);
				stack.push(context.mkBVLSHR(bl, br));
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
				if (l.isArray())
					stack.push(context.mkSelect((ArrayExpr) l, r));
				else // Seq
					stack.push(context.mkAt((SeqExpr) l, (IntExpr) r));
				break;
			case EQUALS:
				stack.push(context.mkEq(l, r));
				break;
			case BIT_CONCAT:
				stack.push(context.mkConcat((BitVecExpr) l,(BitVecExpr) r));
				break;
			case I2BV:
				stack.push(context.mkInt2BV(operation.getImmediate1(), (IntExpr) l));
				break;
			case EXTRACT:
				stack.push(context.mkExtract(operation.getImmediate1(), operation.getImmediate2(), (BitVecExpr)l));
				break;
			case SUBSTRING:
				stack.push(context.mkExtract((SeqExpr)l, (IntExpr) r, (IntExpr) o));
				break;
			case CHARAT:
				if (l.isConst() && r.isIntNum()) {
					String key = l.toString() + "_charat_" + ((IntNum) r).getInt();
					if (!charAts.containsKey(key)) {
						IntExpr e = context.mkIntConst(key);
						domains.add(context.mkAnd(context.mkGe(e, context.mkInt(0)), context.mkLe(e, context.mkInt(128))));
						charAts.put(key, e);
//						v2e.put(new IntVariable(key, 0, 128), e);
						Expr charToInt = null;
						for (int i = 0; i <= 128; i++) {
							if (charToInt == null)
								charToInt = context.mkInt(i);
							else
								charToInt = context.mkITE(context.mkEq(context.mkAt((SeqExpr) l, (IntExpr) r), context.mkString(String.valueOf((char) i))), context.mkInt(i), charToInt);
						}
						charAtHacks.add(context.mkEq(charToInt, e));
					}
					stack.push(context.mkInt2BV(32, (IntExpr) charAts.get(key)));
				}
				else
					stack.push(context.mkAt((SeqExpr)l, (IntExpr) r));
				break;
			case BIT_OR:
				if (l instanceof BitVecExpr && r instanceof IntNum)
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());
				stack.push(context.mkBVOR((BitVecExpr)l, (BitVecExpr)r));
				break;
			case BIT_AND:
				if (l instanceof BitVecExpr && r instanceof IntNum)
					r = context.mkBV(((IntNum)r).getInt64(), ((BitVecExpr)l).getSortSize());
				stack.push(context.mkBVAND((BitVecExpr)l, (BitVecExpr)r));
				break;
			case BIT_NOT:
				stack.push(context.mkBVNot((BitVecExpr)l));
				break;
			case BIT_XOR:
				if (l instanceof IntNum)
					l = context.mkBV(((IntNum)l).getInt(), ((BitVecExpr)r).getSortSize());
				if (r instanceof IntNum)
					r = context.mkBV(((IntNum)r).getInt(), ((BitVecExpr)l).getSortSize());

				stack.push(context.mkBVXOR((BitVecExpr)l, (BitVecExpr)r));
				break;
			case BV2I:
				stack.push(context.mkBV2Int((BitVecExpr)l, true));
				break;
			case STORE:
				o = stack.pop();
				stack.push(context.mkStore((ArrayExpr)o, l, r));
				break;
			case SIGN_EXT:
				stack.push(context.mkSignExt(operation.getImmediate1(), (BitVecExpr)l));
				break;
			case ZERO_EXT:
				stack.push(context.mkZeroExt(operation.getImmediate1(), (BitVecExpr)l));
				break;
			case I2R:
				if (l.isBV())
					l = context.mkBV2Int((BitVecExpr)l, true);
				stack.push(context.mkInt2Real((IntExpr)l));
				break;
			case NEG:
				stack.push(context.mkUnaryMinus((ArithExpr)l));
				break;
			case CONCAT:
				if (!(r instanceof SeqExpr))
					r = context.mkUnit(r);
				stack.push(context.mkConcat((SeqExpr)l, (SeqExpr)r));
				break;
			default:
				throw new TranslatorUnsupportedOperation(
						"unsupported operation " + operation.getOperator());
			}
		} catch (Z3Exception e) {
			e.printStackTrace();
		}
	}
	
	public static class Z3GreenBridge{
		public Set<String> charAts;
		public Expression constraints;
		public BoolExpr constraints_int;
		public Expression metaConstraints;
		public List<BoolExpr> domains;
		public Set<String> varNames;
		public Collection<Expr> z3vars;
		
		@Override
		public String toString() {
			return "Z3GreenBridge [charAts=" + charAts + ", constraints=" + constraints + ", metaConstraints=" + metaConstraints + "]";
		}
	
		private void splitDisjunctions(Expression expr, Set<Expression> acc) {
			if (expr instanceof Operation) {
				Operation op = (Operation) expr;
				if (op.getOperator() == Operator.AND) {
					acc.add(op.getOperand(1));
					splitDisjunctions(op.getOperand(0), acc);
					return;
				}
			}
			acc.add(expr);
		}
		public Map<Expression, BoolExpr> convertToZ3(Context ctx) throws VisitorException {
			//First convert the regular constraints
			Z3JavaTranslator translator = new Z3JavaTranslator(ctx);
			Set<Expression> disjunctions = new HashSet<>();
			splitDisjunctions(constraints, disjunctions);
			constraints.accept(translator);
			Map<Expression, BoolExpr> ret = new HashMap<>();
			
			for (Expression e : disjunctions) {
				e.accept(translator);
				ret.put(e, translator.getTranslationInternal().constraints_int);
			}
//			System.out.println("Before charat nonsense: " + ret);

			Map<Variable,Expr> v2e = translator.v2e;
			z3vars = v2e.values();

			if (metaConstraints != null) {
				metaConstraints.accept(translator);
				ret.put(metaConstraints, translator.getTranslationInternal().constraints_int);
			}
//			System.out.println("With constraints "+ ret);
			for(String s : charAts)
			{
				SeqExpr strVar = null;
				IntExpr strCharAtVar = null;
				String[] p = s.split("_charat_");
				for (Entry<Variable,Expr> e : v2e.entrySet())
				{
					if (e.getKey().getName().equals(p[0]))
						strVar = (SeqExpr) e.getValue();
					else if (e.getKey().getName().equals(s))
						strCharAtVar = (IntExpr) e.getValue();
				}
				int pos = Integer.valueOf(p[1]);
				Expr charToInt = null;
				IntExpr posExp = ctx.mkInt(pos);
				for (int i = 0; i <= 128; i++) {
					if (charToInt == null)
						charToInt = ctx.mkInt(i);
					else
						charToInt = ctx.mkITE(ctx.mkEq(ctx.mkAt(strVar, posExp), ctx.mkString(String.valueOf((char) i))), ctx.mkInt(i), charToInt);
				}
//				System.out.println(charToInt);
//				System.out.println(strCharAtVar);
				ret.put(new StringVariable(s), ctx.mkEq(charToInt, strCharAtVar));
			}
			return ret;
		}
	}
}
