package org.wps.clientTest;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

public class Test {

	public Test() {
		try {
			String path = "C:\\Users\\lecteur\\Desktop\\mydocs\\workspace\\WPSProject";
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
			Coordinate[] coords = new Coordinate[] { new Coordinate(0, 2), new Coordinate(2, 0), new Coordinate(3, 4),
					new Coordinate(5, 6) };
			LineString line = geometryFactory.createLineString(coords);
			List<LineString> listeSegments = WPSUtils.createSegments(line, 10000);
			List<LineString> listRadiales = new ArrayList<LineString>();
			Point calculatePoint = null;
			Coordinate[] radialCoordinates = null;
			LineString radiale = null;
			for(int i=0;i<listeSegments.size();i++) {
				calculatePoint = LineUtils.getInknowPoint(listeSegments.get(i), WPSUtils.toRealDistance(100000));
				radialCoordinates = new Coordinate[] {listeSegments.get(i).getStartPoint().getCoordinate(),calculatePoint.getCoordinate()};
				radiale = geometryFactory.createLineString(radialCoordinates); 
				listRadiales.add(radiale);
			}
			
			//dernier seguement
			calculatePoint = LineUtils.getLastInknowPoint(listeSegments.get(listeSegments.size()-1), WPSUtils.toRealDistance(100000));
			radialCoordinates = new Coordinate[] {listeSegments.get(listeSegments.size()-1).getEndPoint().getCoordinate(),calculatePoint.getCoordinate()};
			radiale = geometryFactory.createLineString(radialCoordinates); 
			listRadiales.add(radiale);
			
			Geometry[] geometries = new Geometry[listRadiales.size()+1];
			
			for(int i=0;i<listRadiales.size();i++) {
				geometries[i] = listRadiales.get(i);
			}
			geometries[listRadiales.size()-1] = radiale;
			geometries[listRadiales.size()] = line;
			GeometryCollection geometry = new GeometryCollection(geometries, geometryFactory);
			GeoJsonFileUtils.geometryToGeoJsonFile(geometry, path);
			
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Test();
	}

}
