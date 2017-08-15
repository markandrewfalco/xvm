package org.xvm.proto.op;

import org.xvm.proto.Frame;
import org.xvm.proto.Op;
import org.xvm.proto.TypeComposition;

import org.xvm.proto.ObjectHandle;
import org.xvm.proto.ObjectHandle.ExceptionHandle;

import org.xvm.proto.template.xBoolean;
import org.xvm.proto.template.xEnum.EnumHandle;
import org.xvm.proto.template.xOrdered;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * IS_GTE rvalue, rvalue, lvalue-return ; T >= T -> Boolean
 *
 * @author gg 2017.03.08
 */
public class IsGte extends Op
    {
    private final int f_nValue1;
    private final int f_nValue2;
    private final int f_nRetValue;

    public IsGte(int nValue1, int nValue2, int nRet)
        {
        f_nValue1 = nValue1;
        f_nValue2 = nValue2;
        f_nRetValue = nRet;
        }

    public IsGte(DataInput in)
            throws IOException
        {
        f_nValue1 = in.readInt();
        f_nValue2 = in.readInt();
        f_nRetValue = in.readInt();
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_IS_GTE);
        out.writeInt(f_nValue1);
        out.writeInt(f_nValue2);
        out.writeInt(f_nRetValue);
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            ObjectHandle hValue1 = frame.getArgument(f_nValue1);
            ObjectHandle hValue2 = frame.getArgument(f_nValue2);

            if (hValue1 == null || hValue2 == null)
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

            int iResult = clz1.callCompare(frame, hValue1, hValue2, Frame.RET_LOCAL);
            if (iResult == R_EXCEPTION)
                {
                return R_EXCEPTION;
                }

            EnumHandle hResult = (EnumHandle) frame.getFrameLocal();

            frame.assignValue(f_nRetValue,
                    xBoolean.makeHandle(hResult != xOrdered.LESSER));
            return iPC + 1;
            }
        catch (ExceptionHandle.WrapperException e)
            {
            frame.m_hException = e.getExceptionHandle();
            return R_EXCEPTION;
            }
        }
    }