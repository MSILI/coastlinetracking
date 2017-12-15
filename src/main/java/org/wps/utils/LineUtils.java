package org.wps.utils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

public class LineUtils {

	public static double getSlope(LineString segment) {

		return (segment.getEndPoint().getCoordinate().y - segment.getStartPoint().getCoordinate().y)
				/ (segment.getEndPoint().getCoordinate().x - segment.getStartPoint().getCoordinate().x);
	}

	public static double calculateX(LineString segment, double length) {
		double x = segment.getStartPoint().getCoordinate().x;
		double slope = getSlope(segment);

		return -1 * slope * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + x;
	}

	public static double calculateY(LineString segment, double length) {
		double y = segment.getStartPoint().getCoordinate().y;
		double slope = getSlope(segment);

		return Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + y;
	}

	public static Point getInknowPoint(LineString segment, double length) {

		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
		double x = calculateX(segment, length);
		double y = calculateY(segment, length);
		return geometryFactory.createPoint(new Coordinate(x, y));
	}

	public static double calculateLastX(LineString segment, double length) {
		double x = segment.getEndPoint().getCoordinate().x;
		double slope = getSlope(segment);

		return -1 * slope * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + x;
	}

	public static double calculateLastY(LineString segment, double length) {
		double y = segment.getEndPoint().getCoordinate().y;
		double slope = getSlope(segment);

		return Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + y;
	}

	public static Point getLastInknowPoint(LineString segment, double length) {

		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
		double x = calculateLastX(segment, length);
		double y = calculateLastY(segment, length);
		return geometryFactory.createPoint(new Coordinate(x, y));
	}
}
