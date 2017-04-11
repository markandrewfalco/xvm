package org.xvm.proto.template;

import org.xvm.proto.ObjectHandle;
import org.xvm.proto.TypeComposition;
import org.xvm.proto.TypeCompositionTemplate;
import org.xvm.proto.TypeSet;

/**
 * TODO:
 *
 * @author gg 2017.02.27
 */
public class xRef
        extends TypeCompositionTemplate
    {
    public xRef(TypeSet types)
        {
        super(types, "x:Ref<RefType>", "x:Object", Shape.Interface);

        addImplement("x:Referent");
        }

    @Override
    public void initDeclared()
        {
        //    @ro Boolean assigned;
        //    conditional RefType peek()
        //    RefType get();
        //    Void set(RefType value);
        //    @ro Type ActualType;
        //    static Boolean equals(Ref value1, Ref value2)
        //    @ro String? name;
        //    @ro Int byteLength;
        //    @ro Boolean selfContained;

        ensurePropertyTemplate("assigned", "x:Boolean").makeReadOnly();
        ensureMethodTemplate("peek", VOID, new String[]{"x:ConditionalTuple<RefType>"});
        ensureMethodTemplate("get", VOID, new String[]{"RefType"});
        ensureMethodTemplate("set", new String[]{"RefType"}, VOID);
        ensurePropertyTemplate("ActualType", "x:Type").makeReadOnly();
        ensurePropertyTemplate("name", "x:String|x:Nullable").makeReadOnly();
        ensurePropertyTemplate("byteLength", "x:Int").makeReadOnly();
        ensurePropertyTemplate("selfContained", "x:Boolean").makeReadOnly();

        ensureFunctionTemplate("equals", new String[]{"x:Ref", "x:Ref"}, BOOLEAN);
        }

    // a reference
    public static class RefHandle
            extends ObjectHandle
        {
        protected ObjectHandle m_hDelegate;

        public RefHandle(TypeComposition clazz)
            {
            super(clazz);
            }

        public RefHandle(TypeComposition clazz, ObjectHandle handle)
            {
            super(clazz);

            m_hDelegate = handle;
            }

        @Override
        public String toString()
            {
            return super.toString() + m_hDelegate;
            }
        }
    }
