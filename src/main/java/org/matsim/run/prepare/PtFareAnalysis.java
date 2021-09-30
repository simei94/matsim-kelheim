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

        SimpleRegression regression = new SimpleRegression();

        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(ptFareFile)),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                double fare = Double.parseDouble(record.get(5));
                String tripType = record.get(6);
                Coord from = new Coord(Double.parseDouble(record.get(1)), Double.parseDouble(record.get(2)));
                Coord to = new Coord(Double.parseDouble(record.get(3)), Double.parseDouble(record.get(4)));
                double distance = CoordUtils.calcEuclideanDistance(transformation.transform(from), transformation.transform(to));
                if (tripType.equals("short")) {
                    distance -= 1;
                    fare -= 2;
                    if (distance<0){
                        distance = 0;
                        fare = 0;
                    }
                    regression.addData(distance, fare);
                }
            }

        }

        double slope = regression.getSlope();
        double intercept = regression.getIntercept();

        System.out.println("Slope is " + slope);
        System.out.println("Intercept is " + intercept);
        System.out.println("Standard error is " + regression.getSlopeStdErr());


    }


}
