package hai913i.tp1;

import hai913i.tp1.extract.STRUCTURE_TYPE;
import hai913i.tp1.metrics.MetricsCalculator;
import hai913i.tp1.model.CallInfo;
import hai913i.tp1.model.FieldInfo;
import hai913i.tp1.model.MethodInfo;
import hai913i.tp1.model.ProjectInfo;
import hai913i.tp1.model.TypeInfo;
import hai913i.tp1.extract.VISIBILITY;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsCalculatorTest {

    @Test
    void includesCutoffTiesAndAppliesStrictThreshold() {
        List<TypeInfo> types = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TypeInfo type = new TypeInfo();
            type.qualifiedName = "sample.Type" + i;
            type.packageName = "sample";
            type.kind = STRUCTURE_TYPE.classT;
            if (i < 2) {
                type.fields.add(new FieldInfo("field", "int", VISIBILITY.privateV));
                type.fields.add(new FieldInfo("another", "int", VISIBILITY.privateV));
                type.methods.add(method(type.qualifiedName, "large", 7, 2));
                type.methods.add(method(type.qualifiedName, "sameSize", 7, 1));
                type.methods.add(method(type.qualifiedName, "empty", 0, 0));
            } else if (i == 2) {
                type.methods.add(method(type.qualifiedName, "small", 1, 0));
            }
            types.add(type);
        }

        MetricsCalculator.Report report = MetricsCalculator.calculate(
                new ProjectInfo(types, List.of("sample"), 123), 2);

        assertEquals(10, report.classCount());
        assertEquals(123, report.applicationLines());
        assertEquals(7, report.methodCount());
        assertEquals(1, report.packageCount());
        assertEquals(List.of("sample.Type0", "sample.Type1"),
                report.topTypesByMethodCount().stream().map(MetricsCalculator.TypeRanking::typeName).toList());
        assertEquals(List.of("sample.Type0", "sample.Type1"), report.intersection());
        assertEquals(List.of("sample.Type0", "sample.Type1"),
                report.typesOverThreshold().stream().map(MetricsCalculator.TypeRanking::typeName).toList());
        assertEquals(2, report.topMethodsByType().get(0).methods().size());
        assertTrue(report.topMethodsByType().get(5).methods().isEmpty());
        assertEquals(2, report.maxParameters());
        assertEquals(List.of("sample.Type0#large()", "sample.Type1#large()"),
                report.methodsWithMaxParameters());
    }

    private MethodInfo method(String typeName, String name, int bodyLines, int parameterCount) {
        return new MethodInfo(name, parameterCount, false, typeName + "#" + name + "()", bodyLines, List.<CallInfo>of());
    }
}
