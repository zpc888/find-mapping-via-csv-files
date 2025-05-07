package prot.csv;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.cli.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        if (csvFile == null || parentId == null || childId == null) {
            System.out.println("Error: Missing required options");
            showUsage(options, 2);
        }
        Map<String, Object> tree = new JsonTree(csvFile, parentId, childId, nodeAttrs, cli.getOptionValue("s")).build();
        JsonMapper mapper = new JsonMapper();
        OutputStream os = null;
        Writer writer = null;
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
            if (outputFile != null) {
                os = new FileOutputStream(outputFile);
                writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                writer.write("@startjson");
                writer.write(System.lineSeparator());
                writer.write(json);
                writer.write(System.lineSeparator());
                writer.write("@endjson");
                writer.write(System.lineSeparator());
                writer.flush();
                System.out.println("Tree written to file: " + outputFile);
            } else {
                System.out.println("@startjson");
                System.out.println(json);
                System.out.println("@endjson");
            }
        } catch (IOException e) {
            System.err.println("Error writing JSON output: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error closing writer: " + e.getMessage());
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    System.err.println("Error closing output stream: " + e.getMessage());
                }
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
