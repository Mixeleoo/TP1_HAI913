package hai913i.tp1.model;

import java.util.ArrayList;
import java.util.List;

public record MethodInfo(String name, int paramCount, boolean isConstructor,
                         String signature, int bodyLines, List<CallInfo> calls) {
    public MethodInfo {
        calls = new ArrayList<>(calls);
    }
}
