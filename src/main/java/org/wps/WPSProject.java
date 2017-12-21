package org.wps;

import java.util.LinkedList;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
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
import org.wps.utils.WPSUtils;

import com.vividsolutions.jts.geom.LineString;

@DescribeProcess(title = "WPS project", description = "WPS for the tracking of coastlines")
public class WPSProject extends StaticMethodsProcessFactory<WPSProject> implements GeoServerProcess {

	public WPSProject() {
		super(Text.text("WPS for the tracking of coastlines "), "coa", WPSProject.class);
	}

	@DescribeProcess(title = "draw radial", description = "draw radial from reference Line")
	@DescribeResult(name = " resulFeatureCollection", description = "the result of drawing radials in reference Line")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> drawRadial(
			@DescribeParameter(name = "referenceLine", description = "the input referenceLine") final FeatureCollection<SimpleFeatureType, SimpleFeature> referenceLine,
			@DescribeParameter(name = "radialLength", description = "the length of radial in M") final double length,
			@DescribeParameter(name = "radialDistance", description = "the distance between radials in M") final double distance,
			@DescribeParameter(name = "radialSense", description = "the sense of radial (true or false)") final boolean sense) {
		DefaultFeatureCollection resultFeatureCollection = null;
		double realLength = length * Math.pow(10, -5);
		try {
			SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
			tb.setName("featureType");
			tb.add("geometry", LineString.class);
			tb.add("id", Integer.class);
			LineString refLine = WPSUtils.getLineFromFeature(referenceLine);
			LinkedList<LineString> segements = WPSUtils.createSegments(refLine, distance);
			LinkedList<LineString> listRadiales = new LinkedList<LineString>();
			LineString radiale = null;
			// create radials
			for (LineString l : segements) {
				radiale = WPSUtils.createRadialSegment(l, realLength, sense, true);
				listRadiales.add(radiale);
			}

			radiale = WPSUtils.createRadialSegment(segements.get(segements.size() - 1), realLength, sense, false);
			listRadiales.add(radiale);

			// init DefaultFeatureCollection
			SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());
			resultFeatureCollection = new DefaultFeatureCollection(null, b.getFeatureType());

			// add geometrie to defaultDeatures
			for (int i = 0; i < listRadiales.size(); i++) {
				b.add(listRadiales.get(i));
				b.add(i);
				resultFeatureCollection.add(b.buildFeature(i + ""));
			}

			b.add(refLine);
			b.add(listRadiales.size() + 1);

			resultFeatureCollection.add(b.buildFeature(listRadiales.size() + ""));
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return resultFeatureCollection;
	}

}
