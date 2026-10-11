package hai913i.tp1;

import java.nio.file.Path;
import java.util.List;

import hai913i.tp1.extract.ProjectExtractor;
import hai913i.tp1.graph.CallGraph;
import hai913i.tp1.metrics.MetricsCalculator;
import hai913i.tp1.output.*;
import hai913i.tp1.model.TypeInfo;
import org.eclipse.jdt.core.compiler.IProblem;

import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;

public final class Main {

    public static void main(String[] args) throws Exception {
        Path source = null;
        boolean validate = false;
        String astFile = null;
        boolean structure = false;
        boolean calls = false;
        boolean metrics = false;
        boolean graph = false;
        String method = null;
        Integer threshold = null;

        // Lecture des arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--source" -> {
                    if (i + 1 >= args.length) usage("--source attend un chemin");
                    source = Path.of(args[++i]);
                }
                case "--validate" -> validate = true;
                case "--ast" -> {
                    if (i + 1 >= args.length) usage("--ast attend un nom de fichier");
                    astFile = args[++i];
                }
                case "--structure" -> structure = true;
                case "--calls" -> calls = true;
                case "--metrics" -> metrics = true;
                case "--graph" -> graph = true;
                case "--method" -> {
                    if (i + 1 >= args.length) usage("--method attend une signature, ex : \"library.model.Item#getTitle()\"");
                    method = args[++i];
                }
                case "--threshold" -> {
                    if (i + 1 >= args.length) usage("--threshold attend un entier positif ou nul");
                    try {
                        threshold = Integer.parseInt(args[++i]);
                        if (threshold < 0) usage("--threshold doit être positif ou nul");
                    } catch (NumberFormatException e) {
                        usage("--threshold doit être un entier positif ou nul");
                    }
                }
                default -> usage("Option inconnue : " + args[i]);
            }
        }
        if (source == null) usage("--source est obligatoire");
        if (!validate && astFile == null && !structure && !calls && !metrics && !graph && method == null) usage("Aucune action demandée");
        if (metrics && threshold == null) usage("--threshold est obligatoire avec --metrics");
        if (!metrics && threshold != null) usage("--threshold nécessite --metrics");

        ProjectSources sources;
        try {
            sources = ProjectSources.of(source);
        } catch (IllegalArgumentException e) {
            System.err.println("Erreur : " + e.getMessage());
            System.exit(3);
            return;
        }
        List<ParsedFile> files = JdtParser.parse(sources, List.of());

        if (validate) {
            printValidation(sources, files);
        }
        if (astFile != null) {
            printAst(files, astFile);
        }
        if (structure || calls || metrics || graph || method != null) {
            ProjectExtractor extractor = new ProjectExtractor();
            if (metrics) {
                var project = extractor.extractProject(files);
                MetricsPrint.print(MetricsCalculator.calculate(project, threshold));
                if (structure) StructurePrint.allDir(project.types());
                if (calls) CallPrint.allCalls(project.types());
                if (graph || method != null) printGraph(CallGraph.build(project.types()), graph, method);
            } else {
                List<TypeInfo> extractedTypes = extractor.extract(files);
                if (structure) {
                    StructurePrint.allDir(extractedTypes);
                }
                if (calls) {
                    CallPrint.allCalls(extractedTypes);
                }
                if (graph || method != null) {
                    printGraph(CallGraph.build(extractedTypes), graph, method);
                }
            }
        }
    }

    // On met toutes les commandes ici
    private static void usage(String message) {
        System.err.println("Erreur : " + message);
        System.err.println("Usage : java -jar analyzer.jar --source DOSSIER [--validate] [--ast FICHIER.java] [--structure] [--calls] [--metrics --threshold X] [--graph] [--method SIGNATURE]");
        System.exit(2);
    }

    private static void printGraph(CallGraph callGraph, boolean graph, String method) {
        if (graph) GraphPrint.print(callGraph);
        if (method != null) GraphPrint.printMethod(callGraph, method);
    }

    // Méthode de Parsing
    private static void printValidation(ProjectSources sources, List<ParsedFile> files) {
        int errors = 0;
        for (ParsedFile file : files) {
            for (IProblem problem : file.unit().getProblems()) {
                if (problem.isError()) {
                    errors++;
                    System.err.println(file.path().getFileName() + ":" + problem.getSourceLineNumber()
                            + " " + problem.getMessage());
                }
            }
        }
        System.out.println("Racine des sources     : " + sources.sourceRoot());
        System.out.println("Unites de compilation  : " + files.size());
        System.out.println("Erreurs de compilation : " + errors);
    }

    // Méthode qui visite un fichier passé en paramètres
    private static void printAst(List<ParsedFile> files, String fileName) {
        boolean found = false;
        for (ParsedFile file : files) {
            if (file.path().getFileName().toString().equals(fileName)) {
                System.out.println("=== " + fileName + " ===");
                file.unit().accept(new ASTPrinter());
                found = true;
            }
        }
        if (!found) {
            System.err.println("Erreur : fichier introuvable dans le projet : " + fileName);
            System.exit(4);
        }
    }
}
