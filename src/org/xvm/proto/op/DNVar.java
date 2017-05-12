package org.xvm.proto.op;

import org.xvm.asm.constants.CharStringConstant;
import org.xvm.proto.Frame;
import org.xvm.proto.Op;
import org.xvm.proto.ServiceContext;

import org.xvm.proto.template.xRef.RefHandle;

/**
 * DNVAR CONST_REF_CLASS, CONST_STRING ; next register is a named "dynamic reference" variable
 *
 * @author gg 2017.03.08
 */
public class DNVar extends Op
    {
    final private int f_nClassConstId;
    final private int f_nNameConstId;

    public DNVar(int nClassConstId, int nNameConstId)
        {
        f_nClassConstId = nClassConstId;
        f_nNameConstId = nNameConstId;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        int iScope   = frame.f_aiIndex[I_SCOPE];
        int nNextVar = frame.f_anNextVar[iScope];

        ServiceContext context = frame.f_context;

        RefHandle hRef = (RefHandle) context.f_heapGlobal.ensureHandle(f_nClassConstId);

        CharStringConstant constName =
                (CharStringConstant) context.f_constantPool.getConstantValue(f_nNameConstId);

        frame.introduceVar(nNextVar, hRef.f_clazz, constName.getValue(), VAR_DYNAMIC, hRef);

        frame.f_anNextVar[iScope] = nNextVar + 1;

        return iPC + 1;
        }
    }
