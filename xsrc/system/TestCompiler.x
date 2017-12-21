class TestCompiler<TestType1 extends Number,
                   TestType2 extends TestType1,
                   TestType3 extends TestType2>
    {
    interface MyMap<KM, VM>
        {
        VM get(KM key);
        Void put(KM key, VM value);
        Boolean containsValue(VM value);
        }

    interface Consumer<VC>
        {
        Boolean containsValue(VC value);
        }

    class MyClass1<K1, VC>
        implements MyMap<K1, VC> {}

    class MyClass2<K2, V2>
            implements Consumer<V2>
        {
        Boolean containsValue(V2 value) {return false;}
        }

    class MyConsumer<VMC>
            implements Consumer<VMC>
        {
        Boolean containsValue(VMC value) {return true;}
        }

    mixin MyConsumer2<VMC>
            implements Consumer<VMC>
        {
        Boolean containsValue(VMC value) {return true;}
        }

    class MyClass3<K3, V3>
        extends MyConsumer<V3> {}

    class MyClass4<K4>
            implements Consumer<Number>
        {
        Boolean containsValue(Int value) {return false;}
        }

    class MyClass5<K5>
        extends MyConsumer<Int> {}

    class MyClass6<K6, V6>
        extends MyClass3<K6, V6> {}

    class MyClass7
        extends MyClass3<String, String> {}

    class MyClass8<K8, V8>
        incorporates MyConsumer2<V8> {}

    class MyClass9<K9, V9>
        incorporates conditional MyConsumer2<V9 extends Number> {}

    class MyClass10<V10>
        extends MyClass9<String, V10> {}

    static Void test1(MyClass1<String, Number> c1,
                      MyClass2<String, Number> c2,
                      MyClass3<String, Number> c3,
                      MyClass4<String> c4,
                      MyClass5<String> c5,
                      MyClass6<String, Int> c6,
                      MyClass7 c7,
                      MyClass8<String, Number> c8,
                      MyClass10<Number> c10)
        {
        Consumer<Int> finder1 = c1; // OK; duck-typing

        Consumer<Int> finder2 = c2; // OK; "Implements"

        Consumer<Int> finder3 = c3; // OK; "Extends-Implements"

        Consumer<Int> finder4 = c4; // OK; "Implements"

        Consumer<Int> finder5 = c5; // OK; "Extends"

        Consumer<Int> finder6 = c6; // OK; "Extends-Extends-Implements"
        MyConsumer<Int> finder6a = c6; // OK; "Extends-Extend"

        Consumer<String> finder7 = c7; // OK; "Extends-Extends-Implements"

        Consumer<Int> finder8 = c8; // OK; "Incorporates-Implements"
        Consumer<Int> finder8a = c8; // OK; "Incorporates-Extends"

        Consumer<Number> finder10 = c10; // OK; "Extends-Incorporates-Implements"
        }

    static Void test1ExpectedFailure1(MyClass7 c7ExpectedFailure)
        {
        Consumer<Int> finder7a = c7ExpectedFailure;
        }

    static Void test1ExpectedFailure2(MyClass9<String, String> c9)
        {
        MyConsumer<Int> finder9 = c9; // fail; "Incorporates"
        }

    static Void test1ExpectedFailure3(MyClass9<String, String> c9)
        {
        Consumer<Int> finder9a = c9; // fail; "Incorporates-Extends"
        }

    static Void test1ExpectedFailure4(MyClass10<String> c10a)
        {
        MyConsumer<Int> finder10a = c10a; // fail; "Extends-Incorporates"
        }

//    static Void test1ExpectedFailure5(MyClass10<Int> c10)
//        {
//        immutable MyConsumer<Int> finder10b = c10; // fail; "Extends-Incorporates"
//        }

    class P<T>
        {
        T produce()
            {
            T t;
            return t;
            }
        }

    class P2<T2>
        {
        P<T2> produce()
            {
            P<T2> p;
            return p;
            }
        }

    interface C<T>
        {
        Void consume(T value);
        }

    interface C2<T2>
        {
        C<T2> consume();
        }

    class PC<T> extends P<T> implements C<T>
        {
        }

    class FakePCofObject
        {
        Object produce() {return "";}
        Void consume(Object value) {}
        }

    class FakePCofString
        {
        String produce() {return "";}
        Void consume(String value) {}
        }

    static Void testPC(C<Object>  co,
                       C2<Object> c2o,
                       PC<Object> pco,
                       P<String>  ps,
                       P2<String> p2s,
                       PC<String> pcs)
        {
        C<String> cs = co;

        C2<String> c2s = c2o;

        C<String> cs1 = pco;

        P<Object> po = ps;

        P2<Object> p20 = p2s;

        PC<Object> pco = pcs; // ok, but the RT needs to "safe-wrap" the consuming methods
        }

    static Void testPCExpectedFailure1(C<String> y3)
        {
        C<Object> x3 = y3;
        }

    static Void testPCExpectedFailure2(PC<String> y4)
        {
        C<Object> x4 = y4;
        }

    static Void testPCExpectedFailure3(P<Object> y5)
        {
        P<String> x5 = y5;
        }

    static Void testPCExpectedFailure4(PC<String> y7)
        {
        FakePCofObject x7 = y7;
        }

    static Void testPCExpectedFailure5(PC<Object> y8)
        {
        PC<String> x8 = y8;
        }

    static Int test2()
        {
        Int i = 0;
        return i;
        }

//    TestType1 extends Number,
//    TestType2 extends TestType1,
//    TestType3 extends TestType2
    Void test3(TestType1 t1,
               TestType2 t2,
               TestType3 t3,
               C<TestType1> ct1,
               P<TestType1> pt1,
               C<TestType2> ct2,
               P<TestType2> pt2,
               C<TestType3> ct3,
               P<TestType3> pt3,
               C<Number> cn,
               P<Number> pn)
        {
        Number n1 = t1;
        Number n3 = t3;

        TestType1 t11 = t2;
        TestType1 t13 = t3;

        C<TestType3> ct31 = ct1;
        P<TestType1> pt11 = pt3;

        C<TestType1> ct2 = ct1;
        }

    Void test3ExpectedFailure1(TestType3 t3)
        {
        Int n = t3;
        }
    }
