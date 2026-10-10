package hai913i.tp1.extract;

import hai913i.tp1.model.CallInfo;
import hai913i.tp1.model.FieldInfo;
import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.ProjectInfo;
import hai913i.tp1.model.TypeInfo;
import hai913i.tp1.parse.JdtParser;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProjectExtractor {

    public List<TypeInfo> extract(List<JdtParser.ParsedFile> files) {
        List<TypeInfo> result = new ArrayList<>();
        extractTypes(files, result);
        return result;
    }

    public ProjectInfo extractProject(List<JdtParser.ParsedFile> files) throws IOException {
        List<TypeInfo> types = new ArrayList<>();
        extractTypes(files, types);

        Set<String> packageNames = new LinkedHashSet<>();
        int applicationLines = 0;
        for (JdtParser.ParsedFile file : files) {
            CompilationUnit cu = file.unit();
            packageNames.add(packageNameOf(cu));
            applicationLines += Files.readAllLines(file.path()).size();
        }
        return new ProjectInfo(types, new ArrayList<>(packageNames), applicationLines);
    }

    private void extractTypes(List<JdtParser.ParsedFile> files, List<TypeInfo> result) {
        for (JdtParser.ParsedFile file : files) {
            CompilationUnit cu = file.unit();
            String pkg = packageNameOf(cu);

            for (Object declaration : cu.types()) {
                if (declaration instanceof AbstractTypeDeclaration type) {
                    extractType(type, pkg, cu, result);
                }
            }
        }
    }

    private String packageNameOf(CompilationUnit cu) {
        return cu.getPackage() == null
                ? "(défaut)"
                : cu.getPackage().getName().getFullyQualifiedName();
    }

    private TypeInfo extractType(AbstractTypeDeclaration decl, String pkg, CompilationUnit cu,
                                 List<TypeInfo> out) {
        if (decl instanceof AnnotationTypeDeclaration) return null;

        TypeInfo info = new TypeInfo();
        info.packageName = pkg;
        info.kind = kindOf(decl);

        ITypeBinding binding = decl.resolveBinding();
        if (binding != null) {
            info.qualifiedName = binding.getQualifiedName();
            fillSuperTypes(binding, info);
        } else {
            info.qualifiedName = decl.getName().getIdentifier();
            System.err.println("Binding non résolu pour le type " + info.qualifiedName);
        }

        boolean inInterface = info.kind == STRUCTURE_TYPE.interfaceT;
        for (Object member : decl.bodyDeclarations()) {
            if (member instanceof FieldDeclaration field) {
                extractFields(field, inInterface, info);
            } else if (member instanceof MethodDeclaration method) {
                info.methods.add(extractMethod(method, info.qualifiedName, cu));
            } else if (member instanceof AbstractTypeDeclaration nested) {
                TypeInfo nestedInfo = extractType(nested, pkg, cu, out);
                if (nestedInfo != null) info.members.add(nestedInfo);
            }
        }
        out.add(info);
        return info;
    }

    private MethodInfo extractMethod(MethodDeclaration method, String typeName, CompilationUnit cu) {
        IMethodBinding binding = method.resolveBinding();
        String signature = binding != null
                ? signatureOf(binding)
                : typeName + "#" + method.getName().getIdentifier() + "(?)";
        List<CallInfo> calls = new ArrayList<>();

        if (method.getBody() != null) {
            method.getBody().accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation node) {
                    IMethodBinding target = node.resolveMethodBinding();
                    String receiver = receiverOf(node, target);
                    calls.add(buildCall(node.getName(), receiver, target, false, cu));
                    return true;
                }

                @Override
                public boolean visit(SuperMethodInvocation node) {
                    IMethodBinding target = node.resolveMethodBinding();
                    String receiver = nameOf(enclosingType(node).getSuperclass());
                    calls.add(buildCall(node.getName(), receiver, target, true, cu));
                    return true;
                }
            });
        }

        return new MethodInfo(method.getName().getIdentifier(), method.parameters().size(),
                method.isConstructor(), signature, bodyLines(method, cu), calls);
    }

    private int bodyLines(MethodDeclaration method, CompilationUnit cu) {
        Block body = method.getBody();
        if (body == null || body.getLength() == 0) return 0;
        int firstLine = cu.getLineNumber(body.getStartPosition());
        int lastLine = cu.getLineNumber(body.getStartPosition() + body.getLength() - 1);
        return lastLine - firstLine + 1;
    }

    private CallInfo buildCall(SimpleName name, String receiver, IMethodBinding target,
                               boolean isSuper, CompilationUnit cu) {
        int line = cu.getLineNumber(name.getStartPosition());
        if (target == null) {
            return new CallInfo(name.getIdentifier(), receiver, null, line, isSuper, CALL_STATUS.unresolvedC);
        }
        IMethodBinding declaration = target.getMethodDeclaration();
        CALL_STATUS status = declaration.getDeclaringClass().isFromSource()
                ? CALL_STATUS.internalC
                : CALL_STATUS.externalC;
        return new CallInfo(name.getIdentifier(), receiver, signatureOf(declaration), line, isSuper, status);
    }

    private String receiverOf(MethodInvocation node, IMethodBinding target) {
        if (node.getExpression() != null) {
            return nameOf(node.getExpression().resolveTypeBinding());
        }
        if (target != null && Modifier.isStatic(target.getModifiers())) {
            return nameOf(target.getDeclaringClass());
        }
        return nameOf(enclosingType(node));
    }

    private ITypeBinding enclosingType(ASTNode node) {
        ASTNode parent = node.getParent();
        while (!(parent instanceof AbstractTypeDeclaration)) {
            parent = parent.getParent();
        }
        return ((AbstractTypeDeclaration) parent).resolveBinding();
    }

    private String signatureOf(IMethodBinding binding) {
        StringBuilder signature = new StringBuilder(nameOf(binding.getDeclaringClass()))
                .append('#').append(binding.getName()).append('(');
        ITypeBinding[] parameters = binding.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) signature.append(',');
            signature.append(nameOf(parameters[i]));
        }
        return signature.append(')').toString();
    }

    private String nameOf(ITypeBinding type) {
        return type == null ? null : type.getErasure().getQualifiedName();
    }

    private STRUCTURE_TYPE kindOf(AbstractTypeDeclaration decl) {
        if (decl instanceof EnumDeclaration) return STRUCTURE_TYPE.enumT;
        if (decl instanceof RecordDeclaration) return STRUCTURE_TYPE.recordT;
        if (decl instanceof TypeDeclaration type && type.isInterface()) return STRUCTURE_TYPE.interfaceT;
        return STRUCTURE_TYPE.classT;
    }

    private void fillSuperTypes(ITypeBinding binding, TypeInfo info) {
        ITypeBinding superclass = binding.getSuperclass();
        while (superclass != null
                && !superclass.getErasure().getQualifiedName().equals("java.lang.Object")) {
            info.superclasses.add(superclass.getErasure().getQualifiedName());
            superclass = superclass.getSuperclass();
        }
        for (ITypeBinding implementedInterface : binding.getInterfaces()) {
            info.interfaces.add(implementedInterface.getErasure().getQualifiedName());
        }
    }

    private void extractFields(FieldDeclaration declaration, boolean inInterface, TypeInfo info) {
        String type = declaration.getType().toString();
        VISIBILITY visibility = visibilityOf(declaration.getModifiers(), inInterface);
        for (Object fragment : declaration.fragments()) {
            VariableDeclarationFragment field = (VariableDeclarationFragment) fragment;
            info.fields.add(new FieldInfo(field.getName().getIdentifier(), type, visibility));
        }
    }

    private VISIBILITY visibilityOf(int modifiers, boolean inInterface) {
        if (Modifier.isPublic(modifiers)) return VISIBILITY.publicV;
        if (Modifier.isProtected(modifiers)) return VISIBILITY.protectedV;
        if (Modifier.isPrivate(modifiers)) return VISIBILITY.privateV;
        return inInterface ? VISIBILITY.publicV : VISIBILITY.packageV;
    }
}
