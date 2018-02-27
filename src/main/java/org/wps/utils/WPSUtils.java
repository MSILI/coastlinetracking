package org.wps.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * Cette classe est une classe utilitaire : elle contient
 * 
 * @author Fatah M'SILI
 *
 */
public class WPSUtils {

	/**
	 * @param track
	 * @param segmentLength
	 * @return
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */

	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

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
			e.printStackTrace();
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
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		double slope = 0;
		double X = 0;
		double resultX = 0;
		Coordinate[] coordinates = segment.getCoordinates();
		LineString newSegment = null;

		if (segmentType) {
			X = segment.getStartPoint().getX();
		} else {
			X = segment.getEndPoint().getX();
		}

		if (coordinates.length == 2) {
			slope = getSlope(segment);
		} else {
			newSegment = geometryFactory.createLineString(new Coordinate[] { coordinates[0], coordinates[1] });
			slope = getSlope(newSegment);
		}

		if (sense) {
			resultX = slope * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + X;
		} else {
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
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		double slope = 0;
		double Y = 0;
		double resultY = 0;
		Coordinate[] coordinates = segment.getCoordinates();
		LineString newSegment = null;
		if (segmentType) {
			Y = segment.getStartPoint().getY();
		} else {
			Y = segment.getEndPoint().getY();
		}

		if (coordinates.length == 2) {
			slope = getSlope(segment);
		} else {
			newSegment = geometryFactory.createLineString(new Coordinate[] { coordinates[0], coordinates[1] });
			slope = getSlope(newSegment);
		}

		if (sense) {
			resultY = -1 * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + Y;
		} else {
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
			e.printStackTrace();
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

			e.printStackTrace();
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
}
