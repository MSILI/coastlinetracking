package org.wps.clientTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.wps.utils.GeoJsonFileUtils;
import org.wps.utils.LineUtils;
import org.wps.utils.WPSUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

public class WPSTest {

	public WPSTest() {
//		try {
//			String path = "C:\\Users\\lecteur\\Desktop\\mydocs\\workspace\\WPSProject";
//			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
//			Coordinate[] coords = new Coordinate[] { new Coordinate(0, 2), new Coordinate(2, 0), new Coordinate(3, 4),
//					new Coordinate(5, 6) };
//
//			LineString line = geometryFactory.createLineString(coords);
//
//			List<LineString> listeSegments = WPSUtils.createSegments(line, 100000);
//			//List<LineString> listRadiales = new ArrayList<LineString>();
//			
//			LineString segment = listeSegments.get(0);
//			Point calculatePoint = LineUtils.getInknowPoint(segment, WPSUtils.toRealDistance(10000));
//			
//			Coordinate[] radialCoordinates = new Coordinate[] {segment.getStartPoint().getCoordinate(),calculatePoint.getCoordinate()};
//			LineString radiale = geometryFactory.createLineString(radialCoordinates); 
//			Geometry[] geometries = new Geometry[2];
//			geometries[0] = segment;
//			geometries[1] = radiale;
//
////			for (int i = 0; i < listeSegments.size(); i++) {
////				geometries[i] = listeSegments.get(i);
////				System.out.println(geometries[i]);
////			}
//
//			GeometryCollection geometry = new GeometryCollection(geometries, geometryFactory);
//			GeoJsonFileUtils.geometryToGeoJsonFile(geometry, path);
//
//		} catch (FactoryRegistryException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchAuthorityCodeException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (FactoryException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}

	public static void main(String[] args) {
		new WPSTest();
	}

}
