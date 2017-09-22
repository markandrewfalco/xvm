package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;

import org.xvm.asm.Constant;
import org.xvm.asm.Op;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.Type;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.TypeSet;

import org.xvm.runtime.template.collections.xTuple;
import org.xvm.runtime.template.collections.xTuple.TupleHandle;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * TVAR #values:(TYPE_CONST, rvalue-src) ; next register is an initialized anonymous Tuple variable
 */
public class TVar
        extends Op
    {
    final private int[] f_anClassConstId;
    final private int[] f_anArgValue;

    public TVar(int[] anClassConstId, int[] anValue)
        {
        f_anClassConstId = anClassConstId;
        f_anArgValue = anValue;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public TVar(DataInput in, Constant[] aconst)
            throws IOException
        {
        int c = in.readUnsignedByte();

        f_anClassConstId = new int[c];
        f_anArgValue = new int[c];
        for (int i = 0; i < c; i++)
            {
            f_anClassConstId[i] = readPackedInt(in);
            f_anArgValue[i] = readPackedInt(in);
            }
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_TVAR);

        int c = f_anArgValue.length;
        out.write(c);
        for (int i = 0; i < c; i++)
            {
            writePackedLong(out, f_anClassConstId[i]);
            writePackedLong(out, f_anArgValue[i]);
            }
        }

    @Override
    public int getOpCode()
        {
        return OP_TVAR;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        int[] anClassId = f_anClassConstId;

        int cArgs = anClassId.length;
        assert cArgs == f_anArgValue.length;

        try
            {
            ObjectHandle[] ahArg = frame.getArguments(f_anArgValue, cArgs);
            if (ahArg == null)
                {
                return R_REPEAT;
                }

            TypeSet types = frame.f_context.f_types;
            Map<String, Type> mapActual = frame.getActualTypes();

            Type[] aType = new Type[cArgs];
            for (int i = 0; i < cArgs; i++)
                {
                TypeComposition clazz = types.ensureComposition(anClassId[i], mapActual);

                aType[i] = clazz.ensurePublicType();
                }

            TupleHandle hTuple = xTuple.makeHandle(aType, ahArg);

            frame.introduceVar(hTuple.f_clazz, null, Frame.VAR_STANDARD, hTuple);

            return iPC + 1;
            }
        catch (ObjectHandle.ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }

    }
