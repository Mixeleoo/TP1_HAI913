package hai913i.tp1.calls;

import java.util.ArrayList;
import java.util.List;

public class MethodCallsInfo {
    public String typeName;
    public String signature;    // ex : library.service.Catalog#add(library.model.Item)
    public List<CallInfo> calls = new ArrayList<>();
}
