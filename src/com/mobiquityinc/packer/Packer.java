package com.mobiquityinc.packer;

import com.mobiquityinc.exception.APIException;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

/**
 * @author vladov
 *         10/07/2018
 */
public class Packer {

    static class Item {
        private Integer id;
        private Double weight;
        private Integer cost;

        public Item(Integer id, Double weight, Integer cost) {
            this.id = id;
            this.weight = weight;
            this.cost = cost;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Double getWeight() {
            return weight;
        }

        public void setWeight(Double weight) {
            this.weight = weight;
        }

        public Integer getCost() {
            return cost;
        }

        public void setCost(Integer cost) {
            this.cost = cost;
        }

        @Override
        public String toString() {
            return id.toString();
        }
    }

    public static void main(String[] args) throws IOException {
        String absolutePath = args[0];
        try {
            System.out.println(pack(absolutePath));
        } catch (APIException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * finds path to the file
     * generates pack map
     * finds all combos in map
     * prints result
     * @param absolutePath path to file
     * @throws IOException
     * returns best combination
     */
    public static String pack(String absolutePath) throws APIException {
        Path path = Paths.get(absolutePath);
        List<String> contents = null;
        try {
            contents = Files.readAllLines(path);
        } catch (IOException e) {
            throw new APIException("Can't read from path " + path);
        }

        Map<Integer, Set<Item>> packMap = generatePackMap(contents);

        StringBuilder sb = new StringBuilder();
        packMap.forEach((weight, items) -> {
            Set<Set<Item>> allCombinations = findAllCombos(packMap.get(weight));
            Set<Item> maxWeightItems = getItemsWithMaxWeight(allCombinations, weight);
            if (maxWeightItems.isEmpty()) {
                sb.append("- \n");
            } else {
                sb.append(maxWeightItems.toString()
                        .replace("[", "")
                        .replace("]", "") + " \n");
            }
        });
        return sb.toString();
    }

    /**
     * iterating through content
     * if we can't parse key we add items to previous key
     * @param contents list of file content
     * @return map of target weight and items to pack
     */
    private static Map<Integer, Set<Item>> generatePackMap(List<String> contents) {
        Map<Integer, Set<Item>> packMap = new LinkedHashMap<>();
        Iterator<String> iterator = contents.iterator();
        String key = "";
        Set<Item> items = new HashSet<>();
        while (iterator.hasNext()) {
            String[] split = iterator.next().split(":");
            if (split.length > 1) {
                checkAndAddValues(items, key, packMap);
                items = new HashSet<>();
                key = split[0].trim();
                items.addAll(parseItems(split[1]));
            } else {
                items.addAll(parseItems(split[0]));
            }
        }
        checkAndAddValues(items, key, packMap);
        return packMap;
    }

    private static void checkAndAddValues(Set<Item> items, String key, Map<Integer, Set<Item>> packMap) {
        if (!items.isEmpty() && key.length() > 0 && parseInt(key) <= 100 && items.size() <= 15) {
            packMap.put(parseInt(key), items);
        }
    }

    /**
     * parsing items from string of key value pair of items and target weight
     * @param itemsString
     * @return parsed items
     */
    private static List<Item> parseItems(String itemsString) {
        List<Item> items = new ArrayList<>();
        String[] splitItemsArr = itemsString
                .trim()
                .split(" ");

        for (String itemStr : splitItemsArr) {
            String[] singleItemArr = itemStr.substring(1, itemStr.length() - 1).split(",");
            Double weight = parseDouble(singleItemArr[1]);
            Integer cost = parseInt(singleItemArr[2].substring(1));
            if (weight <= 100 && cost <= 100) {
                items.add(new Item(parseInt(singleItemArr[0]),
                        weight,
                        cost));
            }
        }
        return items;
    }

    /**
     * powerset problem
     * recursive alghoritm
     * divides hashset to one element in every iteration and then added them to powerset
     * @param itemSet
     * @return all available combinations of given item set
     */
    private static Set<Set<Item>> findAllCombos(Set<Item> itemSet) {
        Set<Set<Item>> itemsPowerSet = new HashSet<>();
        if (itemSet.isEmpty()) {
            itemsPowerSet.add(new HashSet<>());
            return itemsPowerSet;
        }
        List<Item> list = new ArrayList<>(itemSet);
        Item head = list.get(0);
        Set<Item> rest = new HashSet<>(list.subList(1, list.size()));
        for (Set<Item> set : findAllCombos(rest)) {
            Set<Item> newSet = new HashSet<>();
            newSet.add(head);
            newSet.addAll(set);
            itemsPowerSet.add(newSet);
            itemsPowerSet.add(set);
        }
        return itemsPowerSet;
    }

    /**
     * just trying to find items with max weight in combinations power set
     * iterates trough all combos
     * calculates sum of cost and weight
     * @param allCombinations - all available combos of items
     * @param targetWeight - weight that we must not exceed
     * @return combo with max cost
     */
    private static Set<Item> getItemsWithMaxWeight(Set<Set<Item>> allCombinations, Integer targetWeight) {
        Map<Integer, Set<Item>> sumMap = new HashMap<>();
        for (Set<Item> combo : allCombinations) {
            Pair<Integer, Double> weightCostSum = calculateSum(combo);
            Integer cost = weightCostSum.getKey();
            Double weight = weightCostSum.getValue();
            if (weight <= targetWeight) {
                if (sumMap.containsKey(cost)) {
                    if (sumMap.get(cost).size() < combo.size()) {
                        sumMap.put(cost, combo);
                    }
                } else {
                    sumMap.put(cost, combo);
                }
            }

        }
        Integer maxKey = sumMap.keySet().stream().max(Integer::compareTo).orElse(null);
        return sumMap.get(maxKey);
    }

    private static Pair<Integer, Double> calculateSum(Set<Item> combo) {
        return new Pair<>(
                combo.stream().mapToInt(Item::getCost).sum(),
                combo.stream().mapToDouble(Item::getWeight).sum());
    }
}
