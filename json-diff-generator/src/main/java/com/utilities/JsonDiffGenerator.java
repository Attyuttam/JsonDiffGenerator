package com.utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.text.html.HTMLDocument.Iterator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.utilities.utils.JsonDiffHolder;

public class JsonDiffGenerator {

    public TreeMap<String, List<JsonDiffHolder>> generateDiff(String json1, String json2) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode1 = objectMapper.readTree(json1);
        JsonNode rootNode2 = objectMapper.readTree(json2);

        return generateDiff(rootNode1, rootNode2);
    }

    public TreeMap<String, List<JsonDiffHolder>> generateDiff(JsonNode rootNode1, JsonNode rootNode2)
            throws IOException {
        Map<String, List<JsonDiffHolder>> diffResult = new HashMap<>();

        if (rootNode1.getNodeType() == JsonNodeType.ARRAY && rootNode2.getNodeType() == JsonNodeType.ARRAY) {
            handleJsonArrays(diffResult, rootNode1, rootNode2, ".");
        }

        Map<String, JsonNode> rootNode1Fields = new HashMap<>();
        Map<String, JsonNode> rootNode2Fields = new HashMap<>();

        rootNode1.fields().forEachRemaining(field -> rootNode1Fields.put(field.getKey(), field.getValue()));
        rootNode2.fields().forEachRemaining(field -> rootNode2Fields.put(field.getKey(), field.getValue()));

        // System.out.println(rootNode1Fields);
        // System.out.println(rootNode2Fields);

        Set<String> keysOnlyInRootNode1 = new TreeSet<>();
        Set<String> keysOnlyInRootNode2 = new TreeSet<>();
        Set<String> keysInBothRootNodes = new TreeSet<>();

        keysInBothRootNodes = new HashSet<>(rootNode1Fields.keySet());
        keysInBothRootNodes.retainAll(rootNode2Fields.keySet());

        // System.out.println(rootNode1Fields);
        // System.out.println(rootNode2Fields);

        for (String keyInRootNode1 : rootNode1Fields.keySet()) {
            // System.out.println("Checking for "+keyInRootNode1+" in "+rootNode2Fields);
            if (!rootNode2Fields.containsKey(keyInRootNode1)) {
                keysOnlyInRootNode1.add(keyInRootNode1);
            }
        }

        for (String keyInRootNode2 : rootNode2Fields.keySet()) {
            if (!rootNode1Fields.containsKey(keyInRootNode2)) {
                keysOnlyInRootNode2.add(keyInRootNode2);
            }
        }
        // System.out.println(keysOnlyInRootNode1);
        // System.out.println(keysOnlyInRootNode2);
        // System.out.println(keysInBothRootNodes);

        for (String key : keysOnlyInRootNode1) {
            diffResult.put(key, Arrays.asList(new JsonDiffHolder(rootNode1Fields.get(key), null)));
        }

        for (String key : keysOnlyInRootNode2) {
            diffResult.put(key, Arrays.asList(new JsonDiffHolder(null, rootNode2Fields.get(key))));
        }

        for (String key : keysInBothRootNodes) {
            if (rootNode1Fields.get(key).getNodeType() == JsonNodeType.STRING
                    && rootNode2Fields.get(key).getNodeType() == JsonNodeType.STRING
                    || rootNode1Fields.get(key).getNodeType() == JsonNodeType.NUMBER
                            && rootNode2Fields.get(key).getNodeType() == JsonNodeType.NUMBER) {
                if (!rootNode1Fields.get(key).equals(rootNode2Fields.get(key))) {
                    diffResult.put(key,
                            Arrays.asList(new JsonDiffHolder(rootNode1Fields.get(key), rootNode2Fields.get(key))));
                }

            } else if (rootNode1Fields.get(key).getNodeType() == JsonNodeType.OBJECT
                    && rootNode2Fields.get(key).getNodeType() == JsonNodeType.OBJECT) {

                Map<String, List<JsonDiffHolder>> subResult = generateDiff(rootNode1Fields.get(key),
                        rootNode2Fields.get(key));
                for (Map.Entry<String, List<JsonDiffHolder>> entry : subResult.entrySet()) {
                    diffResult.put(key + "." + entry.getKey(), entry.getValue());
                }

            } else if (rootNode1Fields.get(key).getNodeType() == JsonNodeType.ARRAY
                    && rootNode2Fields.get(key).getNodeType() == JsonNodeType.ARRAY) {
                // handle json arrays
                handleJsonArrays(diffResult, rootNode1Fields.get(key), rootNode2Fields.get(key), key);
            }
        }

        TreeMap<String, List<JsonDiffHolder>> sortedDiffResult = new TreeMap<>();
        sortedDiffResult.putAll(diffResult);

        return sortedDiffResult;
    }

    private void handleJsonArrays(Map<String, List<JsonDiffHolder>> diffResult, JsonNode rootNode1Field,
            JsonNode rootNode2Field, String key) throws IOException {

        if (((rootNode1Field.size() == 0 && rootNode2Field.size() > 0
                && rootNode2Field.get(0).getNodeType() == JsonNodeType.STRING) ||
                (rootNode2Field.size() == 0 && rootNode1Field.size() > 0
                        && rootNode1Field.get(0).getNodeType() == JsonNodeType.STRING)

                ||

                ((rootNode1Field.size() == 0 && rootNode2Field.size() > 0
                        && rootNode2Field.get(0).getNodeType() == JsonNodeType.NUMBER) ||
                        (rootNode2Field.size() == 0 && rootNode1Field.size() > 0
                                && rootNode1Field.get(0).getNodeType() == JsonNodeType.NUMBER)
                
                ||
                
                rootNode1Field.size()>0 && rootNode2Field.size()>0 && rootNode1Field.get(0).getNodeType() == JsonNodeType.NUMBER
                                && rootNode2Field.get(0).getNodeType() == JsonNodeType.NUMBER)
                ||
                rootNode1Field.size()>0 && rootNode2Field.size()>0 && rootNode1Field.get(0).getNodeType() == JsonNodeType.STRING
                        && rootNode2Field.get(0).getNodeType() == JsonNodeType.STRING)) {

            // as of now do a O(n^2) check
            ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<String>>() {
            });
            List<String> stringElementsInJson1 = reader.readValue(rootNode1Field);
            List<String> stringElementsInJson2 = reader.readValue(rootNode2Field);
            Set<String> combinedList = Stream.concat(stringElementsInJson1.stream(), stringElementsInJson2.stream())
                    .collect(Collectors.toSet());

            List<JsonDiffHolder> jsonDiffHolderList = new ArrayList<>();

            for (String e : combinedList) {
                if (stringElementsInJson1.contains(e) && !stringElementsInJson2.contains(e)) {
                    jsonDiffHolderList.add(new JsonDiffHolder(e, ""));
                }
                if (!stringElementsInJson1.contains(e) && stringElementsInJson2.contains(e)) {
                    jsonDiffHolderList.add(new JsonDiffHolder("", e));
                }
            }
            if (jsonDiffHolderList.size() > 0)
                diffResult.put(key, jsonDiffHolderList);
        } else if (rootNode1Field.size() == 0 && rootNode2Field.size() > 0
                && rootNode2Field.get(0).getNodeType() == JsonNodeType.OBJECT 
                
                ||
                rootNode2Field.size() == 0 && rootNode1Field.size() > 0
                        && rootNode1Field.get(0).getNodeType() == JsonNodeType.OBJECT
                ||
                rootNode1Field.size()>0 && rootNode2Field.size()>0 && 
                rootNode1Field.get(0).getNodeType() == JsonNodeType.OBJECT
                && rootNode2Field.get(0).getNodeType() == JsonNodeType.OBJECT) {

            String comparisonKey = "name"; // This key will be passed as a parameter, some way to find the exact path of
                                           // the key parameter

            ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<JsonNode>>() {
            });
            List<JsonNode> rootNode1FieldContentList = reader.readValue(rootNode1Field);
            List<JsonNode> rootNode2FieldContentList = reader.readValue(rootNode2Field);

            int ind1 = 0;
            for (JsonNode rootNode1FieldContent : rootNode1FieldContentList) {
                int ind2 = 0;
                boolean flag = false;
                for (JsonNode rootNode2FieldContent : rootNode2FieldContentList) {
                    if (rootNode1FieldContent.get(comparisonKey).equals(rootNode2FieldContent.get(comparisonKey))) {
                        flag = true;
                        Map<String, List<JsonDiffHolder>> subResult = generateDiff(rootNode1FieldContent,
                                rootNode2FieldContent);

                        for (Map.Entry<String, List<JsonDiffHolder>> entry : subResult.entrySet()) {
                            diffResult.put(key + "[index(JSON 1):" + ind1 + " vs index(JSON 2):" + ind2 + "]."
                                    + entry.getKey(), entry.getValue());
                        }
                    }
                    ind2++;
                }
                if (flag == false) {
                    diffResult.put(key + "[index(JSON 1): " + ind1 + "]:",
                            Collections.singletonList(new JsonDiffHolder(rootNode1FieldContent, "")));
                }
                ind1++;
            }
            ind1 = 0;
            for (JsonNode rootNode2FieldContent : rootNode2FieldContentList) {
                boolean flag = false;
                for (JsonNode rootNode1FieldContent : rootNode1FieldContentList) {
                    if (rootNode1FieldContent.get(comparisonKey).equals(rootNode2FieldContent.get(comparisonKey))) {
                        flag = true;
                        break;
                    }
                }
                if (flag == false) {
                    diffResult.put(key + "[index(JSON 2): " + ind1 + "]:",
                            Collections.singletonList(new JsonDiffHolder("", rootNode2FieldContent)));
                }
                ind1++;
            }
        }else if(rootNode1Field.size() == 0 && rootNode2Field.size() > 0
                && rootNode2Field.get(0).getNodeType() == JsonNodeType.ARRAY 
                
                ||
                rootNode2Field.size() == 0 && rootNode1Field.size() > 0
                        && rootNode1Field.get(0).getNodeType() == JsonNodeType.ARRAY
                ||
                rootNode1Field.size()>0 && rootNode2Field.size()>0 && 
                rootNode1Field.get(0).getNodeType() == JsonNodeType.ARRAY
                && rootNode2Field.get(0).getNodeType() == JsonNodeType.ARRAY){
                    
                    ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<JsonNode>>() {
                    });
                    List<JsonNode> rootNode1FieldContentList = reader.readValue(rootNode1Field);
                    List<JsonNode> rootNode2FieldContentList = reader.readValue(rootNode2Field);

                    List<JsonDiffHolder> jsonDiffHolderList = new ArrayList<>();

                    //only in rootNode1Field
                    for(int i=0;i<rootNode1FieldContentList.size();i++){
                        boolean flag = false;
                        for(int j=0; j<rootNode2FieldContentList.size();j++){
                            if(rootNode1FieldContentList.get(i).equals(rootNode2FieldContentList.get(j))){
                                flag = true;
                                break;
                            }
                        }
                        if(!flag){
                            jsonDiffHolderList.add(new JsonDiffHolder(rootNode1FieldContentList.get(i), ""));
                        }
                    }

                    //only in rootNode2Field
                    for(int i=0;i<rootNode2FieldContentList.size();i++){
                        boolean flag = false;
                        for(int j=0; j<rootNode1FieldContentList.size();j++){
                            if(rootNode2FieldContentList.get(i).equals(rootNode1FieldContentList.get(j))){
                                flag = true;
                                break;
                            }
                        }
                        if(!flag){
                            //need to put into diff result
                            jsonDiffHolderList.add(new JsonDiffHolder("",rootNode2FieldContentList.get(i)));
                        }
                    }
                    if (jsonDiffHolderList.size() > 0)
                        diffResult.put(key, jsonDiffHolderList);
                }
    }
}
