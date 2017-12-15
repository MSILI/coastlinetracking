package org.wps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.StaticMethodsProcessFactory;
import org.geotools.text.Text;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.wps.utils.GeoJsonFileUtils;
import org.wps.utils.LineUtils;
import org.wps.utils.WPSUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

@DescribeProcess(title = "WPS project", description = "WPS for the tracking of coastlines")
public class WPSProject extends StaticMethodsProcessFactory<WPSProject> implements GeoServerProcess {

	public WPSProject() {
		super(Text.text("WPS for the tracking of coastlines "), "coastlinesWps", WPSProject.class);
	}

	@DescribeProcess(title = "draw radial", description = "draw radial from reference Line")
	@DescribeResult(name = " resulFeatureCollection", description = "the result of drawing radials in reference Line")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> drawRadial(
			@DescribeParameter(name = "referenceLine", description = "the input referenceLine") final FeatureCollection<SimpleFeatureType, SimpleFeature> referenceLine,
			@DescribeParameter(name = "radialLength", description = "the length of radial in Km") final double length,
			@DescribeParameter(name = "radialDistance", description = "the distance between radials in Km") final int distance,
			@DescribeParameter(name = "radialSense", description = "the sense of radial (1 to the left 2 to the right)") final int sense) {

		DefaultFeatureCollection featureCollection = null;
		try {
			LineString line = null;
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
			double lengthOfRadiale = WPSUtils.kilometreToMetre(length);
			double distanceBetweenRadials = WPSUtils.kilometreToMetre(distance);
			//
			SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
			tb.setName("featureType");
		    tb.add("geometry", LineString.class);
		    tb.add("id", Integer.class);
		    
		    SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());
			featureCollection = new DefaultFeatureCollection(null, b.getFeatureType());
			FeatureIterator<SimpleFeature> iteratorTDC = referenceLine.features();

			// getLineString from Feature
			while (iteratorTDC.hasNext()) {
				SimpleFeature feature = iteratorTDC.next();
				line = (LineString) feature.getDefaultGeometry();
			}
			iteratorTDC.close();

			List<LineString> listeSegments = WPSUtils.createSegments(line, distanceBetweenRadials);
			List<LineString> listRadiales = new ArrayList<LineString>();
			Point calculatePoint = null;
			Coordinate[] radialCoordinates = null;
			LineString radiale = null;

			for (int i = 0; i < listeSegments.size(); i++) {
				calculatePoint = LineUtils.getInknowPoint(listeSegments.get(i),
						lengthOfRadiale,sense,1);
				radialCoordinates = new Coordinate[] { listeSegments.get(i).getStartPoint().getCoordinate(),
						calculatePoint.getCoordinate() };
				radiale = geometryFactory.createLineString(radialCoordinates);
				listRadiales.add(radiale);
			}

			// ajout de la radiale du dernier seguement.
			calculatePoint = LineUtils.getInknowPoint(listeSegments.get(listeSegments.size() - 1),
					lengthOfRadiale,sense,2);
			radialCoordinates = new Coordinate[] {
					listeSegments.get(listeSegments.size() - 1).getEndPoint().getCoordinate(),
					calculatePoint.getCoordinate() };
			radiale = geometryFactory.createLineString(radialCoordinates);
			listRadiales.add(radiale);
			//
			
			// add geometrie to defaultDeatures
			for (int i = 0; i < listRadiales.size(); i++) {
				b.add(listRadiales.get(i));
		        b.add(i);
		        featureCollection.add(b.buildFeature(i + ""));
			}
			b.add(line);
	        b.add(listRadiales.size()+1);
	        featureCollection.add(b.buildFeature(listRadiales.size()+1 + ""));
			
			System.out.println(featureCollection.size());
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return featureCollection;
	}

	public static void main(String[] args) {
		try {
			String path = "C:\\Users\\lecteur\\Desktop\\mydocs\\workspace\\WPSProject";
			FeatureCollection<SimpleFeatureType, SimpleFeature> ref = GeoJsonFileUtils.geoJsonToFeatureCollection(new File("data_plouzane.json"));
			
			FeatureCollection<SimpleFeatureType, SimpleFeature> referenceLine =  WPSProject.drawRadial(ref, 0.1, 1, 2);
			GeoJsonFileUtils.featureCollectionToGeoJsonFile(referenceLine, path);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
