package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.OpCallable;
import org.xvm.asm.Register;

import org.xvm.runtime.CallChain;
import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;
import org.xvm.runtime.Utils;

import org.xvm.runtime.template.xException;
import org.xvm.runtime.template.Function.FunctionHandle;

import org.xvm.runtime.template.collections.xTuple.TupleHandle;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * CALL_T1 rvalue-function, rvalue-params-tuple, lvalue-tuple-return
 */
public class Call_TT
        extends OpCallable
    {
    /**
     * Construct a CALL_TT op.
     *
     * @param nFunction       the r-value indicating the function to call
     * @param nTupleArg       the r-value indicating the tuple holding the arguments
     * @param nRetTupleValue  the l-value location for the tuple result
     *
     * @deprecated
     */
    public Call_TT(int nFunction, int nTupleArg, int nRetTupleValue)
        {
        super(nFunction);

        m_nArgTupleValue = nTupleArg;
        m_nRetTupleValue = nRetTupleValue;
        }

    /**
     * Construct a CALL_TT op based on the passed arguments.
     *
     * @param argFunction  the function Argument
     * @param argValue     the value Argument
     * @param regReturn    the return Register
     */
    public Call_TT(Argument argFunction, Argument argValue, Register regReturn)
        {
        super(argFunction);

        m_argValue = argValue;
        m_regReturn = regReturn;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public Call_TT(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(readPackedInt(in));

        m_nArgTupleValue = readPackedInt(in);
        m_nRetTupleValue = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out, ConstantRegistry registry)
            throws IOException
        {
        super.write(out, registry);

        if (m_argValue != null)
            {
            m_nArgTupleValue = encodeArgument(m_argValue, registry);
            m_nRetTupleValue = encodeArgument(m_regReturn, registry);
            }

        writePackedLong(out, m_nArgTupleValue);
        writePackedLong(out, m_nRetTupleValue);
        }

    @Override
    public int getOpCode()
        {
        return OP_CALL_TT;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            // while the argument could be a local property holding a Tuple,
            // the Tuple values cannot be local properties
            ObjectHandle hArg = frame.getArgument(m_nArgTupleValue);
            if (hArg == null)
                {
                return R_REPEAT;
                }

            if (m_nFunctionValue == A_SUPER)
                {
                CallChain chain = frame.m_chain;
                if (chain == null)
                    {
                    throw new IllegalStateException();
                    }

                if (isProperty(hArg))
                    {
                    ObjectHandle[] ahArg = new ObjectHandle[] {hArg};
                    Frame.Continuation stepLast = frameCaller ->
                        chain.callSuperN1(frameCaller, ((TupleHandle) ahArg[0]).m_ahValue, -m_nRetTupleValue - 1);

                    return new Utils.GetArgument(ahArg, stepLast).doNext(frame);
                    }
                return chain.callSuperN1(frame, ((TupleHandle) hArg).m_ahValue, -m_nRetTupleValue - 1);
                }

            if (m_nFunctionValue < 0)
                {
                MethodStructure function = getMethodStructure(frame);

                if (isProperty(hArg))
                    {
                    ObjectHandle[] ahArg = new ObjectHandle[] {hArg};
                    Frame.Continuation stepLast = frameCaller ->
                        complete(frameCaller, function, (TupleHandle) ahArg[0]);

                    return new Utils.GetArgument(ahArg, stepLast).doNext(frame);
                    }

                return complete(frame, function, (TupleHandle) hArg);
                }

            FunctionHandle hFunction = (FunctionHandle) frame.getArgument(m_nFunctionValue);
            if (hFunction == null)
                {
                return R_REPEAT;
                }

            if (isProperty(hArg))
                {
                ObjectHandle[] ahArg = new ObjectHandle[] {hArg};
                Frame.Continuation stepLast = frameCaller ->
                    complete(frameCaller, hFunction, (TupleHandle) ahArg[0]);

                return new Utils.GetArgument(ahArg, stepLast).doNext(frame);
                }

            return complete(frame, hFunction, (TupleHandle) hArg);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }

    protected int complete(Frame frame, MethodStructure function, TupleHandle hArg)
        {
        ObjectHandle[] ahArg = hArg.m_ahValue;
        if (ahArg.length != function.getParamCount())
            {
            return frame.raiseException(xException.makeHandle("Invalid tuple argument"));
            }

        ObjectHandle[] ahVar = Utils.ensureSize(ahArg, function.getMaxVars());

        return frame.call1(function, null, ahVar, -m_nRetTupleValue - 1);
        }

    protected int complete(Frame frame, FunctionHandle hFunction, TupleHandle hArg)
        {
        ObjectHandle[] ahArg = hArg.m_ahValue;
        if (ahArg.length != hFunction.getParamCount())
            {
            return frame.raiseException(xException.makeHandle("Invalid tuple argument"));
            }

        ObjectHandle[] ahVar = Utils.ensureSize(ahArg, hFunction.getVarCount());

        return hFunction.call1(frame, null, ahVar, -m_nRetTupleValue - 1);
        }

    @Override
    public void registerConstants(ConstantRegistry registry)
        {
        super.registerConstants(registry);

        registerArgument(m_argValue, registry);
        }


    private int m_nArgTupleValue;
    private int m_nRetTupleValue;

    private Argument m_argValue;
    private Register m_regReturn;
    }