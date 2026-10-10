package hai913i.tp1.metrics;

import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.ProjectInfo;
import hai913i.tp1.model.TypeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class MetricsCalculator {

    private static final Comparator<TypeInfo> TYPE_ORDER =
            Comparator.comparing(type -> type.qualifiedName);
    private static final Comparator<MethodInfo> METHOD_ORDER =
            Comparator.comparing(MethodInfo::signature);

    private MetricsCalculator() {
    }

    public static Report calculate(ProjectInfo project, int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Le seuil X doit être positif ou nul.");
        }

        List<TypeInfo> types = project.types().stream().sorted(TYPE_ORDER).toList();
        List<MethodInfo> methods = types.stream().flatMap(type -> type.methods.stream()).toList();
        int classCount = types.size();
        int methodCount = methods.size();
        int fieldCount = types.stream().mapToInt(type -> type.fields.size()).sum();
        List<MethodInfo> methodsWithBody = methods.stream().filter(method -> method.bodyLines() > 0).toList();
        int bodyLines = methodsWithBody.stream().mapToInt(MethodInfo::bodyLines).sum();

        List<TypeRanking> topMethods = rankTypes(types, type -> type.methods.size());
        List<TypeRanking> topFields = rankTypes(types, type -> type.fields.size());
        Set<String> topMethodNames = topMethods.stream().map(TypeRanking::typeName).collect(Collectors.toSet());
        List<String> intersection = topFields.stream().map(TypeRanking::typeName)
                .filter(topMethodNames::contains).sorted().toList();
        List<TypeRanking> overThreshold = types.stream()
                .filter(type -> type.methods.size() > threshold)
                .map(type -> new TypeRanking(type.qualifiedName, type.methods.size()))
                .toList();

        List<ClassMethodRanking> topMethodsByType = types.stream()
                .map(type -> new ClassMethodRanking(type.qualifiedName,
                        rankMethods(type.methods)))
                .toList();

        int maxParameters = methods.stream().mapToInt(MethodInfo::paramCount).max().orElse(0);
        List<String> methodsWithMaxParameters = methods.stream()
                .filter(method -> method.paramCount() == maxParameters)
                .sorted(METHOD_ORDER)
                .map(MethodInfo::signature)
                .toList();

        return new Report(classCount, project.applicationLines(), methodCount, project.packageNames().size(),
                mean(methodCount, classCount), mean(bodyLines, methodsWithBody.size()),
                mean(fieldCount, classCount), topMethods, topFields, intersection, threshold,
                overThreshold, topMethodsByType, maxParameters, methodsWithMaxParameters,
                methodsWithBody.size(), bodyLines);
    }

    private static List<TypeRanking> rankTypes(List<TypeInfo> types,
                                                java.util.function.ToIntFunction<TypeInfo> measure) {
        List<TypeInfo> ranked = new ArrayList<>(types);
        ranked.sort(Comparator.<TypeInfo>comparingInt(measure).reversed().thenComparing(TYPE_ORDER));
        int countToSelect = ceilingTenth(ranked.size());
        return selectNonZeroTies(ranked, countToSelect, type -> measure.applyAsInt(type))
                .stream()
                .map(type -> new TypeRanking(type.qualifiedName, measure.applyAsInt(type)))
                .toList();
    }

    private static List<MethodRanking> rankMethods(List<MethodInfo> methods) {
        List<MethodInfo> ranked = new ArrayList<>(methods);
        ranked.sort(Comparator.comparingInt(MethodInfo::bodyLines).reversed().thenComparing(METHOD_ORDER));
        int countToSelect = ceilingTenth(ranked.size());
        return selectNonZeroTies(ranked, countToSelect, MethodInfo::bodyLines)
                .stream()
                .map(method -> new MethodRanking(method.signature(), method.bodyLines()))
                .toList();
    }

    private static <T> List<T> selectNonZeroTies(List<T> sorted, int countToSelect,
                                                  java.util.function.ToIntFunction<T> measure) {
        if (countToSelect == 0 || sorted.isEmpty()) return List.of();
        int cutoff = measure.applyAsInt(sorted.get(countToSelect - 1));
        if (cutoff == 0) return List.of();
        return sorted.stream().takeWhile(item -> measure.applyAsInt(item) >= cutoff).toList();
    }

    private static int ceilingTenth(int count) {
        return (count + 9) / 10;
    }

    private static double mean(int total, int count) {
        return count == 0 ? 0.0 : (double) total / count;
    }

    public record TypeRanking(String typeName, int value) {
    }

    public record MethodRanking(String signature, int bodyLines) {
    }

    public record ClassMethodRanking(String typeName, List<MethodRanking> methods) {
        public ClassMethodRanking {
            methods = List.copyOf(methods);
        }
    }

    public record Report(int classCount, int applicationLines, int methodCount, int packageCount,
                         double meanMethodsPerClass, double meanBodyLinesPerMethod,
                         double meanFieldsPerClass, List<TypeRanking> topTypesByMethodCount,
                         List<TypeRanking> topTypesByFieldCount, List<String> intersection,
                         int threshold, List<TypeRanking> typesOverThreshold,
                         List<ClassMethodRanking> topMethodsByType, int maxParameters,
                         List<String> methodsWithMaxParameters, int methodsWithBodyCount,
                         int totalMethodBodyLines) {
        public Report {
            topTypesByMethodCount = List.copyOf(topTypesByMethodCount);
            topTypesByFieldCount = List.copyOf(topTypesByFieldCount);
            intersection = List.copyOf(intersection);
            typesOverThreshold = List.copyOf(typesOverThreshold);
            topMethodsByType = List.copyOf(topMethodsByType);
            methodsWithMaxParameters = List.copyOf(methodsWithMaxParameters);
        }
    }
}
