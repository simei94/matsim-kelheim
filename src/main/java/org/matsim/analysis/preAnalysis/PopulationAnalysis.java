package org.matsim.analysis.preAnalysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

public class PopulationAnalysis implements MATSimAppCommand {
    public static void main(String[] args) throws IOException {
        new PopulationAnalysis().execute(args);
    }

    @CommandLine.Option(names= "--population", description = "Path to input population", required = true)
    private String populationPath;

    @CommandLine.Option(names= "--output", description = "Path to analysis output", required = true)
    private String outputPath;

    @Override
    public Integer call() throws Exception {
        String shapeFilePath = "./scenarios/input/shp/dilutionArea.shp";
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:25832");
        config.plans().setInputFile(populationPath);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFilePath);
        if (features.size() != 1) {
            throw new RuntimeException("Please use the shapefile that only contain Kelheim region");
        }
        Geometry kelheim = (Geometry) features.iterator().next().getDefaultGeometry();

        FileWriter csvWriter = new FileWriter(outputPath);
        csvWriter.append("person");
        csvWriter.append(",");
        csvWriter.append("home_x");
        csvWriter.append(",");
        csvWriter.append("home_y");
        csvWriter.append("\n");

        System.out.println("Start processing population file...");
        int counter = 0;
        int homelessPersons = 0;
        for (Person person : population.getPersons().values()) {
            boolean homeless = true;
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    String actType = ((Activity) planElement).getType();
                    if (actType.startsWith("home")) {
                        Coord homeCoord = ((Activity) planElement).getCoord();
                        homeless = false;
//                        if (kelheim.contains(MGC.coord2Point(homeCoord))) {
                        csvWriter.append(person.getId().toString());
                        csvWriter.append(",");
                        csvWriter.append(Double.toString(homeCoord.getX()));
                        csvWriter.append(",");
                        csvWriter.append(Double.toString(homeCoord.getY()));
                        csvWriter.append("\n");
                        counter += 1;
//                        }
                        break;
                    }
                }
            }

            if (homeless) {
                homelessPersons += 1;
            }
        }
        csvWriter.close();
//        System.out.println("There are " + counter + " persons living in Kelheim area (with home location within the Kelheim and its neighboring area)");
        System.out.println("There are " + counter + " persons with home activity");
        System.out.println("There are " + homelessPersons + " persons without any home activity");

        return 0;
    }
}
