package org.xvm.compiler.ast;


import org.xvm.util.ListMap;

import java.util.List;
import java.util.Map;


/**
 * This is a complicated form of the variable declaration statement that allows for multiple
 * L-values to be assigned, of which any number can be a new variable declaration.
 *
 * @author cp 2017.04.10
 */
public class MultipleDeclarationStatement
        extends Statement
    {
    // ----- constructors --------------------------------------------------------------------------

    public MultipleDeclarationStatement(List<Statement> lvalues, Expression rvalue)
        {
        this.lvalues = lvalues;
        this.rvalue  = rvalue;
        }


    // ----- accessors -----------------------------------------------------------------------------


    // ----- debugging assistance ------------------------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append('(');

        boolean first = true;
        for (Statement stmt : lvalues)
            {
            if (first)
                {
                first = false;
                }
            else
                {
                sb.append(", ");
                }

            sb.append(stmt);
            }

        sb.append(" = ")
          .append(rvalue)
          .append(';');

        return sb.toString();
        }

    @Override
    public String getDumpDesc()
        {
        return toString();
        }

    @Override
    public Map<String, Object> getDumpChildren()
        {
        ListMap<String, Object> map = new ListMap();
        map.put("lvalues", lvalues);
        map.put("rvalue", rvalue);
        return map;
        }


    // ----- fields --------------------------------------------------------------------------------

    protected List<Statement> lvalues;
    protected Expression      rvalue;
    }