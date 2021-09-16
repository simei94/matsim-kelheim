package org.matsim.analysis.preAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.mutable.MutableInt;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegAnalysis {

    public static void main(String[] args) throws IOException {
        String populationFile = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/population-analysis/population.xml.gz";
        String personsLivingInKelheimFile = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/population-analysis/persons-living-in-Kelheim.csv";
        List<String> personsLivingInKelheim = new ArrayList<>();
        Map<String, MutableInt> tripRecordMap = new HashMap<>();
        tripRecordMap.put("car", new MutableInt(0));
        tripRecordMap.put("ride", new MutableInt(0));
        tripRecordMap.put("pt", new MutableInt(0));
        tripRecordMap.put("bike", new MutableInt(0));
        tripRecordMap.put("walk", new MutableInt(0));

        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:25832");
        config.plans().setInputFile(populationFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        System.out.println("Reading persons living in Kelheim csv file...");
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(personsLivingInKelheimFile)),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                personsLivingInKelheim.add(record.get(0));
            }
        }

        System.out.println("Start processing the population file...");
        int processed = 0;
        double personInKelheim = 0;
        double numOfTrips = 0;
        for (Person person : population.getPersons().values()) {
            if (personsLivingInKelheim.contains(person.getId().toString())) {
                personInKelheim++;
                for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                    if (planElement instanceof Leg) {
                        tripRecordMap.get(((Leg) planElement).getMode()).increment();
                        numOfTrips++;
                    }
                }
            }
            processed++;
            if (processed % 5000 == 0) {
                System.out.println("Processing: " + processed + " persons have been processed");
            }
        }
        System.out.println("Complete! Total processed person: " + processed);
        System.out.println("There are " + personInKelheim + " persons living in Kelheim");
        System.out.println("There are in total " + numOfTrips + " made by persons living in Kelheim");
        System.out.println("Average trips per person is " + numOfTrips/personInKelheim);

        double sum = 0;
        for (String mode : tripRecordMap.keySet()) {
            sum = sum + tripRecordMap.get(mode).doubleValue();
        }

        double carShare = tripRecordMap.get("car").doubleValue() / sum;
        double rideShare = tripRecordMap.get("ride").doubleValue() / sum;
        double ptShare = tripRecordMap.get("pt").doubleValue() / sum;
        double bikeShare = tripRecordMap.get("bike").doubleValue() / sum;
        double walkShare = tripRecordMap.get("walk").doubleValue() / sum;

        System.out.println("Car share is " + carShare);
        System.out.println("Ride share is " + rideShare);
        System.out.println("PT share is " + ptShare);
        System.out.println("Bike share is " + bikeShare);
        System.out.println("walk share is " + walkShare);

    }

}
