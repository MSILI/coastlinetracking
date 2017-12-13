package org.wps.clientTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.Line;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.wps.utils.GeoJsonFileUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

public class WPSTest {

	public WPSTest() {
		try {
			String path = "C:\\Users\\lecteur\\Desktop\\mydocs\\workspace\\WPSProject";
			GeodeticCalculator calculator = new GeodeticCalculator(CRS.decode("EPSG:4326"));
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
			Coordinate[] coords = new Coordinate[] { new Coordinate(0, 2), new Coordinate(2, 0), new Coordinate(3, 4),
					new Coordinate(5, 6) };
			
			LineString line = geometryFactory.createLineString(coords);
			Point p1 = line.getStartPoint();
			Point p2 = line.getEndPoint();
			calculator.setStartingGeographicPoint(p1.getCoordinate().x, p1.getCoordinate().y);
			calculator.setDestinationGeographicPoint(p2.getCoordinate().x, p2.getCoordinate().y);
			double length = calculator.getOrthodromicDistance();
			GeoJsonFileUtils.geometryToGeoJsonFile(line, path);
			System.out.println(length+" M");
			List<LineString> listeSegments = createSegments(line, 10000);
			System.out.println(listeSegments.size());
			Geometry[] geometries = new Geometry[listeSegments.size()];
			
			//System.out.println(listeSegments.size());

			for(int i=0;i<listeSegments.size();i++) {
				geometries[i]=listeSegments.get(i);
				//System.out.println(listeSegments.get(i).toText());
			}
			
			GeometryCollection  geometry = new GeometryCollection(geometries, geometryFactory);
			
			//GeoJsonFileUtils.geometryToGeoJsonFile(geometry, path);

		} catch (FactoryRegistryException e) {
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

	public List<LineString> createSegments(Geometry track, double segmentLength)
			throws NoSuchAuthorityCodeException, FactoryException {

		GeodeticCalculator calculator = new GeodeticCalculator(CRS.decode("EPSG:4326")); // KML uses WGS84
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);

		LinkedList<Coordinate> coordinates = new LinkedList<Coordinate>();
		Collections.addAll(coordinates, track.getCoordinates());

		double accumulatedLength = 0;
		List<Coordinate> lastSegment = new ArrayList<Coordinate>();
		List<LineString> segments = new ArrayList<LineString>();
		Iterator<Coordinate> itCoordinates = coordinates.iterator();

		for (int i = 0; itCoordinates.hasNext() && i < coordinates.size() - 1; i++) {
			Coordinate c1 = coordinates.get(i);
			Coordinate c2 = coordinates.get(i + 1);

			lastSegment.add(c1);

			calculator.setStartingGeographicPoint(c1.x, c1.y);
			calculator.setDestinationGeographicPoint(c2.x, c2.y);

			double length = calculator.getOrthodromicDistance();
			

			if (length + accumulatedLength >= segmentLength) {
				double offsetLength = segmentLength - accumulatedLength;
				double ratio = offsetLength / length;
				double dx = c2.x - c1.x;
				double dy = c2.y - c1.y;

				Coordinate segmentationPoint = new Coordinate(c1.x + (dx * ratio), c1.y + (dy * ratio));

				lastSegment.add(segmentationPoint); // Last point of the segment is the segmentation point
				segments.add(geometryFactory.createLineString(lastSegment.toArray(new Coordinate[lastSegment.size()])));

				lastSegment = new ArrayList<Coordinate>(); // Resets the variable since a new segment will be built
				accumulatedLength = 0D;
				coordinates.add(i + 1, segmentationPoint);
			} else {
				accumulatedLength += length;
			}
		}

		lastSegment.add(coordinates.getLast()); // Because the last one is never added in the loop above
		segments.add(geometryFactory.createLineString(lastSegment.toArray(new Coordinate[lastSegment.size()])));

		return segments;
	}

	public void changementDeRepere(Point origine, Point i, Point j) {

	}

	public static void main(String[] args) {
		new WPSTest();
	}

}
