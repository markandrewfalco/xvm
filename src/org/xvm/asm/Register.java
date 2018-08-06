package org.xvm.asm;


import org.xvm.asm.constants.TypeConstant;


/**
 * A Register represents a specific, typed, machine register of the XVM.
 */
public class Register
        implements Argument
    {
    // ----- constructors --------------------------------------------------------------------------

    /**
     * Construct a Register of the specified type.
     *
     * @param type  the TypeConstant specifying the Register type
     */
    public Register(TypeConstant type)
        {
        this(type, UNKNOWN);
        }

    /**
     * Construct a Register of the specified type.
     *
     * @param type  the TypeConstant specifying the Register type
     * @param iArg  the argument index, which is either a pre-defined argument index, or a register
     *              ID
     */
    public Register(TypeConstant type, int iArg)
        {
        if (type == null && iArg != Op.A_IGNORE)
            {
            throw new IllegalArgumentException("type required");
            }

        validateIndex(iArg);

        m_type = type;
        m_iArg = iArg;
        m_fRO  = isPredefinedReadonly(iArg);
        }


    // ----- Argument methods ----------------------------------------------------------------------

    /**
     * Determine the type of the value that the Register holds.
     *
     * @return the Register's type
     */
    @Override
    public TypeConstant getType()
        {
        return m_type;
        }

    @Override
    public boolean isStack()
        {
        return m_iArg == Op.A_STACK;
        }

    @Override
    public Register registerConstants(Op.ConstantRegistry registry)
        {
        if (m_typeReg != null)
            {
            m_typeReg = (TypeConstant) registry.register(m_typeReg);
            }
        else if (m_type != null)
            {
            m_type = (TypeConstant) registry.register(m_type);
            }
        return this;
        }


    // ----- Register methods ----------------------------------------------------------------------

    /**
     * @return true iff this register was created by a DVAR op
     */
    public boolean isDVar()
        {
        return m_typeReg != null;
        }

    public TypeConstant ensureRegType(boolean fRO)
        {
        assert fRO | !m_fRO;

        if (m_typeReg != null)
            {
            return m_typeReg;
            }

        ConstantPool pool = m_type.getConstantPool();
        return pool.ensureParameterizedTypeConstant(fRO ? pool.typeRef() : pool.typeVar(), m_type);
        }

    /**
     * Specify the type of the register itself.
     *
     * @param typeReg  the type of the register (something that "is a" Ref)
     */
    public void specifyRegType(TypeConstant typeReg)
        {
        assert m_typeReg == null;
        assert typeReg != null && typeReg.isA(typeReg.getConstantPool().typeRef());

        this.m_typeReg = typeReg;
        if (!typeReg.isA(typeReg.getConstantPool().typeVar()))
            {
            m_fRO = true;
            }
        }

    /**
     * Create a register that is collocated with this register, but narrows its type.
     */
    public Register narrowType(TypeConstant typeNarrowed)
        {
        assert typeNarrowed.isA(m_type);

        return new ShadowRegister(typeNarrowed);
        }

    /**
     * @return the argument index for the Register
     */
    public int getIndex()
        {
        if (m_iArg == UNKNOWN)
            {
            throw new IllegalStateException("index has not been assigned!");
            }

        return m_iArg;
        }

    /**
     * Assign an argument index.
     *
     * @param iArg a valid argument index
     */
    public int assignIndex(int iArg)
        {
        if (m_iArg != UNKNOWN)
            {
            throw new IllegalStateException("index has already been assigned (old=" + m_iArg + ", new=" + iArg);
            }

        validateIndex(iArg);
        return m_iArg = iArg;
        }

    /**
     * Verify that the specified argument index is valid.
     *
     * @param iReg  the argument index
     *
     * @throws IllegalArgumentException if the index is invalid
     */
    protected static void validateIndex(int iReg)
        {
        if (!(iReg >= 0 || iReg == UNKNOWN || isPredefinedRegister(iReg)))
            {
            throw new IllegalArgumentException("invalid register ID: " + iReg);
            }
        }

    /**
     * Determine if the specified argument index is for a pre-defined register.
     *
     * @param iArg  the argument index
     *
     * @return true iff the index specifies a pre-defined argument
     */
    protected static boolean isPredefinedRegister(int iArg)
        {
        switch (iArg)
            {
            case Op.A_STACK:
            case Op.A_IGNORE:
            case Op.A_PUBLIC:
            case Op.A_PROTECTED:
            case Op.A_PRIVATE:
            case Op.A_TARGET:
            case Op.A_STRUCT:
            case Op.A_SERVICE:
            case Op.A_MODULE:
            case Op.A_TYPE:
            case Op.A_SUPER:
            case Op.A_THIS:
                return true;

            default:
                if (iArg < 0)
                    {
                    throw new IllegalArgumentException("illegal argument index: " + iArg);
                    }
                return false;
            }
        }

    /**
     * Determine if the specified argument index is for a pre-defined read-only register.
     *
     * @param iArg  the argument index
     *
     * @return true iff the index specifies a pre-defined argument that is in a read-only register
     */
    protected static boolean isPredefinedReadonly(int iArg)
        {
        switch (iArg)
            {
            case Op.A_PUBLIC:
            case Op.A_PROTECTED:
            case Op.A_PRIVATE:
            case Op.A_TARGET:
            case Op.A_STRUCT:
            case Op.A_SERVICE:
            case Op.A_MODULE:
            case Op.A_TYPE:
            case Op.A_SUPER:
            case Op.A_THIS:
                return true;

            default:
            case Op.A_STACK:
            case Op.A_IGNORE:
                return false;
            }
        }

    /**
     * @return true iff the register is a pre-defined argument
     */
    public boolean isPredefined()
        {
        return m_iArg < 0 && m_iArg != UNKNOWN;
        }

    /**
     * @return true iff the register represents "this:target" pre-defined argument
     */
    public boolean isTarget()
        {
        return m_iArg == Op.A_TARGET;
        }

    /**
     * Determine if this register has an "unknown" index. This is used to indicate a "next"
     * index.
     *
     * @return true iff this register has an "unknown" index
     */
    public boolean isUnknown()
        {
        return m_iArg == UNKNOWN;
        }

    /**
     * Determine if this register is readable. This is equivalent to the Ref for the register
     * supporting the get() operation.
     *
     * @return true iff this register is readable
     */
    public boolean isReadable()
        {
        return m_iArg != Op.A_IGNORE;
        }

    /**
     * Force the register to be treated as effectively final from this point forward.
     */
    public void markEffectivelyFinal()
        {
        m_fRO               = true;
        m_fEffectivelyFinal = true;
        }

    /**
     * Determine if this register is writable. This is equivalent to the Ref for the register
     * supporting the set() operation.
     *
     * @return true iff this register is writable
     */
    public boolean isWritable()
        {
        return !m_fRO;
        }

    public boolean isEffectivelyFinal()
        {
        return m_fEffectivelyFinal;
        }

    /**
     * @return true iff this is a normal (not D_VAR), readable and writable, local variable
     */
    public boolean isNormal()
        {
        return !isPredefined() && isReadable() && isWritable() && !isDVar();
        }

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        if (m_type != null)
            {
            sb.append(m_type.getValueString())
              .append(' ');
            }

        if (m_fEffectivelyFinal)
            {
            sb.append("@Final ");
            }
        else if (m_fRO)
            {
            sb.append("@RO ");
            }

        sb.append(getIdString());

        return sb.toString();
        }

    /**
     * @return a String that denotes the identity of the register, for debugging purposes
     */
    public String getIdString()
        {
        return m_iArg == UNKNOWN
                ? "#? (@" + System.identityHashCode(this) + ")"
                : getIdString(m_iArg);
        }

    /**
     * Format the register identifier into a String
     *
     * @param nReg  the register number
     *
     * @return an identity String, for debugging purposes
     */
    public static String getIdString(int nReg)
        {
        switch (nReg)
            {
            case Op.A_STACK:
                return "this:stack";

            case Op.A_IGNORE:
                return "_";

            case Op.A_TARGET:
                return "this:target";

            case Op.A_PUBLIC:
                return "this:public";

            case Op.A_PROTECTED:
                return "this:protected";

            case Op.A_PRIVATE:
                return "this:private";

            case Op.A_STRUCT:
                return "this:struct";

            case Op.A_SERVICE:
                return "this:service";

            case Op.A_SUPER:
                return "super";

            case UNKNOWN:
                return "#???";

            default:
                return "#" + nReg;
            }
        }


    // ----- inner classes -------------------------------------------------------------------------

    /**
     * A register that represents the underlying (base) register, but overrides its type.
     */
    private class ShadowRegister
            extends Register
        {
        /**
         * Create a ShadowRegister of the specified type.
         *
         * @param typeNew  the overriding type
         */
        protected ShadowRegister(TypeConstant typeNew)
            {
            super(typeNew);
            }

        @Override
        public boolean isStack()
            {
            return Register.this.isStack();
            }

        @Override
        public boolean isDVar()
            {
            return Register.this.isDVar();
            }

        @Override
        public TypeConstant ensureRegType(boolean fRO)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void specifyRegType(TypeConstant typeReg)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public int getIndex()
            {
            return Register.this.getIndex();
            }

        @Override
        public int assignIndex(int iArg)
            {
            return Register.this.assignIndex(iArg);
            }

        @Override
        public boolean isPredefined()
            {
            return Register.this.isPredefined();
            }

        @Override
        public boolean isTarget()
            {
            return Register.this.isTarget();
            }

        @Override
        public boolean isUnknown()
            {
            return Register.this.isUnknown();
            }

        @Override
        public boolean isReadable()
            {
            return Register.this.isReadable();
            }

        @Override
        public boolean isWritable()
            {
            return Register.this.isWritable();
            }

        @Override
        public boolean isEffectivelyFinal()
            {
            return Register.this.isEffectivelyFinal();
            }

        @Override
        public boolean isNormal()
            {
            return Register.this.isNormal();
            }

        @Override
        public String getIdString()
            {
            return " shadow of " + Register.this.toString();
            }
        }

    // ----- inner class: Assignment ---------------------------------------------------------------

    /**
     * The Assignment enumeration represents the various possible states of a variable's assignment.
     * <p/>
     * Specifically:
     * <ul>
     * <li>If the variable is known to be definitely unassigned;</li>
     * <li>If the variable is known to be definitely assigned;</li>
     * <li>If the variable is known to be effectively final.</li>
     * </ul>
     */
    public enum Assignment
        {
        //           def    def    eff
        //           unasn  asn    final
        //           -----  -----  -----
        Unassigned  (true , false, false),      // definitely unassigned
        UnknownOnce (false, false, true ),      // not definitely assigned; effectively final
        UnknownMulti(false, false, false),      // not definitely assigned
        Assigned    (false, true , false),      // definitely assigned
        AssignedOnce(false, true , true );      // definitely assigned; effectively final

        private Assignment(boolean fUnassigned, boolean fAssigned, boolean fExactlyOnce)
            {
            assert !(fAssigned & fUnassigned);

            this.fUnassigned  = fUnassigned;
            this.fAssigned    = fAssigned;
            this.fExactlyOnce = fExactlyOnce;
            }

        public boolean isDefinitelyUnassigned()
            {
            return fUnassigned;
            }

        public boolean isDefinitelyAssigned()
            {
            return fAssigned;
            }

        public boolean isEffectivelyFinal()
            {
            return fExactlyOnce;
            }

        public Assignment applyAssignment()
            {
            switch (this)
                {
                case Unassigned:    return AssignedOnce;
                case UnknownOnce:   return Assigned;
                case UnknownMulti:  return Assigned;
                case Assigned:      return Assigned;
                case AssignedOnce:  return Assigned;

                default:
                    throw new IllegalStateException();
                }
            }

        /**
         * Apply a potentially asynchronous assignment that occurs from a lambda or anonymous inner
         * cass via a variable capture. Because the assignment is potentially asynchronous, its
         * outcome may be indeterminate.
         *
         * @return the resulting Assignment state
         */
        public Assignment applyAssignmentFromCapture()
            {
            switch (this)
                {
                case Unassigned:    return UnknownMulti;
                case UnknownOnce:   return UnknownMulti;
                case UnknownMulti:  return UnknownMulti;
                case Assigned:      return Assigned;
                case AssignedOnce:  return Assigned;

                default:
                    throw new IllegalStateException();
                }
            }

        // TODO? public Assignment applyJoin(Assignment... )

        private final boolean fUnassigned;
        private final boolean fAssigned;
        private final boolean fExactlyOnce;
        }


    // ----- fields --------------------------------------------------------------------------------

    /**
     * A singleton "black hole" register.
     */
    public static final Register IGNORE = new Register(null, Op.A_IGNORE);

    /**
     * Empty array of registers.
     */
    public static final Register[] NO_REGS = new Register[0];

    /**
     * A reserved argument index that represents an unknown or otherwise unassigned index.
     */
    public static final int UNKNOWN = Integer.MIN_VALUE;

    /**
     * The type of the value that will be held in the register.
     */
    private TypeConstant m_type;

    /**
     * The type of the register itself (typically null).
     */
    private TypeConstant m_typeReg;

    /**
     * The register ID (>=0), or the pre-defined argument identifier in the range -1 to -16.
     */
    private int m_iArg;

    /**
     * Read-only flag.
     */
    private boolean m_fRO;

    /**
     * Effectively final flag.
     */
    private boolean m_fEffectivelyFinal;
    }
