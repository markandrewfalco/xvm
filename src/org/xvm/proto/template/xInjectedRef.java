package org.xvm.proto.template;

import org.xvm.asm.ClassStructure;
import org.xvm.proto.ObjectHandle;
import org.xvm.proto.Type;
import org.xvm.proto.TypeComposition;
import org.xvm.proto.TypeSet;

import java.util.concurrent.ExecutionException;

/**
 * TODO:
 *
 * @author gg 2017.02.27
 */
public class xInjectedRef
        extends xRef
    {
    public static xInjectedRef INSTANCE;

    public xInjectedRef(TypeSet types, ClassStructure structure, boolean fInstance)
        {
        super(types, structure, false);

        if (fInstance)
            {
            INSTANCE = this;
            }
        }

    @Override
    public void initDeclared()
        {
        // TODO: how to inherit this from Ref?
        markNativeMethod("get", VOID, new String[]{"RefType"});
        markNativeMethod("set", new String[]{"RefType"}, VOID);
        }

    @Override
    protected ClassStructure getSuperStructure()
        {
        // REVIEW: InjectedRef is a mixin, but xInjectedRef is native; is this right?
        return xObject.INSTANCE.f_struct;
        }

    @Override
    public RefHandle createRefHandle(TypeComposition clazz, String sName)
        {
        if (sName == null)
            {
            throw new IllegalArgumentException("Name is not present");
            }
        return new InjectedHandle(clazz, sName);
        }

    public static class InjectedHandle
            extends RefHandle
        {
        protected InjectedHandle(TypeComposition clazz, String sName)
            {
            super(clazz, sName);
            }

        @Override
        protected ObjectHandle getInternal()
                throws ExceptionHandle.WrapperException
            {
            ObjectHandle hValue = m_hDelegate;
            if (hValue == null)
                {
                Type typeEl = f_clazz.getActualType("RefType");
                hValue = m_hDelegate =
                        f_clazz.f_template.f_types.f_container.getInjectable(m_sName, typeEl.f_clazz);
                if (hValue == null)
                    {
                    throw xException.makeHandle("Unknown injectable property " + m_sName).getException();
                    }
                }

            return hValue;
            }

        @Override
        protected ExceptionHandle setInternal(ObjectHandle handle)
            {
            return xException.makeHandle("InjectedRef cannot be re-assigned");
            }

        @Override
        public String toString()
            {
            try
                {
                return "(" + f_clazz + ") " + get();
                }
            catch (Throwable e)
                {
                if (e instanceof ExecutionException)
                    {
                    e = e.getCause();
                    }
                return e.toString();
                }
            }
        }
    }