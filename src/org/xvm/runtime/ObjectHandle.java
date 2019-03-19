package org.xvm.runtime;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xvm.asm.Constants;
import org.xvm.asm.Op;

import org.xvm.asm.constants.IdentityConstant.NestedIdentity;
import org.xvm.asm.constants.PropertyConstant;
import org.xvm.asm.constants.TypeConstant;

import org.xvm.runtime.template.xRef.RefHandle;

import org.xvm.util.ListMap;


/**
 * Runtime operates on Object handles holding the struct references or the values themselves
 * for the following types:
 *  Bit, Boolean, Char, Int, UInt, Nullable.Null, and optionally for some Tuples
 *
 * Note, that the equals() and hashCode() methods should be only for immutable handles.
 */
public abstract class ObjectHandle
        implements Cloneable
    {
    protected TypeComposition m_clazz;
    protected boolean m_fMutable = false;

    public static final ObjectHandle DEFAULT = new ObjectHandle(null) {};

    protected ObjectHandle(TypeComposition clazz)
        {
        m_clazz = clazz;
        }

    /**
     * Clone this handle using the specified TypeComposition.
     *
     * @param clazz  the TypeComposition to mask/reveal this handle as
     *
     * @return the new handle
     */
    public ObjectHandle cloneAs(TypeComposition clazz)
        {
        try
            {
            ObjectHandle handle = (ObjectHandle) super.clone();
            handle.m_clazz = clazz;
            return handle;
            }
        catch (CloneNotSupportedException e)
            {
            throw new IllegalStateException();
            }
        }

    /**
     * Reveal this handle using the "inception" type.
     *
     * @return the "fully accessible" handle
     */
    public ObjectHandle revealOrigin()
        {
        return m_clazz.ensureOrigin(this);
        }

    public boolean isMutable()
        {
        return m_fMutable;
        }

    public void makeImmutable()
        {
        m_fMutable = false;
        }

    /**
     * @return null iff all the fields are assigned; a list of unassigned names otherwise
     */
    public List<String> validateFields()
        {
        return null;
        }

    public boolean isSelfContained()
        {
        return false;
        }

    /**
     * @return the TypeComposition for this handle
     */
    public TypeComposition getComposition()
        {
        return m_clazz;
        }

    /**
     * @return the underlying template for this handle
     */
    public ClassTemplate getTemplate()
        {
        return m_clazz.getTemplate();
        }

    /**
     * @return the OpSupport for the inception type of this handle
     */
    public OpSupport getOpSupport()
        {
        return m_clazz.getSupport();
        }

    /**
     * @return the revealed type of this handle
     */
    public TypeConstant getType()
        {
        return m_clazz.getType();
        }

    public ObjectHandle ensureAccess(Constants.Access access)
        {
        return m_clazz.ensureAccess(this, access);
        }

    /**
     * Check whether or not the property referred by the specified constant has a custom code or
     * Ref-annotation.
     *
     * @param idProp  the property to check
     *
     * @return true iff the specified property has custom code or is Ref-annotated
     */
    public boolean isInflated(PropertyConstant idProp)
        {
        return m_clazz.isInflated(idProp.getNestedIdentity());
        }

    /**
     * Check whether or not the property referred by the specified constant has an injected value.
     *
     * @param idProp  the property to check
     *
     * @return true iff the specified property has an injected value
     */
    public boolean isInjected(PropertyConstant idProp)
        {
        return m_clazz.isInjected(idProp);
        }

    /**
     * Check whether or not the property referred by the specified constant has to be treated as
     * an atomic value.
     *
     * @param idProp  the property to check
     *
     * @return true iff the specified property has an atomic value
     */
    public boolean isAtomic(PropertyConstant idProp)
        {
        return m_clazz.isAtomic(idProp);
        }

    /**
     * @return true iff the handle represents a struct
     */
    public boolean isStruct()
        {
        return m_clazz.isStruct();
        }

    /**
     * @return true iff the handle itself could be used for the equality check
     */
    public boolean isNativeEqual()
        {
        return true;
        }

    /**
     * @return the result of comparison (only for isNativeEqual() handles)
     */
    public int compareTo(ObjectHandle that)
        {
        throw new UnsupportedOperationException(getClass() + " cannot compare");
        }

    @Override
    public int hashCode()
        {
        if (isNativeEqual())
            {
            throw new UnsupportedOperationException(getClass() + " must implement \"hashCode()\"");
            }

        return System.identityHashCode(this);
        }

    @Override
    public boolean equals(Object obj)
        {
        if (isNativeEqual())
            {
            throw new UnsupportedOperationException(getClass() + " must implement \"equals()\"");
            }

        // we don't use this for natural equality check
        return this == obj;
        }

    @Override
    public String toString()
        {
        return "(" + m_clazz + ") ";
        }

    public static class GenericHandle
            extends ObjectHandle
        {
        public GenericHandle(TypeComposition clazz)
            {
            super(clazz);

            m_fMutable = true;

            m_mapFields = clazz.createFields();
            }

        public boolean containsField(PropertyConstant idProp)
            {
            return m_mapFields != null && m_mapFields.containsKey(idProp.getNestedIdentity());
            }

        public ObjectHandle getField(PropertyConstant idProp)
            {
            return m_mapFields == null ? null : m_mapFields.get(idProp.getNestedIdentity());
            }

        public ObjectHandle getField(String sProp)
            {
            return m_mapFields == null ? null : m_mapFields.get(sProp);
            }

        public void setField(PropertyConstant idProp, ObjectHandle hValue)
            {
            if (m_mapFields == null)
                {
                m_mapFields = new ListMap<>();
                }
            m_mapFields.put(idProp.getNestedIdentity(), hValue);
            }

        public void setField(String sProp, ObjectHandle hValue)
            {
            if (m_mapFields == null)
                {
                m_mapFields = new ListMap<>();
                }
            m_mapFields.put(sProp, hValue);
            }

        @Override
        public ObjectHandle cloneAs(TypeComposition clazz)
            {
            boolean fCloneFields = isStruct();

            GenericHandle hClone = (GenericHandle) super.cloneAs(clazz);

            if (fCloneFields && m_mapFields != null)
                {
                for (Map.Entry<Object, ObjectHandle> entry : m_mapFields.entrySet())
                    {
                    if (clazz.isInflated(entry.getKey()))
                        {
                        RefHandle    hValue = (RefHandle) entry.getValue();
                        ObjectHandle hOuter = hValue.getField(OUTER);
                        if (hOuter != null)
                            {
                            assert hOuter == this;
                            hValue.setField(OUTER, hClone);
                            }
                        }
                    }
                }
            return hClone;
            }

        @Override
        public void makeImmutable()
            {
            if (isMutable())
                {
                if (m_mapFields != null)
                    {
                    for (ObjectHandle hValue : m_mapFields.values())
                        {
                        hValue.makeImmutable();
                        }
                    }
                super.makeImmutable();
                }
            }

        @Override
        public List<String> validateFields()
            {
            List<String> listUnassigned = null;
            if (m_mapFields != null)
                {
                for (Map.Entry<Object, ObjectHandle> entry : m_mapFields.entrySet())
                    {
                    ObjectHandle hValue = entry.getValue();
                    if (hValue == null)
                        {
                        Object idProp = entry.getKey();
                        if (idProp instanceof NestedIdentity)
                            {
                            // must be a private property, which is always implicitly "@Unassigned"
                            continue;
                            }
                        if (listUnassigned == null)
                            {
                            listUnassigned = new ArrayList<>();
                            }
                        listUnassigned.add((String) idProp);
                        }
                    // no need to recurse to a field; it would throw during its own construction
                    }
                }
            return listUnassigned;
            }

        public static boolean compareIdentity(GenericHandle h1, GenericHandle h2)
            {
            if (h1 == h2)
                {
                return true;
                }

            if (h1.isMutable() || h2.isMutable())
                {
                return false;
                }

            Map<Object, ObjectHandle> map1 = h1.m_mapFields;
            Map<Object, ObjectHandle> map2 = h2.m_mapFields;

            if (map1 == map2)
                {
                return true;
                }

            if (map1.size() != map2.size())
                {
                return false;
                }

            for (Object idProp : map1.keySet())
                {
                ObjectHandle hV1 = map1.get(idProp);
                ObjectHandle hV2 = map2.get(idProp);

                // TODO: need to prevent a potential infinite loop
                ClassTemplate template = hV1.getTemplate();
                if (template != hV2.getTemplate() || !template.compareIdentity(hV1, hV2))
                    {
                    return false;
                    }
                }

            return true;
            }

        @Override
        public boolean isNativeEqual()
            {
            return false;
            }

        // keyed by the property name or a NestedIdentity
        private Map<Object, ObjectHandle> m_mapFields;

        /**
         * Synthetic property holding a reference to a parent instance.
         */
        public final static String OUTER = "$outer";
        }

    public static class ExceptionHandle
            extends GenericHandle
        {
        protected WrapperException m_exception;

        public ExceptionHandle(TypeComposition clazz, boolean fInitialize, Throwable eCause)
            {
            super(clazz);

            if (fInitialize)
                {
                m_exception = eCause == null ?
                        new WrapperException() : new WrapperException(eCause);;
                }
            }

        public WrapperException getException()
            {
            return new WrapperException();
            }

        public class WrapperException
                extends Exception
            {
            public WrapperException()
                {
                super();
                }

            public WrapperException(Throwable cause)
                {
                super(cause);
                }

            public ExceptionHandle getExceptionHandle()
                {
                return ExceptionHandle.this;
                }

            @Override
            public String toString()
                {
                return getExceptionHandle().toString();
                }
            }
        }

    // anything that fits in a long
    public static class JavaLong
            extends ObjectHandle
        {
        protected long m_lValue;

        public JavaLong(TypeComposition clazz, long lValue)
            {
            super(clazz);
            m_lValue = lValue;
            }

        @Override
        public boolean isSelfContained()
            {
            return true;
            }

        public long getValue()
            {
            return m_lValue;
            }

        @Override
        public int hashCode()
            {
            return Long.hashCode(m_lValue);
            }

        @Override
        public int compareTo(ObjectHandle that)
            {
            return Long.compare(m_lValue, ((JavaLong) that).m_lValue);
            }

        @Override
        public boolean equals(Object obj)
            {
            return obj instanceof JavaLong
                && m_lValue == ((JavaLong) obj).m_lValue;
            }

        @Override
        public String toString()
            {
            return super.toString() + m_lValue;
            }
        }

    // abstract array handle
    public abstract static class ArrayHandle
            extends ObjectHandle
        {
        public MutabilityConstraint m_mutability;
        public int m_cSize;

        protected ArrayHandle(TypeComposition clzArray)
            {
            super(clzArray);

            m_fMutable = true;
            }

        @Override
        public void makeImmutable()
            {
            super.makeImmutable();

            m_mutability = MutabilityConstraint.Constant;
            }

        @Override
        public String toString()
            {
            return super.toString() + m_mutability + ", size=" + m_cSize;
            }
        }

    // a handle representing a deferred action, such as a property access or a method call;
    // this handle cannot be allocated naturally and must be processed in a special way
    public static class DeferredCallHandle
            extends ObjectHandle
        {
        protected Frame f_frameNext;

        protected DeferredCallHandle(Frame frameNext)
            {
            super(null);

            f_frameNext = frameNext;
            }

        // continue with the processing
        public int proceed(Frame frameCaller, Frame.Continuation continuation)
            {
            Frame frameNext = f_frameNext;
            if (frameNext.m_hException == null)
                {
                frameNext.setContinuation(continuation);
                return frameCaller.call(frameNext);
                }

            frameCaller.m_hException = frameNext.m_hException;
            return Op.R_EXCEPTION;
            }

        public ExceptionHandle.WrapperException getDeferredException()
            {
            return f_frameNext.m_hException.getException();
            }

        @Override
        public String toString()
            {
            return f_frameNext.m_hException == null
                ? "Deferred call: " + f_frameNext
                : "Deferred exception: " + f_frameNext.m_hException;
            }
        }

    public enum MutabilityConstraint
        {Mutable, FixedSize, Persistent, Constant}

    // ----- DEFERRED ----

    public static long createHandle(int nTypeId, int nIdentityId, boolean fMutable)
        {
        assert (nTypeId & ~MASK_TYPE) == 0;
        return (((long) nTypeId) & MASK_TYPE) | ((long) nIdentityId << 32 & MASK_IDENTITY) | HANDLE;
        }

    /**
     * @return true iff the specified long value could be used "in place" of the handle
     */
    public static boolean isNaked(long lValue)
        {
        return (lValue & HANDLE) == 0;
        }

    /**
     * @return true iff the specified double value could be used "in place" of the handle
     */
    public static boolean isNaked(double dValue)
        {
        return (Double.doubleToRawLongBits(dValue) & HANDLE) == 0;
        }

    public static int getTypeId(long lHandle)
        {
        assert (lHandle & HANDLE) != 0;
        return (int) (lHandle & MASK_TYPE);
        }

    public static int getIdentityId(long lHandle)
        {
        assert (lHandle & HANDLE) != 0;
        return (int) (lHandle & MASK_IDENTITY >>> 32);
        }

    public static boolean isImmutable(long lHandle)
        {
        assert (lHandle & HANDLE) != 0;
        return (lHandle & STYLE_IMMUTABLE) != 0;
        }

    public static boolean isService(long lHandle)
        {
        assert (lHandle & HANDLE) != 0;
        return (lHandle & STYLE_SERVICE) != 0;
        }

    public static boolean isFunction(long lHandle)
        {
        assert (lHandle & HANDLE) != 0;
        return (lHandle & FUNCTION) != 0;
        }

    public static boolean isGlobal(long lHandle)
        {
        assert (lHandle & HANDLE) != 0;
        return (lHandle & GLOBAL) != 0;
        }

    // zero identity indicates a non-initialized handle
    public static boolean isAssigned(long lHandle)
        {
        return getIdentityId(lHandle) != 0;
        }

    // bits 0-26: type id
    private final static long MASK_TYPE       = 0x07FF_FFFF;

    // bit 26: always set unless the value is a naked Int or Double
    private final static long HANDLE          = 0x0800_0000;

    // bit 27: reserved
    private final static long BIT_27          = 0x1000_0000;

    // bits 28-29 style
    private final static long MASK_STYLE      = 0x3000_0000;
    private final static long STYLE_MUTABLE   = 0x0000_0000;
    private final static long STYLE_IMMUTABLE = 0x1000_0000;
    private final static long STYLE_SERVICE   = 0x2000_0000;
    private final static long STYLE_RESERVED  = 0x3000_0000;

    // bit 30: function
    private final static long FUNCTION        = 0x4000_0000L;

    // bit 31: global - if indicates that the object resides in the global heap; must be immutable
    private final static long GLOBAL = 0x8000_0000L;

    // bits 32-63: identity id
    private final static long MASK_IDENTITY   = 0xFFFF_FFFF_0000_0000L;
    }
