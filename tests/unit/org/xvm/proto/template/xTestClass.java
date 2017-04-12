package org.xvm.proto.template;

import org.xvm.proto.*;

import org.xvm.proto.op.*;

/**
 * A test class.
 *
 * @author gg 2017.03.15
 */
public class xTestClass extends TypeCompositionTemplate
    {
    private final ConstantPoolAdapter adapter;

    public xTestClass(TypeSet types)
        {
        super(types, "x:TestClass", "x:Object", Shape.Class);

        adapter = types.f_constantPool;
        }

    @Override
    public void initDeclared()
        {
        ensurePropertyTemplate("prop1", "x:String");

        // --- constructor()
        FunctionTemplate ftConstruct = ensureFunctionTemplate(
                "construct", new String[]{"x:TestClass", "x:String"}, new String[]{"x:Function"});
        FunctionTemplate ftFinally = ensureFunctionTemplate(
                "construct:finally", new String[]{"x:TestClass", "x:String"}, VOID);

        ftConstruct.m_aop = new Op[]
            {
            new X_Print(-adapter.ensureValueConstantId("# in constructor: TestClass #")),
            new Set(0, adapter.getPropertyConstId("x:TestClass", "prop1"), 1),
            new Return_1(-adapter.getMethodConstId("x:TestClass", "construct:finally")),
            };
        ftConstruct.m_cVars = 2;

        ftFinally.m_aop = new Op[]
            {
            new X_Print(-adapter.ensureValueConstantId("# in finally: #")),
            new X_Print(0),
            new Return_0(),
            };
        ftFinally.m_cVars = 2;

        // --- method1()
        MethodTemplate mtMethod1 = ensureMethodTemplate("method1", VOID, INT);
        mtMethod1.m_aop = new Op[]
            { // #0 (this)
            new X_Print(-adapter.ensureValueConstantId("# in TestClass.method1 #")),
            new Var(adapter.getClassConstId("x:String")), // #1 (s)
            new Get(0, adapter.getPropertyConstId("x:TestClass", "prop1"), 1),
            new Var(adapter.getClassConstId("x:Int64")), // #2 (of)
            new IVar(adapter.getClassConstId("x:String"), adapter.ensureValueConstantId("world")), // #3
            new Invoke_11(1, -adapter.getMethodConstId("x:String", "indexOf"), 3, 2),
            new Var(adapter.getClassConstId("x:Int64")), // #4
            new Get(1, adapter.getPropertyConstId("x:String", "length"), 4),
            new Add(4, 2, 4),
            new Return_1(4),
            };
        mtMethod1.m_cVars = 5;

        MethodTemplate mtThrowing = ensureMethodTemplate("throwing", STRING, VOID);
        mtThrowing.m_aop = new Op[]
            { // #0 (this), #1 (s)
            new Var(adapter.getClassConstId("x:Exception")), // #2
            new New_N(adapter.getMethodConstId("x:Exception", "construct"),
                        new int[]{1, -adapter.getClassConstId("x:Nullable$Null")}, 2),
            new Throw(2),
            };
        mtThrowing.m_cVars = 3;
        }
    }