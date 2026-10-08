package hai913i.tp1.calls;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class CallExtractor {

    public List<MethodCallsInfo> extract(CompilationUnit cu) {
        List<MethodCallsInfo> result = new ArrayList<>();
        for (Object o : cu.types()) {
            if (o instanceof AbstractTypeDeclaration t) {
                extractType(t, cu, result);
            }
        }
        return result;
    }

    // meme parcours que dans StructureExtractor (bodyDeclarations) sinon les methodes de Loan partent dans LoanService
    private void extractType(AbstractTypeDeclaration decl, CompilationUnit cu, List<MethodCallsInfo> out) {
        if (decl instanceof AnnotationTypeDeclaration) return;

        ITypeBinding binding = decl.resolveBinding();
        String typeName = (binding != null) ? binding.getQualifiedName() : decl.getName().getIdentifier();

        for (Object o : decl.bodyDeclarations()) {
            if (o instanceof MethodDeclaration md) {
                out.add(extractMethod(md, typeName, cu));
            } else if (o instanceof AbstractTypeDeclaration nested) {
                extractType(nested, cu, out);
            }
            // les champs et blocs static{} on s'en fiche, les appels hors methode sont ignorés
        }
    }

    private MethodCallsInfo extractMethod(MethodDeclaration md, String typeName, CompilationUnit cu) {
        MethodCallsInfo info = new MethodCallsInfo();
        info.typeName = typeName;
        IMethodBinding mb = md.resolveBinding();
        info.signature = (mb != null) ? signatureOf(mb) : typeName + "#" + md.getName().getIdentifier() + "(?)";

        // methode abstraite / d'interface -> pas de body
        if (md.getBody() == null) return info;

        // le visiteur va aussi dans les lambdas et classes anonymes donc leurs appels restent dans cette methode
        md.getBody().accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
                IMethodBinding target = node.resolveMethodBinding();
                String receiver = receiverOf(node, target);
                info.calls.add(buildCall(node.getName(), receiver, target, false, cu));
                return true; // true pour avoir aussi les appels dans les arguments
            }

            @Override
            public boolean visit(SuperMethodInvocation node) {
                IMethodBinding target = node.resolveMethodBinding();
                // super.m() -> superclasse directe
                String receiver = nameOf(enclosingType(node).getSuperclass());
                info.calls.add(buildCall(node.getName(), receiver, target, true, cu));
                return true;
            }
        });
        return info;
    }

    private CallInfo buildCall(SimpleName name, String receiver, IMethodBinding target, boolean isSuper, CompilationUnit cu) {
        int line = cu.getLineNumber(name.getStartPosition());

        if (target == null) {
            return new CallInfo(name.getIdentifier(), receiver, null, line, isSuper, CALL_STATUS.unresolvedC);
        }
        // getMethodDeclaration : List<Item>.add(Item) -> List.add(E)
        IMethodBinding decl = target.getMethodDeclaration();
        CALL_STATUS status = decl.getDeclaringClass().isFromSource() ? CALL_STATUS.internalC : CALL_STATUS.externalC;
        return new CallInfo(name.getIdentifier(), receiver, signatureOf(decl), line, isSuper, status);
    }

    private String receiverOf(MethodInvocation node, IMethodBinding target) {
        // a.m() ou Type.m()
        if (node.getExpression() != null) {
            return nameOf(node.getExpression().resolveTypeBinding());
        }
        // m() statique
        if (target != null && Modifier.isStatic(target.getModifiers())) {
            return nameOf(target.getDeclaringClass());
        }
        // m() tout court = this.m()
        return nameOf(enclosingType(node));
    }

    private ITypeBinding enclosingType(ASTNode node) {
        ASTNode p = node.getParent();
        while (!(p instanceof AbstractTypeDeclaration)) {
            p = p.getParent();
        }
        return ((AbstractTypeDeclaration) p).resolveBinding();
    }

    // ex : library.service.Catalog#add(library.model.Item)
    static String signatureOf(IMethodBinding b) {
        StringBuilder s = new StringBuilder(nameOf(b.getDeclaringClass()))
                .append('#').append(b.getName()).append('(');
        ITypeBinding[] params = b.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) s.append(',');
            s.append(nameOf(params[i]));
        }
        return s.append(')').toString();
    }

    // getErasure pour avoir java.util.List et pas java.util.List<Item>
    static String nameOf(ITypeBinding t) {
        return (t == null) ? null : t.getErasure().getQualifiedName();
    }
}
