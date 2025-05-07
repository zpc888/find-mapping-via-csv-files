package prot.csv;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.util.*;

// -f src/test/resources/json-tree/employee.csv -p f.managerId -c f.id
// -n "f.name==>>f.age"
// -s "f.name"
//
public class JsonTree {
    private static final String KIDS_ATTR = "__KIDS__";
    private final String csvFile;
    private final String parentId;
    private final String childId;
    private final String[] nodeAttrs;
    private final String singleAttr;
    private final CompiledTemplate singleAttrExpr;
    private final CompiledTemplate parentExpr;
    private final CompiledTemplate childExpr;
    private final Map<CompiledTemplate, CompiledTemplate> nodeAttrsExpr;

    public JsonTree(String csvFile, String parentId, String childId, String[] nodeAttrs, String singleAttr) {
        this.csvFile = csvFile;
        this.parentId = parentId;
        this.childId = childId;
        this.parentExpr = TemplateCompiler.compileTemplate("@{" + parentId + "}");
        this.childExpr = TemplateCompiler.compileTemplate("@{" + childId + "}");
        this.nodeAttrs = nodeAttrs;
        this.singleAttr = singleAttr;
        if (this.singleAttr != null && !this.singleAttr.isEmpty()) {
            this.singleAttrExpr = TemplateCompiler.compileTemplate("@{" + this.singleAttr + "}");
        } else {
            this.singleAttrExpr = null;
        }
        if (nodeAttrs == null || nodeAttrs.length == 0) {
            this.nodeAttrsExpr = Collections.emptyMap();
            return;
        }
        this.nodeAttrsExpr = new java.util.HashMap<>(nodeAttrs.length);
        for (String nodeAttr : nodeAttrs) {
            String[] parts = nodeAttr.split("==>>");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid node attribute format: " + nodeAttr);
            }
            String name = parts[0].trim();
            String value = parts[1].trim();
            CompiledTemplate nameExpr = TemplateCompiler.compileTemplate("@{" + name + "}");
            CompiledTemplate valueExpr = TemplateCompiler.compileTemplate("@{" + value + "}");
            nodeAttrsExpr.put(nameExpr, valueExpr);
        }

    }

    public Map<String, Object> build() {
        List<Map<String, String>> rows = new CSVParser().parse(csvFile);
        Map<String, Map<String, Object>> byIds = new HashMap<>(rows.size());
        final Map<String, String> toParents = new HashMap<>(rows.size());
        for (Map<String, String> row : rows) {
            final Map<String, Map<String, String>> ctx = new HashMap<>(4);
            ctx.put("f", row);
            String parentIdVal = (String) TemplateRuntime.execute(parentExpr, ctx);
            String childIdVal = (String) TemplateRuntime.execute(childExpr, ctx);
            byIds.put(childIdVal, new HashMap<>(row));
            toParents.put(childIdVal, parentIdVal);
        }
        List<Map<String, Object>> roots = new ArrayList<>(rows.size());
        for (String childIdVal : byIds.keySet()) {
            Map<String, Object> row = byIds.get(childIdVal);
            String parentIdVal = toParents.get(childIdVal);
            if (parentIdVal == null || parentIdVal.isEmpty()) {
                roots.add(row);
            } else {
                Map<String, Object> parentRow = byIds.get(parentIdVal);
                if (parentRow != null) {
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parentRow
                            .computeIfAbsent(KIDS_ATTR, k -> new ArrayList<>(16));
                    children.add(row);
                } else {
                    System.err.println("Error: Parent ID " + parentIdVal + " not found for child ID " + childIdVal);
                }
            }
        }
        if (roots.isEmpty()) {
            System.out.println("Error: No root nodes found.");
            return Collections.emptyMap();
        } else {
            boolean changeOutput = shouldChangeOutputNodeShape();
            if (roots.size() == 1) {
                Map<String, Object> ret = roots.getFirst();
                return !changeOutput ? ret : buildOutputNode(ret);
            } else {
                Map<String, Object> root = new HashMap<>(16);
                List<Map<String, Object>> nRoots = roots;
                if (changeOutput) {
                    nRoots = roots.stream().map(this::buildOutputNode).toList();
                }
                root.put("__MULTI-ROOTS_", nRoots);
                return root;
            }
        }
    }

    private boolean shouldChangeOutputNodeShape() {
        return (nodeAttrs != null && nodeAttrs.length > 0) || (singleAttr != null && !singleAttr.isEmpty());
    }

    private Map<String, Object> buildOutputNode(Map<String, Object> row) {
        if (!shouldChangeOutputNodeShape()) {
            return row;
        }
        Map<String, Object> outputNode = new HashMap<>(row.size() + 1);
        final Map<String, Map<String, Object>> ctx = new HashMap<>(4);
        ctx.put("f", row);
        if (singleAttr != null && !singleAttr.isEmpty()) {
            // single attribute directly pointing to kids
            String singleAttrKey = (String) TemplateRuntime.execute(singleAttrExpr, ctx);
            List<Map<String, Object>> kids = (List<Map<String, Object>>) row.get(KIDS_ATTR);
            if (kids == null) {
                kids = Collections.emptyList();
            } else {
                kids = kids.stream().map(this::buildOutputNode).toList();
            }
            outputNode.put(singleAttrKey, kids);
        } else {
            for (Map.Entry<CompiledTemplate, CompiledTemplate> entry : nodeAttrsExpr.entrySet()) {
                String name = (String) TemplateRuntime.execute(entry.getKey(), ctx);
                String value = (String) TemplateRuntime.execute(entry.getValue(), ctx);
                outputNode.put(name, value);
                List<Map<String, Object>> kids = (List<Map<String, Object>>) row.get(KIDS_ATTR);
                if (kids != null && !kids.isEmpty()) {
                    outputNode.put(KIDS_ATTR, kids.stream().map(this::buildOutputNode).toList());
                }
            }
        }
        return outputNode;
    }
}
