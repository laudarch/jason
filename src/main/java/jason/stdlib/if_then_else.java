package jason.stdlib;

import java.util.Iterator;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.IntendedMeans;
import jason.asSemantics.InternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.PlanBody;
import jason.asSyntax.Term;

/** 
Implementation of <b>if</b>. 

<p>Syntax:
<pre>
  if ( <i>logical formula</i> ) {
     <i>plan_body1</i>
  [ } else { <i>plan_body2</i> ]
  }
</pre>
</p>

<p>if <i>logical formula</i> holds, <i>plan_body1</i> is executed; 
otherwise, <i>plan_body2</i> is executed.</p>

<p>Example:
<pre>
+event : context
  <- ....
     if (vl(X) & X > 10) { // where vl(X) is a belief
       .print("value > 10");
     }
     ....
</pre>
The unification is changed by the evaluation of the logical formula, i.e., X might have a value after if.
</p>

*/
public class if_then_else extends DefaultInternalAction {

    private static InternalAction singleton = null;
    public static InternalAction create() {
        if (singleton == null) 
            singleton = new if_then_else();
        return singleton;
    }
    
    @Override public Term[] prepareArguments(Literal body, Unifier un) {
        return body.getTermsArray();
    }
    

    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 3; }
    
    @Override protected void checkArguments(Term[] args) throws JasonException {
        super.checkArguments(args); // check number of arguments
        if ( !(args[0] instanceof LogicalFormula))
            throw JasonException.createWrongArgument(this,"first argument (test) must be a logical formula.");
        if ( !args[1].isPlanBody())
            throw JasonException.createWrongArgument(this,"second argument (then) must be a plan body term.");
        if ( args.length == 3 && !args[2].isPlanBody())
            throw JasonException.createWrongArgument(this,"third argument (else) must be a plan body term.");
    }
    
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
            
        LogicalFormula logExpr = (LogicalFormula)args[0];
        PlanBody whattoadd = null;
            
        Iterator<Unifier> iu = logExpr.logicalConsequence(ts.getAg(), un);
        if (iu.hasNext()) { // .if THEN
            whattoadd = (PlanBody)args[1].clone(); // need to clone due to setAsBodyTerm(false)
            un.compose(iu.next());
        } else if (args.length == 3) { // .if ELSE
            whattoadd = (PlanBody)args[2].clone();
        }

        if (whattoadd != null) {
            IntendedMeans im = ts.getC().getSelectedIntention().peek();
            whattoadd.add(im.getCurrentStep().getBodyNext());
            whattoadd.setAsBodyTerm(false);
            im.insertAsNextStep(whattoadd);
        }
        return true;
    }
}
