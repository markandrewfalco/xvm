package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.OpCallable;

import org.xvm.runtime.CallChain;
import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;

import org.xvm.runtime.template.xException;
import org.xvm.runtime.template.Function.FunctionHandle;

import org.xvm.runtime.template.collections.xTuple.TupleHandle;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * CALL_T1 rvalue-function, rvalue-params-tuple, lvalue return
 */
public class Call_T1
        extends OpCallable
    {
    private final int f_nFunctionValue;
    private final int f_nArgTupleValue;
    private final int f_nRetValue;

    public Call_T1(int nFunction, int nTupleArg, int nRetValue)
        {
        f_nFunctionValue = nFunction;
        f_nArgTupleValue = nTupleArg;
        f_nRetValue = nRetValue;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public Call_T1(DataInput in, Constant[] aconst)
            throws IOException
        {
        f_nFunctionValue = readPackedInt(in);
        f_nArgTupleValue = readPackedInt(in);
        f_nRetValue = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out)
    throws IOException
        {
        out.write(OP_CALL_T1);
        writePackedLong(out, f_nFunctionValue);
        writePackedLong(out, f_nArgTupleValue);
        writePackedLong(out, f_nRetValue);
        }

    @Override
    public int getOpCode()
        {
        return OP_CALL_T1;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        if (f_nFunctionValue == A_SUPER)
            {
            CallChain chain = frame.m_chain;
            if (chain == null)
                {
                throw new IllegalStateException();
                }

            return chain.callSuperN1(frame, new int[]{f_nArgTupleValue}, f_nRetValue);
            }

        try
            {
            TupleHandle hArgTuple = (TupleHandle) frame.getArgument(f_nArgTupleValue);
            if (hArgTuple == null)
                {
                return R_REPEAT;
                }

            ObjectHandle[] ahArg = hArgTuple.m_ahValue;

            if (f_nFunctionValue < 0)
                {
                MethodStructure function = getMethodStructure(frame, -f_nFunctionValue);
                if (ahArg.length != function.getParamCount())
                    {
                    return frame.raiseException(xException.makeHandle("Invalid tuple argument"));
                    }

                ObjectHandle[] ahVar = new ObjectHandle[function.getMaxVars()];
                System.arraycopy(ahArg, 0, ahVar, 0, ahArg.length);

                return frame.call1(function, null, ahVar, f_nRetValue);
                }

            FunctionHandle hFunction = (FunctionHandle) frame.getArgument(f_nFunctionValue);
            if (hFunction == null)
                {
                return R_REPEAT;
                }

            ObjectHandle[] ahVar = new ObjectHandle[hFunction.getVarCount()];

            if (ahArg.length != getMethodStructure(frame, f_nFunctionValue).getParamCount())
                {
                return frame.raiseException(xException.makeHandle("Invalid tuple argument"));
                }

            System.arraycopy(ahArg, 0, ahVar, 0, ahArg.length);

            return hFunction.call1(frame, null, ahVar, f_nRetValue);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }
    }