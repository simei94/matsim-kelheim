package org.matsim.analysis.preAnalysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;


@CommandLine.Command(
        name = "trips-within-shapeFile",
        description = "Analyze the number of trips within the shape file"
)
public class TripsWithinAVServiceArea implements MATSimAppCommand {
    @CommandLine.Option(names = "--input", description = "Path to input plans", required = true)
    private String inputPlansFile;

    @CommandLine.Option(names = "--output-folder", description = "Path to analysis output folder", required = true)
    private String outputFolder;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();


    public static void main(String[] args) {
        new TripsWithinAVServiceArea().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Population inputPlans = PopulationUtils.readPopulation(inputPlansFile);
        Geometry serviceArea = shp.getGeometry();
        Geometry bufferedServiceArea = serviceArea.buffer(1000); // maximum walking distance for AV

        int counter = 0;
        List<TripStructureUtils.Trip> potentialTrips = new ArrayList<>();
        for (Person person: inputPlans.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
            for (TripStructureUtils.Trip trip: trips) {
                Coord from = trip.getOriginActivity().getCoord();
                Coord to = trip.getDestinationActivity().getCoord();
                if (MGC.coord2Point(from).within(bufferedServiceArea) && MGC.coord2Point(to).within(bufferedServiceArea)){
                    potentialTrips.add(trip);
                    counter++;
                }
            }
        }

        System.out.println("There are " + potentialTrips.size() + " potential trips within the AV service area");

        return 0;
    }


}
