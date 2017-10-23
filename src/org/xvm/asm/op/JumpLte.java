package org.xvm.asm.op;


import java.io.DataInput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.Op;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.Utils;

import org.xvm.runtime.template.xOrdered;


/**
 * JMP_LTE rvalue1, rvalue2, addr ; jump if value1 is less than or equal than value2
 */
public class JumpLte
        extends JumpCond
    {
    /**
     * Construct a JMP_LTE op.
     *
     * @param nValue1   the first value to compare
     * @param nValue2   the second value to compare
     * @param nRelAddr  the relative address to jump to
     */
    public JumpLte(int nValue1, int nValue2, int nRelAddr)
        {
        super((Argument) null, null, null);

        m_nArg  = nValue1;
        m_nArg2 = nValue2;
        m_ofJmp = nRelAddr;
        }

    /**
     * Construct a JMP_LTE op.
     *
     * @param arg1  the first argument to compare
     * @param arg2  the second argument to compare
     * @param op    the op to conditionally jump to
     */
    public JumpLte(Argument arg1, Argument arg2, Op op)
        {
        super(arg1, arg2, op);
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public JumpLte(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(in, aconst);
        }

    @Override
    public int getOpCode()
        {
        return OP_JMP_LTE;
        }

    @Override
    protected boolean isBinaryOp()
        {
        return true;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            ObjectHandle hArg1 = frame.getArgument(m_nArg);
            ObjectHandle hArg2 = frame.getArgument(m_nArg2);
            if (hArg1 == null || hArg2 == null)
                {
                return R_REPEAT;
                }

            if (isProperty(hArg1) || isProperty(hArg2))
                {
                ObjectHandle[] ahArg = new ObjectHandle[] {hArg1, hArg2};
                Frame.Continuation stepNext = frameCaller ->
                    complete(frame, iPC, ahArg[0], ahArg[1]);

                return new Utils.GetArguments(ahArg, stepNext).doNext(frame);
                }

            return complete(frame, iPC, hArg1, hArg2);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }

    protected int complete(Frame frame, int iPC, ObjectHandle hArg1, ObjectHandle hArg2)
        {
        TypeComposition clz1 = frame.getArgumentClass(m_nArg);
        TypeComposition clz2 = frame.getArgumentClass(m_nArg2);
        if (clz1 != clz2)
            {
            // this shouldn't have compiled
            throw new IllegalStateException();
            }

        switch (clz1.callEquals(frame, hArg1, hArg2, Frame.RET_LOCAL))
            {
            case R_NEXT:
                return frame.getFrameLocal() == xOrdered.GREATER ? iPC + 1 : iPC + m_ofJmp;

            case R_CALL:
                frame.m_frameNext.setContinuation(frameCaller ->
                    frameCaller.getFrameLocal() == xOrdered.GREATER ? iPC + 1 : iPC + m_ofJmp);
                return R_CALL;

            case R_EXCEPTION:
                return R_EXCEPTION;

            default:
                throw new IllegalStateException();
            }
        }
    }
