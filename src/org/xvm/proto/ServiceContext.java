package org.xvm.proto;

import org.xvm.proto.TypeCompositionTemplate.InvocationTemplate;
import org.xvm.proto.ObjectHandle.ExceptionHandle;

import org.xvm.proto.template.xFunction.FunctionHandle;
import org.xvm.proto.template.xService.ServiceHandle;

import java.util.concurrent.CompletableFuture;

/**
 * The service context.
 *
 * @author gg 2017.02.15
 */
public class ServiceContext
    {
    public final Container f_container;
    public final TypeSet f_types;
    public final ObjectHeap f_heapGlobal;
    public final ConstantPoolAdapter f_constantPool;

    private ServiceHandle m_hService;
    private ServiceDaemon m_daemon;
    public Frame m_frameCurrent;

    ServiceContext(Container container)
        {
        f_container = container;
        f_heapGlobal = container.f_heapGlobal;
        f_types = container.f_types;
        f_constantPool = container.f_constantPoolAdapter;
        }

    @Override
    public String toString()
        {
        return "Service(" + m_daemon.getThread().getName() + ')';
        }

    public Frame createFrame(Frame framePrev, InvocationTemplate template,
                             ObjectHandle hTarget, ObjectHandle[] ahVar)
        {
        return new Frame(this, framePrev, template, hTarget, ahVar);
        }

    public void start(ServiceHandle hService, String sName)
        {
        m_hService = hService;

        // TODO: we need to be able to share native threads across services
        ServiceDaemon daemon = m_daemon = new ServiceDaemon(sName, this);
        daemon.start();
        }

    // wait for any messages coming in
    public void yield()
        {
        m_daemon.dispatch(100l);
        }

    public CompletableFuture<ObjectHandle[]> sendRequest(ServiceContext context, FunctionHandle hFunction,
                                                 ObjectHandle[] ahArg, int cReturns)
        {
        CompletableFuture<ObjectHandle[]> future = new CompletableFuture<>();

        context.m_daemon.add(new Request(this, hFunction, ahArg, cReturns, future));

        return future;
        }

    public void sendResponse(ServiceContext context, ExceptionHandle hException, ObjectHandle[] ahReturn,
                             CompletableFuture<ObjectHandle[]> future)
        {
        context.m_daemon.add(new Response(hException, ahReturn, future));
        }

    public interface Message
        {
        void process(ServiceContext context);
        }

    /**
     * Represents a call from one service onto another.
     */
    public static class Request
            implements Message
        {
        private final ServiceContext f_contextCaller;
        private final FunctionHandle f_hFunction;
        private final ObjectHandle[] f_ahArg;
        private final int f_cReturns;
        private final CompletableFuture<ObjectHandle[]> f_future;

        public Request(ServiceContext contextCaller, FunctionHandle hFunction,
                       ObjectHandle[] ahArg, int cReturns,
                       CompletableFuture<ObjectHandle[]> future)
            {
            f_contextCaller = contextCaller;
            f_hFunction = hFunction;
            f_ahArg = ahArg;
            f_cReturns = cReturns;
            f_future = future;
            }

        @Override
        public void process(ServiceContext context)
            {
            ObjectHandle[] ahReturn = f_cReturns == 0 ? Utils.OBJECTS_NONE : new ObjectHandle[f_cReturns];

            ExceptionHandle hException = f_hFunction.execute(context, null,
                    f_ahArg[0], f_hFunction.prepareVars(f_ahArg), ahReturn);

            context.sendResponse(f_contextCaller, hException, ahReturn, f_future);
            }
        }

    /**
     * Represents a call return.
     */
    public static class Response
            implements Message
        {
        private final ExceptionHandle f_hException;
        private final ObjectHandle[] f_ahReturn;
        private final CompletableFuture<ObjectHandle[]> f_future;

        public Response(ExceptionHandle hException, ObjectHandle[] ahReturn, CompletableFuture<ObjectHandle[]> future)
            {
            f_hException = hException;
            f_ahReturn = ahReturn;
            f_future = future;
            }

        @Override
        public void process(ServiceContext context)
            {
            if (f_hException == null)
                {
                f_future.complete(f_ahReturn);
                }
            else
                {
                f_future.completeExceptionally(f_hException.m_exception);
                }
            }
        }
    }
