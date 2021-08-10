package com.presight.ai.services;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.presight.ai.configuration.Config;
import com.presight.ai.entities.*;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CsvGenerator {

    private static final String CALLS_CSV_FILE = "./calls.csv";
    private static final String PEOPLE_CSV_FILE = "./people.csv";

    private static Random random = new Random();
    private static final char[] charArray = IntStream.rangeClosed('a', 'z')
            .mapToObj(c -> "" + (char) c)
            .collect(Collectors.joining()).toCharArray();

    public static void generate() {
        Set<String> phones = generatePersons();
        generateCalls(phones);
    }

    private static void generateCalls(Set<String> phones) {
        List<String> phoneList = new ArrayList<>(phones);
        List<Call> callSet = new ArrayList<>();
        while (callSet.size() < Config.totalCalls) {
            int srcIndex = random.nextInt(phoneList.size());
            String sourcePhone = phoneList.get(srcIndex);

            //replace last with src to avoid calling from same source/des
            String toReplace = phoneList.get(phoneList.size() - 1);
            phoneList.add(srcIndex, toReplace);
            phoneList.add(phoneList.size() - 1, sourcePhone);

            int callsForEachPhone = getRandom(1, Config.maxCallsForEachPhone + 1);
            for (int j = 0; j < callsForEachPhone; j++) {
                int desIndex = random.nextInt(phoneList.size() - 1);
                String desPhone = phoneList.get(desIndex);
                Call call = new Call(sourcePhone, desPhone);
                call.setRegineFrom(randomEnum(RegineTypeEnum.class));
                call.setRegineTo(randomEnum(RegineTypeEnum.class));
                call.setCalTime(getRandom(Config.callTimeSince, LocalDateTime.now(ZoneOffset.UTC)));
                call.setCallDuration(Duration.ofSeconds(getRandom(1, ((int) Config.maxCallDuration.getSeconds()) + 1)));
                callSet.add(call);
                if (callSet.size() == Config.totalCalls)
                    break;
            }
        }

        Collections.sort(callSet, Comparator.comparing(Call::getCalTime));
        writerCsv(Arrays.asList(callSet.toArray()), CALLS_CSV_FILE);
    }

    private static <T extends Enum> T randomEnum(Class<T> clazz) {
        int index = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[index];
    }

    private static LocalDateTime getRandom(LocalDateTime from, LocalDateTime to) {
        Duration between = Duration.between(from, to);
        int randomSec = getRandom(1, ((int) between.getSeconds()) + 1);
        return LocalDateTime.from(from).plus(Duration.ofSeconds(randomSec));
    }

    private static void randomOnMap(Set<String> phones) {
        int mapSize = 10;
        List<String> phoneList = new ArrayList<>(phones);
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < mapSize; i++) {
            map.put(i, phoneList.get(i));
        }

        int size = map.size();

        for (int i = 0; i < size; i++) {
            int currentMapSize = map.size();
            int r = random.nextInt(currentMapSize - i);
            String value = map.get(r);
            System.out.println(value);
            map.replace(r, map.get(currentMapSize - (i + 1)));
        }
    }

    private static Set<String> generatePersons() {
        Map<String, Person> phoneToPeopleMap = new HashMap<>();
        for (int i = 0; i < Config.totalPersons; i++) {
            Person person = new Person(i + 1);

            person.setFirstName(generateName());
            person.setLastName(generateName());

            int phoneNum = getRandom(1, Config.maxPhonesForEachPerson + 1);
            for (int j = 0; j < phoneNum; j++) {
                String phone = generatePhoneNum();
                person.addPhone(phone);
                phoneToPeopleMap.put(phone, person);
            }

            person.setGenderTypeEnum(randomEnum(GenderTypeEnum.class));
            person.setCitizen(random.nextBoolean());

            PersonMetaData personMetaData = new PersonMetaData();
            personMetaData.setHeight(getRandom(Config.minHeightInSm, Config.maxHeightInSm + 1));
            personMetaData.setEyeColor(randomEnum(ColorEnum.class));
            personMetaData.setHairColor(randomEnum(ColorEnum.class));
            person.setPersonMetaData(personMetaData);
        }

        writerCsv(Arrays.asList(phoneToPeopleMap.values().toArray()), PEOPLE_CSV_FILE);
        return phoneToPeopleMap.keySet();
    }

    private static <T> void writerCsv(List<T> personList, String csvFileName) {
        try (Writer writer = Files.newBufferedWriter(Paths.get(csvFileName))) {
            StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .build();

            beanToCsv.write(personList);
        } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            e.printStackTrace();
        }
    }

    private static int getRandom(int fromInclusive, int toExclusive) {
        return random.nextInt(toExclusive - fromInclusive) + fromInclusive;
    }

    private static String generateName() {
        StringBuilder firstName = new StringBuilder();
        int nameLength = getRandom(1, Config.maxCharNum + 1);
        for (int i = 0; i <= nameLength; i++) {
            firstName.append(charArray[random.nextInt(charArray.length - 1)]);
        }
        return firstName.toString();
    }

    private static String generatePhoneNum() {
        StringBuilder phoneNum = new StringBuilder().append("+");
        for (int i = 0; i < Config.phoneDigits; i++) {
            phoneNum.append(random.nextInt(10));
        }
        return phoneNum.toString();
    }
}