package hai913i.tp1.graph;

import hai913i.tp1.extract.CALL_STATUS;
import hai913i.tp1.model.CallInfo;
import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.TypeInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CallGraph {
    // TreeSet / TreeMap pour avoir toujours le meme ordre a l'affichage
    public Set<String> nodes = new TreeSet<>();
    public Map<String, Map<String, Integer>> arcs = new TreeMap<>();   // f -> (g -> poids)
    public int externalCalls = 0;
    public int unresolvedCalls = 0;

    public static CallGraph build(List<TypeInfo> types) {
        CallGraph graph = new CallGraph();

        // les noeuds = toutes les methodes comptées
        for (TypeInfo type : types) {
            for (MethodInfo method : type.methods) {
                graph.nodes.add(method.signature());
            }
        }

        for (TypeInfo type : types) {
            for (MethodInfo method : type.methods) {
                for (CallInfo call : method.calls()) {
                    if (call.status() == CALL_STATUS.externalC) {
                        graph.externalCalls++;
                    } else if (call.status() == CALL_STATUS.unresolvedC) {
                        graph.unresolvedCalls++;
                    } else if (graph.nodes.contains(call.target())) {
                        // plusieurs appels f -> g = un seul arc, on augmente le poids
                        graph.arcs.computeIfAbsent(method.signature(), k -> new TreeMap<>())
                                .merge(call.target(), 1, Integer::sum);
                    }
                }
            }
        }
        return graph;
    }

    public Map<String, Integer> callees(String method) {
        return arcs.getOrDefault(method, Map.of());
    }

    // graphe inversé : on cherche tous les f qui ont un arc vers method
    public Map<String, Integer> callers(String method) {
        Map<String, Integer> result = new TreeMap<>();
        for (String f : arcs.keySet()) {
            Integer weight = arcs.get(f).get(method);
            if (weight != null) result.put(f, weight);
        }
        return result;
    }

    public int arcCount() {
        int count = 0;
        for (Map<String, Integer> targets : arcs.values()) count += targets.size();
        return count;
    }

    public int internalSites() {
        int count = 0;
        for (Map<String, Integer> targets : arcs.values()) {
            for (int weight : targets.values()) count += weight;
        }
        return count;
    }
}
