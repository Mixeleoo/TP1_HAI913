package hai913i.tp1.output;

import hai913i.tp1.metrics.MetricsCalculator.ClassMethodRanking;
import hai913i.tp1.metrics.MetricsCalculator.MethodRanking;
import hai913i.tp1.metrics.MetricsCalculator.Report;
import hai913i.tp1.metrics.MetricsCalculator.TypeRanking;

import java.util.List;
import java.util.Locale;

public final class MetricsPrint {

    private MetricsPrint() {
    }

    public static void print(Report report) {
        System.out.println("=== Métriques du projet ===");
        System.out.printf("1. Nombre de classes : %d%n", report.classCount());
        System.out.printf("2. Lignes physiques de l'application : %d%n", report.applicationLines());
        System.out.printf("3. Nombre total de méthodes : %d%n", report.methodCount());
        System.out.printf("4. Nombre de paquetages : %d%n", report.packageCount());
        System.out.printf(Locale.ROOT, "5. Moyenne de méthodes par classe : %.2f%n",
                report.meanMethodsPerClass());
        System.out.printf(Locale.ROOT, "6. Moyenne de lignes par méthode avec corps : %.2f (%d lignes / %d méthodes)%n",
                report.meanBodyLinesPerMethod(), report.totalMethodBodyLines(), report.methodsWithBodyCount());
        System.out.printf(Locale.ROOT, "7. Moyenne d'attributs par classe : %.2f%n",
                report.meanFieldsPerClass());
        System.out.printf("8. Top 10 %% des classes par nombre de méthodes : %s%n",
                formatTypes(report.topTypesByMethodCount()));
        System.out.printf("9. Top 10 %% des classes par nombre d'attributs : %s%n",
                formatTypes(report.topTypesByFieldCount()));
        System.out.printf("10. Dans les deux catégories : %s%n",
                report.intersection().isEmpty() ? "(aucune)" : String.join(", ", report.intersection()));
        System.out.printf("11. Classes ayant plus de %d méthodes : %s%n",
                report.threshold(), formatTypes(report.typesOverThreshold()));
        System.out.println("12. Top 10 % des méthodes par lignes de corps, pour chaque classe :");
        for (ClassMethodRanking ranking : report.topMethodsByType()) {
            System.out.printf("  %s : %s%n", ranking.typeName(), formatMethods(ranking.methods()));
        }
        System.out.printf("13. Nombre maximal de paramètres : %d (%s)%n",
                report.maxParameters(), report.methodsWithMaxParameters().isEmpty()
                        ? "(aucune méthode)"
                        : String.join(", ", report.methodsWithMaxParameters()));
    }

    private static String formatTypes(List<TypeRanking> rankings) {
        if (rankings.isEmpty()) return "(aucun)";
        return rankings.stream()
                .map(ranking -> ranking.typeName() + " (" + ranking.value() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("(aucun)");
    }

    private static String formatMethods(List<MethodRanking> rankings) {
        if (rankings.isEmpty()) return "(aucune)";
        return rankings.stream()
                .map(ranking -> ranking.signature() + " (" + ranking.bodyLines() + " lignes)")
                .reduce((left, right) -> left + ", " + right)
                .orElse("(aucune)");
    }
}
