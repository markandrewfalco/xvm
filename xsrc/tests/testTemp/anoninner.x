module TestAnonInner.xqiz.it
    {
    void run()
        {
        testSimple();
        }

    class Inner
        {
        construct(String s) {}
        }

    void testSimple()
        {
        @Inject X.io.Console console;
        console.println("\n** testSimple()");

        Int i = 4;

        // var o = new Object()
        var o = new Inner("hello")
            {
            // @Inject X.io.Console console; // TODO test without this to force capture
            void run()
                {
                console.println("in run (i=" + i + ")");
                ++i;
                // foo();
                }
            };

        o.run();

        console.println("done (i=" + i + ")");
        }
    }
