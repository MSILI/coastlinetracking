package fr.indigeo.wps.clt.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Cette classe est une classe utilitaire : elle contient
 * 
 * @author Fatah M'SILI
 *
 */
public class WPSUtils {

	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private static final Logger LOGGER = LogManager.getLogger(WPSUtils.class);

	/**
	 * @param track
	 * @param segmentLength
	 * @return
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static LinkedList<LineString> createSegments(Geometry track, double segmentLength)
			throws NoSuchAuthorityCodeException, FactoryException {

		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);

		LinkedList<Coordinate> coordinates = new LinkedList<Coordinate>();
		Collections.addAll(coordinates, track.getCoordinates());

		double accumulatedLength = 0;
		List<Coordinate> lastSegment = new ArrayList<Coordinate>();
		LinkedList<LineString> segments = new LinkedList<LineString>();
		Iterator<Coordinate> itCoordinates = coordinates.iterator();

		for (int i = 0; itCoordinates.hasNext() && i < coordinates.size() - 1; i++) {
			Coordinate c1 = coordinates.get(i);
			Coordinate c2 = coordinates.get(i + 1);

			lastSegment.add(c1);

			double length = Math.sqrt(Math.pow(c2.x - c1.x, 2) + Math.pow(c2.y - c1.y, 2));

			if (length + accumulatedLength >= segmentLength) {
				double offsetLength = segmentLength - accumulatedLength;
				double ratio = offsetLength / length;
				double dx = c2.x - c1.x;
				double dy = c2.y - c1.y;

				Coordinate segmentationPoint = new Coordinate(c1.x + (dx * ratio), c1.y + (dy * ratio));

				lastSegment.add(segmentationPoint); // segmentation point
				segments.add(geometryFactory.createLineString(lastSegment.toArray(new Coordinate[lastSegment.size()])));

				lastSegment = new ArrayList<Coordinate>(); // Resets the variable since a new segment will be built
				accumulatedLength = 0D;
				coordinates.add(i + 1, segmentationPoint);
			} else {
				accumulatedLength += length;
			}
		}

		lastSegment.add(coordinates.getLast()); // add the last seguement
		segments.add(geometryFactory.createLineString(lastSegment.toArray(new Coordinate[lastSegment.size()])));

		return segments;
	}

	/**
	 * @param featureCollection
	 * @return
	 */
	public static LineString getReferenceLineFromFeature(
			FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) throws Exception {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		try {
			// getLineString from Feature
			if (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				if (geometry instanceof LineString)
					return geometryFactory.createLineString(geometry.getCoordinates());
				else
					throw new Exception("la geometrie n'est pas une LineString !");
			} else
				throw new Exception("aucune LineString dans les donn�es.");
		} catch (Exception e) {
			LOGGER.error("Error while executing getReferenceLineFromFeature", e);
		} finally {
			iterator.close();
		}

		return null;
	}

	/**
	 * @param segment
	 * @return
	 */
	private static double getSlope(LineString segment) {
		return (segment.getEndPoint().getCoordinate().y - segment.getStartPoint().getCoordinate().y)
				/ (segment.getEndPoint().getCoordinate().x - segment.getStartPoint().getCoordinate().x);
	}

	/**
	 * @param segment
	 * @param length
	 * @param sense
	 * @param segmentType
	 * @return
	 */
	private static double calculateX(LineString segment, double length, boolean sense, boolean segmentType) {
		//choisir le referentiel en metre 2154
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		double slope = 0;
		double X = 0;
		double resultX = 0;
		//récupérer les coordonées de la lineString qui représente un seguement de la ligne de référence
		Coordinate[] coordinates = segment.getCoordinates();
		LineString newSegment = null;
		// segmentType = true => tout les seguement sauf le dernier
		if (segmentType) {
			X = segment.getStartPoint().getX();
		// segmentType = false => le dernier seguement
		} else {
			X = segment.getEndPoint().getX();
		}
		
		// si le seguement est tout droit
		if (coordinates.length == 2) {
			slope = getSlope(segment);
		// si le seguement n'est tout droit on calcule la pente du premier sous-seguement
		} else {
			newSegment = geometryFactory.createLineString(new Coordinate[] { coordinates[0], coordinates[1] });
			slope = getSlope(newSegment);
		}
		//sens = true => x = pente du seguement * racineCarrée(langueur au carée / (pente/2 le tout au carré + 1)) + la coordonée x d'un point du seguement
		if (sense) {
			resultX = slope * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + X;
		} else {
		//sens = false => x = -1 * pente du seguement * racineCarrée(langueur au carée / (pente/2 le tout au carré + 1)) + la coordonée x d'un pont du seguement
			resultX = -1 * slope * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + X;
		}

		return resultX;
	}

	/**
	 * @param segment
	 * @param length
	 * @param sense
	 * @param segmentType
	 * @return
	 */
	private static double calculateY(LineString segment, double length, boolean sense, boolean segmentType) {
		//choisir le referentiel en metre 2154
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		double slope = 0;
		double Y = 0;
		double resultY = 0;
		//récupérer les coordonées de la lineString qui représente un seguement de la ligne de référence
		Coordinate[] coordinates = segment.getCoordinates();
		LineString newSegment = null;
		// segmentType = true => tout les seguement sauf le dernier
		if (segmentType) {
			Y = segment.getStartPoint().getY();
		// segmentType = false => le dernier seguement
		} else {
			Y = segment.getEndPoint().getY();
		}
		// si le seguement est tout droit
		if (coordinates.length == 2) {
			slope = getSlope(segment);
		} else {
		// si le seguement n'est tout droit on calcule la pente du premier sous-seguement
			newSegment = geometryFactory.createLineString(new Coordinate[] { coordinates[0], coordinates[1] });
			slope = getSlope(newSegment);
		}
		//sens = true => y = -1 * racineCarrée(longueur au carré / (pente/2 le tout au carré + 1)) + la coordonée y d'un point du seguement
		if (sense) {
			resultY = -1 * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + Y;
		} else {
		//sens = true => y = racineCarrée(longueur au carré / (pente/2 le tout au carré + 1)) + la coordonée y d'un point du seguement
			
			resultY = Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + Y;
		}

		return resultY;
	}

	/**
	 * @param segment
	 * @param length
	 * @param sense
	 * @param segmentType
	 * @return
	 */
	public static LineString createRadialSegment(LineString segment, double length, boolean sense,
			boolean segmentType) {
		LineString radialSegment = null;
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		double X = calculateX(segment, length, sense, segmentType);

		double Y = calculateY(segment, length, sense, segmentType);

		if (segmentType) {
			if (sense && X < segment.getStartPoint().getX()) {
				X = calculateX(segment, length, !sense, segmentType);
				Y = calculateY(segment, length, !sense, segmentType);
			}

			if (!sense && X > segment.getStartPoint().getX() && Y > segment.getStartPoint().getY()) {
				X = calculateX(segment, length, !sense, segmentType);
				Y = calculateY(segment, length, !sense, segmentType);
			}

			radialSegment = geometryFactory.createLineString(
					new Coordinate[] { new Coordinate(X, Y), segment.getStartPoint().getCoordinate() });
		} else {
			if (sense && X < segment.getEndPoint().getX()) {
				X = calculateX(segment, length, !sense, segmentType);
				Y = calculateY(segment, length, !sense, segmentType);
			}

			if (!sense && X > segment.getStartPoint().getX() && Y > segment.getEndPoint().getY()) {
				X = calculateX(segment, length, !sense, segmentType);
				Y = calculateY(segment, length, !sense, segmentType);
			}
			radialSegment = geometryFactory
					.createLineString(new Coordinate[] { new Coordinate(X, Y), segment.getEndPoint().getCoordinate() });
		}
		return radialSegment;
	}

	public static Map<String, LineString> getLinesByType(FeatureCollection<SimpleFeatureType, SimpleFeature> input,
			int type) throws Exception {
		Map<String, LineString> linesBytType = new HashMap<String, LineString>();
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		FeatureIterator<SimpleFeature> iterator = input.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				if (geometry instanceof LineString) {
					// 1 pour radials
					if (type == 1) {

						LineString radiale = geometryFactory.createLineString(geometry.getCoordinates());
						linesBytType.put(feature.getProperty("name").getValue().toString(), radiale);
					}

					// 2 pour coastLines
					if (type == 2) {

						LineString coastline = geometryFactory.createLineString(geometry.getCoordinates());
						String date = feature.getProperty("dte").getValue().toString();
						date = date.substring(0, date.length() - 1);
						linesBytType.put(date, coastline);
					}
				} else {
					throw new Exception("Les geometries sont pas des LineString !");
				}

			}

		} catch (Exception e) {
			LOGGER.error("Error while executing getLinesByType", e);
		} finally {
			iterator.close();
		}

		return linesBytType;
	}

	public static Map<Date, LineString> sortBydate(Map<String, LineString> map) {

		try {
			if (!map.isEmpty()) {
				Map<Date, LineString> dataToSort = new HashMap<Date, LineString>();
				for (Map.Entry<String, LineString> entry : map.entrySet()) {

					dataToSort.put(dateFormat.parse(entry.getKey()), entry.getValue());
				}

				return new TreeMap<Date, LineString>(dataToSort);
			}
		} catch (ParseException e) {
			LOGGER.error("Error while executing sortBydate", e);
		}

		return null;
	}

	public static List<Date> getDatesFromCoastLinesMap(Map<Date, LineString> coastLineMap) {

		List<Date> dates = null;
		if (!coastLineMap.isEmpty()) {
			dates = new LinkedList<Date>();
			for (Map.Entry<Date, LineString> entry : coastLineMap.entrySet()) {

				dates.add(entry.getKey());
			}
		}

		return dates;
	}

	public static List<Date> getBeforDates(List<Date> dates, Date date) {

		List<Date> datesBefor = new ArrayList<Date>();
		for (Date d : dates) {
			if (date.compareTo(d) > 0)
				datesBefor.add(d);
		}

		return datesBefor;
	}

	// ordonner selon les dates
	public static Map<String, Map<Date, Point>> getIntersectedPoints(Map<String, LineString> radialsMap,
			Map<Date, LineString> coastLinesMap) {
		Map<String, Map<Date, Point>> intersectedPoints = new HashMap<String, Map<Date, Point>>();
		for (Map.Entry<String, LineString> radial : radialsMap.entrySet()) {
			Map<Date, Point> intersectPoints = new HashMap<Date, Point>();
			for (Map.Entry<Date, LineString> coastLine : coastLinesMap.entrySet()) {
				if (radial.getValue().intersects(coastLine.getValue())) {

					intersectPoints.put(coastLine.getKey(),
							(Point) radial.getValue().intersection(coastLine.getValue()));
				}
			}

			intersectedPoints.put(radial.getKey(), new TreeMap<Date, Point>(intersectPoints));
		}

		return intersectedPoints;
	}

	public static Map<String, Map<Date[], LineString>> getComposedSegment(
			Map<String, Map<Date, Point>> intersectedPoints) {

		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		Map<String, Map<Date[], LineString>> composedSegments = new HashMap<String, Map<Date[], LineString>>();

		for (Map.Entry<String, Map<Date, Point>> radial : intersectedPoints.entrySet()) {

			if (radial.getValue().size() > 1) {
				Map<Date[], LineString> lines = new HashMap<Date[], LineString>();

				List<Date> keyList = new ArrayList<Date>(radial.getValue().keySet());

				for (int i = 0; i < keyList.size() - 1; i++) {
					Coordinate[] coordinates = new Coordinate[2];
					Date[] formToCoastLinesDate = new Date[2];

					Date firstPointKey = keyList.get(i);
					Date secondPointKey = keyList.get(i + 1);

					coordinates[0] = radial.getValue().get(firstPointKey).getCoordinate();
					coordinates[1] = radial.getValue().get(secondPointKey).getCoordinate();

					formToCoastLinesDate[0] = firstPointKey;
					formToCoastLinesDate[1] = secondPointKey;

					lines.put(formToCoastLinesDate, geometryFactory.createLineString(coordinates));
				}

				composedSegments.put(radial.getKey(), lines);
			}

		}
		return composedSegments;
	}

	public static double getCumulatedDistance(Map<String, Map<Date[], LineString>> distanceSeguments,
			List<Date> datesBefor, String radialeName) {
		double cumulDist = 0;

		for (Map.Entry<String, Map<Date[], LineString>> radial : distanceSeguments.entrySet()) {
			for (Map.Entry<Date[], LineString> line : radial.getValue().entrySet()) {

				for (Date d : datesBefor) {
					double separateDistance = 0;
					if (line.getKey()[0].compareTo(d) == 0 && radial.getKey().equals(radialeName))
						if ((line.getValue().getStartPoint().getX() < line.getValue().getEndPoint().getX())
								&& (line.getValue().getStartPoint().getY() > line.getValue().getEndPoint().getY())) {

							separateDistance = -line.getValue().getLength();
						} else {
							separateDistance = line.getValue().getLength();
						}
					cumulDist = cumulDist + separateDistance;
				}
			}
		}
		return cumulDist;
	}

	public static int getNbrDaysBetweenTwoDate(Date date1, Date date2) {

		long diff = date2.getTime() - date1.getTime();

		return (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

	}

	public static List<Date> getDatesFromFeatures(
			FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		List<Date> listOfDate = new ArrayList<Date>();
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Date fromDate = dateFormat.parse(feature.getProperty("fromDate").getValue().toString());
				Date toDate = dateFormat.parse(feature.getProperty("toDate").getValue().toString());

				if (!listOfDate.contains(fromDate))
					listOfDate.add(fromDate);
				if (!listOfDate.contains(toDate))
					listOfDate.add(toDate);

			}

			Collections.sort(listOfDate);

		} catch (Exception e) {
			LOGGER.error("Error while executing getDatesFromFeatures", e);
		} finally {
			iterator.close();
		}

		return listOfDate;
	}

	public static List<String> getRadialsNameFromFeatures(
			FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		List<String> listOfRadialsName = new ArrayList<String>();
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				String radiale = feature.getProperty("radiale").getValue().toString();

				if (!listOfRadialsName.contains(radiale))
					listOfRadialsName.add(radiale);

			}

		} catch (Exception e) {
			LOGGER.error("Error while executing getRadialsNameFromFeatures", e);
		} finally {
			iterator.close();
		}

		return sortRadialsByName(listOfRadialsName);
	}

	public static double getDistanceByType(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
			int type, Date date, String radialeName) {

		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				String radiale = feature.getProperty("radiale").getValue().toString();
				Date toDate = dateFormat.parse(feature.getProperty("toDate").getValue().toString());
				if (radialeName.equals(radiale) && date.equals(toDate)) {
					// separate_dist
					if (type == 1)
						return (Double) feature.getProperty("separate_dist").getValue();
					// cumulate_dist
					if (type == 2)
						return (Double) feature.getProperty("cumulate_dist").getValue();
					// taux_recul
					if (type == 3)
						return (Double) feature.getProperty("taux_recul").getValue();

				}

			}

		} catch (Exception e) {
			LOGGER.error("Error while executing getDistanceByType", e);
		} finally {
			iterator.close();
		}

		return -1;
	}

	private static List<String> sortRadialsByName(List<String> radialsNames) {
		Collections.sort(radialsNames, new Comparator<String>() {
			public int compare(String s1, String s2) {
				return extractInt(s1) - extractInt(s2);
			}

			int extractInt(String s) {
				String num = s.replaceAll("\\D", "");
				// return 0 if no digits found
				return num.isEmpty() ? 0 : Integer.parseInt(num);
			}
		});

		return radialsNames;
	}

	/*
	 * private static double round(double value, int places) { if (places < 0) throw
	 * new IllegalArgumentException();
	 * 
	 * long factor = (long) Math.pow(10, places); value = value * factor; long tmp =
	 * Math.round(value); return (double) tmp / factor; }
	 */

}
