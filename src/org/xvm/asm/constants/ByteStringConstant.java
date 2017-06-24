package org.xvm.asm.constants;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;

import static org.xvm.util.Handy.byteArrayToHexString;
import static org.xvm.util.Handy.readMagnitude;
import static org.xvm.util.Handy.writePackedLong;


/**
 * Represent an octet string (string of unsigned 8-bit bytes) constant.
 */
public class ByteStringConstant
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
    public ByteStringConstant(ConstantPool pool, Format format, DataInput in)
            throws IOException
        {
        super(pool);

        int    cb = readMagnitude(in);
        byte[] ab = new byte[cb];
        in.readFully(ab);
        m_abVal = ab;
        }

    /**
     * Construct a constant whose value is an octet string. Note that this constructor does not make
     * a copy of the passed {@code byte[]}.
     *
     * @param pool   the ConstantPool that will contain this Constant
     * @param abVal  the octet string value
     */
    public ByteStringConstant(ConstantPool pool, byte[] abVal)
        {
        super(pool);

        assert abVal != null;
        m_abVal = abVal;
        }


    // ----- type-specific functionality -----------------------------------------------------------

    /**
     * Get the value of the constant.
     *
     * @return  the constant's octet string value as a <tt>byte[]</tt>; the
     *          caller must treat the value as immutable
     */
    public byte[] getValue()
        {
        return m_abVal;
        }


    // ----- Constant methods ----------------------------------------------------------------------

    @Override
    public Format getFormat()
        {
        return Format.ByteString;
        }

    @Override
    protected int compareDetails(Constant that)
        {
        byte[] abThis = this.m_abVal;
        byte[] abThat = ((ByteStringConstant) that).m_abVal;

        int cbThis  = abThis.length;
        int cbThat  = abThat.length;
        for (int of = 0, cb = Math.min(cbThis, cbThat); of < cb; ++of)
            {
            if (abThis[of] != abThat[of])
                {
                return (abThis[of] & 0xFF) - (abThat[of] & 0xFF);
                }
            }
        return cbThis - cbThat;
        }

    @Override
    public String getValueString()
        {
        return byteArrayToHexString(m_abVal);
        }


    // ----- XvmStructure methods ------------------------------------------------------------------

    @Override
    protected void assemble(DataOutput out)
            throws IOException
        {
        out.writeByte(getFormat().ordinal());
        final byte[] ab = m_abVal;
        writePackedLong(out, ab.length);
        out.write(ab);
        }

    @Override
    public String getDescription()
        {
        return "byte-string=" + getValueString();
        }


    // ----- Object methods ------------------------------------------------------------------------

    @Override
    public int hashCode()
        {
        int nHash = m_nHash;
        if (nHash == 0)
            {
            byte[] ab = m_abVal;
            nHash = ab.length;
            for (int of = 0, cb = ab.length, cbInc = Math.max(1, cb >>> 6); of < cb; of += cbInc)
                {
                nHash *= 19 + ab[of];
                }
            m_nHash = nHash;
            }
        return nHash;
        }


    // ----- fields --------------------------------------------------------------------------------

    /**
     * The constant octet string value stored as a <tt>byte[]</tt>.
     */
    private final byte[] m_abVal;

    /**
     * Cached hash code.
     */
    private transient int m_nHash;
    }