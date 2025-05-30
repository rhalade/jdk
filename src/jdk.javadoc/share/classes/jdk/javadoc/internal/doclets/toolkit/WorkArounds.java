/*
 * Copyright (c) 2015, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.javadoc.internal.doclets.toolkit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import jdk.javadoc.internal.doclets.toolkit.util.Utils;
import jdk.javadoc.internal.tool.ToolEnvironment;
import jdk.javadoc.internal.tool.DocEnvImpl;

import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;

import static javax.lang.model.element.ElementKind.*;

/**
 * A quarantine class to isolate all the workarounds and bridges to
 * a locality. This class should eventually disappear once all the
 * standard APIs support the needed interfaces.
 */
public class WorkArounds {

    public final BaseConfiguration configuration;
    public final ToolEnvironment toolEnv;
    public final Utils utils;
    public final Elements elementUtils;
    public final Types typeUtils;
    public final com.sun.tools.javac.code.Types javacTypes;

    public WorkArounds(BaseConfiguration configuration) {
        this.configuration = configuration;
        this.utils = this.configuration.utils;

        elementUtils = configuration.docEnv.getElementUtils();
        typeUtils = configuration.docEnv.getTypeUtils();

        // Note: this one use of DocEnvImpl is what prevents us tunnelling extra
        // info from a doclet to its taglets via a doclet-specific subtype of
        // DocletEnvironment.
        toolEnv = ((DocEnvImpl)this.configuration.docEnv).toolEnv;
        javacTypes = toolEnv.getTypes();
    }

    /*
     * TODO: This method exists because of a bug in javac which does not
     *       handle "@deprecated tag in package-info.java", when this issue
     *       is fixed this method and its uses must be jettisoned.
     */
    public boolean isDeprecated0(Element e) {
        if (!utils.getDeprecatedTrees(e).isEmpty()) {
            return true;
        }
        TypeMirror deprecatedType = utils.getDeprecatedType();
        for (AnnotationMirror anno : e.getAnnotationMirrors()) {
            if (typeUtils.isSameType(anno.getAnnotationType().asElement().asType(), deprecatedType))
                return true;
        }
        return false;
    }

    public boolean isMandated(AnnotationMirror aDesc) {
        return elementUtils.getOrigin(null, aDesc) == Elements.Origin.MANDATED;
    }

    // TODO: DocTrees: Trees.getPath(Element e) is slow a factor 4-5 times.
    public Map<Element, TreePath> getElementToTreePath() {
        return toolEnv.elementToTreePath;
    }

    // TODO: implement in either jx.l.m API (preferred) or DocletEnvironment.
    FileObject getJavaFileObject(PackageElement packageElement) {
        return ((PackageSymbol)packageElement).sourcefile;
    }

    // TODO: jx.l.m ?
    public Location getLocationForModule(ModuleElement mdle) {
        ModuleSymbol msym = (ModuleSymbol)mdle;
        return msym.sourceLocation != null
                ? msym.sourceLocation
                : msym.classLocation;
    }

    //------------------Start of Serializable Implementation---------------------//
    private final Map<TypeElement, NewSerializedForm> serializedForms = new HashMap<>();

    private NewSerializedForm getSerializedForm(TypeElement typeElem) {
        return serializedForms.computeIfAbsent(typeElem,
                te -> new NewSerializedForm(utils, configuration.docEnv.getElementUtils(), te));
    }

    public SortedSet<VariableElement> getSerializableFields(TypeElement typeElem) {
        return getSerializedForm(typeElem).fields;
    }

    public SortedSet<ExecutableElement>  getSerializationMethods(TypeElement typeElem) {
        return getSerializedForm(typeElem).methods;
    }

    public boolean definesSerializableFields(TypeElement typeElem) {
        if (!utils.isSerializable(typeElem) || utils.isExternalizable(typeElem)) {
            return false;
        } else {
            return getSerializedForm(typeElem).definesSerializableFields;
        }
    }

    /* TODO we need a clean port to jx.l.m
     * The serialized form is the specification of a class' serialization state.
     * <p>
     *
     * It consists of the following information:
     * <p>
     *
     * <pre>
     * 1. Whether class is Serializable or Externalizable.
     * 2. Javadoc for serialization methods.
     *    a. For Serializable, the optional readObject, writeObject,
     *       readResolve and writeReplace.
     *       serialData tag describes, in prose, the sequence and type
     *       of optional data written by writeObject.
     *    b. For Externalizable, writeExternal and readExternal.
     *       serialData tag describes, in prose, the sequence and type
     *       of optional data written by writeExternal.
     * 3. Javadoc for serialization data layout.
     *    a. For Serializable, the name,type and description
     *       of each Serializable fields.
     *    b. For Externalizable, data layout is described by 2(b).
     * </pre>
     *
     */
    static class NewSerializedForm {

        final Utils utils;
        final Elements elements;

        final SortedSet<ExecutableElement> methods;

        /* List of FieldDocImpl - Serializable fields.
         * Singleton list if class defines Serializable fields explicitly.
         * Otherwise, list of default serializable fields.
         * 0 length list for Externalizable.
         */
        final SortedSet<VariableElement> fields;

        /* True if class specifies serializable fields explicitly.
         * using special static member, serialPersistentFields.
         */
        boolean definesSerializableFields = false;

        // Specially treated field/method names defined by Serialization.
        private static final String SERIALIZABLE_FIELDS = "serialPersistentFields";
        private static final String READOBJECT = "readObject";
        private static final String WRITEOBJECT = "writeObject";
        private static final String READRESOLVE = "readResolve";
        private static final String WRITEREPLACE = "writeReplace";
        private static final String READOBJECTNODATA = "readObjectNoData";

        NewSerializedForm(Utils utils, Elements elements, TypeElement te) {
            this.utils = utils;
            this.elements = elements;
            methods = new TreeSet<>(utils.comparators.generalPurposeComparator());
            fields = new TreeSet<>(utils.comparators.generalPurposeComparator());
            if (utils.isExternalizable(te)) {
                /* look up required public accessible methods,
                 *   writeExternal and readExternal.
                 */
                String[] readExternalParamArr = {"java.io.ObjectInput"};
                String[] writeExternalParamArr = {"java.io.ObjectOutput"};

                ExecutableElement md = findMethod(te, "readExternal", Arrays.asList(readExternalParamArr));
                if (md != null) {
                    methods.add(md);
                }
                md = findMethod(te, "writeExternal", Arrays.asList(writeExternalParamArr));
                if (md != null) {
                    methods.add(md);
                }
            } else if (utils.isSerializable(te)) {
                VarSymbol dsf = getDefinedSerializableFields((ClassSymbol) te);
                if (dsf != null) {
                    /* Define serializable fields with array of ObjectStreamField.
                     * Each ObjectStreamField should be documented by a
                     * serialField tag.
                     */
                    definesSerializableFields = true;
                    fields.add(dsf);
                } else {

                    /* Calculate default Serializable fields as all
                     * non-transient, non-static fields.
                     * Fields should be documented by serial tag.
                     */
                    computeDefaultSerializableFields((ClassSymbol) te);
                }

                /* Check for optional customized readObject, writeObject,
                 * readResolve and writeReplace, which can all contain
                 * the serialData tag.        */
                addMethodIfExist((ClassSymbol) te, READOBJECT);
                addMethodIfExist((ClassSymbol) te, WRITEOBJECT);
                addMethodIfExist((ClassSymbol) te, READRESOLVE);
                addMethodIfExist((ClassSymbol) te, WRITEREPLACE);
                addMethodIfExist((ClassSymbol) te, READOBJECTNODATA);
            }
        }

        private VarSymbol getDefinedSerializableFields(ClassSymbol def) {
            Names names = def.name.table.names;

            /* SERIALIZABLE_FIELDS can be private,
             */
            for (Symbol sym : def.members().getSymbolsByName(names.fromString(SERIALIZABLE_FIELDS))) {
                if (sym.kind == VAR) {
                    VarSymbol f = (VarSymbol) sym;
                    if ((f.flags() & Flags.STATIC) != 0
                            && (f.flags() & Flags.PRIVATE) != 0) {
                        return f;
                    }
                }
            }
            return null;
        }

        /*
         * Catalog Serializable method if it exists in current ClassSymbol.
         * Do not look for method in superclasses.
         *
         * Serialization requires these methods to be non-static.
         *
         * @param method should be an unqualified Serializable method
         *               name either READOBJECT, WRITEOBJECT, READRESOLVE
         *               or WRITEREPLACE.
         * @param visibility the visibility flag for the given method.
         */
        private void addMethodIfExist(ClassSymbol def, String methodName) {
            Names names = def.name.table.names;

            for (Symbol sym : def.members().getSymbolsByName(names.fromString(methodName))) {
                if (sym.kind == MTH) {
                    MethodSymbol md = (MethodSymbol) sym;
                    if ((md.flags() & Flags.STATIC) == 0) {
                        /*
                         * WARNING: not robust if unqualifiedMethodName is overloaded
                         *          method. Signature checking could make more robust.
                         * READOBJECT takes a single parameter, java.io.ObjectInputStream.
                         * WRITEOBJECT takes a single parameter, java.io.ObjectOutputStream.
                         */
                        methods.add(md);
                    }
                }
            }
        }

        /*
         * Compute default Serializable fields from all members of ClassSymbol.
         *
         * must walk over all members of ClassSymbol.
         */
        private void computeDefaultSerializableFields(ClassSymbol te) {
            for (Symbol sym : te.members().getSymbols(NON_RECURSIVE)) {
                if (sym != null && sym.kind == VAR) {
                    VarSymbol f = (VarSymbol) sym;
                    if ((f.flags() & Flags.STATIC) == 0
                            && (f.flags() & Flags.TRANSIENT) == 0) {
                        //### No modifier filtering applied here.
                        //### Add to beginning.
                        //### Preserve order used by old 'javadoc'.
                        fields.add(f);
                    }
                }
            }
        }

        /**
         * Find a method in this class scope. Search order: this class, interfaces, superclasses,
         * outerclasses. Note that this is not necessarily what the compiler would do!
         *
         * @param methodName the unqualified name to search for.
         * @param paramTypes the array of Strings for method parameter types.
         * @return the first MethodDocImpl which matches, null if not found.
         */
        public ExecutableElement findMethod(TypeElement te, String methodName,
                List<String> paramTypes) {
            List<? extends Element> allMembers = this.elements.getAllMembers(te);
            loop:
            for (Element e : allMembers) {
                if (e.getKind() != METHOD) {
                    continue;
                }
                ExecutableElement ee = (ExecutableElement) e;
                if (!ee.getSimpleName().contentEquals(methodName)) {
                    continue;
                }
                List<? extends VariableElement> parameters = ee.getParameters();
                if (paramTypes.size() != parameters.size()) {
                    continue;
                }
                for (int i = 0; i < parameters.size(); i++) {
                    VariableElement ve = parameters.get(i);
                    if (!ve.asType().toString().equals(paramTypes.get(i))) {
                        break loop;
                    }
                }
                return ee;
            }
            TypeElement encl = utils.getEnclosingTypeElement(te);
            if (encl == null) {
                return null;
            }
            return findMethod(encl, methodName, paramTypes);
        }
    }

    public boolean isRestrictedAPI(Element el) {
        Symbol sym = (Symbol) el;
        return sym.kind == MTH && (sym.flags() & Flags.RESTRICTED) != 0;
    }

    public boolean isPreviewAPI(Element el) {
        Symbol sym = (Symbol) el;
        return (sym.flags() & Flags.PREVIEW_API) != 0;
    }

    public boolean isReflectivePreviewAPI(Element el) {
        Symbol sym = (Symbol) el;
        return (sym.flags() & Flags.PREVIEW_REFLECTIVE) != 0;
    }

    /**
     * Returns whether or not to permit dynamically loaded components to access
     * part of the javadoc internal API. The flag is the same (hidden) compiler
     * option that allows javac plugins and annotation processors to access
     * javac internal API.
     *
     * As with all workarounds, it is better to consider updating the public API,
     * rather than relying on undocumented features like this, that may be withdrawn
     * at any time, without notice.
     *
     * @return true if access is permitted to internal API
     */
    public boolean accessInternalAPI() {
        Options compilerOptions = Options.instance(toolEnv.context);
        return compilerOptions.isSet("accessInternalAPI");
    }

    /*
     * If a similar query is ever added to javax.lang.model, use that instead.
     */
    public static boolean isImplicitlyDeclaredClass(Element e) {
        return e instanceof ClassSymbol c && c.isImplicit();
    }
}
