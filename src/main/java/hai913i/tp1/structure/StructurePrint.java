package hai913i.tp1.structure;

import hai913i.tp1.MethodInfo;
import hai913i.tp1.TypeInfo;
import hai913i.tp1.parse.JdtParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StructurePrint {

    private static List<TypeInfo> getAllType(List<JdtParser.ParsedFile> files){
        StructureExtractor extractor = new StructureExtractor();
        List<TypeInfo> allTypes = new ArrayList<>();

        for (JdtParser.ParsedFile file : files) {
            allTypes.addAll(extractor.extract(file.unit()));
        }
        allTypes.sort(Comparator.comparing(t -> t.qualifiedName));

        return allTypes;
    }

    private static List<TypeInfo> getSpecificType(List<JdtParser.ParsedFile> files, String className) {

        StructureExtractor extractor = new StructureExtractor();
        List<TypeInfo> types = new ArrayList<>();

        int i = 0;
        while(i < files.size() && types.isEmpty()) {
            types = extractor.extractSpecific(files.get(i).unit(), className);
            i++;
        }
        types.sort(Comparator.comparing(t -> t.qualifiedName));

        return types;
    }

    public static void allDir(List<JdtParser.ParsedFile> files){
        List<TypeInfo> allTypes = getAllType(files);

        int classes = 0, interfaces = 0, enumerations = 0, records = 0, methods = 0, constructors = 0, fields = 0;

        for (TypeInfo t : allTypes) {
            switch (t.kind) {
                case classT ->  classes++;
                case interfaceT -> interfaces++;
                case enumT -> enumerations++;
                case  recordT -> records++;
            }
            for(MethodInfo method : t.methods){
                if(method.isConstructor()) constructors++;
                methods++;
            }
            fields += t.fields.size();
        }
        System.out.println(
                classes + interfaces + enumerations + records + " classes : " + classes + " classes, " +
                        interfaces + " interfaces, " +
                        enumerations + " énumération ; " +
                        methods + " méthodes, dont " +
                        constructors + " constructeurs ; " +
                        fields + " attributs.");
    }

    public static void classSpecific(List<JdtParser.ParsedFile> files, String className) {

        List<TypeInfo> types = getSpecificType(files, className);
        TypeInfo root = types.get(0);

        StringBuilder s = new StringBuilder(root.qualifiedName + " : " + root.methods.size() + " méthodes et "
                + root.fields.size() + " attributs");

        List<TypeInfo> nested = types.stream()
                .filter(t -> t.qualifiedName.startsWith(root.qualifiedName + "."))
                .toList();

        for (TypeInfo n : nested) {
            s.append(" ; sa classe membre ").append(n.qualifiedName).append(" : ").append(n.methods.size()).append(" méthodes et ").append(n.fields.size()).append(" attributs");
        }

        System.out.println(s + ".");
    }
}
