package org.xvm.compiler.ast;


import java.lang.reflect.Field;

import java.util.List;

import org.xvm.asm.Argument;
import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.ErrorListener;
import org.xvm.asm.MethodStructure.Code;

import org.xvm.asm.constants.TypeCollector;
import org.xvm.asm.constants.TypeConstant;

import org.xvm.asm.op.Var_S;


/**
 * A list expression is an expression containing some number (0 or more) expressions of some common
 * type.
 * <p/>
 * <pre>
 * ListLiteral
 *     "[" ExpressionList-opt "]"
 *     "Sequence:{" ExpressionList-opt "}"
 *     "List:{" ExpressionList-opt "}"
 *     "Array:{" ExpressionList-opt "}"
 * </pre>
 */
public class ListExpression
        extends Expression
    {
    // ----- constructors --------------------------------------------------------------------------

    public ListExpression(TypeExpression type, List<Expression> exprs, long lStartPos, long lEndPos)
        {
        this.type      = type;
        this.exprs     = exprs;
        this.lStartPos = lStartPos;
        this.lEndPos   = lEndPos;
        }


    // ----- accessors -----------------------------------------------------------------------------

    public List<Expression> getExpressions()
        {
        return exprs;
        }

    @Override
    public long getStartPosition()
        {
        return lStartPos;
        }

    @Override
    public long getEndPosition()
        {
        return lEndPos;
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- compilation ---------------------------------------------------------------------------

    @Override
    public TypeConstant getImplicitType(Context ctx)
        {
        TypeConstant typeExplicit = type == null ? null : type.ensureTypeConstant(ctx);
        if (typeExplicit != null)
            {
            if (typeExplicit.containsUnresolved())
                {
                return null;
                }
            if (typeExplicit.resolveGenericType("Element") != null)
                {
                return typeExplicit;
                }
            }

        // see if there is an implicit element type
        TypeConstant typeArray   = typeExplicit == null ? pool().typeArray() : typeExplicit;
        TypeConstant typeElement = getImplicitElementType(ctx);
        if (typeElement != null)
            {
            typeArray = pool().ensureParameterizedTypeConstant(typeArray, typeElement);
            }
        return typeArray;
        }

    private TypeConstant getImplicitElementType(Context ctx)
        {
        int cElements = exprs.size();
        if (cElements > 0)
            {
            TypeConstant[] aElementTypes = new TypeConstant[cElements];
            for (int i = 0; i < cElements; ++i)
                {
                aElementTypes[i] = exprs.get(i).getImplicitType(ctx);
                }
            return TypeCollector.inferFrom(aElementTypes, pool());
            }
        return null;
        }

    @Override
    protected TypeFit calcFit(Context ctx, TypeConstant typeIn, TypeConstant typeOut)
        {
        if (exprs.isEmpty())
            {
            // an empty list fits any type
            return TypeFit.Fit;
            }

        ConstantPool pool = pool();
        if (typeOut != null && typeOut.isA(pool.typeSequence()) &&
            typeIn  != null && typeIn .isA(pool.typeSequence()))
            {
            typeOut = typeOut.resolveGenericType("Element");
            typeIn  = typeIn .resolveGenericType("Element");
            }
        return super.calcFit(ctx, typeIn, typeOut);
        }

    @Override
    protected Expression validate(Context ctx, TypeConstant typeRequired, ErrorListener errs)
        {
        ConstantPool     pool        = pool();
        TypeFit          fit         = TypeFit.Fit;
        List<Expression> listExprs   = exprs;
        int              cExprs      = listExprs.size();
        boolean          fConstant   = true;
        TypeConstant     typeElement = null;

        if (typeRequired != null)
            {
            typeElement = typeRequired.resolveGenericType("Element");
            }

        if (typeElement == null || typeElement.equals(pool.typeObject()))
            {
            typeElement = getImplicitElementType(ctx);
            }

        if (typeElement == null)
            {
            typeElement = pool.typeObject();
            }

        TypeExpression exprTypeOld = type;
        if (exprTypeOld != null)
            {
            TypeConstant   typeSeqType = pool.typeSequence().getType();
            TypeExpression exprTypeNew = (TypeExpression) exprTypeOld.validate(ctx, typeSeqType, errs);
            if (exprTypeNew == null)
                {
                fit       = TypeFit.NoFit;
                fConstant = false;
                }
            else
                {
                if (exprTypeNew != exprTypeOld)
                    {
                    type = exprTypeNew;
                    }
                typeElement = exprTypeNew.ensureTypeConstant(ctx).resolveAutoNarrowingBase(pool).
                        resolveGenericType("Element");
                }
            }

        TypeConstant typeActual = pool.ensureParameterizedTypeConstant(pool.typeArray(), typeElement);
        if (cExprs > 0)
            {
            ctx = ctx.enterList();
            for (int i = 0; i < cExprs; ++i)
                {
                Expression exprOld = listExprs.get(i);
                Expression exprNew = exprOld.validate(ctx, typeElement, errs);
                if (exprNew == null)
                    {
                    fit       = TypeFit.NoFit;
                    fConstant = false;
                    }
                else
                    {
                    if (exprNew != exprOld)
                        {
                        listExprs.set(i, exprNew);
                        }
                    fConstant &= exprNew.isConstant();
                    }
                }
            ctx = ctx.exit();
            }

        // build a constant if it's a known container type and all of the elements are constants
        Constant constVal = null;
        if (fConstant)
            {
            TypeConstant typeImpl = pool.ensureImmutableTypeConstant(
                    pool.ensureParameterizedTypeConstant(pool.typeArray(),
                    typeElement == null ? pool.typeObject() : typeElement));
            if (typeRequired == null || typeImpl.isA(typeRequired)) // Array<Element> or List<Element>
                {
                Constant[] aconstVal = new Constant[cExprs];
                for (int i = 0; i < cExprs; ++i)
                    {
                    aconstVal[i] = listExprs.get(i).toConstant();
                    }

                constVal = pool.ensureArrayConstant(typeImpl, aconstVal);
                }
            }

        return finishValidation(typeRequired, typeActual, fit, constVal, errs);
        }

    @Override
    public boolean isCompletable()
        {
        for (Expression expr : exprs)
            {
            if (!expr.isCompletable())
                {
                return false;
                }
            }

        return true;
        }

    @Override
    public boolean isShortCircuiting()
        {
        for (Expression expr : exprs)
            {
            if (expr.isShortCircuiting())
                {
                return true;
                }
            }

        return false;
        }

    @Override
    public Argument generateArgument(
            Context ctx, Code code, boolean fLocalPropOk, boolean fUsedOnce, ErrorListener errs)
        {
        if (isConstant())
            {
            return toConstant();
            }

        List<Expression> listExprs = exprs;
        int              cArgs     = listExprs.size();
        Argument[]       aArgs     = new Argument[cArgs];
        for (int i = 0; i < cArgs; ++i)
            {
            aArgs[i] = listExprs.get(i).generateArgument(ctx, code, false, true, errs);
            }
        code.add(new Var_S(getType(), aArgs));
        return code.lastRegister();
        }


    // ----- debugging assistance ------------------------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append('[');

        boolean first = true;
        for (Expression expr : exprs)
            {
            if (first)
                {
                first = false;
                }
            else
                {
                sb.append(", ");
                }
            sb.append(expr);
            }

        sb.append(']');

        return sb.toString();
        }

    @Override
    public String getDumpDesc()
        {
        return toString();
        }


    // ----- fields --------------------------------------------------------------------------------

    protected TypeExpression   type;
    protected List<Expression> exprs;
    protected long             lStartPos;
    protected long             lEndPos;

    private static final Field[] CHILD_FIELDS = fieldsForNames(ListExpression.class, "type", "exprs");
    }
