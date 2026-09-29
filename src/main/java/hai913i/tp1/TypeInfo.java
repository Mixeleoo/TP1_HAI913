package hai913i.tp1;

import hai913i.tp1.structure.STRUCTURE_TYPE;

import java.util.ArrayList;
import java.util.List;

public class TypeInfo {
    public String qualifiedName;
    public STRUCTURE_TYPE kind;
    public String packageName;
    public List<String> superclasses = new ArrayList<>();
    public List<String> interfaces = new ArrayList<>();
    public List<FieldInfo> fields = new ArrayList<>();
    public List<MethodInfo> methods = new ArrayList<>();
}