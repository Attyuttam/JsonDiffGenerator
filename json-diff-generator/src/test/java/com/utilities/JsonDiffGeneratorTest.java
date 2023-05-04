package com.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utilities.utils.JsonDiffHolder;

public class JsonDiffGeneratorTest {
    public static void main(String args[]) throws IOException{
        testJsonDiffGeneratorIsWorkingAsExpected();
    }
    private static void testJsonDiffGeneratorIsWorkingAsExpected() throws IOException{
        JsonDiffGenerator jsonDiffGenerator = new JsonDiffGenerator();
        
        var file = "src\\main\\resources\\sample.json";
        var json = "";
        try {
            json = readFileAsString(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);


        for (int i = 0; i < jsonNode.size(); i++) {
            JsonNode first = jsonNode.get(i).get("first");
            JsonNode second = jsonNode.get(i).get("second");
            System.out.println("Comparing: ");
            System.out.println(first);
            System.out.println(second);
            TreeMap<String,List<JsonDiffHolder>> sortedDiffResult = jsonDiffGenerator.generateDiff(first, second);
            System.out.println(new ObjectMapper().writeValueAsString(sortedDiffResult));
            System.out.println("====================");
        }
    }
    private static String readFileAsString(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)));
    }
}
