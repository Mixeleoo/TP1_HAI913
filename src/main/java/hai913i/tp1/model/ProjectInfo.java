package hai913i.tp1.model;

import java.util.List;

public record ProjectInfo(List<TypeInfo> types, List<String> packageNames, int applicationLines) {
    public ProjectInfo {
        types = List.copyOf(types);
        packageNames = List.copyOf(packageNames);
    }
}
