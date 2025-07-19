import java.util.Properties;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.util.Configuration;

public class GreenZ3Test {
    public static void main(String[] args) {
        System.out.println("Testing Green solver with Z3-TurnKey...");
        
        try {
            // Create Green solver
            Green solver = new Green();
            
            // Configure with Z3
            Properties props = new Properties();
            props.setProperty("green.services", "sat");
            props.setProperty("green.service.sat", "z3");
            props.setProperty("green.service.sat.z3", "za.ac.sun.cs.green.service.z3.SATZ3JavaService");
            
            Configuration config = new Configuration(solver, props);
            config.configure();
            System.out.println("✓ Green solver configured with Z3");
            
            // Create constraint: x > 5 && x < 10
            IntVariable x = new IntVariable("x", 0, 100);
            IntConstant five = new IntConstant(5);
            IntConstant ten = new IntConstant(10);
            
            BinaryOperation gt = new BinaryOperation(Operation.Operator.GT, x, five);
            BinaryOperation lt = new BinaryOperation(Operation.Operator.LT, x, ten);
            BinaryOperation constraint = new BinaryOperation(Operation.Operator.AND, gt, lt);
            
            System.out.println("✓ Created constraint: x > 5 && x < 10");
            
            // Create instance and solve
            Instance instance = new Instance(solver, null, constraint);
            Boolean result = (Boolean) instance.request("sat");
            
            System.out.println("✓ SAT result: " + (result ? "SATISFIABLE" : "UNSATISFIABLE"));
            
            if (result) {
                System.out.println("✓ Constraint is satisfiable (expected: x ∈ {6,7,8,9})");
            }
            
            solver.report();
            System.out.println("\n🎉 Green + Z3-TurnKey integration test PASSED!");
            
        } catch (Exception e) {
            System.err.println("❌ Green + Z3-TurnKey test FAILED:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}