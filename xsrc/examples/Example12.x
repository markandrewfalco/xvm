// resolving types (flattening classes) - basic examples:
class B
    {
    private Void pri();
    protected Void pro();
    public Void pub();
    }
// private B: pri(), pro(), pub()
// protected B: pro(), pub()
// public B: pub()

class D
        extends B
    {
    }
// private D: pro(), pub()
// protected D: pro(), pub()
// public D: pub()

// but protected methods have to be accumulated, even if it will ultimately be removed, because
// they are theoretically reachable as part of a method chain; similar for properties
class B
    {
    private Void pri();
    protected Void pro();
    public Void pub();
    }

class D
        extends B
    {
    private Int pri();
    public Void pro();
    public Void pub();
    }
// private D: Int pri() with no super, Void pro() with protected super, Void pub() with public super
// protected D: Void pro() with protected super, Void pub() with public super
// public D: Void pro() with protected super, Void pub() with public super

// alternatively ...
class D
        extends B
    {
    private Int pri();
    protected Void pro();
    public Void pro();
    public Void pub();
    }
// private D: Int pri() with no super, (protected) Void pro() with protected super, (public) Void pub() with public super
// protected D: (protected) Void pro() with protected super, (public) Void pub() with public super
// public D: (public) Void pro() with protected super, (public) Void pub() with public super

// property
public/private Int x;
// acts something like:
Int x
    {
    public Int get()
        {
        return super();
        }
    public Void set(Int v)
        {
        throw new ReadOnlyException();
        }
    private Void set(Int v)
        {
        super(v);
        }
    }

// adding mixin
public/private Int x;
// acts something like:
Int x
    {
    public Int get()
        {
        return super();
        }
    public Void set(Int v)
        {
        throw new ReadOnlyException();
        }
    private Void set(Int v)
        {
        super(v);
        }
    }

mixin SoftRef
        into Ref<T>
    {
    private set(T v)
        {
        // private-only functionality here
        super(v);
        }
    protected set(T v)
        {
        // protected-only functionality here
        super(v);
        }
    public set(T v)
        {
        super(v);
        }
    }

mixin LazyRef<RefType>
        into Ref<RefType>
    {
    private Boolean assignable = false;

    RefType get()
        {
        if (!assigned)
            {
            RefType value = calc();
            try
                {
                assignable = true;
                set(value);
                }
            finally
                {
                assignable = false;
                }

            return value;
            }

        return super();                              // _WHICH_ super?
        }

    Void set(RefType value)
        {
        assert !assigned && assignable;
        super(value);                               // _WHICH_ super?
        }
                                                                                       s
    protected RefType calc();
    }

// -- example for splitting out set() into a separate interface

interface Ref<RefType> // basically the old Ref interface, minus set()
    {
    RefType get();
    @RO Boolean assigned;
    @RO Type ActualTypes;
    }

interface Var<RefType>
        extends Ref<RefType>
    {
    Void set(RefType value);
    }

class B
    {
    @Lazy public/private Int Hashcode.calc()
        {
        return ...;
        }

    Void bar()
        {
        Ref<Int> r = &Hashcode;
        }
    }

Void foo(B b)
    {
    Ref<Int> r = &b.Hashcode;   // does NOT compile because Hashcode in the public interface is a CRef
    }

// So all properties/variables/constants/etc. are Ref's (@RO), and some (those with an accessible
// setter) are Var's, although a Var can still reject a set() (e.g. an immutable object)