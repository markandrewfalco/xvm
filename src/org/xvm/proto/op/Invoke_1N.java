package org.xvm.proto.op;

import org.xvm.proto.CallChain;
import org.xvm.proto.Frame;
import org.xvm.proto.ObjectHandle;
import org.xvm.proto.ObjectHandle.ExceptionHandle;
import org.xvm.proto.OpInvocable;
import org.xvm.proto.TypeComposition;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * INVOKE_1N rvalue-target, CONST-METHOD, rvalue-param, #returns:(lvalue)
 *
 * @author gg 2017.03.08
 */
public class Invoke_1N extends OpInvocable
    {
    private final int f_nTargetValue;
    private final int f_nMethodId;
    private final int f_nArgValue;
    private final int[] f_anRetValue;

    public Invoke_1N(int nTarget, int nMethodId, int nArg, int[] anRet)
        {
        f_nTargetValue = nTarget;
        f_nMethodId = nMethodId;
        f_nArgValue = nArg;
        f_anRetValue = anRet;
        }

    public Invoke_1N(DataInput in)
            throws IOException
        {
        f_nTargetValue = in.readInt();
        f_nMethodId = in.readInt();
        f_nArgValue = in.readInt();
        f_anRetValue = readIntArray(in);
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_INVOKE_1N);
        out.writeInt(f_nTargetValue);
        out.writeInt(f_nMethodId);
        out.writeInt(f_nArgValue);
        writeIntArray(out, f_anRetValue);
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            ObjectHandle hTarget = frame.getArgument(f_nTargetValue);
            ObjectHandle hArg = frame.getArgument(f_nArgValue);

            if (hTarget == null || hArg == null)
                {
                return R_REPEAT;
                }

            TypeComposition clz = hTarget.f_clazz;
            CallChain chain = getCallChain(frame, clz, f_nMethodId);

            if (chain.isNative())
                {
                return clz.f_template.invokeNativeNN(frame, chain.getTop(), hTarget,
                        new ObjectHandle[]{hArg}, f_anRetValue);
                }

            ObjectHandle[] ahVar = new ObjectHandle[frame.f_adapter.getVarCount(chain.getTop())];
            ahVar[0] = hArg;

            return clz.f_template.invokeN(frame, chain, hTarget, ahVar, f_anRetValue);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            frame.m_hException = e.getExceptionHandle();
            return R_EXCEPTION;
            }
        }
    }
