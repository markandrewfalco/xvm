package org.xvm.compiler.ast;


import org.xvm.compiler.Token;

import org.xvm.util.ListMap;

import java.util.Map;


/**
 * A case statement. This can only occur within a switch statement. (It's not a "real" statement;
 * it's more like a label.)
 *
 * @author cp 2017.04.09
 */
public class CaseStatement
        extends Statement
    {
    // ----- constructors --------------------------------------------------------------------------

    public CaseStatement(Token keyword, Expression expr)
        {
        this.keyword = keyword;
        this.expr    = expr;
        }


    // ----- accessors -----------------------------------------------------------------------------


    // ----- debugging assistance ------------------------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append(keyword.getId().TEXT);

        if (expr != null)
            {
            sb.append(' ')
              .append(expr);
            }

        sb.append(':');

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
        map.put("keyword", keyword);
        map.put("expr", expr);
        return map;
        }


    // ----- fields --------------------------------------------------------------------------------

    protected Token keyword;
    protected Expression expr;
    }