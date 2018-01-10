package org.wps;

import java.util.LinkedList;
import java.util.List;

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

/**
 * @author lecteur
 *
 */
@DescribeProcess(title = "WPS project", description = "WPS for the tracking of coastlines")
public class WPSProject extends StaticMethodsProcessFactory<WPSProject> implements GeoServerProcess {

	/**
	 * 
	 */
	public WPSProject() {
		super(Text.text("WPS for the tracking of coastlines "), "coa", WPSProject.class);
	}

	/**
	 * @param referenceLine
	 * @param length
	 * @param distance
	 * @param sense
	 * @return
	 */
	@DescribeProcess(title = "draw radial", description = "draw radial from reference Line")
	@DescribeResult(name = " resulFeatureCollection", description = "the result of drawing radials in reference Line")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> drawRadial(
			@DescribeParameter(name = "referenceLine", description = "the input referenceLine") final FeatureCollection<SimpleFeatureType, SimpleFeature> referenceLine,
			@DescribeParameter(name = "radialLength", description = "the length of radial in M") final double length,
			@DescribeParameter(name = "radialDistance", description = "the distance between radials in M") final double distance,
			@DescribeParameter(name = "radialSense", description = "the sense of radial (true or false)") final boolean sense,
			@DescribeParameter(name = "coaslines", description = "the input Coaslines") final FeatureCollection<SimpleFeatureType, SimpleFeature> coasLines) {
		DefaultFeatureCollection resultFeatureCollection = null;

		try {
			SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
			simpleFeatureTypeBuilder.setName("featureType");
			simpleFeatureTypeBuilder.add("geometry", LineString.class);
			simpleFeatureTypeBuilder.add("type", String.class);

			List<LineString> coaslineList = WPSUtils.getLineStringFromFeatureCollection(coasLines);

			LineString refLine = WPSUtils.getLineFromFeature(referenceLine);
			LinkedList<LineString> segements = WPSUtils.createSegments(refLine, distance);
			LinkedList<LineString> listRadiales = new LinkedList<LineString>();
			LineString radiale = null;
			// create radials
			for (LineString l : segements) {
				radiale = WPSUtils.createRadialSegment(l, length, sense, true);
				listRadiales.add(radiale);
			}

			radiale = WPSUtils.createRadialSegment(segements.get(segements.size() - 1), length, sense, false);
			listRadiales.add(radiale);

			// init DefaultFeatureCollection
			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(
					simpleFeatureTypeBuilder.buildFeatureType());
			resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());

			// add geometrie to defaultFeatures
			for (int i = 0; i < listRadiales.size(); i++) {
				simpleFeatureBuilder.add(listRadiales.get(i));
				simpleFeatureBuilder.add("radiale");
				resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(i + 1 + ""));
			}

			// add coastLines to resultFeatureCollection
			for (int i = 0; i < coaslineList.size(); i++) {
				simpleFeatureBuilder.add(coaslineList.get(i));
				simpleFeatureBuilder.add("coastLine");
				resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(listRadiales.size() + i + 1 + ""));
			}
			
			// add reference line to result feature
			simpleFeatureBuilder.add(refLine);
			simpleFeatureBuilder.add("refLine");
			resultFeatureCollection
					.add(simpleFeatureBuilder.buildFeature(listRadiales.size() + coaslineList.size() + 1 + ""));

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
