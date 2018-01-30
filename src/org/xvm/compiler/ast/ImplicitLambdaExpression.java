package org.xvm.compiler.ast;


import org.xvm.compiler.Token;

import java.lang.reflect.Field;

import java.util.List;

import static org.xvm.util.Handy.indentLines;


/**
 * Lambda expression is an inlined function. This version uses parameters that are assumed to be
 * names only.
 */
public class ImplicitLambdaExpression
        extends Expression
    {
    // ----- constructors --------------------------------------------------------------------------

    public ImplicitLambdaExpression(List<Expression> params, Token operator, StatementBlock body, long lStartPos)
        {
        this.params    = params;
        this.operator  = operator;
        this.body      = body;
        this.lStartPos = lStartPos;
        }


    // ----- accessors -----------------------------------------------------------------------------

    @Override
    public long getStartPosition()
        {
        return lStartPos;
        }

    @Override
    public long getEndPosition()
        {
        return body.getEndPosition();
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- compilation ---------------------------------------------------------------------------

    @Override
    public boolean isConstant()
        {
        // there has to be one or more return statements in the lambda, but it can only be treated
        // as a constant if the entire lambda is just one return statement
        if (body.stmts.size() == 1 && body.stmts.get(0) instanceof ReturnStatement)
            {
            List<Expression> exprs = ((ReturnStatement) body.stmts.get(0)).exprs;
            return exprs.size() == 1 && exprs.get(0).isConstant();
            }

        return false;
        }


    // ----- debugging assistance ------------------------------------------------------------------

    public String toSignatureString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append('(');
        boolean first = true;
        for (Expression param : params)
            {
            if (first)
                {
                first = false;
                }
            else
                {
                sb.append(", ");
                }
            sb.append(param);
            }

        sb.append(')')
          .append(' ')
          .append(operator.getId().TEXT);

        return sb.toString();
        }

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append(toSignatureString());

        String s = body.toString();
        if (s.indexOf('\n') >= 0)
            {
            sb.append('\n')
              .append(indentLines(s, "    "));
            }
        else
            {
            sb.append(' ')
              .append(s);
            }

        return sb.toString();
        }

    @Override
    public String toDumpString()
        {
        return toSignatureString() + " {...}";
        }


    // ----- fields --------------------------------------------------------------------------------

    protected List<Expression> params;
    protected Token            operator;
    protected StatementBlock   body;
    protected long             lStartPos;

    private static final Field[] CHILD_FIELDS = fieldsForNames(ImplicitLambdaExpression.class, "params", "body");
    }
