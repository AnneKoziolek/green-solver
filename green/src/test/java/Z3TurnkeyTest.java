import com.microsoft.z3.*;

public class Z3TurnkeyTest {
    public static void main(String[] args) {
        System.out.println("Testing Z3-TurnKey functionality...");
        
        try {
            // Create Z3 context
            Context ctx = new Context();
            System.out.println("✓ Z3 Context created successfully");
            
            // Create solver
            Solver solver = ctx.mkSolver();
            System.out.println("✓ Z3 Solver created successfully");
            
            // Create simple constraint: x > 0
            IntExpr x = ctx.mkIntConst("x");
            BoolExpr constraint = ctx.mkGt(x, ctx.mkInt(0));
            solver.add(constraint);
            System.out.println("✓ Constraint added: x > 0");
            
            // Check satisfiability
            Status result = solver.check();
            System.out.println("✓ SAT check result: " + result);
            
            if (result == Status.SATISFIABLE) {
                Model model = solver.getModel();
                System.out.println("✓ Model found: " + model);
                System.out.println("✓ x = " + model.evaluate(x, false));
            }
            
            // Clean up
            ctx.close();
            System.out.println("✓ Z3 Context closed successfully");
            
            System.out.println("\n🎉 Z3-TurnKey integration test PASSED!");
            
        } catch (Exception e) {
            System.err.println("❌ Z3-TurnKey test FAILED:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}