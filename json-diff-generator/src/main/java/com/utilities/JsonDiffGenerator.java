package com.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.utilities.utils.JsonDiffHolder;

public class JsonDiffGenerator {
    public static void main(String[] args) throws IOException {
        var file1 = "src\\main\\resources\\json1.json";
        var file2 = "src\\main\\resources\\json2.json";
        var json1 = "";
        var json2 = "";
        try {
            json1 = readFileAsString(file1);
            json2 = readFileAsString(file2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        generateDiff(json1, json2);
    }

    private static void generateDiff(String json1, String json2) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode1 = objectMapper.readTree(json1);
        JsonNode rootNode2 = objectMapper.readTree(json2);

        TreeMap<String,List<JsonDiffHolder>> sortedDiffResult = new TreeMap<>();
        sortedDiffResult.putAll(generateDiff(rootNode1, rootNode2));
        System.out.println(new ObjectMapper().writeValueAsString(sortedDiffResult));
    }

    private static Map<String,List<JsonDiffHolder>> generateDiff(JsonNode rootNode1, JsonNode rootNode2) throws IOException {
        Map<String,List<JsonDiffHolder>> diffResult = new HashMap<>();

        Map<String, JsonNode> rootNode1Fields = new HashMap<>();
        Map<String, JsonNode> rootNode2Fields = new HashMap<>();

        rootNode1.fields().forEachRemaining(field -> rootNode1Fields.put(field.getKey(), field.getValue()));
        rootNode2.fields().forEachRemaining(field -> rootNode2Fields.put(field.getKey(), field.getValue()));

        Set<String> keysOnlyInRootNode1 = new TreeSet<>();
        Set<String> keysOnlyInRootNode2 = new TreeSet<>();
        Set<String> keysInBothRootNodes = new TreeSet<>();

        keysInBothRootNodes = rootNode1Fields.keySet();
        keysInBothRootNodes.retainAll(rootNode2Fields.keySet());

        for(String keyInRootNode1 : rootNode1Fields.keySet()){
            if(!rootNode2Fields.containsKey(keyInRootNode1)){
                keysOnlyInRootNode1.add(keyInRootNode1);
            }
        }
        
        for(String keyInRootNode2 : rootNode2Fields.keySet()){
            if(!rootNode1Fields.containsKey(keyInRootNode2)){
                keysOnlyInRootNode2.add(keyInRootNode2);
            }
        }

        for(String key : keysOnlyInRootNode1){
            diffResult.put(key, Arrays.asList(new JsonDiffHolder(rootNode1Fields.get(key), null)));
        }

        for(String key : keysOnlyInRootNode2){
            diffResult.put(key, Arrays.asList(new JsonDiffHolder(null,rootNode2Fields.get(key))));
        }

        for(String key : keysInBothRootNodes){
            if(rootNode1Fields.get(key).getNodeType() == JsonNodeType.STRING && rootNode2Fields.get(key).getNodeType() == JsonNodeType.STRING 
            || rootNode1Fields.get(key).getNodeType() == JsonNodeType.NUMBER && rootNode2Fields.get(key).getNodeType() == JsonNodeType.NUMBER){
                if(!rootNode1Fields.get(key).equals(rootNode2Fields.get(key))){
                    diffResult.put(key, Arrays.asList(new JsonDiffHolder(rootNode1Fields.get(key), rootNode2Fields.get(key))));
                }

            }else if(rootNode1Fields.get(key).getNodeType() == JsonNodeType.OBJECT && rootNode2Fields.get(key).getNodeType() == JsonNodeType.OBJECT){

                Map<String,List<JsonDiffHolder>> subResult = generateDiff(rootNode1Fields.get(key), rootNode2Fields.get(key));
                for(Map.Entry<String, List<JsonDiffHolder>> entry : subResult.entrySet()){
                    diffResult.put(key+"."+entry.getKey(),entry.getValue());
                }

            }else if(rootNode1Fields.get(key).getNodeType() == JsonNodeType.ARRAY && rootNode2Fields.get(key).getNodeType() == JsonNodeType.ARRAY){
                //handle json arrays   
                if(rootNode1Fields.get(key).get(0).getNodeType() == JsonNodeType.STRING && rootNode2Fields.get(key).get(0).getNodeType() == JsonNodeType.STRING  ){
                    //as of now do a O(n^2) check
                    ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<String>>() {});
                    List<String> stringElementsInJson1 = reader.readValue(rootNode1Fields.get(key));
                    List<String> stringElementsInJson2 = reader.readValue(rootNode2Fields.get(key));
                    List<String> combinedList = Stream.concat(stringElementsInJson1.stream(), stringElementsInJson2.stream()).collect(Collectors.toList());
                    
                    List<JsonDiffHolder> jsonDiffHolderList = new ArrayList<>();
                    
                    for(String e : combinedList){
                        if(stringElementsInJson1.contains(e) && !stringElementsInJson2.contains(e)){
                            jsonDiffHolderList.add(new JsonDiffHolder(e, ""));
                        }
                        if(!stringElementsInJson1.contains(e) && stringElementsInJson2.contains(e)){
                            jsonDiffHolderList.add(new JsonDiffHolder("",e));
                        }
                    }
                    if(jsonDiffHolderList.size()>0)
                        diffResult.put(key,jsonDiffHolderList);
                } else if(rootNode1Fields.get(key).get(0).getNodeType() == JsonNodeType.OBJECT && rootNode2Fields.get(key).get(0).getNodeType() == JsonNodeType.OBJECT){
                    String comparisonKey = "name"; //This key will be passed as a parameter, some way to find the exact path of the key parameter
                    
                    ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<JsonNode>>() {});
                    List<JsonNode> rootNode1FieldsList = reader.readValue(rootNode1Fields.get(key));
                    List<JsonNode> rootNode2FieldsList = reader.readValue(rootNode2Fields.get(key));

                    int ind1 = 0;
                    for(JsonNode rootNode1Field : rootNode1FieldsList){
                        int ind2 = 0;
                        boolean flag = false;
                        for(JsonNode rootNode2Field : rootNode2FieldsList){
                            if(rootNode1Field.get(comparisonKey).equals(rootNode2Field.get(comparisonKey))){
                                flag = true;
                                Map<String,List<JsonDiffHolder>> subResult = generateDiff(rootNode1Field, rootNode2Field);
    
                                for(Map.Entry<String, List<JsonDiffHolder>> entry : subResult.entrySet()){
                                    diffResult.put(key+"[index(JSON 1):"+ind1+" vs index(JSON 2):"+ind2+"]."+entry.getKey(),entry.getValue());
                                }
                            }
                            ind2++;
                        }
                        if(flag == false){
                            diffResult.put(key+"[index(JSON 1): "+ind1+"]:",Collections.singletonList(new JsonDiffHolder(rootNode1Field,"")));
                        }
                        ind1++;
                    }
                    ind1 = 0;
                    for(JsonNode rootNode2Field : rootNode2FieldsList){
                        boolean flag = false;
                        for(JsonNode rootNode1Field : rootNode1FieldsList){
                            if(rootNode1Field.get(comparisonKey).equals(rootNode2Field.get(comparisonKey))){
                                flag = true;
                                break;
                            }
                        }
                        if(flag == false){
                            diffResult.put(key+"[index(JSON 2): "+ind1+"]:",Collections.singletonList(new JsonDiffHolder("",rootNode2Field)));
                        }
                        ind1++;
                    }
                }
            }
        }
        return diffResult;
    }

    public static String readFileAsString(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)));
    }

}
