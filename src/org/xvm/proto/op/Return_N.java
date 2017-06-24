package org.xvm.proto.op;

import org.xvm.proto.Frame;
import org.xvm.proto.Op;

/**
 * RETURN_N #vals:(rvalue)
 *
 * @author gg 2017.03.08
 */
public class Return_N extends Op
    {
    private final int[] f_anArgValue;

    public Return_N(int[] anValue)
        {
        f_anArgValue = anValue;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        int iRet = frame.f_iReturn;
        if (iRet >= 0 || iRet == Frame.RET_LOCAL)
            {
            throw new IllegalStateException(); // assertion
            }

        switch (iRet)
            {
            case Frame.RET_UNUSED:
                break;

            case Frame.RET_MULTI:
                int[] aiRet = frame.f_aiReturn;

                // it's possible that the caller doesn't care about some of the return values
                boolean fBlock = false;
                for (int i = 0, c = aiRet.length; i < c; i++)
                    {
                    int iResult = frame.returnValue(aiRet[i], f_anArgValue[i]);
                    switch (iResult)
                        {
                        case Op.R_EXCEPTION:
                            return Op.R_RETURN_EXCEPTION;

                        case Op.R_BLOCK:
                            fBlock = true;
                            break;
                        }
                    }

                if (fBlock)
                    {
                    return R_BLOCK_RETURN;
                    }
                break;

            default:
                // the caller needs a tuple
                return frame.returnTuple(-iRet - 1, f_anArgValue);
            }
        return R_RETURN;
        }
    }