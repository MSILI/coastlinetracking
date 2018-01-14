package org.wps.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
			FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		try {
			// getLineString from Feature
			if (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				return geometryFactory.createLineString(geometry.getCoordinates());
			}
		} catch (Exception e) {
			// TODO: handle exception
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
			int type) {
		Map<String, LineString> linesBytType = new HashMap<String, LineString>();
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		FeatureIterator<SimpleFeature> iterator = input.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				// 1 pour radials
				if (type == 1) {

					LineString radiale = geometryFactory.createLineString(geometry.getCoordinates());
					linesBytType.put(feature.getProperty("name").getValue().toString(), radiale);
				}

				// 2 pour coastLines
				if (type == 2) {
					LineString coastline = geometryFactory.createLineString(geometry.getCoordinates());
					linesBytType.put(feature.getProperty("dte").getValue().toString(), coastline);
				}
			}

		} catch (

		Exception e) {
			e.printStackTrace();
		} finally {
			iterator.close();
		}

		return linesBytType;
	}

	public static Map<String, Map<String, Point>> getIntersectedPoints(Map<String, LineString> radialsMap,
			Map<String, LineString> coastLinesMap) {
		Map<String, Map<String, Point>> intersectedPoints = new HashMap<String, Map<String, Point>>();
		for (Map.Entry<String, LineString> radial : radialsMap.entrySet()) {
			Map<String, Point> intersectPoints = new HashMap<String, Point>();
			for (Map.Entry<String, LineString> coastLine : coastLinesMap.entrySet()) {
				if (radial.getValue().intersects(coastLine.getValue())) {
					intersectPoints.put(coastLine.getKey(),
							(Point) radial.getValue().intersection(coastLine.getValue()));
				}
			}

			intersectedPoints.put(radial.getKey(), intersectPoints);
		}

		return intersectedPoints;
	}

	public static Map<String, Map<String[], LineString>> getComposedSegment(
			Map<String, Map<String, Point>> intersectedPoints) {

		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 2154);
		Map<String, Map<String[], LineString>> composedSegments = new HashMap<String, Map<String[], LineString>>();

		for (Map.Entry<String, Map<String, Point>> radial : intersectedPoints.entrySet()) {

			if (radial.getValue().size() > 1) {
				Map<String[], LineString> lines = new HashMap<String[], LineString>();

				List<String> keyList = new ArrayList<String>(radial.getValue().keySet());

				for (int i = 0; i < keyList.size() - 1; i++) {
					Coordinate[] coordinates = new Coordinate[2];
					String[] formToCoastLinesDate = new String[2];
					
					String firstPointKey = keyList.get(i);
					String secondPointKey = keyList.get(i + 1);

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

}
