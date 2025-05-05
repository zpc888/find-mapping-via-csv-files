package prot.csv;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CSVParserTest {

    @Test
    void parse() {
        // Test with a valid CSV file
        String csvFile = "src/test/resources/csv-parser/person01.csv";
        CSVParser parser = new CSVParser();
        List<Map<String, String>> result = parser.parse(csvFile);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (Map<String, String> map : result) {
            System.out.println(map);
        }
    }
}