package org.xvm.proto.template;

import org.xvm.asm.Constant;
import org.xvm.asm.constants.ClassConstant;
import org.xvm.asm.constants.MethodConstant;

import org.xvm.proto.template.xService.ServiceHandle;

import org.xvm.proto.*;

/**
 * TODO:
 *
 * @author gg 2017.02.27
 */
public class xFunction
        extends TypeCompositionTemplate
    {
    public static xFunction INSTANCE;

    public xFunction(TypeSet types)
        {
        super(types, "x:Function", "x:Object", Shape.Interface);

        addImplement("x:Const");

        INSTANCE = this;
        }

    @Override
    public void initDeclared()
        {
        //    Tuple invoke(Tuple args)
        //
        //    Type[] ReturnType;
        //
        //    Type[] ParamType;

        ensurePropertyTemplate("ReturnType", "x:collections.Array<x:Type>");
        ensurePropertyTemplate("ParamType", "x:collections.Array<x:Type>");

        ensureMethodTemplate("invoke", new String[]{"x:Tuple"}, new String[]{"x:Tuple"});
        }

    @Override
    public ObjectHandle createHandle(TypeComposition clazz)
        {
        return new DelegatingHandle(clazz, null);
        }

    @Override
    public ObjectHandle createConstHandle(Constant constant)
        {
        if (constant instanceof MethodConstant)
            {
            MethodConstant constFunction = (MethodConstant) constant; // TODO: replace with function when implemented
            ClassConstant constClass = (ClassConstant) constFunction.getNamespace();

            String sTargetClz = ConstantPoolAdapter.getClassName(constClass);
            TypeCompositionTemplate target = f_types.getTemplate(sTargetClz);
            FunctionTemplate function = target.getFunctionTemplate(constFunction);

            if (function != null)
                {
                return new FunctionHandle(f_clazzCanonical, function);
                }
            }
        return null;
        }

    public static class FunctionHandle
            extends ObjectHandle
        {
        protected InvocationTemplate m_invoke;

        protected FunctionHandle(TypeComposition clazz, InvocationTemplate function)
            {
            super(clazz);

            m_invoke = function;
            }

        public int getReturnSize()
            {
            return m_invoke.m_cReturns;
            }

        // ----- FunctionHandle interface -----

        // call with one return value to be placed into the specified slot
        // this method is simply a helper - it shouldn't be overridden
        // it simply collects ths specified arguments off the frame's vars
        final public ExceptionHandle call1(Frame frame, int[] anArg, int iReturn)
            {
            try
                {
                return call1(frame, frame.getArguments(anArg, m_invoke.m_cVars, 0), iReturn);
                }
            catch (ExceptionHandle.WrapperException e)
                {
                return e.getExceptionHandle();
                }
            }

        // calls with multiple return values // TODO: replace with the int[]
        // this method is simply a helper - it shouldn't be overridden
        // it simply collects ths specified arguments off the frame's vars
        final public ExceptionHandle callN(Frame frame, int[] anArg, int[] aiReturn)
            {
            try
                {
                return callN(frame, frame.getArguments(anArg, m_invoke.m_cVars, 0), aiReturn);
                }
            catch (ExceptionHandle.WrapperException e)
                {
                return e.getExceptionHandle();
                }
            }

        // call with one return value to be placed into the specified slot
        // this method is overridden by the "bound" handle to inject the arguments
        public ExceptionHandle call1(Frame frame, ObjectHandle[] ahArg, int iReturn)
            {
            ObjectHandle[] ahVar = prepareVars(ahArg);

            addBoundArguments(ahVar);

            return call1Impl(frame, ahVar, iReturn);
            }

        // calls with multiple return values // TODO: replace with the int[]
        // this method is overridden by the "bound" handle
        public ExceptionHandle callN(Frame frame, ObjectHandle[] ahArg, int[] aiReturn)
            {
            ObjectHandle[] ahVar = prepareVars(ahArg);

            addBoundArguments(ahVar);

            return callNImpl(frame, ahVar, aiReturn);
            }

        // ----- internal implementation -----

        protected ObjectHandle[] prepareVars(ObjectHandle[] ahArg)
            {
            int cArgs = ahArg.length;
            int cVars = m_invoke.m_cVars;

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
        protected ExceptionHandle call1Impl(Frame frame, ObjectHandle[] ahVar, int iReturn)
            {
            ObjectHandle hTarget = m_invoke instanceof MethodTemplate ? ahVar[0] : null;

            return frame.f_context.createFrame1(frame, m_invoke, hTarget, ahVar, iReturn).execute();
            }

        // invoke with multiple return values;
        protected ExceptionHandle callNImpl(Frame frame, ObjectHandle[] ahVar, int[] aiReturn)
            {
            ObjectHandle hTarget = m_invoke instanceof MethodTemplate ? ahVar[0] : null;

            return frame.f_context.createFrameN(frame, m_invoke, hTarget, ahVar, aiReturn).execute();
            }

        public FunctionHandle bind(int iArg, ObjectHandle hArg)
            {
            return new BoundHandle(f_clazz, this, iArg, hArg);
            }

        protected void addBoundArguments(ObjectHandle[] ahVar)
            {
            }

        @Override
        public String toString()
            {
            return super.toString() + m_invoke;
            }
        }

    public static class DelegatingHandle
            extends FunctionHandle
        {
        protected FunctionHandle m_hDelegate;

        protected DelegatingHandle(TypeComposition clazz, FunctionHandle hDelegate)
            {
            super(clazz, hDelegate == null ? null : hDelegate.m_invoke);

            m_hDelegate = hDelegate;
            }

        @Override
        protected ExceptionHandle call1Impl(Frame frame, ObjectHandle[] ahVar, int iReturn)
            {
            return m_hDelegate.call1Impl(frame, ahVar, iReturn);
            }

        @Override
        protected ExceptionHandle callNImpl(Frame frame, ObjectHandle[] ahVar, int[] aiReturn)
            {
            return m_hDelegate.callNImpl(frame, ahVar, aiReturn);
            }

        @Override
        protected void addBoundArguments(ObjectHandle[] ahVar)
            {
            m_hDelegate.addBoundArguments(ahVar);
            }

        @Override
        public String toString()
            {
            return getClass().getSimpleName() + " -> " + m_hDelegate;
            }
        }


    // partially bound function
    public static class BoundHandle
            extends DelegatingHandle
        {
        protected int m_iArg; // the bound argument index
        protected ObjectHandle m_hArg;

        protected BoundHandle(TypeComposition clazz, FunctionHandle hDelegate,
                              int iArg, ObjectHandle hArg)
            {
            super(clazz, hDelegate);

            m_iArg = iArg;
            m_hArg = hArg;
            }

        @Override
        protected void addBoundArguments(ObjectHandle[] ahVar)
            {
            super.addBoundArguments(ahVar);

            int cMove = m_invoke.m_cArgs - (m_iArg + 1); // number of args to move to the right
            if (cMove > 0)
                {
                System.arraycopy(ahVar, m_iArg, ahVar, m_iArg + 1, cMove);
                }
            ahVar[m_iArg] = m_hArg;
            }
        }

    public static class AsyncHandle
            extends FunctionHandle
        {
        protected AsyncHandle(TypeComposition clazz, InvocationTemplate function)
            {
            super(clazz, function);
            }

        // ----- FunctionHandle interface -----

        @Override
        protected ExceptionHandle call1Impl(Frame frame, ObjectHandle[] ahVar, int iReturn)
            {
            ServiceHandle hService = (ServiceHandle) ahVar[0];

            // native method on the service means "execute on the caller's thread"
            if (m_invoke.isNative() || frame.f_context == hService.m_context)
                {
                return super.call1Impl(frame, ahVar, iReturn);
                }

            xService service = (xService) m_invoke.getClazzTemplate();
            return service.asyncInvoke1(frame, hService, this, ahVar, iReturn);
            }

        @Override
        protected ExceptionHandle callNImpl(Frame frame, ObjectHandle[] ahVar, int[] aiReturn)
            {
            ServiceHandle hService = (ServiceHandle) ahVar[0];

            // native method on the service means "execute on the caller's thread"
            if (m_invoke.isNative() || frame.f_context == hService.m_context)
                {
                return super.callNImpl(frame, ahVar, aiReturn);
                }

            xService service = (xService) m_invoke.getClazzTemplate();
            return service.asyncInvokeN(frame, hService, this, ahVar, aiReturn);
            }
        }

    public static AsyncHandle makeAsyncHandle(InvocationTemplate function)
        {
        assert function.getClazzTemplate().isService();

        return new AsyncHandle(INSTANCE.f_clazzCanonical, function);
        }

    public static FunctionHandle makeHandle(InvocationTemplate function)
        {
        return new FunctionHandle(INSTANCE.f_clazzCanonical, function);
        }
    }
