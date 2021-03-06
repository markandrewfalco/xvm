package org.xvm.runtime.template;


import java.util.List;

import java.util.function.ToIntFunction;

import org.xvm.asm.ClassStructure;
import org.xvm.asm.Constants.Access;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.Op;

import org.xvm.asm.constants.AnnotatedTypeConstant;
import org.xvm.asm.constants.ClassConstant;
import org.xvm.asm.constants.NativeRebaseConstant;
import org.xvm.asm.constants.PropertyConstant;
import org.xvm.asm.constants.SignatureConstant;
import org.xvm.asm.constants.TypeConstant;

import org.xvm.runtime.CallChain;
import org.xvm.runtime.ClassComposition;
import org.xvm.runtime.ClassTemplate;
import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.GenericHandle;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.TemplateRegistry;
import org.xvm.runtime.Utils;
import org.xvm.runtime.VarSupport;

import org.xvm.runtime.template._native.reflect.xRTType.TypeHandle;

import org.xvm.runtime.template.annotations.xFutureVar.FutureHandle;

import org.xvm.runtime.template.numbers.xInt64;


/**
 * Native Ref implementation.
 */
public class xRef
        extends ClassTemplate
        implements VarSupport
    {
    public static xRef INSTANCE;
    public static ClassConstant INCEPTION_CLASS;

    public xRef(TemplateRegistry templates, ClassStructure structure, boolean fInstance)
        {
        super(templates, structure);

        if (fInstance)
            {
            INSTANCE = this;
            INCEPTION_CLASS = new NativeRebaseConstant(
                    (ClassConstant) structure.getIdentityConstant());
            }
        }

    @Override
    public void initDeclared()
        {
        s_sigGet = f_struct.findMethod("get", 0).getIdentityConstant().getSignature();

        markNativeMethod("equals", null, BOOLEAN);

        getCanonicalType().invalidateTypeInfo();
        }

    @Override
    protected ClassConstant getInceptionClassConstant()
        {
        return this == INSTANCE ? INCEPTION_CLASS : getClassConstant();
        }

    @Override
    public ClassTemplate getTemplate(TypeConstant type)
        {
        return this;
        }

    @Override
    public int invokeNativeGet(Frame frame, String sPropName,
                               ObjectHandle hTarget, int iReturn)
        {
        RefHandle hRef = (RefHandle) hTarget;

        switch (sPropName)
            {
            case "actualType":
                return actOnReferent(frame, hRef,
                    h -> frame.assignValue(iReturn, h.getType().getTypeHandle()));

            case "assigned":
                return frame.assignValue(iReturn, xBoolean.makeHandle(hRef.isAssigned()));

            case "isService":
                return actOnReferent(frame, hRef,
                    h -> frame.assignValue(iReturn,
                        xBoolean.makeHandle(h.getTemplate().isService())));

            case "isConst":
                return actOnReferent(frame, hRef,
                    h -> frame.assignValue(iReturn,
                        xBoolean.makeHandle(h.getComposition().isConst())));

            case "isImmutable":
                return actOnReferent(frame, hRef,
                    h -> frame.assignValue(iReturn,
                        xBoolean.makeHandle(!h.isMutable())));

            case "refName":
                {
                String sName = hRef.getName();
                return frame.assignValue(iReturn, sName == null ? xNullable.NULL
                                                                : xString.makeHandle(sName));
                }

            case "byteLength":
                return frame.assignValue(iReturn, xInt64.makeHandle(8)); // TODO

            case "selfContained":
                return frame.assignValue(iReturn, xBoolean.makeHandle(hRef.isSelfContained()));
            }

        return super.invokeNativeGet(frame, sPropName, hTarget, iReturn);
        }

    @Override
    public int invokeNative1(Frame frame, MethodStructure method, ObjectHandle hTarget,
                             ObjectHandle hArg, int iReturn)
        {
        RefHandle hRef = (RefHandle) hTarget;

        switch (method.getName())
            {
            case "instanceOf":
                return actOnReferent(frame, hRef,
                    h -> frame.assignValue(iReturn,
                        xBoolean.makeHandle(instanceOf(h, (TypeHandle) hArg))));

            case "maskAs":
                return actOnReferent(frame, hRef,
                    h -> maskAs(frame, h, (TypeHandle) hArg, iReturn));
            }

        return super.invokeNative1(frame, method, hTarget, hArg, iReturn);
        }

    @Override
    public int invokeNativeN(Frame frame, MethodStructure method, ObjectHandle hTarget,
                             ObjectHandle[] ahArg, int iReturn)
        {
        RefHandle hRef = (RefHandle) hTarget;

        switch (method.getName())
            {
            case "get":
                return getReferentImpl(frame, hRef, true, iReturn);

            case "equals":
                {
                RefHandle hRef1 = (RefHandle) ahArg[1];
                RefHandle hRef2 = (RefHandle) ahArg[2];
                return new CompareReferents(hRef1, hRef2, this, iReturn).doNext(frame);
                }
            }

        return super.invokeNativeN(frame, method, hTarget, ahArg, iReturn);
        }

    @Override
    public int invokeNativeNN(Frame frame, MethodStructure method,
                              ObjectHandle hTarget, ObjectHandle[] ahArg, int[] aiReturn)
        {
        RefHandle hRef = (RefHandle) hTarget;

        switch (method.getName())
            {
            case "peek":
                return hRef.isAssigned()
                    ? actOnReferent(frame, hRef,
                        h -> frame.assignValues(aiReturn, xBoolean.TRUE, h))
                    : frame.assignValue(aiReturn[0], xBoolean.FALSE);

            case "revealAs":
                return actOnReferent(frame, hRef,
                    h -> revealAs(frame, h, (TypeHandle) ahArg[0], aiReturn));
            }

        return super.invokeNativeNN(frame, method, hTarget, ahArg, aiReturn);
        }

    @Override
    public RefHandle createRefHandle(TypeComposition clazz, String sName)
        {
        return new RefHandle(clazz, sName);
        }

    @Override
    public int introduceRef(Frame frame, TypeComposition clazz, String sName, int iReturn)
        {
        if (this != INSTANCE && this != xVar.INSTANCE)
            {
            // native Ref/Var no need for further initialization
            frame.introduceResolvedVar(iReturn, clazz.getType(), sName,
                    Frame.VAR_DYNAMIC_REF, createRefHandle(clazz, sName));
            return Op.R_NEXT;
            }

        RefHandle hRef;
        int       iResult;
        boolean   fStack;

        TypeConstant typeRef = clazz.getType();
        if (typeRef instanceof AnnotatedTypeConstant)
            {
            AnnotatedTypeConstant typeAnno  = (AnnotatedTypeConstant) typeRef;
            TypeConstant          typeMixin = typeAnno.getAnnotationType();
            Mixin                 mixin     = (Mixin) f_templates.getTemplate(typeMixin);

            hRef    = createRefHandle(clazz.ensureAccess(Access.STRUCT), sName);
            iResult = mixin.proceedConstruction(frame, null, true, hRef, Utils.OBJECTS_NONE, Op.A_STACK);
            fStack  = true;
            }
        else
            {
            hRef    = createRefHandle(clazz, sName);
            iResult = hRef.initializeCustomFields(frame);
            fStack  = false;
            }

        switch (iResult)
            {
            case Op.R_NEXT:
                {
                frame.introduceResolvedVar(iReturn, typeRef, sName, Frame.VAR_DYNAMIC_REF, hRef);
                return Op.R_NEXT;
                }

            case Op.R_CALL:
                frame.m_frameNext.addContinuation(frameCaller ->
                        {
                        frameCaller.introduceResolvedVar(iReturn, typeRef, sName,
                            Frame.VAR_DYNAMIC_REF, fStack ? frameCaller.popStack() : hRef);
                        return Op.R_NEXT;
                        });
                return Op.R_CALL;

            case Op.R_EXCEPTION:
                return Op.R_EXCEPTION;

            default:
                throw new IllegalStateException();
            }
        }

    @Override
    public int callEquals(Frame frame, ClassComposition clazz,
                          ObjectHandle hValue1, ObjectHandle hValue2, int iReturn)
        {
        RefHandle hRef1 = (RefHandle) hValue1;
        RefHandle hRef2 = (RefHandle) hValue2;

        // From Ref.x:
        // Reference equality is used to determine if two references are referring to the same referent
        // _identity_. Specifically, two references are equal iff they reference the same runtime
        // object, or the two objects that they reference are both immutable and structurally identical.
        return new CompareReferents(hRef1, hRef2, this, iReturn).doNext(frame);
        }


    // ----- VarSupport implementation -------------------------------------------------------------

    @Override
    public int getReferent(Frame frame, RefHandle hTarget, int iReturn)
        {
        return getReferentImpl(frame, hTarget, false, iReturn);
        }

    /**
     * @return one of the {@link Op#R_NEXT}, {@link Op#R_CALL} or {@link Op#R_EXCEPTION}
     */
    protected int getReferentImpl(Frame frame, RefHandle hRef, boolean fNative, int iReturn)
        {
        ObjectHandle hValue;
        switch (hRef.m_iVar)
            {
            case RefHandle.REF_REFERENT:
                {
                if (fNative)
                    {
                    hValue = hRef.getReferent();
                    }
                else
                    {
                    return invokeGetReferent(frame, hRef, iReturn);
                    }
                break;
                }

            case RefHandle.REF_REF:
                {
                RefHandle hDelegate = (RefHandle) hRef.getReferentHolder();
                return hDelegate.getVarSupport().getReferent(frame, hDelegate, iReturn);
                }

            case RefHandle.REF_PROPERTY:
                {
                ObjectHandle hDelegate = hRef.getReferentHolder();
                return hDelegate.getTemplate().getPropertyValue(
                    frame, hDelegate, hRef.getPropertyId(), iReturn);
                }

            case RefHandle.REF_ARRAY:
                {
                IndexedRefHandle hIndexedRef = (IndexedRefHandle) hRef;
                ObjectHandle     hArray      = hRef.getReferentHolder();
                IndexSupport     template    = (IndexSupport) hArray.getTemplate();

                return template.extractArrayValue(frame, hArray, hIndexedRef.f_lIndex, iReturn);
                }

            default:
                {
                Frame frameRef = hRef.m_frame;
                int   nVar     = hRef.m_iVar;
                assert frameRef != null && nVar >= 0;

                hValue = frameRef.f_ahVar[nVar];
                break;
                }
            }

        return hValue == null
                ? frame.raiseException(xException.unassignedReference(frame))
                : frame.assignValue(iReturn, hValue);
        }

    /**
     * @return one of the {@link Op#R_NEXT}, {@link Op#R_CALL} or {@link Op#R_EXCEPTION}
     */
    protected int invokeGetReferent(Frame frame, RefHandle hRef, int iReturn)
        {
        CallChain chain = hRef.getComposition().getMethodCallChain(s_sigGet);
        return chain.isExplicit()
            ? chain.invoke(frame, hRef, iReturn)
            : getReferentImpl(frame, hRef, true, iReturn);
        }

    @Override
    public int invokeVarPreInc(Frame frame, RefHandle hTarget, int iReturn)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarPostInc(Frame frame, RefHandle hTarget, int iReturn)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarPreDec(Frame frame, RefHandle hTarget, int iReturn)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarPostDec(Frame frame, RefHandle hTarget, int iReturn)
        {
        return readOnly(frame);
        }

    @Override
    public int setReferent(Frame frame, RefHandle hTarget, ObjectHandle hValue)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarAdd(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarSub(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarMul(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarDiv(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarMod(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarShl(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarShr(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarShrAll(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarAnd(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarOr(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }

    @Override
    public int invokeVarXor(Frame frame, RefHandle hTarget, ObjectHandle hArg)
        {
        return readOnly(frame);
        }


    // ----- helper methods ------------------------------------------------------------------------

    protected int readOnly(Frame frame)
        {
        return frame.raiseException("Ref cannot be assigned");
        }

    /**
     * Get the referent and apply the specified action.
     *
     * @param frame   the current frame
     * @param hRef    the target RefHandle
     * @param action  the action
     *
     * @return one of the {@link Op#R_NEXT}, {@link Op#R_CALL}, {@link Op#R_EXCEPTION}
     */
    protected int actOnReferent(Frame frame, RefHandle hRef, ToIntFunction<ObjectHandle> action)
        {
        switch (getReferent(frame, hRef, Op.A_STACK))
            {
            case Op.R_NEXT:
                return action.applyAsInt(frame.popStack());

            case Op.R_CALL:
                frame.addContinuation(frameCaller ->
                    action.applyAsInt(frameCaller.popStack()));
                return Op.R_CALL;

            case Op.R_BLOCK:
                return frame.raiseException(xException.unassignedReference(frame));

            case Op.R_EXCEPTION:
                return Op.R_EXCEPTION;

            default:
                throw new IllegalStateException();
            }
        }

    /**
     * Mask the specified target as a wider type.
     *
     * @param frame    the current frame
     * @param hTarget  the target object
     * @param hType    the type handle to mask as
     * @param iReturn  the register to return the result into
     *
     * @return one of the {@link Op#R_NEXT}, {@link Op#R_CALL}, {@link Op#R_EXCEPTION}
     */
    protected int maskAs(Frame frame, ObjectHandle hTarget, TypeHandle hType, int iReturn)
        {
        if (hTarget instanceof GenericHandle)
            {
            ObjectHandle hMasked =
                ((GenericHandle) hTarget).maskAs(frame, hType.getDataType());

            return hMasked == null
                ? frame.raiseException(xException.illegalCast(frame, hType.getDataType().getValueString()))
                : frame.assignValue(iReturn, hMasked);
            }
        else
            {
            return frame.raiseException(xException.unsupportedOperation(frame, "maskAs"));
            }
        }

    /**
     * Reveal the specified target as a narrower type.
     *
     * @param frame     the current frame
     * @param hTarget   the target object
     * @param hType     the type handle to reveal as
     * @param aiReturn  the register to return the conditional result into
     *
     * @return one of the {@link Op#R_NEXT}, {@link Op#R_CALL}, {@link Op#R_EXCEPTION}
     */
    protected int revealAs(Frame frame, ObjectHandle hTarget, TypeHandle hType, int[] aiReturn)
        {
        if (hTarget instanceof GenericHandle)
            {
            ObjectHandle hRevealed =
                ((GenericHandle) hTarget).revealAs(frame, hType.getDataType());

            return hRevealed == null
                ? frame.assignValue(aiReturn[0], xBoolean.FALSE)
                : frame.assignValues(aiReturn, xBoolean.TRUE, hRevealed);
            }
        else
            {
            return frame.raiseException(xException.unsupportedOperation(frame, "revealAs"));
            }
        }

    /**
     * @return true iff the specified target is of the specified type
     */
    protected boolean instanceOf(ObjectHandle hTarget, TypeHandle hType)
        {
        return hTarget.getType().isA(hType.getDataType());
        }


    // ----- handle class -----

    public static class RefHandle
            extends GenericHandle
        {
        /**
         * Create an unassigned RefHandle for a given clazz.
         *
         * @param clazz  the class of the Ref (e.g. FutureRef<String>)
         * @param sName  optional name
         */
        protected RefHandle(TypeComposition clazz, String sName)
            {
            super(clazz);

            m_sName    = sName;
            m_fMutable = true;
            m_iVar     = REF_REFERENT;
            }

        /**
         * Create a RefHandle for a given property.
         *
         * @param clazz    the class of the Ref (e.g. FutureRef<String>)
         * @param hTarget  the target object
         * @param idProp   the property id
         */
        public RefHandle(TypeComposition clazz, ObjectHandle hTarget, PropertyConstant idProp)
            {
            super(clazz);

            assert hTarget != null;

            m_hReferent = hTarget;
            m_idProp    = idProp;
            m_sName     = idProp.getNestedIdentity().toString();
            m_fMutable  = hTarget.isMutable();
            m_iVar      = REF_PROPERTY;
            }

        /**
         * Create a RefHandle for a frame register.
         *
         * @param clazz  the class of the Ref
         * @param frame  the frame
         * @param iVar   the register index
         */
        public RefHandle(TypeComposition clazz, Frame frame, int iVar)
            {
            super(clazz);

            m_fMutable = true;

            assert iVar >= 0;

            Frame.VarInfo infoSrc = frame.getVarInfo(iVar);

            RefHandle refCurrent = infoSrc.getRef();
            if (refCurrent == null)
                {
                infoSrc.setRef(this);
                m_frame = frame;
                m_iVar  = iVar;
                }
            else
                {
                // there is already a Ref pointing to that register;
                // simply link to it
                m_iVar      = REF_REF;
                m_hReferent = refCurrent;
                }
            }

        /**
         * Ensure the RefHandle fields are initialized (only necessary for stateful Ref/Var mixins).
         *
         * @param frame  the current frame
         *
         * @return R_NEXT, R_CALL or R_EXCEPTION
         */
        public int initializeCustomFields(Frame frame)
            {
            MethodStructure methodInit = getComposition().ensureAutoInitializer();
            if (methodInit == null)
                {
                return Op.R_NEXT;
                }

            Frame frameID = frame.createFrame1(methodInit,
                    this.ensureAccess(Access.STRUCT), Utils.OBJECTS_NONE, Op.A_IGNORE);

            frameID.addContinuation(frameCaller ->
                {
                List<String> listUnassigned;
                return (listUnassigned = this.validateFields()) == null
                    ? Op.R_NEXT
                    : frameCaller.raiseException(xException.unassignedFields(frame, listUnassigned));
                });
            return frame.callInitialized(frameID);
            }

        public ObjectHandle getReferentHolder()
            {
            assert m_iVar != REF_REFERENT;
            return m_hReferent;
            }

        public ObjectHandle getReferent()
            {
            assert m_iVar == REF_REFERENT;
            return getField(REFERENT);
            }

        public void setReferent(ObjectHandle hReferent)
            {
            assert m_iVar == REF_REFERENT;
            setField(REFERENT, hReferent);
            }

        public String getName()
            {
            String sName = m_sName;
            if (sName == null && m_iVar >= 0)
                {
                m_sName = sName = m_frame.f_aInfo[m_iVar].getName();
                }
            return sName;
            }

        public PropertyConstant getPropertyId()
            {
            return m_idProp;
            }

        public VarSupport getVarSupport()
            {
            return (VarSupport) getOpSupport();
            }

        public boolean isAssigned()
            {
            switch (m_iVar)
                {
                case REF_REFERENT:
                    return getReferent() != null;

                case REF_REF:
                    return ((RefHandle) getReferentHolder()).isAssigned();

                case REF_PROPERTY:
                    {
                    GenericHandle hTarget = (GenericHandle) getReferentHolder();
                    ObjectHandle  hValue  = hTarget.getField(m_idProp);
                    if (hValue == null)
                        {
                        return false;
                        }
                    if (hTarget.isInflated(m_idProp))
                        {
                        return ((RefHandle) hValue).isAssigned();
                        }
                    return true;
                    }
                case REF_ARRAY:
                    return getReferentHolder() != null;

                default: // assertion m_frame != null && m_iVar >= 0
                    return m_frame.f_ahVar[m_iVar] != null;
                }
            }

        public boolean isSelfContained()
            {
            return m_hReferent == null || m_hReferent.isSelfContained();
            }

        // dereference the Ref from a register-bound to a handle-bound
        public void dereference()
            {
            assert m_frame != null && m_iVar >= 0;

            m_hReferent = m_frame.f_ahVar[m_iVar];
            m_frame     = null;
            m_iVar      = REF_REFERENT;
            }

        @Override
        public String toString()
            {
            String s = super.toString();
            switch (m_iVar)
                {
                case REF_REFERENT:
                    return isAssigned() ? s + getReferent() : s;

                case REF_REF:
                    return s + "--> " + getReferentHolder();

                case REF_PROPERTY:
                    return s + "-> " + getReferentHolder().getComposition() + "#" + m_sName;

                case REF_ARRAY:
                    return getReferentHolder() + "[" + ((IndexedRefHandle) this).f_lIndex + "]";

                default:
                    return s + "-> #" + m_iVar;
                }
            }

        /**
         * For a lack of a better name, this field holds either
         * <ul>
         *   <li>the referent itself (InjectRefHandle, m_iVar = REF_REFERENT),
         *   <li>a delegation to another RefHandle (m_iVar = REF_REF),
         *   <li>a property container (m_iVar = REF_PROPERTY),
         *   <li>an array holding the referent (IndexRefHandle, m_iVar = REF_ARRAY),
         *   <li>a delegation to a frame variable (m_frame != null, m_iVar >= 0), or
         *   <li>null, for m_iVar = REF_REFERENT, in which case the field REFERENT holds the value
         * </ul>
         *
         * What is quite important is that this field must not change while set - this will ensure
         * the correct behavior of a {@link #cloneAs} operation.
         */
        protected ObjectHandle     m_hReferent;
        protected PropertyConstant m_idProp;
        protected String           m_sName;
        protected Frame            m_frame;
        protected int              m_iVar;     // non-negative value represents a register;
                                               // negative values are described below

        // indicates that the m_hReferent field holds a referent
        protected static final int REF_REFERENT = -1;

        // indicates that the m_hReferent field holds a Ref that this Ref is "chained" to
        protected static final int REF_REF      = -2;

        // indicates that the m_hReferent field holds a property target
        protected static final int REF_PROPERTY = -3;

        // indicates that the m_hReferent field holds an array target
        protected static final int REF_ARRAY    = -4;

        /**
         * Synthetic property holding a referent.
         */
        protected final static String REFERENT = "$value";
        }

    /***
     * RefHandle for an indexed element of an Array or a Tuple.
     */
    public static class IndexedRefHandle
            extends RefHandle
        {
        protected final long f_lIndex;

        public IndexedRefHandle(TypeComposition clazz, ObjectHandle hTarget, long lIndex)
            {
            super(clazz, null);

            m_iVar      = REF_ARRAY;
            m_hReferent = hTarget;
            f_lIndex    = lIndex;
            }

        @Override
        public void dereference()
            {
            // no op
            }
        }

    /**
     * Helper class for calculating the referent equality.
     */
    protected static class CompareReferents
            implements Frame.Continuation
        {
        public CompareReferents(RefHandle hRef1, RefHandle hRef2, xRef template, int iReturn)
            {
            this.hRef1 = hRef1;
            this.hRef2 = hRef2;
            this.template = template;
            this.iReturn = iReturn;
            }

        @Override
        public int proceed(Frame frameCaller)
            {
            updateResult(frameCaller);

            return doNext(frameCaller);
            }

        protected void updateResult(Frame frameCaller)
            {
            if (index == 0)
                {
                hReferent1 = frameCaller.popStack();
                }
            else
                {
                hReferent2 = frameCaller.popStack();
                }
            }

        public int doNext(Frame frameCaller)
            {
            while (++index < 2)
                {
                RefHandle hRef = index == 0 ? hRef1 : hRef2;

                switch (template.getReferent(frameCaller, hRef, Op.A_STACK))
                    {
                    case Op.R_NEXT:
                        updateResult(frameCaller);
                        break;

                    case Op.R_CALL:
                        frameCaller.m_frameNext.addContinuation(this);
                        return Op.R_CALL;

                    case Op.R_EXCEPTION:
                        return Op.R_EXCEPTION;

                    case Op.R_BLOCK:
                        return ((FutureHandle) hRef).makeDeferredHandle(frameCaller).
                            proceed(frameCaller, this);

                    default:
                        throw new IllegalStateException();
                    }
                }

            ClassTemplate template = hReferent1.getTemplate();
            boolean fEquals = template == hReferent2.getTemplate()
                && template.compareIdentity(hReferent1, hReferent2);

            return frameCaller.assignValue(iReturn, xBoolean.makeHandle(fEquals));
            }

        private final RefHandle hRef1;
        private final RefHandle hRef2;
        private final xRef      template;
        private final int       iReturn;

        private ObjectHandle    hReferent1;
        private ObjectHandle    hReferent2;
        private int             index = -1;
        }

    // ----- constants -----------------------------------------------------------------------------

    protected static SignatureConstant s_sigGet;
    }
