package org.matsim.analysis.postAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.mutable.MutableDouble;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
        name = "score-analysis",
        description = "Analyze scoring of selected person"
)
public class ScoreAnalysis implements MATSimAppCommand {
    @CommandLine.Option(names = "--plans", description = "Path to plans with score", required = true)
    private String plansPath;

    @CommandLine.Option(names = "--person-list", description = "Path to list of persons in consideration (csv file)", required = true)
    private String personsToConsider;

    public static void main(String[] args) {
        new ScoreAnalysis().execute(args);
    }


    @Override
    public Integer call() throws Exception {
        System.out.println("Reading persons living in Kelheim csv file...");
        List<String> personsLivingInKelheim = new ArrayList<>();
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(personsToConsider)),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                personsLivingInKelheim.add(record.get(0));
            }
        }
        System.out.println("There are " + personsLivingInKelheim.size() + " persons living in Kelheim");

        Population population = PopulationUtils.readPopulation(plansPath);
        System.out.println("Plan loaded. There total population size is " + population.getPersons().size());

        double numOfPersons = 0;
        Map<String, MutableDouble> recordMap = new HashMap<>();
        recordMap.put("best", new MutableDouble(0));
        recordMap.put("worst", new MutableDouble(0));
        recordMap.put("average", new MutableDouble(0));
        recordMap.put("executed", new MutableDouble(0));

        for (Person person : population.getPersons().values()) {
            if (!personsLivingInKelheim.contains(person.getId().toString())) {
                continue;
            }
            double sum = 0;
            double numOfPlans = 0;
            double bestScore = Double.MAX_VALUE * -1;
            double worstScore = Double.MAX_VALUE;
            for (Plan plan : person.getPlans()) {
                double score = plan.getScore();
                sum += score;
                numOfPlans++;
                if (score > bestScore) {
                    bestScore = score;
                }
                if (score < worstScore) {
                    worstScore = score;
                }
            }
            double averageScore = sum / numOfPlans;
            double executedScore = person.getSelectedPlan().getScore();

            recordMap.get("best").add(bestScore);
            recordMap.get("worst").add(worstScore);
            recordMap.get("average").add(averageScore);
            recordMap.get("executed").add(executedScore);
            numOfPersons++;
        }

        // Print result
        System.out.println("Average Best Score: " + recordMap.get("best").doubleValue() / numOfPersons);
        System.out.println("Average Worst Score: " + recordMap.get("worst").doubleValue() / numOfPersons);
        System.out.println("Average of Average Score: " + recordMap.get("average").doubleValue() / numOfPersons);
        System.out.println("Average executed Score: " + recordMap.get("executed").doubleValue() / numOfPersons);

        return 0;
    }
}
