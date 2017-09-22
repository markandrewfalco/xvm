package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.Op;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;
import org.xvm.runtime.TypeComposition;

import org.xvm.runtime.template.xBoolean.BooleanHandle;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * JMP_NEQ rvalue, rvalue, rel-addr ; jump if value is NOT equal
 */
public class JumpNotEq
        extends Op
    {
    private final int f_nValue1;
    private final int f_nValue2;
    private final int f_nRelAddr;

    public JumpNotEq(int nValue1, int nValue2, int nRelAddr)
        {
        f_nValue1 = nValue1;
        f_nValue2 = nValue2;
        f_nRelAddr = nRelAddr;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public JumpNotEq(DataInput in, Constant[] aconst)
            throws IOException
        {
        f_nValue1 = readPackedInt(in);
        f_nValue2 = readPackedInt(in);
        f_nRelAddr = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_JMP_NEQ);
        writePackedLong(out, f_nValue1);
        writePackedLong(out, f_nValue2);
        writePackedLong(out, f_nRelAddr);
        }

    @Override
    public int getOpCode()
        {
        return OP_JMP_NEQ;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            ObjectHandle hTest1 = frame.getArgument(f_nValue1);
            ObjectHandle hTest2 = frame.getArgument(f_nValue2);

            if (hTest1 == null || hTest2 == null)
                {
                return R_REPEAT;
                }

            TypeComposition clz1 = frame.getArgumentClass(f_nValue1);
            TypeComposition clz2 = frame.getArgumentClass(f_nValue2);

            if (clz1 != clz2)
                {
                // this should've not compiled
                throw new IllegalStateException();
                }

            switch (clz1.callEquals(frame, hTest1, hTest2, Frame.RET_LOCAL))
                {
                case R_EXCEPTION:
                    return R_EXCEPTION;

                case R_NEXT:
                    {
                    BooleanHandle hValue = (BooleanHandle) frame.getFrameLocal();
                    return hValue.get() ? iPC + 1 : iPC + f_nRelAddr;
                    }

                case R_CALL:
                    frame.m_frameNext.setContinuation(frameCaller ->
                        {
                        BooleanHandle hValue = (BooleanHandle) frame.getFrameLocal();
                        return hValue.get() ? iPC + 1 : iPC + f_nRelAddr;
                        });
                    return R_CALL;

                default:
                    throw new IllegalStateException();
                }

            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }
    }
