package hai913i.tp1.extract;

import hai913i.tp1.model.ProjectInfo;
import hai913i.tp1.model.TypeInfo;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.graph.CallGraph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class FilesExtractor {
    List<ParsedFile> files;
    ProjectSources sources;
    public FilesExtractor(Path source) {
        try {
            sources = ProjectSources.of(source);
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Erreur : " + e.getMessage());
            System.exit(3);
            return;
        }
        files = JdtParser.parse(sources, List.of());
    }

    public List<ParsedFile> getFiles() {
        return files;
    }

    public ProjectSources getSources() {
        return sources;
    }

    public ProjectInfo extractProject() {
        try {
            return new ProjectExtractor().extractProject(files);
        } catch (IOException e) {
            System.err.println("Erreur : impossible de lire les fichiers du projet");
            System.exit(4);
            return null;
        }
    }

    public List<TypeInfo> extract() {
        return new ProjectExtractor().extract(files);
    }
    
    public CallGraph callGraphBuild() {
        return CallGraph.build(extractProject().types());
    }
}
