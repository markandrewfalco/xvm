package org.xvm.asm.constants;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;

import static org.xvm.util.Handy.readMagnitude;
import static org.xvm.util.Handy.writePackedLong;


/**
 * Represent a constant that specifies the intersection ("+") of two types.
 */
public class IntersectionTypeConstant
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
    public IntersectionTypeConstant(ConstantPool pool, Format format, DataInput in)
            throws IOException
        {
        super(pool);

        m_iType1 = readMagnitude(in);
        m_iType2 = readMagnitude(in);
        }

    /**
     * Construct a constant whose value is the intersection of two types.
     *
     * @param pool        the ConstantPool that will contain this Constant
     * @param constType1  the first TypeConstant to intersect
     * @param constType2  the second TypeConstant to intersect
     */
    public IntersectionTypeConstant(ConstantPool pool, TypeConstant constType1, TypeConstant constType2)
        {
        super(pool);

        if (constType1 == null || constType2 == null)
            {
            throw new IllegalArgumentException("types required");
            }

        m_constType1 = constType1;
        m_constType2 = constType2;
        }


    // ----- type-specific functionality -----------------------------------------------------------

    /**
     * @return the first TypeConstant
     */
    public TypeConstant getType1()
        {
        return m_constType1;
        }

    /**
     * @return the second TypeConstant
     */
    public TypeConstant getType2()
        {
        return m_constType2;
        }


    // ----- Constant methods ----------------------------------------------------------------------

    @Override
    public Format getFormat()
        {
        return Format.UnionType;
        }

    @Override
    protected int compareDetails(Constant that)
        {
        int n = this.m_constType1.compareTo(((IntersectionTypeConstant) that).m_constType1);
        if (n == 0)
            {
            n = this.m_constType2.compareTo(((IntersectionTypeConstant) that).m_constType2);
            }
        return n;
        }

    @Override
    public String getValueString()
        {
        return m_constType1.getValueString() + " | " + m_constType2.getValueString();
        }


    // ----- XvmStructure methods ------------------------------------------------------------------

    @Override
    protected void disassemble(DataInput in)
            throws IOException
        {
        m_constType1 = (TypeConstant) getConstantPool().getConstant(m_iType1);
        m_constType2 = (TypeConstant) getConstantPool().getConstant(m_iType2);
        }

    @Override
    protected void registerConstants(ConstantPool pool)
        {
        m_constType1 = (TypeConstant) pool.register(m_constType1);
        m_constType2 = (TypeConstant) pool.register(m_constType2);
        }

    @Override
    protected void assemble(DataOutput out)
            throws IOException
        {
        out.writeByte(getFormat().ordinal());
        writePackedLong(out, indexOf(m_constType1));
        writePackedLong(out, indexOf(m_constType2));
        }


    // ----- Object methods ------------------------------------------------------------------------

    @Override
    public int hashCode()
        {
        return "+".hashCode() ^ m_constType1.hashCode() ^ m_constType2.hashCode();
        }

    @Override
    public boolean equals(Object obj)
        {
        if (this == obj)
            {
            return true;
            }

        if (obj instanceof IntersectionTypeConstant)
            {
            IntersectionTypeConstant that = (IntersectionTypeConstant) obj;
            // note: order is (unfortunately) important, because of the potential for using a type
            // as the basis for determining a default value, e.g. "null" for a "Nullable | AnyType":
            // return this.m_constType1.equals(that.m_constType1)
            //         ? this.m_constType2.equals(that.m_constType2)
            //         : this.m_constType1.equals(that.m_constType2) && this.m_constType2.equals(that.m_constType1);
            return this.m_constType1.equals(that.m_constType1)
                    && this.m_constType2.equals(that.m_constType2);
            }

        return false;
        }


    // ----- fields --------------------------------------------------------------------------------

    /**
     * During disassembly, this holds the index of the first type constant.
     */
    private int m_iType1;

    /**
     * During disassembly, this holds the index of the second type constant.
     */
    private int m_iType2;

    /**
     * The first type referred to.
     */
    private TypeConstant m_constType1;

    /**
     * The second type referred to.
     */
    private TypeConstant m_constType2;
    }