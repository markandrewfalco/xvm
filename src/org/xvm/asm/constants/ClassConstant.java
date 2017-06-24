package org.xvm.asm.constants;


import java.io.DataInput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;


/**
 * Represent a Class constant.
 */
public class ClassConstant
        extends NamedConstant
    {
    // ----- constructors --------------------------------------------------------------------------

    /**
     * Constructor used for deserialization.
     *
     * @param pool    the ConstantPool that will contain this Constant
     * @param format  the format of the Constant in the stream
     * @param in      the DataInput stream to read the Constant value from
     *
     * @throws IOException  if an issue occurs reading the Constant value
     */
    public ClassConstant(ConstantPool pool, Format format, DataInput in)
            throws IOException
        {
        super(pool, format, in);
        }

    /**
     * Construct a constant whose value is a class identifier.
     *
     * @param pool         the ConstantPool that will contain this Constant
     * @param constParent  the module, package, class, or method that contains this class
     * @param sName        the unqualified class name
     */
    public ClassConstant(ConstantPool pool, Constant constParent, String sName)
        {
        super(pool, constParent, sName);

        if (    !( constParent.getFormat() == Format.Module
                || constParent.getFormat() == Format.Package
                || constParent.getFormat() == Format.Class
                || constParent.getFormat() == Format.Method ))
            {
            throw new IllegalArgumentException("parent module, package, class, or method required");
            }
        }

    protected ClassConstant(ConstantPool pool)
        {
        super(pool);
        }

    // ----- type-specific functionality -----------------------------------------------------------

    /**
     * Determine if this ClassConstant is the "Object" class.
     *
     * @return true iff this ClassConstant represents the Ecstasy root Object class
     */
    public boolean isEcstasyObject()
        {
        return getName().equals(CLASS_OBJECT)
                && getNamespace().getFormat() == Format.Module
                && ((ModuleConstant) getNamespace()).isEcstasyModule();
        }

    /**
     * @return the ClassTypeConstant for the public interface of this class
     */
    public ClassTypeConstant asTypeConstant()
        {
        return getConstantPool().ensureClassTypeConstant(this, Access.PUBLIC);
        }


    // ----- Constant methods ----------------------------------------------------------------------

    @Override
    public Format getFormat()
        {
        return Format.Class;
        }


    // ----- XvmStructure methods ------------------------------------------------------------------

    @Override
    public String getDescription()
        {
        Constant constParent = getNamespace();
        while (constParent instanceof ClassConstant)
            {
            constParent = ((ClassConstant) constParent).getNamespace();
            }

        return "class=" + getValueString() + ", " + constParent.getDescription();
        }
    }