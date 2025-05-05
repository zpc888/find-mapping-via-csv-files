package prot.csv;

import org.apache.commons.csv.CSVFormat;

import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CSVParser {
    public List<Map<String, String>> parse(String csvFile) {
        List<Map<String, String>> result = new ArrayList<>(256);
        try (Reader in = new FileReader(csvFile, StandardCharsets.UTF_8);
                org.apache.commons.csv.CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader().setSkipHeaderRecord(true).get().parse(in)) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            System.out.println(headerMap);
            List<String> headerNames = new ArrayList<>(headerMap.keySet());
            for (org.apache.commons.csv.CSVRecord record : parser) {
                Map<String, String> map = new java.util.HashMap<>();
                for (String headerName : headerNames) {
                    map.put(headerName, record.get(headerName));
                }
                result.add(map);
            }
        } catch (Exception e) {
            throw new RuntimeException("Fail to read CSV file: " + csvFile, e);
        }
        return result;
    }
}
