package com.example;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class GroupLines {

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Usage: java -Xmx1G -jar group-lines-1.0.jar <test-file-name-absolute-path>");
            System.exit(1);
        }

        String testFileName = args[0];

        List<String[]> splitStringsList = getSplitStringsList(readFile(testFileName));

        List<Set<String[]>> lists = groupStrings(splitStringsList);

        writeToFile(lists);
    }

    private static List<String[]> getSplitStringsList(BufferedReader reader) {
        return reader.lines()
                .filter(getFilterPredicate())
                .distinct()
                .map(string -> string.split(";", -1))
                .sorted(Comparator.comparingInt(arr -> arr.length))
                .collect(toList());
    }

    private static BufferedReader readFile(String file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        InputStreamReader reader = new InputStreamReader(inputStream);
        return new BufferedReader(reader);
    }

    private static Predicate<String> getFilterPredicate() {
        Pattern pattern = Pattern.compile("^;*(\"[^\";]*\"(;\"[^\";]*\")*;*)+$");
        return string -> {
            Matcher matcher = pattern.matcher(string);
            return matcher.find();
        };
    }

    private static List<Set<String[]>> groupStrings(List<String[]> list) {
        List<Set<String[]>> groupList = new ArrayList<>();

        List<String[]> stringsArrList = groupByIndexes(list);
        Map<String, Map<Integer, List<String[]>>> groupedMap = groupToCommonMap(stringsArrList);

        for (String[] strArr : stringsArrList) {
            Set<String[]> processedGroup = processGroup(strArr, groupedMap);
            if (processedGroup.size() > 1) {
                groupList.add(processedGroup);
            }
        }

        return groupList;
    }

    private static Set<String[]> processGroup(String[] strArr, Map<String, Map<Integer, List<String[]>>> groupedMap) {
        Set<String[]> processedGroup = new HashSet<>();
        Queue<String[]> awaitingGroup = new LinkedList<>();
        awaitingGroup.add(strArr);

        while (!awaitingGroup.isEmpty()) {
            String[] currentStringArr = awaitingGroup.poll();

            if (!processedGroup.add(currentStringArr)) {
                continue;
            }

            processCurrentStringArr(currentStringArr, groupedMap, awaitingGroup);
        }

        return processedGroup;
    }

    private static void processCurrentStringArr(String[] currentStringArr, Map<String, Map<Integer, List<String[]>>> groupedMap, Queue<String[]> awaitingGroup) {
        for (int i = 0; i < currentStringArr.length; i++) {
            String currentString = currentStringArr[i];
            if (!currentString.isEmpty() && !currentString.contains("\"\"")) {
                List<String[]> strings = groupedMap.get(currentString).remove(i);

                if (strings != null && !strings.isEmpty()) {
                    awaitingGroup.addAll(strings);
                }
            }
        }
    }

    private static Map<String, Map<Integer, List<String[]>>> groupToCommonMap(List<String[]> list) {
        Map<String, Map<Integer, List<String[]>>> map = new HashMap<>();

        for (String[] stringArr : list) {
            for (int i = 0; i < stringArr.length; i++) {
                String currentString = stringArr[i];

                map.computeIfAbsent(currentString, k -> new HashMap<>(3))
                        .computeIfAbsent(i, v -> new ArrayList<>(3))
                        .add(stringArr);
            }
        }
        map.remove("\"\"");
        return map;
    }

    private static List<String[]> groupByIndexes(List<String[]> splitStringsList) {
        int maxLength = splitStringsList.get(splitStringsList.size() - 1).length;
        List<String[]> currentList = splitStringsList;
        List<String[]> groupedList = new ArrayList<>();

        for (int i = 0; i < maxLength; i++) {
            int finalI = i;
            Map<String, List<String[]>> map = currentList.stream()
                    .filter(arr -> !arr[finalI].isEmpty())
                    .collect(Collectors.groupingBy(arr -> arr[finalI]));

            List<String[]> list = map.values().stream()
                    .filter(l -> l.size() > 1)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            currentList = currentList.stream()
                    .filter(arr -> arr.length > finalI + 1)
                    .collect(Collectors.toList());

            groupedList.addAll(list);
        }

        return groupedList.stream()
                .distinct()
                .collect(toList());
    }

    private static void writeToFile(List<Set<String[]>> groups) {
        List<Set<String[]>> sortedGroups = groups.stream()
                .sorted((arr1, arr2) -> arr2.size() - arr1.size())
                .collect(toList());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./result.txt"))) {
            writer.write("Общее количество групп: " + groups.size());
            writer.newLine();
            writer.newLine();

            for (int i = 0; i < groups.size(); i++) {
                writer.write("Группа " + (i + 1));
                writer.newLine();
                writer.newLine();
                sortedGroups.get(i).forEach(arr -> {
                    try {
                        writer.write(printformattedString(arr));
                        writer.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String printformattedString(String[] arr) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i < arr.length - 1) {
                builder.append(";");
            }
        }
        return builder.toString();
    }
}