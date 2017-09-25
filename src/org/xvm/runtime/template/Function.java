package org.xvm.runtime.template;


import java.util.concurrent.CompletableFuture;

import org.xvm.asm.ClassStructure;
import org.xvm.asm.Constant;
import org.xvm.asm.Constants;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.Op;

import org.xvm.asm.constants.MethodConstant;

import org.xvm.runtime.CallChain;
import org.xvm.runtime.ClassTemplate;
import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.TypeSet;
import org.xvm.runtime.Utils;

import org.xvm.runtime.template.Service.ServiceHandle;

import org.xvm.runtime.template.annotations.xFutureRef;


/**
 * TODO:
 *
 * @author gg 2017.02.27
 */
public class Function
        extends ClassTemplate
    {
    public static Function INSTANCE;

    public Function(TypeSet types, ClassStructure structure, boolean fInstance)
        {
        super(types, structure);

        if (fInstance)
            {
            INSTANCE = this;
            }
        }

    @Override
    public void initDeclared()
        {
        }

    @Override
    public ObjectHandle createConstHandle(Frame frame, Constant constant)
        {
        if (constant instanceof MethodConstant)
            {
            MethodConstant constFunction = (MethodConstant) constant;
            MethodStructure function = (MethodStructure) constFunction.getComponent();

            // TODO: assert if a method
            return new FunctionHandle(f_clazzCanonical, function);
            }
        return null;
        }

    public static class FunctionHandle
            extends ObjectHandle
        {
        // a function to call
        protected final MethodStructure f_function;

        // a method call chain (not null only if function is null)
        protected final CallChain f_chain;
        protected final int f_nDepth;

        protected FunctionHandle(TypeComposition clazz, MethodStructure function)
            {
            super(clazz);

            f_function = function;
            f_chain = null;
            f_nDepth = 0;
            }

        protected FunctionHandle(TypeComposition clazz, CallChain chain, int nDepth)
            {
            super(clazz);

            f_function = null;
            f_chain = chain;
            f_nDepth = nDepth;
            }

        public int getVarCount()
            {
            return (f_function == null ? f_chain.getMethod(f_nDepth) : f_function).getMaxVars();
            }

        // ----- FunctionHandle interface -----

        // call with one return value to be placed into the specified slot
        // return either R_CALL or R_NEXT
        public int call1(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahArg, int iReturn)
            {
            ObjectHandle[] ahVar = prepareVars(ahArg);

            addBoundArguments(ahVar);

            return call1Impl(frame, hTarget, ahVar, iReturn);
            }

        // calls with multiple return values
        public int callN(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahArg, int[] aiReturn)
            {
            ObjectHandle[] ahVar = prepareVars(ahArg);

            addBoundArguments(ahVar);

            return callNImpl(frame, hTarget, ahVar, aiReturn);
            }

        // ----- internal implementation -----

        protected ObjectHandle[] prepareVars(ObjectHandle[] ahArg)
            {
            int cArgs = ahArg.length;
            int cVars = getVarCount();

            assert cArgs <= cVars;

            ObjectHandle[] ahVar;
            if (cVars > cArgs)
                {
                ahVar = new ObjectHandle[cVars];

                if (cArgs > 0)
                    {
                    if (cArgs == 1)
                        {
                        ahVar[0] = ahArg[0];
                        }
                    else
                        {
                        System.arraycopy(ahArg, 0, ahVar, 0, cArgs);
                        }
                    }
                }
            else
                {
                ahVar = ahArg;
                }

            return ahVar;
            }

        // invoke with zero or one return to be placed into the specified register;
        protected int call1Impl(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahVar, int iReturn)
            {
            return f_function == null ?
                    frame.invoke1(f_chain, f_nDepth, hTarget, ahVar, iReturn) :
                    frame.call1(f_function, hTarget, ahVar, iReturn);
            }

        // invoke with multiple return values;
        protected int callNImpl(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahVar, int[] aiReturn)
            {
            return f_function == null ?
                    frame.invokeN(f_chain, f_nDepth, hTarget, ahVar, aiReturn) :
                    frame.callN(f_function, hTarget, ahVar, aiReturn);
            }

        // bind a specified argument
        public FunctionHandle bind(int iArg, ObjectHandle hArg)
            {
            return new SingleBoundHandle(f_clazz, this, iArg, hArg);
            }

        // bind the target
        public FunctionHandle bindTarget(ObjectHandle hArg)
            {
            return new SingleBoundHandle(f_clazz, this, -1, hArg);
            }

        public FullyBoundHandle bindAll(ObjectHandle hTarget, ObjectHandle[] ahArg)
            {
            return new FullyBoundHandle(f_clazz, this, hTarget, ahArg);
            }

        protected void addBoundArguments(ObjectHandle[] ahVar)
            {
            }

        @Override
        public String toString()
            {
            return super.toString() +
                    (f_function == null ? f_chain.getMethod(f_nDepth) : f_function);
            }
        }

    public static class DelegatingHandle
            extends FunctionHandle
        {
        protected FunctionHandle m_hDelegate;

        protected DelegatingHandle(TypeComposition clazz, FunctionHandle hDelegate)
            {
            super(clazz, hDelegate == null ? null : hDelegate.f_function);

            m_hDelegate = hDelegate;
            }

        @Override
        protected int call1Impl(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahVar, int iReturn)
            {
            return m_hDelegate.call1Impl(frame, hTarget, ahVar, iReturn);
            }

        @Override
        protected int callNImpl(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahVar, int[] aiReturn)
            {
            return m_hDelegate.callNImpl(frame, hTarget, ahVar, aiReturn);
            }

        @Override
        protected void addBoundArguments(ObjectHandle[] ahVar)
            {
            m_hDelegate.addBoundArguments(ahVar);
            }

        @Override
        public int getVarCount()
            {
            return m_hDelegate.getVarCount();
            }

        @Override
        public String toString()
            {
            return getClass().getSimpleName() + " -> " + m_hDelegate;
            }
        }

    public static class NativeMethodHandle
            extends FunctionHandle
        {
        final protected Service.NativeOperation f_op;

        public NativeMethodHandle(Service.NativeOperation op)
            {
            super(INSTANCE.f_clazzCanonical, null);

            f_op = op;
            }

        @Override
        public int call1(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahArg, int iReturn)
            {
            return f_op.invoke(frame, ahArg, iReturn);
            }
        }

    // one parameter bound function
    public static class SingleBoundHandle
            extends DelegatingHandle
        {
        protected int m_iArg; // the bound argument index; -1 stands for the target binding
        protected ObjectHandle m_hArg;

        protected SingleBoundHandle(TypeComposition clazz, FunctionHandle hDelegate,
                                    int iArg, ObjectHandle hArg)
            {
            super(clazz, hDelegate);

            m_iArg = iArg;
            m_hArg = hArg;
            }

        @Override
        protected int call1Impl(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahVar, int iReturn)
            {
            if (m_iArg == -1)
                {
                assert hTarget == null;
                hTarget = m_hArg;
                }
            return super.call1Impl(frame, hTarget, ahVar, iReturn);
            }

        @Override
        protected int callNImpl(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahVar, int[] aiReturn)
            {
            if (m_iArg == -1)
                {
                assert hTarget == null;
                hTarget = m_hArg;
                }
            return super.callNImpl(frame, hTarget, ahVar, aiReturn);
            }

        @Override
        protected void addBoundArguments(ObjectHandle[] ahVar)
            {
            super.addBoundArguments(ahVar);

            if (m_iArg >= 0)
                {
                int cMove = getVarCount() - (m_iArg + 1); // number of args to move to the right
                if (cMove > 0)
                    {
                    System.arraycopy(ahVar, m_iArg, ahVar, m_iArg + 1, cMove);
                    }
                ahVar[m_iArg] = m_hArg;
                }
            }
        }

    // all parameter bound function
    public static class FullyBoundHandle
            extends DelegatingHandle
        {
        protected final ObjectHandle f_hTarget;
        protected final ObjectHandle[] f_ahArg;
        protected FullyBoundHandle m_next;

        protected FullyBoundHandle(TypeComposition clazz, FunctionHandle hDelegate,
                                   ObjectHandle hTarget, ObjectHandle[] ahArg)
            {
            super(clazz, hDelegate);

            f_hTarget = hTarget;
            f_ahArg = ahArg;
            }

        @Override
        protected void addBoundArguments(ObjectHandle[] ahVar)
            {
            super.addBoundArguments(ahVar);

            // to avoid extra array creation, the argument array may contain unused null elements
            System.arraycopy(f_ahArg, 0, ahVar, 0, Math.min(ahVar.length, f_ahArg.length));
            }

        public FullyBoundHandle chain(FullyBoundHandle handle)
            {
            if (handle != NO_OP)
                {
                m_next = handle;
                }
            return this;
            }

        // @param access - if specified, apply to "this"
        // @return R_CALL or R_NEXT (see NO_OP override)
        public int callChain(Frame frame, Constants.Access access, Frame.Continuation continuation)
            {
            Frame frameNext = chainFrames(frame, access, continuation);

            return frame.call(frameNext);
            }

        // @param access - if specified, apply to "this"
        // @return the very first frame to be called
        protected Frame chainFrames(Frame frame, Constants.Access access, Frame.Continuation continuation)
            {
            ObjectHandle hTarget = f_hTarget;
            if (access != null)
                {
                hTarget = hTarget.f_clazz.ensureAccess(hTarget, access);
                }

            Frame frameSave = frame.m_frameNext;

            call1(frame, hTarget, Utils.OBJECTS_NONE, Frame.RET_UNUSED);

            // TODO: what if this function is async and frameThis is null
            Frame frameThis = frame.m_frameNext;

            frame.m_frameNext = frameSave;

            if (m_next == null)
                {
                frameThis.setContinuation(continuation);
                return frameThis;
                }

            Frame frameNext = m_next.chainFrames(frame, access, continuation);
            frameThis.setContinuation(frameCaller -> frameCaller.call(frameNext));
            return frameThis;
            }

        public static FullyBoundHandle NO_OP = new FullyBoundHandle(
                INSTANCE.f_clazzCanonical, null, null, null)
            {
            @Override
            public int callChain(Frame frame, Constants.Access access, Frame.Continuation continuation)
                {
                return continuation.proceed(frame);
                }

            @Override
            public FullyBoundHandle chain(FullyBoundHandle handle)
                {
                return handle;
                }
            };
        }

    public static class AsyncHandle
            extends FunctionHandle
        {
        protected AsyncHandle(TypeComposition clazz, CallChain chain, int nDepth)
            {
            super(clazz, chain, nDepth);
            }

        // ----- FunctionHandle interface -----

        @Override
        protected int call1Impl(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahVar, int iReturn)
            {
            ServiceHandle hService = (ServiceHandle) hTarget;

            // native method on the service means "execute on the caller's thread"
            if (f_chain.isNative() || frame.f_context == hService.m_context)
                {
                return super.call1Impl(frame, hTarget, ahVar, iReturn);
                }

            // TODO: validate that all the arguments are immutable or ImmutableAble;
            //       replace functions with proxies
            int cReturns = iReturn == Frame.RET_UNUSED ? 0 : 1;

            CompletableFuture<ObjectHandle> cfResult = hService.m_context.sendInvoke1Request(
                    frame, this, ahVar, cReturns);

            return cReturns == 0 ? Op.R_NEXT :
                frame.assignValue(iReturn, xFutureRef.makeHandle(cfResult));
            }

        @Override
        protected int callNImpl(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahVar, int[] aiReturn)
            {
            ServiceHandle hService = (ServiceHandle) hTarget;

            // native method on the service means "execute on the caller's thread"
            if (f_chain.isNative() || frame.f_context == hService.m_context)
                {
                return super.callNImpl(frame, hTarget, ahVar, aiReturn);
                }

            // TODO: validate that all the arguments are immutable or ImmutableAble

            int cReturns = aiReturn.length;

            CompletableFuture<ObjectHandle[]> cfResult = hService.m_context.sendInvokeNRequest(
                    frame, this, ahVar, cReturns);

            boolean fBlock = false;
            if (cReturns > 0)
                {
                for (int i = 0; i < cReturns; i++)
                    {
                    final int iRet = i;

                    CompletableFuture<ObjectHandle> cfReturn =
                            cfResult.thenApply(ahResult -> ahResult[iRet]);

                    int nR = frame.assignValue(aiReturn[i], xFutureRef.makeHandle(cfReturn));
                    if (nR == Op.R_EXCEPTION)
                        {
                        return Op.R_EXCEPTION;
                        }

                    // if any of the assignments block, we need to block it all
                    fBlock |= nR == Op.R_BLOCK;
                    }
                }

            return fBlock ? Op.R_BLOCK : Op.R_NEXT;
            }
        }

    public static AsyncHandle makeAsyncHandle(CallChain chain, int nDepth)
        {
        return new AsyncHandle(INSTANCE.f_clazzCanonical, chain, nDepth);
        }

    public static FunctionHandle makeHandle(CallChain chain, int nDepth)
        {
        return new FunctionHandle(INSTANCE.f_clazzCanonical, chain, nDepth);
        }

    public static FunctionHandle makeHandle(MethodStructure function)
        {
        return new FunctionHandle(INSTANCE.f_clazzCanonical, function);
        }
    }