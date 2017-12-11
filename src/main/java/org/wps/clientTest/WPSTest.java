package org.wps.clientTest;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.geometry.coordinate.LineSegment;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class WPSTest {

	
	public WPSTest() {
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		Coordinate[] coords  = new Coordinate[] {new Coordinate(0, 2), new Coordinate(2, 0), new Coordinate(8, 6) };
		LineString line = geometryFactory.createLineString(coords);
				
		
	}
	
	public static void main(String[] args) {
		new WPSTest();
	}

}
