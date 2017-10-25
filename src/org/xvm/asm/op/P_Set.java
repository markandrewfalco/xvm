package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.OpProperty;

import org.xvm.asm.constants.PropertyConstant;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;
import org.xvm.runtime.Utils;


import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * P_SET PROPERTY, rvalue-target, rvalue
 */
public class P_Set
        extends OpProperty
    {
    /**
     * Construct a P_SET op.
     *
     * @param nPropId  the property to set
     * @param nTarget  the target object
     * @param nValue   the value to store in the property
     *
     * @deprecated
     */
    public P_Set(int nPropId, int nTarget, int nValue)
        {
        super(null);

        m_nPropId = nPropId;
        m_nTarget = nTarget;
        m_nValue = nValue;
        }

    /**
     * Construct a P_SET op based on the specified arguments.
     *
     * @param argProperty  the property Argument
     * @param argTarget    the target Argument
     * @param argValue     the value Argument
     */
    public P_Set(Argument argProperty, Argument argTarget, Argument argValue)
        {
        super(argProperty);

        m_argTarget = argTarget;
        m_argValue = argValue;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public P_Set(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(in, aconst);

        m_nTarget = readPackedInt(in);
        m_nValue = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out, ConstantRegistry registry)
            throws IOException
        {
        if (m_argTarget != null)
            {
            m_nTarget = encodeArgument(m_argTarget, registry);
            m_nValue = encodeArgument(m_argValue, registry);
            }

        writePackedLong(out, m_nTarget);
        writePackedLong(out, m_nValue);
        }

    @Override
    public int getOpCode()
        {
        return OP_P_SET;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            ObjectHandle hTarget = frame.getArgument(m_nTarget);
            ObjectHandle hValue = frame.getArgument(m_nValue);
            if (hTarget == null || hValue == null)
                {
                return R_REPEAT;
                }

            PropertyConstant constProperty = (PropertyConstant)
                    frame.f_context.f_pool.getConstant(m_nPropId);
            String sProperty = constProperty.getName();

            if (isProperty(hTarget) || isProperty(hValue))
                {
                ObjectHandle[] ahArg = new ObjectHandle[] {hTarget, hValue};
                Frame.Continuation stepNext = frameCaller ->
                    ahArg[0].f_clazz.f_template.setPropertyValue(
                        frame, ahArg[0], sProperty, ahArg[1]);

                return new Utils.GetArgument(ahArg, stepNext).doNext(frame);
                }

            return hTarget.f_clazz.f_template.setPropertyValue(
                    frame, hTarget, sProperty, hValue);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }

    @Override
    public void registerConstants(ConstantRegistry registry)
        {
        super.registerConstants(registry);

        registerArgument(m_argTarget, registry);
        registerArgument(m_argValue, registry);
        }

    private int m_nTarget;
    private int m_nValue;

    private Argument m_argTarget;
    private Argument m_argValue;
    }