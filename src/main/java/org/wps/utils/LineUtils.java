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

	public static double calculateX(LineString segment, double length, int sense, int typePoint) {
		double slope = getSlope(segment);
		double x = 0;
		if (typePoint == 1)
			x = segment.getStartPoint().getCoordinate().x;
		else if (typePoint == 2)
			x = segment.getEndPoint().getCoordinate().x;

		if (sense == 1)
			x = -1 * slope * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + x;
		else if (sense == 2)
			x = slope * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + x;

		return x;
	}

	public static double calculateY(LineString segment, double length, int sense, int typePoint) {
		double slope = getSlope(segment);
		double y = 0;
		if (typePoint == 1)
			y = segment.getStartPoint().getCoordinate().x;
		else if (typePoint == 2)
			y = segment.getEndPoint().getCoordinate().x;

		if (sense == 1)
			y = Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + y;
		else if (sense == 2)
			y = -1 * Math.sqrt(Math.pow(length, 2) / (Math.pow(slope, 2) + 1)) + y;

		return y;
	}

	public static Point getInknowPoint(LineString segment, double length, int sense, int typePoint) {

		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
		double x = calculateX(segment, length,sense,typePoint);
		double y = calculateY(segment, length,sense,typePoint);
		return geometryFactory.createPoint(new Coordinate(x, y));
	}

}
