package hai913i.tp1.structure;

import org.eclipse.jdt.core.dom.*;
import java.util.*;

public class StructureExtractor {

    public List<TypeInfo> extract(CompilationUnit cu) {
        List<TypeInfo> result = new ArrayList<>();
        String pkg = (cu.getPackage() == null)
                ? "(défaut)"
                : cu.getPackage().getName().getFullyQualifiedName();

        for (Object o : cu.types()) {
            if (o instanceof AbstractTypeDeclaration t) {
                extractType(t, pkg, result);
            }
        }
        return result;
    }
/*
    public List<TypeInfo> extractSpecific(CompilationUnit cu, String className) {
        List<TypeInfo> result = new ArrayList<>();
        for (Object o : cu.types()) {
            if (o instanceof AbstractTypeDeclaration t) {
                if(t.getName().getFullyQualifiedName().equals(className)) {
                    extractType(t, className, result);
                }
            }
        }
        return result;
    }
*/
    private TypeInfo extractType(AbstractTypeDeclaration decl, String pkg, List<TypeInfo> out) {
        // Les types d'annotation ne comptent pas
        if (decl instanceof AnnotationTypeDeclaration) return null;

        TypeInfo info = new TypeInfo();
        info.packageName = pkg;
        info.kind = kindOf(decl);

        // Nom qualifié et super-types viennent du binding
        ITypeBinding binding = decl.resolveBinding();
        if (binding != null) {
            info.qualifiedName = binding.getQualifiedName();
            fillSuperTypes(binding, info);
        } else {
            info.qualifiedName = decl.getName().getIdentifier();   // repli, à signaler
            System.err.println("Binding non résolu pour le type " + info.qualifiedName);
        }

        boolean inInterface = info.kind.equals(STRUCTURE_TYPE.interfaceT);

        // Membres : on lit UNIQUEMENT les bodyDeclarations de ce type
        for (Object o : decl.bodyDeclarations()) {
            if (o instanceof FieldDeclaration fd) {
                extractFields(fd, inInterface, info);
            } else if (o instanceof MethodDeclaration md) {
                info.methods.add(new MethodInfo(
                        md.getName().getIdentifier(),
                        md.parameters().size(),
                        md.isConstructor()));
            } else if (o instanceof AbstractTypeDeclaration nested) {
                // Ici c'est type premier dans notre type premier, donc il faut le mettre dans notre liste de membres qui sont imbriqués
                info.members.add(extractType(nested, pkg, out));
            }
        }
        out.add(info);
        return info;
    }

    private STRUCTURE_TYPE kindOf(AbstractTypeDeclaration decl) {
        if (decl instanceof EnumDeclaration) return STRUCTURE_TYPE.enumT;
        if (decl instanceof RecordDeclaration) return STRUCTURE_TYPE.recordT;
        if (decl instanceof TypeDeclaration td && td.isInterface()) return STRUCTURE_TYPE.interfaceT;
        return STRUCTURE_TYPE.classT;
    }

    private void fillSuperTypes(ITypeBinding binding, TypeInfo info) {
        // Chaîne des superclasses, sans java.lang.Object
        ITypeBinding sup = binding.getSuperclass();
        while (sup != null && !sup.getErasure().getQualifiedName().equals("java.lang.Object")) {
            info.superclasses.add(sup.getErasure().getQualifiedName());
            sup = sup.getSuperclass();
        }
        // Interfaces directes uniquement
        for (ITypeBinding itf : binding.getInterfaces()) {
            info.interfaces.add(itf.getErasure().getQualifiedName());
        }
    }

    private void extractFields(FieldDeclaration fd, boolean inInterface, TypeInfo info) {
        String type = fd.getType().toString();
        VISIBILITY visibility = visibilityOf(fd.getModifiers(), inInterface);
        for (Object f : fd.fragments()) {             // int a, b; => 2 attributs
            VariableDeclarationFragment frag = (VariableDeclarationFragment) f;
            info.fields.add(new FieldInfo(frag.getName().getIdentifier(), type, visibility));
        }
    }

    private VISIBILITY visibilityOf(int modifiers, boolean inInterface) {
        if (Modifier.isPublic(modifiers))    return VISIBILITY.publicV;
        if (Modifier.isProtected(modifiers)) return VISIBILITY.protectedV;
        if (Modifier.isPrivate(modifiers))   return VISIBILITY.privateV;
        return inInterface ? VISIBILITY.publicV : VISIBILITY.packageV;    // membre d'interface : public implicite
    }
}