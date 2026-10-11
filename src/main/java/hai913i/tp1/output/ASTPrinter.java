package hai913i.tp1.output;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class ASTPrinter extends ASTVisitor {
    private int depth = 0;

    @Override
    public boolean preVisit2(ASTNode node) {
        String type = node.getClass().getSimpleName();

        // Pour mieux lire l'arbre
        if (node instanceof TypeDeclaration td) {
            type += " " + td.getName();
        } else if (node instanceof MethodDeclaration md) {
            type += " " + md.getName();
        } else if (node instanceof MethodInvocation mi) {
            type += " " + mi.getName();
        }
        System.out.println("  ".repeat(depth) + type);
        depth++;
        return true;   // true = on descend dans les enfants
    }

    @Override
    public void postVisit(ASTNode node) {
        depth--;
    }
}
