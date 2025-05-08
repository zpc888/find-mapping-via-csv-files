package prot.csv;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.cli.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class JsonTreePlantUml {
    public static void main(String[] args) {
        Options options = buildOptions();
        CommandLineParser cliParser = new DefaultParser();
        CommandLine cli = null;
        try {
            cli = cliParser.parse(options, args);
        } catch (ParseException ex) {
            System.out.println("Error parsing command line: " + ex.getMessage());
            showUsage(options, 1);
        }
        String csvFile = cli.getOptionValue("f");
        String parentId = cli.getOptionValue("p");
        String childId = cli.getOptionValue("c");
        String[] nodeAttrs = cli.getOptionValues("n");
        String outputFile = cli.getOptionValue("o");
        String singleAttr = cli.getOptionValue("s");
        if (csvFile == null || parentId == null || childId == null) {
            System.out.println("Error: Missing required options");
            showUsage(options, 2);
        }
        JsonTree jsonTree = new JsonTree(csvFile, parentId, childId, nodeAttrs, singleAttr);
        Map<String, Object> tree = jsonTree.build();
        boolean hasMindMap = singleAttr != null && !singleAttr.isEmpty();
        try {
            JsonMapper mapper = new JsonMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
            json = "@startjson" + System.lineSeparator()
                    + json + System.lineSeparator()
                    + "@endjson" + System.lineSeparator();
            System.out.println(json);
            if (outputFile != null) {
                writeToFile(outputFile, json);
            }
            if (hasMindMap) {
                StringBuilder sb = new StringBuilder(2048);
                toMindMap(sb, tree, 1);
                String mindMap = "@startmindmap" + System.lineSeparator()
                        +  sb.toString() + System.lineSeparator()
                        + "@endmindmap" + System.lineSeparator();
                System.out.println(mindMap);
                if (outputFile != null) {
                    writeToFile(outputFile + "-mindmap.puml", mindMap);
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing JSON output: " + e.getMessage());
        }
    }

    private static void writeToFile(String fileName, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, StandardCharsets.UTF_8))) {
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + fileName + "; error info: " + e.getMessage());
        }
    }

    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                System.err.println("Error closing resource: " + e.getMessage());
            }
        }
    }

    static void toMindMap(StringBuilder sb, Map<String, Object> node, int level) {
        String key = null;
        List<Map<String, Object>> kids = null;
        for (String k: node.keySet()) {
            key = k;
            kids = (List<Map<String, Object>>) node.get(k);
        }
        if (level == 1) {
            sb.append("* ").append(key).append(System.lineSeparator());
        } else {
            sb.append("*".repeat(level)).append("_ ").append(key).append(System.lineSeparator());
        }
        if (kids != null) {
            for (Map<String, Object> kid : kids) {
                toMindMap(sb, kid, level + 1);
            }
        }
    }

    private static Options buildOptions() {
        Options options = new Options();
        options.addOption("f", "file", true, "Input JSON file");
        options.addOption("p", "parent-id", true, "Parent ID Expression");
        options.addOption("c", "child-id", true, "Child ID Expression");
        options.addOption("s", "single-attr", true, "0..1 node attribute pointing to kids directly");
        Option nodeAttrs = new Option("n", "node-attr", true, "0..n node name==>>node value, exclusive with -s");
        nodeAttrs.setArgs(Option.UNLIMITED_VALUES);
        nodeAttrs.setRequired(false);
        options.addOption(nodeAttrs);
        options.addOption("o", "output", true, "Output file");
        return options;
    }

    private static void showUsage(Options options, int status) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(JsonTreePlantUml.class.getSimpleName(), options);
        System.exit(status);
    }
}
