package org.xvm.compiler.ast;


import java.lang.reflect.Field;

import java.util.Collections;
import java.util.List;

import org.xvm.asm.ConstantPool;
import org.xvm.asm.ErrorListener;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.MethodStructure.Code;
import org.xvm.asm.Argument;

import org.xvm.asm.constants.TypeConstant;

import org.xvm.asm.op.Return_0;
import org.xvm.asm.op.Return_1;
import org.xvm.asm.op.Return_N;
import org.xvm.asm.op.Return_T;

import org.xvm.compiler.Compiler;
import org.xvm.compiler.Token;

import org.xvm.compiler.ast.Expression.Assignable;

import org.xvm.util.Severity;


/**
 * A return statement specifies a return with optional values.
 */
public class ReturnStatement
        extends Statement
    {
    // ----- constructors --------------------------------------------------------------------------

    public ReturnStatement(Token keyword)
        {
        this(keyword, (List) null);
        }

    public ReturnStatement(Token keyword, Expression expr)
        {
        this(keyword, Collections.singletonList(expr));
        }

    public ReturnStatement(Token keyword, List<Expression> exprs)
        {
        this.keyword = keyword;
        this.exprs   = exprs;
        }


    // ----- accessors -----------------------------------------------------------------------------

    /**
     * @return the expression(s) that the return statement returns, or null if there are no
     *         expressions
     */
    public List<Expression> getExpressions()
        {
        return exprs;
        }

    @Override
    public long getStartPosition()
        {
        return keyword.getStartPosition();
        }

    @Override
    public long getEndPosition()
        {
        return exprs == null ? keyword.getEndPosition() : exprs.get(exprs.size()-1).getEndPosition();
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- compilation ---------------------------------------------------------------------------

    @Override
    protected Statement validate(Context ctx, ErrorListener errs)
        {
        boolean fValid = true;

        AstNode        container = getCodeContainer();
        boolean        fConditional;
        TypeConstant[] aRetTypes;
        if (container instanceof MethodDeclarationStatement)
            {
            MethodStructure structMethod = ctx.getMethod();
            fConditional = structMethod.isConditionalReturn();
            aRetTypes    = structMethod.getReturnTypes();
            }
        else if (container instanceof LambdaExpression)
            {
            // TODO
            throw new UnsupportedOperationException("TODO:Lambda");
            }
        else if (container instanceof StatementExpression)
            {
            StatementExpression expr         = (StatementExpression) container;
            TypeConstant        typeRequired = expr.getRequiredType();
            if (typeRequired == null)
                {
                // part of the purpose of validating this statement is to determine the return type
                // of the enclosing StatementExpression, so assume a single value of any type
                typeRequired = pool().typeObject();
                }

            aRetTypes    = new TypeConstant[] {typeRequired};
            fConditional = false;
            }
        else
            {
            throw new IllegalStateException("container=" + container);
            }

        int              cRets        = aRetTypes.length;
        List<Expression> listExprs    = this.exprs;
        int              cExprs       = listExprs == null ? 0 : listExprs.size();

        // resolve auto-narrowing
        for (int i = 0; i < cRets; i++)
            {
            TypeConstant typeRet = aRetTypes[i];
            if (typeRet.isAutoNarrowing())
                {
                aRetTypes[i] = typeRet.resolveAutoNarrowing(ctx.getThisClass().getFormalType());
                }
            }

        // void methods are the simplest
        if (cExprs == 0 || cRets == 0)
            {
            if (cExprs > 0)
                {
                // check the expressions anyhow (even though they can't be used)
                for (int i = 0; i < cExprs; ++i)
                    {
                    Expression exprOld = listExprs.get(i);
                    Expression exprNew = exprOld.validate(ctx, null, errs);
                    if (exprNew != exprOld)
                        {
                        fValid &= exprNew != null;
                        if (exprNew != null)
                            {
                            listExprs.set(i, exprNew);
                            }
                        }
                    }

                // allow the (strange) use of T0D0 or the (strange) return of a void expression
                if (cExprs != 1 || !listExprs.get(0).isAborting() || !listExprs.get(0).isVoid())
                    {
                    // it was supposed to be a void return
                    log(errs, Severity.ERROR, Compiler.RETURN_VOID);
                    fValid = false;
                    }
                }
            else if (cRets > 0)
                {
                // the expressions are missing; it was NOT supposed to be a void return
                log(errs, Severity.ERROR, Compiler.RETURN_EXPECTED);
                fValid = false;
                }
            }
        else if (cExprs > 1)
            {
            // validate each expression, telling it what return type is expected
            for (int i = 0; i < cExprs; ++i)
                {
                TypeConstant typeRet = i < cRets
                        ? aRetTypes[i]
                        : null;
                Expression exprOld = listExprs.get(i);
                Expression exprNew = exprOld.validate(ctx, typeRet, errs);
                if (exprNew != exprOld)
                    {
                    fValid &= exprNew != null;
                    if (exprNew != null)
                        {
                        listExprs.set(i, exprNew);
                        }
                    }
                }

            // make sure the arity is correct (the number of exprs has to match the number of rets)
            if (cExprs != cRets)
                {
                log(errs, Severity.ERROR, Compiler.RETURN_WRONG_COUNT, cRets, cExprs);
                }
            }
        else // cExprs == 1
            {
            Expression exprOld = listExprs.get(0);
            Expression exprNew = null;
            // several possibilities:
            // 1) most likely the expression provides a single value, which matches the single
            //    return type for the method
            if (cRets == 1 && exprOld.testFit(ctx, aRetTypes[0]).isFit())
                {
                exprNew = exprOld.validate(ctx, aRetTypes[0], errs);
                }
            else
                {
                // 2) it could be a tuple return
                ConstantPool pool      = pool();
                TypeConstant typeTuple = pool.ensureParameterizedTypeConstant(pool.typeTuple(), aRetTypes);
                if (exprOld.testFit(ctx, typeTuple).isFit())
                    {
                    exprNew = exprOld.validate(ctx, typeTuple, errs);
                    m_fTupleReturn = true;
                    }
                // 3) it could be a conditional false
                else if (fConditional && exprOld.testFit(ctx, pool.typeFalse()).isFit())
                    {
                    exprNew = exprOld.validate(ctx, pool.typeFalse(), errs);
                    if (exprNew != null && (!exprNew.isConstant() || !exprNew.toConstant().equals(pool.valFalse())))
                        {
                        // it's not clear how this could happen; it's more like an assertion
                        log(errs, Severity.ERROR, Compiler.CONSTANT_REQUIRED);
                        fValid = false;
                        }
                    }
                // 4) otherwise it's an error (so force validation based on the return types, which
                // will log the error)
                else
                    {
                    exprNew = exprOld.validateMulti(ctx, aRetTypes, errs);
                    }
                }

            if (exprNew != exprOld)
                {
                fValid &= exprNew != null;
                if (exprNew != null)
                    {
                    listExprs.set(0, exprNew);
                    }
                }
            }

        if (fValid)
            {
            if (container instanceof LambdaExpression)
                {
                // TODO
                throw new UnsupportedOperationException("TODO:Lambda");
                }
            else if (container instanceof StatementExpression)
                {
                ((StatementExpression) container).addReturnType(listExprs.get(0).getType());
                }
            }

        return fValid
                ? this
                : null;
        }

    @Override
    protected boolean emit(Context ctx, boolean fReachable, Code code, ErrorListener errs)
        {
        AstNode container = getCodeContainer();
        if (container instanceof StatementExpression)
            {
            // emit() for a return inside a StatementExpression produces an assignment from the
            // expression REVIEW tuple return, #exprs > 1
            Assignable LVal = ((StatementExpression) container).getAssignable();
            exprs.get(0).generateAssignment(ctx, code, LVal, errs);

            // "return" does not complete
            return false;
            }

        // first determine what the method declaration indicates the return value is (none, one,
        // or multi)
        MethodStructure  structMethod = ctx.getMethod();
        TypeConstant[]   aRetTypes    = structMethod.getReturnTypes();
        int              cRets        = aRetTypes.length;
        List<Expression> listExprs    = this.exprs;
        int              cExprs       = listExprs == null ? 0 : listExprs.size();

        if (m_fTupleReturn)
            {
            // the return statement has a single expression; the type that the expression has to
            // generate is the "tuple of" all of the return types
            ConstantPool pool = pool();
            Argument     arg  = listExprs.get(0).generateArgument(ctx, code, true, true, errs);
            code.add(new Return_T(arg));
            }
        else
            {
            switch (cExprs)
                {
                case 0:
                    code.add(new Return_0());
                    break;

                case 1:
                    Argument arg = listExprs.get(0).generateArgument(ctx, code, true, true, errs);
                    code.add(new Return_1(arg));
                    break;

                default:
                    Argument[] args = new Argument[cExprs];
                    for (int i = 0; i < cExprs; ++i)
                        {
                        args[i] = listExprs.get(i).generateArgument(ctx, code, true, true, errs);
                        }
                    code.add(new Return_N(args));
                    break;
                }
            }

        // return never completes
        return false;
        }


    // ----- debugging assistance ------------------------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();
        sb.append("return");
        if(exprs != null)
            {
            switch (exprs.size())
                {
                case 0:
                    break;

                case 1:
                    sb.append(' ')
                      .append(exprs.get(0));
                    break;

                default:
                    boolean first = true;
                    for (Expression expr : exprs)
                        {
                        if (first)
                            {
                            first = false;
                            sb.append(" ");
                            }
                        else
                            {
                            sb.append(", ");
                            }
                        sb.append(expr);
                        }
                    break;
                }
            }
        sb.append(';');
        return sb.toString();
        }

    @Override
    public String getDumpDesc()
        {
        return toString();
        }


    // ----- fields --------------------------------------------------------------------------------

    protected Token             keyword;
    protected List<Expression>  exprs;

    protected transient boolean m_fTupleReturn;

    private static final Field[] CHILD_FIELDS = fieldsForNames(ReturnStatement.class, "exprs");
    }
