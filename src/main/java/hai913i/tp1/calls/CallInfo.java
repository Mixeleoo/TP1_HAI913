package hai913i.tp1.calls;

// target = null si l'appel est pas résolu
public record CallInfo(String name, String receiverType, String target, int line,
                       boolean isSuper, CALL_STATUS status) {}
