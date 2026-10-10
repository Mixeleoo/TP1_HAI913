package hai913i.tp1.output;

import hai913i.tp1.model.FieldInfo;
import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.TypeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StructurePrint {

    public static void allDir(List<TypeInfo> extractedTypes) {
        List<TypeInfo> allTypes = new ArrayList<>(extractedTypes);
        allTypes.sort(Comparator.comparing(t -> t.qualifiedName));

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
}
