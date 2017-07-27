package org.xvm.proto;

import org.xvm.asm.ClassStructure;
import org.xvm.asm.Component;
import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.Constants;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.MultiMethodStructure;
import org.xvm.asm.PropertyStructure;

import org.xvm.asm.constants.ClassConstant;
import org.xvm.asm.constants.ClassTypeConstant;
import org.xvm.asm.constants.MethodConstant;
import org.xvm.asm.constants.TypeConstant;
import org.xvm.asm.constants.UnresolvedTypeConstant;

import org.xvm.util.Handy;

import java.util.HashMap;
import java.util.Map;


/**
 * A temporary intermediary between the RT, the ConstantPool and ClassStructure;
 * FOR SIMULATION ONLY
 *
 * @author gg 2017.03.08
 */
public class Adapter
    {
    public final Container f_container;

    // the template composition: name -> the corresponding ClassConstant id
    private Map<String, Integer> m_mapClasses = new HashMap<>();

    public Adapter(Container container)
        {
        f_container = container;
        }

    public int getClassTypeConstId(String sName)
        {
        if (m_mapClasses.containsKey(sName))
            {
            return m_mapClasses.get(sName);
            }

        int ofTypeParam = sName.indexOf('<');
        if (ofTypeParam >= 0)
            {
            String sParam = sName.substring(ofTypeParam + 1, sName.length() - 1);
            String sSimpleName = sName.substring(0, ofTypeParam);

            ClassTypeConstant constType = getClassTypeConstant(sSimpleName);
            if (constType != null)
                {
                String[] asType = Handy.parseDelimitedString(sParam, ',');

                int nTypeId = f_container.f_pool.ensureClassTypeConstant(
                        constType.getClassConstant(), null, getTypeConstants(asType)).getPosition();

                m_mapClasses.put(sName, nTypeId);

                return nTypeId;
                }
            }
        else
            {
            ClassTypeConstant constType = getClassTypeConstant(sName);
            if (constType != null)
                {
                int nTypeId = f_container.f_pool.ensureClassTypeConstant(
                        constType.getClassConstant(), null).getPosition();

                m_mapClasses.put(sName, nTypeId);

                return nTypeId;
                }
            }

        throw new IllegalArgumentException("ClassTypeConstant is not defined: " + sName);
        }

    public int getMethodConstId(String sClassName, String sMethName)
        {
        return getMethodConstId(sClassName, sMethName, null, null);
        }

    // TODO: this will change when MethodIdConst is introduced
    public int getMethodConstId(String sClassName, String sMethName, String[] asArgType, String[] asRetType)
        {
        try
            {
            ClassTemplate template = f_container.f_types.getTemplate(sClassName);
            MethodStructure method = template.getMethod(sMethName, asArgType, asRetType);
            while (method == null)
                {
                template = template.getSuper();
                method = template.getMethod(sMethName, asArgType, asRetType);
                }
            return method.getIdentityConstant().getPosition();
            }
        catch (NullPointerException e)
            {
            throw new IllegalArgumentException("Method is not defined: " + sClassName + '#' + sMethName);
            }
        }

    public int getPropertyConstId(String sClassName, String sPropName)
        {
        try
            {
            return f_container.f_types.getTemplate(sClassName).getProperty(sPropName).
                    getIdentityConstant().getPosition();
            }
        catch (NullPointerException e)
            {
            throw new IllegalArgumentException("Property is not defined: " + sClassName + '#' + sPropName);
            }
        }

    public int ensureValueConstantId(Object oValue)
        {
        return ensureValueConstant(oValue).getPosition();
        }

    private TypeConstant[] getTypeConstants(String[] asType)
        {
        ConstantPool pool = f_container.f_pool;
        int cTypes = asType.length;
        TypeConstant[] aType = new TypeConstant[cTypes];
        for (int i = 0; i < cTypes; i++)
            {
            String sType = asType[i].trim();
            aType[i] = (ClassTypeConstant) pool.getConstant(getClassTypeConstId(sType));
            }
        return aType;
        }

    private ClassTypeConstant getClassTypeConstant(String sClassName)
        {
        ClassConstant constClass = (ClassConstant)
                f_container.f_types.getTemplate(sClassName).f_struct.getIdentityConstant();
        return constClass.asTypeConstant();
        }

    protected Constant ensureValueConstant(Object oValue)
        {
        if (oValue instanceof Integer || oValue instanceof Long)
            {
            return f_container.f_pool.ensureIntConstant(((Number) oValue).longValue());
            }

        if (oValue instanceof String)
            {
            return f_container.f_pool.ensureCharStringConstant((String) oValue);
            }

        if (oValue instanceof Character)
            {
            return f_container.f_pool.ensureCharConstant(((Character) oValue).charValue());
            }

        if (oValue instanceof Boolean)
            {
            return getClassTypeConstant(
                    ((Boolean) oValue).booleanValue() ?
                            "Boolean.True" : "Boolean.False");
            }

        if (oValue instanceof Object[])
            {
            Object[] ao = (Object[]) oValue;
            int c = ao.length;
            Constant[] aconst = new Constant[c];
            for (int i = 0; i < c; i++)
                {
                aconst[i] = ensureValueConstant(ao[i]);
                }
            return f_container.f_pool.ensureTupleConstant(aconst);
            }

        if (oValue == null)
            {
            return getClassTypeConstant("Nullable.Null");
            }

        throw new IllegalArgumentException();
        }

    public MethodStructure addMethod(Component structure, String sName,
                                            String[] asArgType, String[] asRetType)
        {
        MultiMethodStructure mms = structure.ensureMultiMethodStructure(sName);
        return mms.createMethod(false, Constants.Access.PUBLIC,
                getTypeConstants(asRetType), getTypeConstants(asArgType));
        }

    public int getScopeCount(MethodStructure method)
        {
        ClassTemplate.MethodTemplate tm = getMethodTemplate(method);
        return tm == null ? 1 : tm.m_cScopes;
        }

    public static int getArgCount(MethodStructure method)
        {
        MethodConstant constMethod = method.getIdentityConstant();
        return constMethod.getRawParams().length;
        }

    public int getVarCount(MethodStructure method)
        {
        ClassTemplate.MethodTemplate tm = getMethodTemplate(method);
        return tm == null || tm.m_fNative ? // this can only be a constructor
                method.getIdentityConstant().getRawParams().length:
                tm.m_cVars;
        }

    public Op[] getOps(MethodStructure method)
        {
        ClassTemplate.MethodTemplate tm = getMethodTemplate(method);
        return tm == null ? null : tm.m_aop;
        }

    public MethodStructure getFinalizer(MethodStructure constructor)
        {
        ClassTemplate.MethodTemplate tmConstruct = getMethodTemplate(constructor);
        ClassTemplate.MethodTemplate tmFinally = tmConstruct == null ? null : tmConstruct.m_mtFinally;
        return tmFinally == null ? null : tmFinally.f_struct;
        }

    public boolean isNative(MethodStructure method)
        {
        ClassTemplate.MethodTemplate tm = getMethodTemplate(method);
        return tm != null && tm.m_fNative;
        }

    private ClassTemplate.MethodTemplate getMethodTemplate(MethodStructure method)
        {
        MultiMethodStructure mms = (MultiMethodStructure) method.getParent();
        Component container = mms.getParent();

        // the container is either a class or a property
        ClassStructure clazz = (ClassStructure) (container instanceof ClassStructure ? container : container.getParent());
        ClassTemplate template = f_container.f_types.getTemplate(clazz.getIdentityConstant());
        return template.getMethodTemplate(method.getIdentityConstant());
        }

    public static MethodStructure getGetter(PropertyStructure property)
        {
        MultiMethodStructure mms = (MultiMethodStructure) property.getChild("get");

        // TODO: use the type
        return mms == null ? null : (MethodStructure) mms.children().get(0);
        }

    public static MethodStructure getSetter(PropertyStructure property)
        {
        MultiMethodStructure mms = (MultiMethodStructure) property.getChild("set");

        // TODO: use the type
        return mms == null ? null : (MethodStructure) mms.children().get(0);
        }

    public ClassTypeConstant resolveType(PropertyStructure property)
        {
        TypeConstant constType = property.getType();

        if (constType instanceof UnresolvedTypeConstant)
            {
            constType = ((UnresolvedTypeConstant) constType).getResolvedConstant();
            }

        if (constType instanceof ClassTypeConstant)
            {
            return (ClassTypeConstant) constType;
            }

        throw new UnsupportedOperationException("Unsupported type: " + constType + " for " + property);
        }
    }