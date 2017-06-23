package org.xvm.proto.op;

import org.xvm.proto.Frame;
import org.xvm.proto.Op;

/**
 * END_HANDLER rel-addr ; finish an exception handler with a jump
 *
 * @author gg 2017.03.08
 */
public class FinallyEnd extends Op
    {
    private final int f_nRelAddr;

    public FinallyEnd(int iRelAddr)
        {
        f_nRelAddr = iRelAddr;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        frame.exitScope();

        return iPC + f_nRelAddr;
        }
    }