package org.xvm.asm.constants;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.function.Consumer;

import org.xvm.asm.Component.ContributionChain;
import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.ErrorListener;
import org.xvm.util.Severity;

import static org.xvm.util.Handy.readMagnitude;
import static org.xvm.util.Handy.writePackedLong;


/**
 * Represent a constant that specifies an explicitly immutable form of an underlying type.
 */
public class ImmutableTypeConstant
        extends TypeConstant
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
    public ImmutableTypeConstant(ConstantPool pool, Format format, DataInput in)
            throws IOException
        {
        super(pool);

        m_iType = readMagnitude(in);
        }

    /**
     * Construct a constant whose value is an immutable type.
     *
     * @param pool       the ConstantPool that will contain this Constant
     * @param constType  a TypeConstant that this constant modifies to be immutable
     */
    public ImmutableTypeConstant(ConstantPool pool, TypeConstant constType)
        {
        super(pool);

        if (constType == null)
            {
            throw new IllegalArgumentException("type required");
            }

        m_constType = constType;
        }


    // ----- TypeConstant methods ------------------------------------------------------------------

    @Override
    public boolean isModifyingType()
        {
        return true;
        }

    @Override
    public TypeConstant getUnderlyingType()
        {
        return m_constType;
        }

    @Override
    public boolean isImmutabilitySpecified()
        {
        return true;
        }

    @Override
    public boolean isNullable()
        {
        return m_constType.isNullable();
        }

    @Override
    public TypeConstant nonNullable()
        {
        return isNullable()
                ? getConstantPool().ensureImmutableTypeConstant(m_constType.nonNullable())
                : this;
        }

    @Override
    protected TypeConstant cloneSingle(TypeConstant type)
        {
        return getConstantPool().ensureImmutableTypeConstant(type);
        }

    @Override
    protected TypeConstant unwrapForCongruence()
        {
        return m_constType.unwrapForCongruence();
        }

    @Override
    protected TypeInfo buildTypeInfo(ErrorListener errs)
        {
        // the "immutable" keyword does not affect the TypeInfo, even though the type itself is
        // slightly different
        return m_constType.buildTypeInfo(errs);
        }

    // ----- type comparison support ---------------------------------------------------------------

    @Override
    protected boolean validateContributionFrom(TypeConstant thatRight, Access accessLeft, ContributionChain chain)
        {
        // the l-value (this) is immutable; so should be the r-value (that)
        return thatRight.isImmutabilitySpecified()
            && super.validateContributionFrom(thatRight, accessLeft, chain);
        }


    // ----- Constant methods ----------------------------------------------------------------------

    @Override
    public Format getFormat()
        {
        return Format.ImmutableType;
        }

    @Override
    protected Object getLocator()
        {
        return m_constType;
        }

    @Override
    public boolean containsUnresolved()
        {
        return m_constType.containsUnresolved();
        }

    @Override
    public Constant simplify()
        {
        m_constType = (TypeConstant) m_constType.simplify();
        return this;
        }

    @Override
    public void forEachUnderlying(Consumer<Constant> visitor)
        {
        visitor.accept(m_constType);
        }

    @Override
    protected int compareDetails(Constant that)
        {
        return this.m_constType.compareTo(((ImmutableTypeConstant) that).m_constType);
        }

    @Override
    public String getValueString()
        {
        return "immutable " + m_constType.getValueString();
        }


    // ----- XvmStructure methods ------------------------------------------------------------------

    @Override
    protected void disassemble(DataInput in)
            throws IOException
        {
        m_constType = (TypeConstant) getConstantPool().getConstant(m_iType);
        }

    @Override
    protected void registerConstants(ConstantPool pool)
        {
        m_constType = (TypeConstant) pool.register(m_constType);
        }

    @Override
    protected void assemble(DataOutput out)
            throws IOException
        {
        out.writeByte(getFormat().ordinal());
        writePackedLong(out, indexOf(m_constType));
        }

    @Override
    public boolean validate(ErrorListener errs)
        {
        boolean fHalt = false;

        if (!isValidated())
            {
            fHalt |= super.validate(errs);

            // the immutable type constant can modify any type constant other than an immutable
            // type constant
            TypeConstant type = (TypeConstant) m_constType.simplify();
            if (type instanceof ImmutableTypeConstant)
                {
                fHalt |= log(errs, Severity.WARNING, VE_IMMUTABLE_REDUNDANT);
                }

            // a service type cannot be immutable
            if (type.ensureTypeInfo(errs).isService())
                {
                fHalt |= log(errs, Severity.ERROR, VE_IMMUTABLE_SERVICE_ILLEGAL, m_constType.getValueString());
                }
            }

        return fHalt;
        }


    // ----- Object methods ------------------------------------------------------------------------

    @Override
    public int hashCode()
        {
        return -m_constType.hashCode();
        }


    // ----- fields --------------------------------------------------------------------------------

    /**
     * During disassembly, this holds the index of the type constant.
     */
    private int m_iType;

    /**
     * The type referred to.
     */
    private TypeConstant m_constType;
    }
