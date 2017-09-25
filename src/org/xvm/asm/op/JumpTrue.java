package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.Op;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;

import org.xvm.runtime.template.xBoolean.BooleanHandle;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * JMP_TRUE rvalue-bool, rel-addr ; jump if value is true
 */
public class JumpTrue
        extends Op
    {
    /**
     * Construct a JMP_TRUE op.
     *
     * @param nValue    the Boolean value to test
     * @param nRelAddr  the relative address to jump to.
     */
    public JumpTrue(int nValue, int nRelAddr)
        {
        f_nValue   = nValue;
        f_nRelAddr = nRelAddr;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public JumpTrue(DataInput in, Constant[] aconst)
            throws IOException
        {
        f_nValue   = readPackedInt(in);
        f_nRelAddr = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.writeByte(OP_JMP_TRUE);
        writePackedLong(out, f_nValue);
        writePackedLong(out, f_nRelAddr);
        }

    @Override
    public int getOpCode()
        {
        return OP_JMP_TRUE;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            BooleanHandle hTest = (BooleanHandle) frame.getArgument(f_nValue);
            if (hTest == null)
                {
                return R_REPEAT;
                }

            return hTest.get() ? iPC + f_nRelAddr : iPC + 1;
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }

    private final int f_nValue;
    private final int f_nRelAddr;
    }