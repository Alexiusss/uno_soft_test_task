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
            System.err.println("Usage: java -Xmx1G -jar group-lines-1.0.jar <output-file-name>");
            System.exit(1);
        }

        String outputFileName = args[0];

//        long startTime = System.currentTimeMillis();

        List<String[]> splitStringsList = getSplitStringsList(readFile());

        List<Set<String[]>> lists = groupStrings(splitStringsList);

        writeToFile(lists, outputFileName);

//        long endTime = System.currentTimeMillis();
//        System.out.println("================================================");
//        System.out.println("Execution time: " + (endTime - startTime) + " ms");
//        System.out.println("================================================");
    }

    private static List<String[]> getSplitStringsList(BufferedReader reader) {
        return reader.lines()
                .filter(getFilterPredicate())
                .distinct()
                .map(string -> string.split(";", -1))
                .sorted(Comparator.comparingInt(arr -> arr.length))
                .collect(toList());
    }

    private static BufferedReader readFile() throws IOException {
        FileInputStream inputStream = new FileInputStream("./lng-big.csv");
        //InputStream inputStream = new URL("https://github.com/PeacockTeam/new-job/releases/download/v1.0/lng-4.txt.gz").openStream();
        //GZIPInputStream gzip = new GZIPInputStream(inputStream);
        //return new BufferedReader(new InputStreamReader(gzip));
        return new BufferedReader(new InputStreamReader(inputStream));
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
        Map<String, Map<Integer, Set<String[]>>> groupedMap = groupToCommonMap(groupByIndexes(list));

        for (String[] strArr : stringsArrList) {
            Set<String[]> processedGroup = processGroup(strArr, groupedMap);
            if (processedGroup.size() > 1) {
                groupList.add(processedGroup);
            }
        }

        return groupList;
    }

    private static Set<String[]> processGroup(String[] strArr, Map<String, Map<Integer, Set<String[]>>> groupedMap) {
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

    private static void processCurrentStringArr(String[] currentStringArr, Map<String, Map<Integer, Set<String[]>>> groupedMap, Queue<String[]> awaitingGroup) {
        for (int i = 0; i < currentStringArr.length; i++) {
            String currentString = currentStringArr[i];
            if (!currentString.isEmpty()) {
                Set<String[]> strings = groupedMap.get(currentString).remove(i);

                if (strings != null && !strings.isEmpty()) {
                    awaitingGroup.addAll(strings);
                }
            }
        }
    }

    private static Map<String, Map<Integer, Set<String[]>>> groupToCommonMap(List<String[]> list) {
        Map<String, Map<Integer, Set<String[]>>> map = new HashMap<>();

        for (String[] stringArr : list) {
            for (int i = 0; i < stringArr.length; i++) {
                String currentString = stringArr[i];

                map.computeIfAbsent(currentString, k -> new HashMap<>())
                        .computeIfAbsent(i, v -> new HashSet<>())
                        .add(stringArr);
            }
        }
        map.remove("");
        return map;
    }

    private static List<String[]> groupByIndexes(List<String[]> splitStringsList) {
        int maxLength = splitStringsList.get(splitStringsList.size() - 1).length;
        List<String[]> currentList = splitStringsList;
        List<String[]> groupedList = new ArrayList<>();

        for (int i = 0; i < maxLength; i++) {
            int finalI = i;
            Map<String, List<String[]>> map = currentList.stream()
                    .parallel()
                    .filter(arr -> !arr[finalI].isEmpty())
                    .collect(Collectors.groupingBy(arr -> arr[finalI]));

            List<String[]> list = map.values().stream()
                    .parallel()
                    .filter(l -> l.size() > 1)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            currentList = currentList.stream()
                    .parallel()
                    .filter(arr -> arr.length > finalI + 1)
                    .collect(Collectors.toList());

            groupedList.addAll(list);
        }

        return groupedList.stream()
                .distinct()
                .collect(toList());
    }

    private static void writeToFile(List<Set<String[]>> groups, String fileName) {
        List<Set<String[]>> sortedGroups = groups.stream()
                .sorted((arr1, arr2) -> arr2.size() - arr1.size())
                .collect(toList());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./" + fileName))) {
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