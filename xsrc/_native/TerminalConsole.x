/**
 * Simple console.
 */
class TerminalConsole
        implements io.Console
    {
    @Override
    void print(Object o);

    @Override
    void println(Object o = "");

    @Override
    String readLine();

    @Override
    Boolean echo(Boolean flag);
    }
