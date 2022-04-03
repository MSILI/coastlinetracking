package org.wps;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

/**
 * @author lecteur
 *
 */
@DescribeProcess(title = "Coastlines tracking project", description = "WPS for the tracking of coastlines")
public class CoastLinesTrackingWPS extends StaticMethodsProcessFactory<CoastLinesTrackingWPS>
		implements GeoServerProcess {

	private static final Logger LOGGER = LogManager.getLogger(CoastLinesTrackingWPS.class);

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
			LOGGER.error("Error while executing drawRadial", e);
		} catch (FactoryException e) {
			LOGGER.error("Error while executing drawRadial", e);
		} catch (Exception e) {
			LOGGER.error("Error while executing drawRadial", e);
		}

		return resultFeatureCollection;
	}

	/**
	 * @param raidalResultFeatureCollection
	 * @return
	 */
	@DescribeProcess(title = "Calculate distances between coastlines", description = "calculate distances between coastlines using radials intersection.")
	@DescribeResult(name = "resultFeatureCollection", description = "the result of distance calculting.")
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
			simpleFeatureTypeBuilder.add("cumulate_dist", Double.class);
			simpleFeatureTypeBuilder.add("taux_recul", Double.class);

			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(
					simpleFeatureTypeBuilder.buildFeatureType());

			Map<String, LineString> radialsMap = WPSUtils.getLinesByType(radials, 1);
			Map<Date, LineString> coastLinesMap = WPSUtils.sortBydate(WPSUtils.getLinesByType(coastLines, 2));
			List<Date> dates = WPSUtils.getDatesFromCoastLinesMap(coastLinesMap);

			Map<String, Map<Date, Point>> intersectedPoints = WPSUtils.getIntersectedPoints(radialsMap, coastLinesMap);
			Map<String, Map<Date[], LineString>> composedSegments = WPSUtils.getComposedSegment(intersectedPoints);
			resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());
			int id = 0;
			for (Map.Entry<String, Map<Date[], LineString>> radial : composedSegments.entrySet()) {

				for (Map.Entry<Date[], LineString> line : radial.getValue().entrySet()) {

					LineString ln = line.getValue();
					double separateDistance = 0;
					double accumulateDistance = 0;
					id++;

					if ((ln.getStartPoint().getX() < ln.getEndPoint().getX())
							&& (ln.getStartPoint().getY() > ln.getEndPoint().getY())) {
						separateDistance = -line.getValue().getLength();
					} else {
						separateDistance = line.getValue().getLength();
					}

					List<Date> datesBefor = WPSUtils.getBeforDates(dates, line.getKey()[1]);
					if (!datesBefor.isEmpty()) {
						accumulateDistance = WPSUtils.getCumulatedDistance(composedSegments, datesBefor,
								radial.getKey());
					} else {
						accumulateDistance = separateDistance;
					}

					int nbrJours = WPSUtils.getNbrDaysBetweenTwoDate(line.getKey()[0], line.getKey()[1]);
					double taux = separateDistance / nbrJours;

					simpleFeatureBuilder.add(line.getValue());
					simpleFeatureBuilder.add(radial.getKey());
					simpleFeatureBuilder.add(dateFormat.format(line.getKey()[0]));
					simpleFeatureBuilder.add(dateFormat.format(line.getKey()[1]));
					simpleFeatureBuilder.add(separateDistance);
					simpleFeatureBuilder.add(accumulateDistance);
					simpleFeatureBuilder.add(taux);
					resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(id + ""));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while executing getDistances", e);
		}

		return resultFeatureCollection;
	}

	@DescribeProcess(title = "distancesToCSV", description = "parse a feautureCollection of distances to csv String.")
	@DescribeResult(name = "csvString", description = "the result of parsing distances.")
	public static String getDistancesToCSV(
			@DescribeParameter(name = "distancesFeatureCollection", description = "the result distance feature collection") final FeatureCollection<SimpleFeatureType, SimpleFeature> distances) {

		List<Date> dates = WPSUtils.getDatesFromFeatures(distances);

		String sep = "|";
		String eol = "\n";
		String headers = "";
		String subHeaders = "";
		String rad = "rad";
		String csv = "";

		List<String> dataList = new ArrayList<String>();

		for (Date d : dates)
			headers = headers + dateFormat.format(d) + "|";

		headers = headers.substring(0, headers.length() - 1) + eol;

		for (int i = 1; i < dates.size(); i++)
			subHeaders = subHeaders + "separe;cumule;taux" + sep;

		subHeaders = rad + sep + subHeaders.substring(0, subHeaders.length() - 1) + eol;
		dataList.add(headers);
		dataList.add(subHeaders);

		for (String radiale : WPSUtils.getRadialsNameFromFeatures(distances)) {
			String data = radiale + sep;
			for (int i = 1; i < dates.size(); i++) {

				double cumulateDist = WPSUtils.getDistanceByType(distances, 1, dates.get(i), radiale);
				double separateDist = WPSUtils.getDistanceByType(distances, 2, dates.get(i), radiale);
				double taux = WPSUtils.getDistanceByType(distances, 3, dates.get(i), radiale);

				if (cumulateDist != -1)
					data = data + cumulateDist + ";";
				else
					data = data + "none" + ";";

				if (separateDist != -1)
					data = data + separateDist + ";";
				else
					data = data + "none" + ";";

				if (taux != -1)
					data = data + taux;
				else
					data = data + "none";

				data = data + sep;
			}

			data = data.substring(0, data.length() - 1) + eol;
			dataList.add(data);

		}

		for (String line : dataList)
			csv = csv + line;

		return csv;
	}

	@DescribeProcess(title = "coastLinesTracking", description = "coastLinesTracking WPS")
	@DescribeResult(name = "csvString", description = "the result of coastLinesTracking WPS")
	public static String coastLinesTracking(
			@DescribeParameter(name = "referenceLine", description = "the input referenceLine") final FeatureCollection<SimpleFeatureType, SimpleFeature> referenceLine,
			@DescribeParameter(name = "radialLength", description = "the length of radial in M") final double length,
			@DescribeParameter(name = "radialDistance", description = "the distance between radials in M") final double distance,
			@DescribeParameter(name = "radialDirection", description = "the direction of radial (true or false)") final boolean direction,
			@DescribeParameter(name = "coaslines", description = "the input Coaslines") final FeatureCollection<SimpleFeatureType, SimpleFeature> coastLines) {

		FeatureCollection<SimpleFeatureType, SimpleFeature> fc1 = drawRadial(referenceLine, length, distance,
				direction);
		FeatureCollection<SimpleFeatureType, SimpleFeature> fc2 = fc1;
		FeatureCollection<SimpleFeatureType, SimpleFeature> fc3 = getDistances(fc2, coastLines);
		FeatureCollection<SimpleFeatureType, SimpleFeature> fc4 = fc3;
		String csv = getDistancesToCSV(fc4);

		return csv;
	}

}
