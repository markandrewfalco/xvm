import Ecstasy.fs.Directory;
import Ecstasy.fs.File;
import Ecstasy.fs.FileNode;
import Ecstasy.fs.FileStore;
import Ecstasy.fs.FileWatcher;
import Ecstasy.fs.Path;

/**
 * Native OS FileNode implementation.
 */
const OSFileNode
        implements FileNode
        delegates  Stringable(pathString)
    {
    @Override
    @Lazy Path path.calc()
        {
        return new Path(pathString);
        }

    @Override
    @RO String name.get()
        {
        return path.form == Root ? "" : path.name;
        }

    @Override
    @RO Boolean exists;

    @Override
    conditional File linkAsFile()
        {
        return False; // TODO
        }

    @Override
    @Lazy DateTime created.calc()
        {
        // TODO: should it be the "local" timezone?
        return new DateTime(createdMillis*Time.PICOS_PER_MILLI);
        }

    @Override
    DateTime modified.get()
        {
        return new DateTime(modifiedMillis*Time.PICOS_PER_MILLI);
        }

    @Override
    @RO DateTime accessed.get()
        {
        return new DateTime(accessedMillis*Time.PICOS_PER_MILLI);
        }

    @Override
    @RO Boolean readable;

    @Override
    @RO Boolean writable;

    @Override
    Boolean create()
        {
        return !exists && store.create(this:protected);
        }

    @Override
    FileNode ensure()
        {
        if (!exists)
            {
            create();
            }
        return this;
        }

    @Override
    Boolean delete()
        {
        return exists && store.delete(this:protected);
        }

    @Override
    conditional FileNode renameTo(String name);

    // ----- equality support ----------------------------------------------------------------------

    static <CompileType extends OSFileNode> Int hashCode(CompileType value)
        {
        return String.hashCode(value.pathString);
        }

    static <CompileType extends OSFileNode> Boolean equals(CompileType node1, CompileType node2)
        {
        return node1.pathString == node2.pathString &&
               node1.is(OSFile) == node2.is(OSFile);
        }

    // ----- internal -----------------------------------------------------------------------------


    // ----- native --------------------------------------------------------------------------------

    @Abstract protected OSFileStore store;
    @Abstract protected String      pathString;

    @Override
    @Abstract Int size;

    @Abstract private Int createdMillis;
    @Abstract private Int accessedMillis;
    @Abstract private Int modifiedMillis;
    }
