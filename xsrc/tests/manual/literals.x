module TestLiterals.xqiz.it
    {
    import X.rt.Version;

    @Inject Console console;

    void run()
        {
        console.println("*** literal tests ***\n");

        testVersions();
        testPaths();
        testIncludes();
        testMultiline();
        testMultilineTemplate();
        testHex();
        testDirs();
        testDates();
        testTimes();
        testDateTimes();
        testDurations();
        }

    void testVersions()
        {
        console.println("\n** test Versions()");

        Version version = new Version(null, 1);
        console.println($"new Version(null, 1)={version}");

        version = new Version(version, 0);
        // version = new Version(version, 0, "20130313144700");
        console.println($"new Version(version, 0)={version}");

        version = new Version(version, Alpha);
        console.println($"new Version(version, Alpha)={version}");

        version = new Version(version, 2);
        console.println($"new Version(version, 2)={version}");

        for (Int i : 0..3)
            {
            console.println("version[" + i + "]=" + version[i]);
            }

        console.println("version[1..2]=" + version[1..2]);
        console.println("version[0..1]=" + version[0..1]);
        console.println("--version=" + --version);
        console.println("++version=" + ++version);

        for (String s : ["1", "alpha", "1.0", "beta2", "5.6.7.8-alpha", "1.2-beta5", "1.2beta5"])
            {
            console.println("version for String " + s + "=" + new Version(s));
            }

        // "1.2-beta3" to "1.2-beta5"
        console.println("steps from 1.2-beta to 1.2-beta5="
                + new Version("1.2-beta").stepsTo(new Version("1.2-beta5")));
        console.println("steps from 1.2-beta3 to 1.2-beta="
                + new Version("1.2-beta3").stepsTo(new Version("1.2-beta")));
        console.println("steps from 1.2-beta3 to 1.2-beta5="
                + new Version("1.2-beta3").stepsTo(new Version("1.2-beta5")));

        version = v:alpha;
        console.println($"literal v:alpha={version}");

        version = v:1;
        console.println($"literal v:1={version}");

        version = v:1.0;
        console.println($"literal v:1.0={version}");

        version = v:beta2;
        console.println($"literal v:beta2={version}");

        version = v:5.6.7.8-alpha;
        console.println($"literal v:5.6.7.8-alpha={version}");

        version = v:1.2-beta5;
        console.println($"literal v:1.2-beta5={version}");

        version = v:1.2beta5;
        console.println($"literal v:1.2beta5={version}");

        version = v:1.2beta5+123-456.abc;
        console.println($"literal v:1.2beta5+123-456.abc={version}");
        }

    void testPaths()
        {
        console.println("\n** testPaths()");

        Path path = Path:./resources/;
        console.println($"Path ./resources/={path}");

        path = Path:./resources/more/;
        console.println($"Path ./resources/more/={path}");

        path = Path:./resources/more/msgs_EN.txt;
        console.println($"Path ./resources/more/msgs_EN.txt={path}");

        File file = ./resources/more/msgs_EN.txt;
        console.println($"File ./resources/more/msgs_EN.txt={file}");

        file = File:./resources/more/msgs_EN.txt;
        console.println($"File ./resources/more/msgs_EN.txt={file}");

        Directory dir = ./resources/;
        console.println($"Dir ./resources/={dir}");

        dir = Directory:./resources/more/;
        console.println($"Dir ./resources/more/={dir}");
        }

    void testIncludes()
        {
        console.println("\n** testIncludes()");

        String s = $./resources/more/msgs_EN.txt;
        console.println($"./resources/more/msgs_EN.txt={s}");
        }

    void testMultiline()
        {
        console.println("\n** testMultiline()");

        String s = `|<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
                    |<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
                    |<stripes:useActionBean binding="/View.action" />
                    |<wiki:Include page="ViewTemplate.jsp" />
                    |String myString = "This is my string\n" +
                    |        " which I want to be \n" +
                    |        "on multiple lines.";
                    ||s = """ this is a very
                    |        long string if I had the
                    |        energy to type more and more ..."""
                    |`string text`
                    |
                    |`string text line 1
                    | string text line 2`
                    |
                    |`string text ${expression} string text`
                    |
                    |tag `string text ${expression} string text`
                    |//const char* p = "\xfff"; // error: hex escape sequence out of range
                    |const char* p = "\xff""f"; // OK: the literal is const char[3] holding {'\xff','f','\0'}
                  ; // semi-colon is the end of the declaration statement
        console.println($"s={s}");
        }

    void testMultilineTemplate()
        {
        console.println("\n** testMultiline()");

        const Person(String firstname, String lastname);
        Person person = new Person("Bob", "Smith");

        String s = $|# TOML doc
                    |[name]
                    |first = "{person.firstname}"
                    |last = "{person.lastname}"
                    ;

        console.println($"\nTOML=\n{s}");

        s = $|\{
             |"person": \{
             |  "first": "{person.firstname}"
             |  "last": "{person.lastname}"
             |  }
             |}
             ;

        console.println($"\nJSON=\n{s}");
        }

    void testHex()
        {
        console.println("\n** testHex()");

        Byte[] bytes = #123_4567_89aB_cDeF;
        console.println($"bytes={bytes}");

        bytes = #|0123456789aBcDeF 0123456789aBcDeF 0123456789aBcDeF 0123456789aBcDeF
                 |0123456789aBcDeF_0123456789aBcDeF_0123456789aBcDeF_0123456789aBcDeF
                 ;
        console.println($"bytes={bytes[0..10]}...{bytes[bytes.size-10..bytes.size-1]}");

        bytes = #/literals.x;
        console.println($"bytes={bytes[0..10]}...{bytes[bytes.size-10..bytes.size-1]}");
        }

    void testDirs()
        {
        console.println("\n** testDirs()");

        FileStore fs = FileStore:/resources/;
        console.println("fs=" + fs);

        // TODO BUGBUG CP StatementExpression requires a "return"
        console.println($"\n(recursive)\n{{fs.emitListing($); return;}}");
        console.println($"\n(non-recursive)\n{{fs.emitListing($, False); return;}}");

        File file = File:./resources/more/msgs_EN.txt;
        console.println($"File:./resources/more/msgs_EN.txt={file}");

        Directory dir = Directory:./resources/;
        // TODO BUGBUG CP (??) - output didn't look like it recursed into "./more/"
        console.println($"Directory:./resources/=(recursive)\n{{dir.emitListing($); return;}}");
        }

    void testDates()
        {
        console.println("\n** testDates()");

        Date date = new Date("1999-12-25");
        console.println($"date={date} or {Date:1999-12-25}");

        date = new Date("19991225");
        console.println($"date={date} or {Date:19991225}");

        date = new Date("99999-01-23");
        console.println($"date={date} or {Date:9999-01-23} (one less 9)");
        }

    void testTimes()
        {
        console.println("\n** testTimes()");

        Time time = new Time("12:01:23");
        console.println($"time={time} or {Time:12:01:23}");

        time = new Time("120123");
        console.println($"time={time} or {Time:120123}");

        time = new Time("12:01:23.456");
        console.println($"time={time} or {Time:12:01:23.456}");

        time = new Time("120123.456");
        console.println($"time={time} or {Time:120123.456}");
        }

    void testDateTimes()
        {
        console.println("\n** testDateTimes()");

        DateTime dt = new DateTime("1999-12-25T12:01:23");
        console.println($"dt={dt} or {DateTime:1999-12-25T12:01:23}");

        dt = new DateTime("19991225T120123");
        console.println($"dt={dt} or {DateTime:19991225T120123}");

        dt = new DateTime("99999-01-23T12:01:23.456");
        console.println($"dt={dt} or {DateTime:9999-01-23T12:01:23.456} (one less 9)");

        dt = new DateTime("2019-05-22T120123.456Z");
        console.println($"dt={dt} or {DateTime:2019-05-22T120123.456Z}");

        dt = new DateTime("2019-05-22T120123.456+01:30");
        console.println($"dt={dt} or {DateTime:2019-05-22T120123.456+01:30}");

        dt = new DateTime("2019-05-22T120123.456-5:00");
        console.println($"dt={dt} or {DateTime:2019-05-22T120123.456-05:00}");
        }

    void testDurations()
        {
        console.println("\n** testDurations()");

        Duration duration = new Duration("P3DT4H5M6S");
        console.println($"duration={duration} or {Duration:P3DT4H5M6S}");

        duration = new Duration("1DT1H1M1.23456S");
        console.println($"duration={duration} or {Duration:P1DT1H1M1.23456S}");

        duration = new Duration("PT10S");
        console.println($"PT10S duration={duration} or {Duration:PT10S}");

        duration = new Duration("10S");
        console.println($"10S duration={duration} or {Duration:10S}");

        duration = new Duration("PT10.5S");
        console.println($"PT10.5S duration={duration} or {Duration:PT10.5S}");

        duration = new Duration("P10.5S");
        console.println($"P10.5S duration={duration} or {Duration:P10.5S}");

        duration = new Duration("T10.5S");
        console.println($"T10.5S duration={duration} or {Duration:T10.5S}");

        duration = new Duration("10.5S");
        console.println($"10.5S duration={duration} or {Duration:10.5S}");
        }
    }

