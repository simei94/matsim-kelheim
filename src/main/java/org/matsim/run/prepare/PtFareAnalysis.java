package org.matsim.run.prepare;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PtFareAnalysis {
    public static void main(String[] args) throws IOException {
        String ptFareFile = "/Users/luchengqi/Desktop/pt-trips-fare.csv";
        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

        SimpleRegression regressionShortTrips = new SimpleRegression();
        SimpleRegression regressionLongTrips = new SimpleRegression();
        SimpleRegression regressionAllTrips = new SimpleRegression();

        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(ptFareFile)),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                double fare = Double.parseDouble(record.get(5));
                String tripType = record.get(6);
                Coord from = new Coord(Double.parseDouble(record.get(1)), Double.parseDouble(record.get(2)));
                Coord to = new Coord(Double.parseDouble(record.get(3)), Double.parseDouble(record.get(4)));
                double distance = CoordUtils.calcEuclideanDistance(transformation.transform(from), transformation.transform(to));

                // Add to the linear regression model
                regressionAllTrips.addData(distance, fare);
                if (tripType.equals("short")) {
                    regressionShortTrips.addData(distance, fare);
                } else {
                    regressionLongTrips.addData(distance, fare);
                }
            }
        }

        double slopeShort = regressionShortTrips.getSlope();
        double interceptShort = regressionShortTrips.getIntercept();

        double slopeLong = regressionLongTrips.getSlope();
        double interceptLong = regressionLongTrips.getIntercept();

        double slopeAll = regressionAllTrips.getSlope();
        double interceptAll = regressionAllTrips.getIntercept();

        System.out.println("Slope for short trips is " + slopeShort);
        System.out.println("Intercept for short trips is " + interceptShort);
        System.out.println("Standard error for short trips is" + regressionShortTrips.getSlopeStdErr());
        System.out.println("\n");
        System.out.println("Slope for long trips is " + slopeLong);
        System.out.println("Intercept for long trips is " + interceptLong);
        System.out.println("Standard error for long trips is" + regressionLongTrips.getSlopeStdErr());
        System.out.println("\n");
        System.out.println("Slope for all trips is " + slopeAll);
        System.out.println("Intercept for all trips is " + interceptAll);
        System.out.println("Standard error for all trips is" + regressionAllTrips.getSlopeStdErr());
    }


}
