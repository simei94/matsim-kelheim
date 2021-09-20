package org.matsim.analysis.preAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.mutable.MutableInt;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
        name = "analyse-leg",
        description = "Analyze leg of the plan"
)
public class LegAnalysis implements MATSimAppCommand {
    @CommandLine.Option(names = "--population", description = "Path to input population", required = true)
    private String populationPath;

    @CommandLine.Option(names = "--person-list", description = "Path to list of persons in consideration (csv file)", required = true)
    private String personsToConsider;

    @CommandLine.Option(names = "--use-network-distance", defaultValue = "false", description = "use network distance " +
            "for distance statistics, otherwise Euclidean distance will be used")
    private boolean useNetworkDistance;

    private final Map<String, MutableInt> modeCount = new HashMap<>();
    private final Map<String, Map<String, MutableInt>> modeCountPerDistanceGroups = new HashMap<>();

    // default constructor to be used by command line
    public LegAnalysis() {
    }

    // constructor to be used by other script
    public LegAnalysis(String populationPath, String personsToConsider, boolean useNetworkDistance) {
        this.personsToConsider = personsToConsider;
        this.populationPath = populationPath;
        this.useNetworkDistance = useNetworkDistance;
    }

    public static void main(String[] args) throws IOException {
        new LegAnalysis().execute(args);
    }

    public void run() throws IOException {
        prepareStatisticsMap();
        Population population = PopulationUtils.readPopulation(populationPath);

        System.out.println("Reading persons living in Kelheim csv file...");
        List<String> personsLivingInKelheim = new ArrayList<>();
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(personsToConsider)),
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
                        modeCount.get(((Leg) planElement).getMode()).increment();
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
        System.out.println("Average trips per person is " + numOfTrips / personInKelheim);

        double sum = 0;
        for (String mode : modeCount.keySet()) {
            sum = sum + modeCount.get(mode).doubleValue();
        }

        double carShare = modeCount.get("car").doubleValue() / sum;
        double rideShare = modeCount.get("ride").doubleValue() / sum;
        double ptShare = modeCount.get("pt").doubleValue() / sum;
        double bikeShare = modeCount.get("bike").doubleValue() / sum;
        double walkShare = modeCount.get("walk").doubleValue() / sum;

        System.out.println("Car share is " + carShare);
        System.out.println("Ride share is " + rideShare);
        System.out.println("PT share is " + ptShare);
        System.out.println("Bike share is " + bikeShare);
        System.out.println("walk share is " + walkShare);
    }

    public Map<String, MutableInt> getModeCount() {
        return modeCount;
    }

    public Map<String, Map<String, MutableInt>> getModeCountPerDistanceGroups() {
        return modeCountPerDistanceGroups;
    }

    @Override
    public Integer call() throws Exception {
        run();
        return 0;
    }

    private void prepareStatisticsMap() {
        modeCount.put("car", new MutableInt(0));
        modeCount.put("ride", new MutableInt(0));
        modeCount.put("pt", new MutableInt(0));
        modeCount.put("bike", new MutableInt(0));
        modeCount.put("walk", new MutableInt(0));
    }
}
