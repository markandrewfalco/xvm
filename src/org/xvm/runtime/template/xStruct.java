package org.xvm.runtime.template;


import org.xvm.asm.ClassStructure;

import org.xvm.runtime.ClassTemplate;
import org.xvm.runtime.TypeSet;


/**
 * TODO:
 *
 * @author gg 2017.02.27
 */
public class  xStruct
        extends ClassTemplate
    {
    public xStruct(TypeSet types, ClassStructure structure, boolean fInstance)
        {
        super(types, structure);
        }

    @Override
    public void initDeclared()
        {
        }
    }