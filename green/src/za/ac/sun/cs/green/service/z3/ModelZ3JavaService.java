package za.ac.sun.cs.green.service.z3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.z3.ApplyResult;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Goal;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Tactic;
import com.microsoft.z3.Z3Exception;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.ArrayVariable;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.StringVariable;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.ModelService;
import za.ac.sun.cs.green.service.z3.Z3JavaTranslator.Z3GreenBridge;

public class ModelZ3JavaService extends ModelService {
	
	public Context ctx;
	public Solver Z3solver;
	
	public ModelZ3JavaService(Green solver, Properties properties) {
		super(solver);
		HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        cfg.put("unsat_core", "true");

		try{
			ctx = new Context(cfg);		 
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
	    }
	}
	
	public Z3GreenBridge getUnderlyingExpr(Instance instance)
	{
		Z3JavaTranslator translator = new Z3JavaTranslator(ctx);
		try {
			instance.getExpression().accept(translator);
		} catch (VisitorException e1) {
			log.log(Level.WARNING, "Error in translation to Z3"+e1.getMessage());
		}
//		Tactic css = ctx.mkTactic("ctx-solver-simplify");
		Tactic css = ctx.mkTactic("simplify");
		Goal g = ctx.mkGoal(true, true, false);
		Z3GreenBridge ret = translator.getTranslationInternal();
//		System.out.println("In: " + ret.constraints_int);
		g.add(ret.constraints_int);
		ApplyResult a = css.apply(g);
		g = a.getSubgoals()[0];
		ret.constraints_int = ctx.mkAnd(g.getFormulas());
//		System.out.println("Out:" + ret.constraints_int);
		return ret;
	}

	public HashMap<String, Object> solve(Z3GreenBridge data) {
		HashMap<String, Object> results = new HashMap<String, Object>();
		Map<Expression, BoolExpr> map;
		try {
			map = data.convertToZ3(ctx);
		} catch (VisitorException e1) {
			log.log(Level.WARNING, "Error in translation to Z3" + e1.getMessage());
			throw new RuntimeException(e1);
		}

		try {
			if(Z3solver == null)
				Z3solver = ctx.mkSolver();
			else
				Z3solver.reset();

			for (Entry<Expression, BoolExpr> e : map.entrySet())
				Z3solver.assertAndTrack(e.getValue(), ctx.mkBoolConst(e.getKey().toString()));

		} catch (Z3Exception e1) {
			log.log(Level.WARNING, "Error in Z3"+e1.getMessage());
		}
		//solve 		
		try { // Real Stuff is still untested
			if (Status.SATISFIABLE == Z3solver.check()) {
//				System.out.println("SAT: " + data.constraints);
				Model model = Z3solver.getModel();
				for(Expr z3Var : data.z3vars) {
					Expr z3Val = model.evaluate(z3Var, false);
					Object val = null;
					if (z3Val.isIntNum() || z3Val.isBV()) {
						val = Integer.parseInt(z3Val.toString());
					} else if (z3Val.isRatNum()) {
						val = Double.parseDouble(z3Val.toString());
					} else {
						//Must be string?
						String sval = z3Val.toString();
						//Need to clean up string
						Pattern p = Pattern.compile("\\\\x(\\d\\d)");
						Matcher m = p.matcher(sval);
						while(m.find())
						{
							int i = Long.decode("0x" + m.group(1)).intValue();
							sval = sval.replace(m.group(0), String.valueOf((char) i));
						}
						val = sval;
					} 
					results.put(z3Var.toString(), val);
//					String logMessage = "" + greenVar + " has value " + val;
//					log.log(Level.INFO,logMessage);
				}
			} else {
				BoolExpr[] unsat = Z3solver.getUnsatCore();
				if(unsat.length > 0)
					System.out.println(Arrays.toString(Z3solver.getUnsatCore()));
//				log.log(Level.WARNING,"constraint has no model, it is infeasible");
				return null;
			}
		} catch (Z3Exception e) {
			log.log(Level.WARNING, "Error in Z3"+e.getMessage());
		}
		return results;
	}
	@Override
	protected Map<Variable, Object> model(Instance instance) {		
		HashMap<Variable,Object> results = new HashMap<Variable, Object>();
		// translate instance to Z3 
		Z3JavaTranslator translator = new Z3JavaTranslator(ctx);
		try {
			instance.getExpression().accept(translator);
		} catch (VisitorException e1) {
			log.log(Level.WARNING, "Error in translation to Z3"+e1.getMessage());
		}
		// get context out of the translator
		BoolExpr expr = translator.getTranslation();
		// model should now be in ctx
		try {
			Z3solver = ctx.mkSolver();
			Z3solver.add(expr);
		} catch (Z3Exception e1) {
			log.log(Level.WARNING, "Error in Z3"+e1.getMessage());
		}
		//solve 		
		try { // Real Stuff is still untested
			if (Status.SATISFIABLE == Z3solver.check()) {
				Map<Variable, Expr> variableMap = translator.getVariableMap();
				Model model = Z3solver.getModel();
				for(Map.Entry<Variable,Expr> entry : variableMap.entrySet()) {
					Variable greenVar = entry.getKey();
					Expr z3Var = entry.getValue();
					Expr z3Val = model.evaluate(z3Var, false);
					Object val = null;
					if (z3Val.isIntNum()) {
						val = Integer.parseInt(z3Val.toString());
					} else if (z3Val.isRatNum()) {
						val = Double.parseDouble(z3Val.toString());
					} else if(greenVar instanceof StringVariable){
						//Must be string?
						val = z3Val.toString();
					} else if(greenVar instanceof ArrayVariable)
					{
//						System.out.println("Arr: "  +z3Val);
					} else
					{
						log.log(Level.WARNING, "Error unsupported type for variable " + z3Val);
						return null;
					}
					results.put(greenVar, val);
					String logMessage = "" + greenVar + " has value " + val;
					log.log(Level.INFO,logMessage);
				}
			} else {
				log.log(Level.WARNING,"constraint has no model, it is infeasible");
				return null;
			}
		} catch (Z3Exception e) {
			log.log(Level.WARNING, "Error in Z3"+e.getMessage());
		}
		return results;
	}


}
