package org.wps;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

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
import com.vividsolutions.jts.geom.Point;

/**
 * @author lecteur
 *
 */
@DescribeProcess(title = "Coastlines tracking project", description = "WPS for the tracking of coastlines")
public class CoastLinesTrackingWPS extends StaticMethodsProcessFactory<CoastLinesTrackingWPS>
		implements GeoServerProcess {

	/**
	 * 
	 */

	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public CoastLinesTrackingWPS() {
		super(Text.text("WPS for the tracking of coastlines "), "coa", CoastLinesTrackingWPS.class);
	}

	/**
	 * @param referenceLine
	 * @param length
	 * @param distance
	 * @param direction
	 * @return
	 */
	@DescribeProcess(title = "Draw radial", description = "draw radial from reference Line")
	@DescribeResult(name = "resulFeatureCollection", description = "the result of drawing radials in reference Line")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> drawRadial(
			@DescribeParameter(name = "referenceLine", description = "the input referenceLine") final FeatureCollection<SimpleFeatureType, SimpleFeature> referenceLine,
			@DescribeParameter(name = "radialLength", description = "the length of radial in M") final double length,
			@DescribeParameter(name = "radialDistance", description = "the distance between radials in M") final double distance,
			@DescribeParameter(name = "radialDirection", description = "the direction of radial (true or false)") final boolean direction) {
		DefaultFeatureCollection resultFeatureCollection = null;

		try {
			SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
			simpleFeatureTypeBuilder.setName("featureType");
			simpleFeatureTypeBuilder.add("geometry", LineString.class);
			simpleFeatureTypeBuilder.add("type", String.class);
			simpleFeatureTypeBuilder.add("name", String.class);

			LineString refLine = WPSUtils.getReferenceLineFromFeature(referenceLine);
			LinkedList<LineString> segements = WPSUtils.createSegments(refLine, distance);
			LinkedList<LineString> listRadiales = new LinkedList<LineString>();
			LineString radiale = null;
			// create radials
			for (LineString l : segements) {
				radiale = WPSUtils.createRadialSegment(l, length, direction, true);
				listRadiales.add(radiale);
			}

			radiale = WPSUtils.createRadialSegment(segements.get(segements.size() - 1), length, direction, false);
			listRadiales.add(radiale);

			// init DefaultFeatureCollection
			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(
					simpleFeatureTypeBuilder.buildFeatureType());
			resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());

			// add geometrie to defaultFeatures
			for (int i = 0; i < listRadiales.size(); i++) {
				int id = i + 1;
				simpleFeatureBuilder.add(listRadiales.get(i));
				simpleFeatureBuilder.add("radiale");
				simpleFeatureBuilder.add("R" + id);
				resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(id + ""));
			}

		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return resultFeatureCollection;
	}

	/**
	 * @param raidalResultFeatureCollection
	 * @return
	 */
	@DescribeProcess(title = "Calculate distances between coastlines", description = "calculate distances between coastlines using radials intersection.")
	@DescribeResult(name = " resultFeatureCollection", description = "the result of distance calculting.")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> getDistances(
			@DescribeParameter(name = "radials", description = "the result featureCollection from draw radials process") final FeatureCollection<SimpleFeatureType, SimpleFeature> radials,
			@DescribeParameter(name = "coaslines", description = "the input Coaslines") final FeatureCollection<SimpleFeatureType, SimpleFeature> coastLines) {

		DefaultFeatureCollection resultFeatureCollection = null;
		try {
			SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
			simpleFeatureTypeBuilder.setName("featureType");
			simpleFeatureTypeBuilder.add("geometry", LineString.class);
			simpleFeatureTypeBuilder.add("radiale", String.class);
			simpleFeatureTypeBuilder.add("fromDate", String.class);
			simpleFeatureTypeBuilder.add("toDate", String.class);
			simpleFeatureTypeBuilder.add("separate_dist", Double.class);

			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(
					simpleFeatureTypeBuilder.buildFeatureType());

			Map<String, LineString> radialsMap = WPSUtils.getLinesByType(radials, 1);
			Map<Date, LineString> coastLinesMap = WPSUtils.sortBydate(WPSUtils.getLinesByType(coastLines, 2));

			Map<String, Map<Date, Point>> intersectedPoints = WPSUtils.getIntersectedPoints(radialsMap, coastLinesMap);
			Map<String, Map<Date[], LineString>> composedSegments = WPSUtils.getComposedSegment(intersectedPoints);
			resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());
			int id = 0;
			for (Map.Entry<String, Map<Date[], LineString>> radial : composedSegments.entrySet()) {
				
				for (Map.Entry<Date[], LineString> line : radial.getValue().entrySet()) {

					LineString ln = line.getValue();
					double distance = 0;
					id++;

					if ((ln.getStartPoint().getX() < ln.getEndPoint().getX())
							&& (ln.getStartPoint().getY() > ln.getEndPoint().getY())) {
						distance = -line.getValue().getLength();
					} else {
						distance = line.getValue().getLength();
					}

					simpleFeatureBuilder.add(line.getValue());
					simpleFeatureBuilder.add(radial.getKey());
					simpleFeatureBuilder.add(dateFormat.format(line.getKey()[0]));
					simpleFeatureBuilder.add(dateFormat.format(line.getKey()[1]));
					simpleFeatureBuilder.add(distance);
					resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(id + ""));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultFeatureCollection;
	}

	public static String getDistancesToCSV(
			@DescribeParameter(name = "coaslines", description = "") final FeatureCollection<SimpleFeatureType, SimpleFeature> coastLines) {

		return null;
	}

}
