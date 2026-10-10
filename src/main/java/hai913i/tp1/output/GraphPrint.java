package hai913i.tp1.output;

import hai913i.tp1.graph.CallGraph;

import java.util.Map;

public class GraphPrint {

    public static void print(CallGraph graph) {
        System.out.println("=== Graphe d'appel ===");
        System.out.printf("%d noeuds, %d arcs, %d sites d'appel internes, %d appels externes, %d appels non résolus%n%n",
                graph.nodes.size(), graph.arcCount(), graph.internalSites(), graph.externalCalls, graph.unresolvedCalls);

        // une ligne par arc : f -> g (poids)
        for (String f : graph.arcs.keySet()) {
            for (Map.Entry<String, Integer> arc : graph.arcs.get(f).entrySet()) {
                System.out.printf("%s -> %s (%d)%n", f, arc.getKey(), arc.getValue());
            }
        }
    }

    public static void printMethod(CallGraph graph, String method) {
        if (!graph.nodes.contains(method)) {
            System.err.println("Erreur : méthode inconnue dans le graphe : " + method);
            System.exit(4);
        }
        System.out.println("=== " + method + " ===");
        printList("Appelées", graph.callees(method));
        printList("Appelantes", graph.callers(method));
    }

    private static void printList(String title, Map<String, Integer> methods) {
        System.out.printf("%s (%d) :%n", title, methods.size());
        if (methods.isEmpty()) System.out.println("  (aucune)");
        for (Map.Entry<String, Integer> m : methods.entrySet()) {
            System.out.printf("  %s (%d)%n", m.getKey(), m.getValue());
        }
    }
}
