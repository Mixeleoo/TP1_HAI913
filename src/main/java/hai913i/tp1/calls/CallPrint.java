package hai913i.tp1.calls;

import hai913i.tp1.parse.JdtParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CallPrint {

    private static List<MethodCallsInfo> getAllMethods(List<JdtParser.ParsedFile> files) {
        CallExtractor extractor = new CallExtractor();
        List<MethodCallsInfo> allMethods = new ArrayList<>();

        for (JdtParser.ParsedFile file : files) {
            allMethods.addAll(extractor.extract(file.unit()));
        }
        // tri par type, les methodes restent dans l'ordre du fichier
        allMethods.sort(Comparator.comparing(m -> m.typeName));

        return allMethods;
    }

    public static void allCalls(List<JdtParser.ParsedFile> files) {
        List<MethodCallsInfo> allMethods = getAllMethods(files);

        int total = 0, supers = 0, internal = 0, external = 0, unresolved = 0;

        for (MethodCallsInfo m : allMethods) {
            for (CallInfo c : m.calls) {
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
        for (MethodCallsInfo m : allMethods) {
            // nouveau type -> on affiche son nom
            if (!m.typeName.equals(currentType)) {
                currentType = m.typeName;
                System.out.println("-".repeat(70));
                System.out.println(m.typeName);
            }
            System.out.printf("  %s : %d appel%s%n", m.signature.substring(m.signature.indexOf('#') + 1),
                    m.calls.size(), m.calls.size() > 1 ? "s" : "");

            for (CallInfo c : m.calls) {
                System.out.printf("    l.%-4d %-15s receveur : %s%n",
                        c.line(), (c.isSuper() ? "super." : "") + c.name(), c.receiverType() == null ? "non résolu" : c.receiverType());
                System.out.printf("%26s cible    : %s%n", "", targetLabel(c));
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
