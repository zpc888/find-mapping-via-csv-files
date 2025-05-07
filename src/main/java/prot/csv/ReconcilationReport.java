package prot.csv;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReconcilationReport {
    private static final int LEN_TO_SHORT_TOO_COMPARE = 2;

    private final List<Map<String, String>> emptyKeysInFile1 = new java.util.ArrayList<>();
    private final List<Map<String, String>> emptyKeysInFile2 = new java.util.ArrayList<>();
    private final List<Map<String, String>> onlyInFile1 = new java.util.ArrayList<>();
    private final List<Map<String, String>> onlyInFile2 = new java.util.ArrayList<>();
    private final Map<String, Map<String, FieldResult>> matrix;

    public ReconcilationReport(Set<String> fieldNames) {
        matrix = new java.util.HashMap<>(fieldNames.size());
        for (String fieldName : fieldNames) {
            matrix.put(fieldName, new java.util.HashMap<>(64));
        }
    }

    public List<Map<String, String>> getEmptyKeysInFile1() {
        return emptyKeysInFile1;
    }

    public List<Map<String, String>> getEmptyKeysInFile2() {
        return emptyKeysInFile2;
    }

    public List<Map<String, String>> getOnlyInFile1() {
        return onlyInFile1;
    }

    public List<Map<String, String>> getOnlyInFile2() {
        return onlyInFile2;
    }

    private List<String> getOrderedNonEmptyRowHeaders() {
        List<String> ret = new java.util.ArrayList<>(matrix.size());
        for (String header: matrix.keySet()) {
            Map<String, FieldResult> row = matrix.get(header);
            if (row != null && !row.isEmpty()) {
                ret.add(header);
            }
        }
        ret.sort(String::compareTo);
        return ret;
    }

    private List<String> getOrderedNonEmptyColHeaders() {
        List<String> ret = new java.util.ArrayList<>(64);
        for (Map<String, FieldResult> row : matrix.values()) {
            for (String header : row.keySet()) {
                if (!ret.contains(header)) {
                    FieldResult fieldResult = row.get(header);
                    if (fieldResult != null && !fieldResult.isEmpty()) {
                        ret.add(header);
                    }
                }
            }
        }
        ret.sort(String::compareTo);
        return ret;
    }

    public void report(PrintStream out) {
        report(out, out);
    }
    public void report(PrintStream overallOut, PrintStream matrixOut) {
        overallOut.println();
        overallOut.println();
        overallOut.println("Reconcilation Report:");
        overallOut.println("Empty Keys in File 1:" + emptyKeysInFile1);
        overallOut.println("Empty Keys in File 2:" + emptyKeysInFile2);
        overallOut.println("Only in File 1      :" + onlyInFile1);
        overallOut.println("Only in File 2      :" + onlyInFile2);
        overallOut.println("Matrix:");
        List<String> rowHeaders = getOrderedNonEmptyRowHeaders();
        List<String> colHeaders = getOrderedNonEmptyColHeaders();
        matrixOut.print("Matrix");
        if (overallOut != matrixOut) {
            overallOut.print("Matrix");
        }
        for (String colHeader : colHeaders) {
            matrixOut.printf(",\"%s\"", colHeader);
            if (overallOut != matrixOut) {
                overallOut.printf(",\"%s\"", colHeader);
            }
        }
        matrixOut.println();
        if (overallOut != matrixOut) {
            overallOut.println();
        }
        for (String rowHeader : rowHeaders) {
            matrixOut.printf("\"%s\"", rowHeader);
            if (overallOut != matrixOut) {
                overallOut.printf("\"%s\"", rowHeader);
            }
            Map<String, FieldResult> rowMap = matrix.get(rowHeader);
            for (String colHeader : colHeaders) {
                FieldResult fieldResult = rowMap.get(colHeader);
                if (fieldResult == null || fieldResult.isEmpty()) {
                    matrixOut.print(",");
                    if (overallOut != matrixOut) {
                        overallOut.print(",");
                    }
                } else {
                    matrixOut.printf(",\"%s\"", fieldResult);
                    if (overallOut != matrixOut) {
                        overallOut.printf(",\"%s\"", fieldResult);
                    }
                }
            }
            matrixOut.println();
            if (overallOut != matrixOut) {
                overallOut.println();
            }
        }
    }

    public void reconcile(String rowHeader, String rowValue, String colHeader, String colValue) {
        if (rowValue == null || colValue == null || rowValue.isEmpty() || colValue.isEmpty()) {
            return;
        }
        Map<String, FieldResult> rowMap = matrix.get(rowHeader);
        rowMap.computeIfAbsent(colHeader, k -> new FieldResult()).doReconcile(rowValue, colValue);
    }

    private static class FieldResult {
        int eqCounter;  // f1 field1 value = f2 field2 value
        int gtCounter;  // f1 field1 value contains f2 field2 value assuming f1 has more fields, f1.fields are rows
        int ltCounter;  // f1 field1 value is contained in f2 field2 value assuming f1 has more fields, f1.fields are rows

        public void doReconcile(String rowValue, String colValue) {
            rowValue = rowValue.trim().toUpperCase();
            colValue = colValue.trim().toUpperCase();
            if (rowValue.equals(colValue)) {
                eqCounter++;
                return;
            }
            if (rowValue.equals("TRUE") || rowValue.equals("FALSE")
                    || colValue.equals("TRUE") || colValue.equals("FALSE")) {
                // boolean value, do not compare containing or be-contained
                return;
            }
            if (rowValue.length() <= LEN_TO_SHORT_TOO_COMPARE || colValue.length() <= LEN_TO_SHORT_TOO_COMPARE) {
                // too short to compare
                return;
            }
            if (rowValue.contains(colValue)) {
                gtCounter++;
            } else if (colValue.contains(rowValue)) {
                ltCounter++;
            }
        }

        public boolean isEmpty() {
            return eqCounter == 0 && gtCounter == 0 && ltCounter == 0;
        }

        @Override
        public String toString() {
            return String.format("%d,%d,%d", eqCounter, gtCounter, ltCounter);
        }
    }
}
