package prot.csv;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.util.*;

public class Reconciler {
    private int file1HeaderSize = 0;
    private int file2HeaderSize = 0;
    private ReconcilationReport report;

    private final Map<String, CompiledTemplate> f1ColumnTransformers;
    private final Map<String, CompiledTemplate> f2ColumnTransformers;
    private final List<String> f1ColumnDeletions;
    private final List<String> f2ColumnDeletions;

    public Reconciler(String[] transforms, String[] deletes) {
        if (deletes == null || deletes.length == 0) {
            f1ColumnDeletions = Collections.emptyList();
            f2ColumnDeletions = Collections.emptyList();
        } else {
            f1ColumnDeletions = new ArrayList<>(deletes.length);
            f2ColumnDeletions = new ArrayList<>(deletes.length);
            for (String delete : deletes) {
                if (delete.startsWith("f1.")) {
                    f1ColumnDeletions.add(delete.substring(3));
                } else if (delete.startsWith("f2.")) {
                    f2ColumnDeletions.add(delete.substring(3));
                }
            }
        }
        if (transforms == null || transforms.length == 0) {
            f1ColumnTransformers = Collections.emptyMap();
            f2ColumnTransformers = Collections.emptyMap();
        } else {
            int size1 = 0;
            int size2 = 0;
            for (String transform : transforms) {
                if (transform.startsWith("f1.")) {
                    size1++;
                } else if (transform.startsWith("f2.")) {
                    size2++;
                }
            }
            if (size1 > 0) {
                f1ColumnTransformers = new HashMap<>(size1);
            } else {
                f1ColumnTransformers = Collections.emptyMap();
            }
            if (size2 > 0) {
                f2ColumnTransformers = new HashMap<>(size2);
            } else {
                f2ColumnTransformers = Collections.emptyMap();
            }
            for (String transform : transforms) {
                if (transform.startsWith("f1.")) {
                    addTransformColumn(transform, f1ColumnTransformers);
                } else if (transform.startsWith("f2.")) {
                    addTransformColumn(transform, f2ColumnTransformers);
                }
            }
        }
    }

    private void addTransformColumn(String transform, Map<String, CompiledTemplate> colTransformers) {
        String keyExpr = transform.substring(3);
        int eqPos = keyExpr.indexOf('=');
        String key = keyExpr.substring(0, eqPos).trim();
        CompiledTemplate compiled = TemplateCompiler.compileTemplate(keyExpr.substring(eqPos + 1).trim());
        colTransformers.put(key, compiled);
    }

    public ReconcilationReport reconcile(
            List<Map<String, String>> f1, String k1,
            List<Map<String, String>> f2, String k2) {
        file1HeaderSize = f1.getFirst().size();
        file2HeaderSize = f2.getFirst().size();
        if (file1HeaderSize >= file2HeaderSize) {
            report = new ReconcilationReport(f1.getFirst().keySet());
        } else {
            report = new ReconcilationReport(f2.getFirst().keySet());
        }
        Map<String, List<Map<String, String>>> map1 = eval("f1", f1, k1, report.getEmptyKeysInFile1());
        Map<String, List<Map<String, String>>> map2 = eval("f2", f2, k2, report.getEmptyKeysInFile2());
        Set<String> allKeys = new HashSet<>(map1.keySet());
        allKeys.addAll(map2.keySet());
        for (String key : allKeys) {
            List<Map<String, String>> records1 = map1.get(key);
            List<Map<String, String>> records2 = map2.get(key);
            if (records1 != null && records2 != null) {
                System.out.println("Key: " + key);
                System.out.println("Records in f1: " + records1);
                System.out.println("Records in f2: " + records2);
                doReconciliationMany(records1, records2);
            } else if (records1 != null) {
                System.out.println("Key: " + key + " is only in f1");
                System.out.println("Records in f1: " + records1);
                report.getOnlyInFile1().addAll(records1);
            } else if (records2 != null) {
                System.out.println("Key: " + key + " is only in f2");
                System.out.println("Records in f2: " + records2);
                report.getOnlyInFile2().addAll(records2);
            }
            System.out.println("-----------------------------------");
        }
        return report;
    }

    private void doReconciliationMany(List<Map<String, String>> records1, List<Map<String, String>> records2) {
        int size1 = records1.size();
        int size2 = records2.size();
        if (size1 == size2) {
            System.out.println("Records are equal in size: " + size1);
            for (int i = 0; i < size1; i++) {
                Map<String, String> record1 = records1.get(i);
                Map<String, String> record2 = records2.get(i);
                doReconciliationOne(record1, record2);
            }
        } else if (size1 < size2) {
            addOnlyInOneSide(report.getOnlyInFile2(), records2, size1);
        } else {        // size1 > size2
            addOnlyInOneSide(report.getOnlyInFile1(), records1, size2);
        }
    }

    private void doReconciliationOne(Map<String, String> record1, Map<String, String> record2) {
        Map<String, String> row = null;
        Map<String, String> col = null;
        doColumnTransform(f1ColumnTransformers, record1, "f1");
        doColumnTransform(f2ColumnTransformers, record2, "f2");
        if (file1HeaderSize >= file2HeaderSize) {
            row = record1;
            col = record2;
        } else {
            row = record2;
            col = record1;
        }
        for (String rowHeader : row.keySet()) {
            String rowValue = row.get(rowHeader);
            for (String colHeader : col.keySet()) {
                String colValue = col.get(colHeader);
                report.reconcile(rowHeader, rowValue, colHeader, colValue);
            }
        }
    }

    private void doColumnTransform(Map<String, CompiledTemplate> colTransformers, Map<String, String> data, String dataKey) {
        if (colTransformers == null || colTransformers.isEmpty()) {
            return;
        }
        final Map<String, Map<String, String>> context = new HashMap<>(8);
        context.put(dataKey, data);
        for (String key : colTransformers.keySet()) {
            CompiledTemplate compiled = colTransformers.get(key);
            if (compiled != null) {
                String value = (String) TemplateRuntime.execute(compiled, context);
                if (value != null) {
                    data.put(key, value);
                } else {
                    data.remove(key);
                }
            }
        }
    }

    private void addOnlyInOneSide(List<Map<String, String>> holder, List<Map<String, String>> records, int size) {
        for (int i = size + 1; i < records.size(); i++) {
            holder.add(records.get(i));
        }
    }

    private Map<String, List<Map<String, String>>> eval(
            String dataKey, List<Map<String, String>> data,
            String keyExpr, List<Map<String, String>> emptyKeys) {
        CompiledTemplate compiled = TemplateCompiler.compileTemplate("@{" + keyExpr + "}");
        Map<String, List<Map<String, String>>> map = new HashMap<>();
        for (Map<String, String> record : data) {
            final Map<String, Map<String, String>> context = new HashMap<>(8);
            context.put(dataKey, record);
            String keyValue = (String) TemplateRuntime.execute(compiled, context);
            if (keyValue != null) {
                map.computeIfAbsent(keyValue, k -> new java.util.ArrayList<>()).add(record);
            } else {
                emptyKeys.add(record);
            }
        }
        return map;
    }
}
