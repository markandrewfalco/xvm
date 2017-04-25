package org.xvm.asm.constants;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;

import static org.xvm.util.Handy.appendIntAsHex;
import static org.xvm.util.Handy.quotedChar;
import static org.xvm.util.Handy.readUtf8Char;
import static org.xvm.util.Handy.writeUtf8Char;


/**
 * Represent a unicode character constant.
 */
public class CharConstant
        extends Constant
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
    public CharConstant(ConstantPool pool, Format format, DataInput in)
            throws IOException
        {
        super(pool);
        m_chVal = readUtf8Char(in);
        }

    /**
     * Construct a constant whose value is a unicode character.
     *
     * @param pool   the ConstantPool that will contain this Constant
     * @param chVal  the unicode character value
     */
    public CharConstant(ConstantPool pool, int chVal)
        {
        super(pool);
        m_chVal = chVal;
        }


    // ----- type-specific functionality -----------------------------------------------------------

    /**
     * Get the value of the constant.
     *
     * @return  the constant's unicode character value as an <tt>int</tt>
     */
    public int getValue()
        {
        return m_chVal;
        }


    // ----- Constant methods ----------------------------------------------------------------------

    @Override
    public Format getFormat()
        {
        return Format.Char;
        }

    @Override
    public Object getLocator()
        {
        // character only guarantees that the ASCII characters are cached
        return m_chVal <= 0x7F ? Character.valueOf((char) m_chVal) : null;
        }

    @Override
    protected int compareDetails(Constant that)
        {
        int nThis = this.m_chVal;
        int nThat = ((CharConstant) that).m_chVal;
        return nThis - nThat;
        }

    @Override
    public String getValueString()
        {
        return m_chVal > 0xFFFF
                ? appendIntAsHex(new StringBuilder("\'\\U"), m_chVal).append('\'').toString()
                : quotedChar((char) m_chVal);
        }


    // ----- XvmStructure methods ------------------------------------------------------------------

    @Override
    protected void assemble(DataOutput out)
            throws IOException
        {
        out.writeByte(getFormat().ordinal());
        writeUtf8Char(out, m_chVal);
        }

    @Override
    public String getDescription()
        {
        return "char=" + getValueString();
        }


    // ----- Object methods ------------------------------------------------------------------------

    @Override
    public int hashCode()
        {
        return m_chVal;
        }


    // ----- fields --------------------------------------------------------------------------------

    /**
     * The constant character code-point value stored as an integer.
     */
    private final int m_chVal;
    }
