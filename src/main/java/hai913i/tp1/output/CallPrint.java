package hai913i.tp1.output;

import hai913i.tp1.model.CallInfo;
import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.TypeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CallPrint {

    public static void allCalls(List<TypeInfo> extractedTypes) {
        List<TypeInfo> allTypes = new ArrayList<>(extractedTypes);
        allTypes.sort(Comparator.comparing(t -> t.qualifiedName));
        List<MethodInfo> allMethods = allTypes.stream()
                .flatMap(type -> type.methods.stream())
                .toList();

        int total = 0, supers = 0, internal = 0, external = 0, unresolved = 0;

        for (MethodInfo m : allMethods) {
            for (CallInfo c : m.calls()) {
                total++;
                if (c.isSuper()) supers++;
                switch (c.status()) {
                    case internalC -> internal++;
                    case externalC -> external++;
                    case unresolvedC -> unresolved++;
                }
            }
        }

        System.out.println("=== Appels du projet ===");
        System.out.printf("%d appels (%d MethodInvocation, %d SuperMethodInvocation)%n", total, total - supers, supers);
        System.out.printf("%d vers des méthodes du projet, %d vers l'extérieur, %d non résolus%n%n",
                internal, external, unresolved);

        String currentType = null;
        for (TypeInfo type : allTypes) {
            if (!type.qualifiedName.equals(currentType)) {
                currentType = type.qualifiedName;
                System.out.println("-".repeat(70));
                System.out.println(type.qualifiedName);
            }
            for (MethodInfo m : type.methods) {
                System.out.printf("  %s : %d appel%s%n", m.signature().substring(m.signature().indexOf('#') + 1),
                        m.calls().size(), m.calls().size() > 1 ? "s" : "");

                for (CallInfo c : m.calls()) {
                    System.out.printf("    l.%-4d %-15s receveur : %s%n",
                            c.line(), (c.isSuper() ? "super." : "") + c.name(),
                            c.receiverType() == null ? "non résolu" : c.receiverType());
                    System.out.printf("%26s cible    : %s%n", "", targetLabel(c));
                }
            }
        }
        System.out.println("-".repeat(70));
    }

    private static String targetLabel(CallInfo c) {
        return switch (c.status()) {
            case internalC -> c.target();
            case externalC -> c.target() + "  [externe]";
            case unresolvedC -> "non résolue";
        };
    }
}
