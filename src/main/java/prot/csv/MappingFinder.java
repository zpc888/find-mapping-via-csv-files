package prot.csv;

import org.apache.commons.cli.*;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public class MappingFinder {
    // --file1 src/test/resources/csv-parser/person01.csv --file2 src/test/resources/csv-parser/person02.csv --key1 "f1['First Name'] + ' ' + f1['Last Name']" --key2 f2.FullName
    public static void main(String[] args) {
//        String csvFile1 = "src/test/resources/csv-parser/person01.csv";
//        String csvFile2 = "src/test/resources/csv-parser/person02.csv";
//        ReconcilationReport report = reconciler.reconcile(f1, "f1['First Name'] + ' ' + f1['Last Name']", f2, "f2.FullName");

        Options options = buildOptions();
        CommandLineParser cliParser = new DefaultParser();
        CommandLine cli = null;
        try {
            cli = cliParser.parse(options, args);
        } catch (ParseException ex) {
            System.out.println("Error parsing command line: " + ex.getMessage());
            showUsage(options, 1);
        }
        String csvFile1 = cli.getOptionValue("f1");
        String csvFile2 = cli.getOptionValue("f2");
        String key1 = cli.getOptionValue("k1");
        String key2 = cli.getOptionValue("k2");
        if (csvFile1 == null || csvFile2 == null || key1 == null || key2 == null) {
            System.out.println("Error: Missing required options");
            showUsage(options, 2);
        }
        String[] transforms = cli.getOptionValues("t");
        String[] deletes = cli.getOptionValues("d");
        String outputFile = cli.getOptionValue("o");
        CSVParser parser = new CSVParser();
        List<Map<String, String>> f1 = parser.parse(csvFile1);
        List<Map<String, String>> f2 = parser.parse(csvFile2);
        Reconciler reconciler = new Reconciler(transforms, deletes);
        ReconcilationReport report = reconciler.reconcile(f1, key1, f2, key2);
        System.out.println();
        System.out.println();
        System.out.println();
        if (outputFile == null || outputFile.isEmpty()) {
            report.report(System.out);
        } else {
            try (PrintStream out = new PrintStream(outputFile)) {
                report.report(System.out, out);
                out.flush();
            } catch (Exception e) {
                System.out.println("Error writing to output file: [" + outputFile + "]");
                e.printStackTrace();
                System.exit(3);
            }
        }
        System.out.println();
        System.out.println();
    }

    private static void showUsage(Options options, int status) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(MappingFinder.class.getSimpleName(), options);
        System.exit(status);
    }

    private static Options buildOptions() {
        Options options = new Options();
        options.addOption("f1", "file1", true, "First csv file to compare");
        options.addOption("f2", "file2", true, "Second csv file to compare");
        options.addOption("k1", "key1", true, "Build unique key expression for first csv file");
        options.addOption("k2", "key2", true, "Build unique key expression for second csv file");
        options.addOption("o", "output", true, "Optional matrix result output file path");
        Option transfer = new Option("t", "transform", true, "Optional a list of column value transformers: E.g. f1.c1 = f1.c2 == 'M' ? 'Male' : 'Female'");
        transfer.setArgs(Option.UNLIMITED_VALUES);
        transfer.setRequired(false);
        options.addOption(transfer);
        Option delete = new Option("d", "delete", true, "Optional a list of columns to be deleted. E.g. f1.c1");
        delete.setArgs(Option.UNLIMITED_VALUES);
        delete.setRequired(false);
        options.addOption(delete);
        return options;
    }
}
