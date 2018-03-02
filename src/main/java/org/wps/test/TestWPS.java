package org.wps.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wps.CoastLinesTrackingWPS;
import org.wps.utils.GeoJsonFileUtils;

public class TestWPS {

	private static final File dataDir = new File("data");
	private static final File refLineFile = new File(dataDir, "refLine.json");
	private static final File coastLinesFile = new File(dataDir, "coastLines.json");

	public static void main(String[] args) {
		try {
			// draw radials Test
			FeatureCollection<SimpleFeatureType, SimpleFeature> refLineFc = getFeatureCollections(refLineFile);
			FeatureCollection<SimpleFeatureType, SimpleFeature> drawRadialsFc = getRadialsTest(refLineFc, 200, 20,
					true);
			getGeoJsonFile(drawRadialsFc, dataDir, "drawRadialsFc");
			System.out.println(
					"drawRadialsFc.json est généré dans le dossier data de votre projet ! vous pouvez le visualiser maintenant.");

			// get distance Test
			FeatureCollection<SimpleFeatureType, SimpleFeature> coastLines = getFeatureCollections(coastLinesFile);
			FeatureCollection<SimpleFeatureType, SimpleFeature> distanceFc = getDistancesTest(drawRadialsFc,
					coastLines);
			getGeoJsonFile(distanceFc, dataDir, "distancesFc");
			System.out.println(
					"distanceFc.json est généré dans le dossier data de votre projet ! vous pouvez le visualiser maintenant.");
			// get distance to csv Test
			String csvString = getDistancesToCSVTest(distanceFc);
			getCSVFile(csvString, dataDir, "distances.csv");
			System.out.println(
					"distances.csv est généré dans le dossier data de votre projet ! vous pouvez le visualiser maintenant.");

		} catch (FileNotFoundException e) {
			System.out.println("Fichiers introuvables");
		} catch (IOException e) {
			System.out.println("Erreur entrées sorties");
		}
	}

	public static FeatureCollection<SimpleFeatureType, SimpleFeature> getRadialsTest(
			FeatureCollection<SimpleFeatureType, SimpleFeature> refLine, double length, double distance,
			boolean direction) {

		return CoastLinesTrackingWPS.drawRadial(refLine, length, distance, direction);
	}

	public static FeatureCollection<SimpleFeatureType, SimpleFeature> getDistancesTest(
			FeatureCollection<SimpleFeatureType, SimpleFeature> radials,
			FeatureCollection<SimpleFeatureType, SimpleFeature> coastLines) {

		return CoastLinesTrackingWPS.getDistances(radials, coastLines);
	}

	public static String getDistancesToCSVTest(FeatureCollection<SimpleFeatureType, SimpleFeature> distances) {

		return CoastLinesTrackingWPS.getDistancesToCSV(distances);
	}

	public static FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollections(File refLineFile)
			throws FileNotFoundException, IOException {

		return GeoJsonFileUtils.geoJsonToFeatureCollection(refLineFile);
	}

	public static void getGeoJsonFile(FeatureCollection<SimpleFeatureType, SimpleFeature> data, File dir,
			String fileName) throws FileNotFoundException, IOException {
		GeoJsonFileUtils.featureCollectionToGeoJsonFile(data, dir, fileName);
	}

	public static void getCSVFile(String csvString, File dataDir, String fileName) {
		BufferedWriter bw = null;
		try {

			bw = new BufferedWriter(new FileWriter(new File(dataDir, fileName)));
			bw.write(csvString); // Replace with the string
									// you are trying to write
		} catch (IOException e) {
			System.out.println("erreur entrées sorties");
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				System.out.println("erreur entrées sorties");
			}
		}

	}
}
