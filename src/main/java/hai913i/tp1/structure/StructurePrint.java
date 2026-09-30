package hai913i.tp1.structure;

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
/*
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
*/
    public static void allDir(List<JdtParser.ParsedFile> files){
        List<TypeInfo> allTypes = getAllType(files);

        int classes = 0, interfaces = 0, enumerations = 0, records = 0, methods = 0, constructors = 0, fields = 0;

        for (TypeInfo t : allTypes) {
            switch (t.kind) {
                case classT ->  classes++;
                case interfaceT -> interfaces++;
                case enumT -> enumerations++;
                case recordT -> records++;
            }
            for(MethodInfo method : t.methods){
                if(method.isConstructor()) constructors++;
                methods++;
            }
            fields += t.fields.size();
        }

        int total = classes + interfaces + enumerations + records;

        System.out.println("=== Structure du projet ===");
        System.out.printf("%d types : %d classes, %d interfaces, %d énumérations, %d records%n",
                total, classes, interfaces, enumerations, records);
        System.out.printf("%d méthodes (dont %d constructeurs), %d attributs%n%n",
                methods, constructors, fields);

        for (TypeInfo t : allTypes) {
            System.out.println("-".repeat(70));
            System.out.printf("%s %s%n", t.kind, t.qualifiedName);   // ex : classe library.model.Book
            System.out.println("  Paquetage     : " + t.packageName);
            System.out.println("  Superclasses  : " + joinOrNone(t.superclasses, " > "));
            System.out.println("  Interfaces    : " + joinOrNone(t.interfaces, ", "));

            System.out.printf("  Attributs (%d)%n", t.fields.size());
            for (FieldInfo f : t.fields) {
                System.out.printf("    %-10s %-20s %s%n", f.visibility(), f.type(), f.name());
            }

            System.out.printf("  Méthodes (%d)%n", t.methods.size());
            for (MethodInfo m : t.methods) {
                System.out.printf("    %-12s %s(%d param%s)%n",
                        m.isConstructor() ? "constructeur" : "méthode",
                        m.name(), m.paramCount(), m.paramCount() > 1 ? "s" : "");
            }
        }
        System.out.println("-".repeat(70));
    }

    private static String joinOrNone(List<String> items, String sep) {
        return items.isEmpty() ? "(aucune)" : String.join(sep, items);
    }
/*
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
*/
}
