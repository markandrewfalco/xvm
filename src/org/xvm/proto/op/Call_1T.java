package org.xvm.proto.op;

import org.xvm.proto.Frame;
import org.xvm.proto.ObjectHandle;
import org.xvm.proto.ObjectHandle.ExceptionHandle;
import org.xvm.proto.OpCallable;
import org.xvm.proto.TypeCompositionTemplate.InvocationTemplate;

import org.xvm.proto.template.xFunction.FunctionHandle;

/**
 * CALL_1T rvalue-function, rvalue-param, lvalue-return-tuple
 *
 * (generated by the compiler when the callee (function) has a multi-return, but the
 *  caller needs a tuple back)
 *
 * @author gg 2017.03.08
 */
public class Call_1T extends OpCallable
    {
    private final int f_nFunctionValue;
    private final int f_nArgValue;
    private final int f_nRetValue;

    public Call_1T(int nFunction, int nArg, int nRet)
        {
        f_nFunctionValue = nFunction;
        f_nArgValue = nArg;
        f_nRetValue = nRet;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            if (f_nFunctionValue == A_SUPER)
                {
                return callSuperN1(frame, new int[]{f_nArgValue}, -f_nRetValue - 1);
                }

            ObjectHandle hArg = frame.getArgument(f_nArgValue);
            if (hArg == null)
                {
                return R_REPEAT;
                }

            if (f_nFunctionValue < 0)
                {
                InvocationTemplate function = getFunctionTemplate(frame, -f_nFunctionValue);

                ObjectHandle[] ahVar = new ObjectHandle[function.m_cVars];
                ahVar[0] = hArg;

                return frame.call1(function, null, ahVar, -f_nRetValue - 1);
                }

            FunctionHandle hFunction = (FunctionHandle) frame.getArgument(f_nFunctionValue);
            if (hFunction == null)
                {
                return R_REPEAT;
                }

            ObjectHandle[] ahVar = new ObjectHandle[hFunction.getVarCount()];
            ahVar[0] = hArg;

            return hFunction.call1(frame, ahVar, -f_nRetValue - 1);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            frame.m_hException = e.getExceptionHandle();
            return R_EXCEPTION;
            }
        }
    }