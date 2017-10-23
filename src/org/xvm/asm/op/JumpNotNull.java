package org.xvm.asm.op;


import java.io.DataInput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.Op;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;
import org.xvm.runtime.Utils;

import org.xvm.runtime.template.xNullable;


/**
 * JMP_NNULL rvalue, addr ; jump if value is NOT null
 */
public class JumpNotNull
        extends JumpCond
    {
    /**
     * Construct a JMP_NNULL op.
     *
     * @param nValue    the Nullable value to test
     * @param nRelAddr  the relative address to jump to
     *
     * @deprecated
     */
    public JumpNotNull(int nValue, int nRelAddr)
        {
        super((Argument) null, null);

        m_nArg  = nValue;
        m_ofJmp = nRelAddr;
        }

    /**
     * Construct a JMP_NNULL op.
     *
     * @param arg  the argument to test
     * @param op   the op to conditionally jump to
     */
    public JumpNotNull(Argument arg, Op op)
        {
        super(arg, op);
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public JumpNotNull(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(in, aconst);
        }

    @Override
    public int getOpCode()
        {
        return OP_JMP_NNULL;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            ObjectHandle hArg = frame.getArgument(m_nArg);
            if (hArg == null)
                {
                return R_REPEAT;
                }

            if (isProperty(hArg))
                {
                ObjectHandle[] ahArg = new ObjectHandle[] {hArg};
                Frame.Continuation stepNext = frameCaller ->
                    hArg == xNullable.NULL ? iPC + 1 : iPC + m_ofJmp;

                return new Utils.GetArgument(ahArg, stepNext).doNext(frame);
                }

            return hArg == xNullable.NULL ? iPC + 1 : iPC + m_ofJmp;
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }
    }
