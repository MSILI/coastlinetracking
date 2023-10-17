package fr.indigeo.wps.clt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.indigeo.wps.clt.utils.WPSUtils;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

/**
 * @author lecteur
 *
 */
@DescribeProcess(title = "Coastlines tracking project", description = "WPS for the tracking of coastlines")
public class CoastLinesTrackingWPS extends StaticMethodsProcessFactory<CoastLinesTrackingWPS>
		implements GeoServerProcess {

	private static final Logger LOGGER = Logger.getLogger(CoastLinesTrackingWPS.class);

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

		LOGGER.info("DrawRadial with params - length : " + length + " | distance : " + distance + " | direction : "
				+ direction);
		try {
			SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
			simpleFeatureTypeBuilder.setName("featureType");
			simpleFeatureTypeBuilder.add("geometry", LineString.class);
			simpleFeatureTypeBuilder.add("type", String.class);
			simpleFeatureTypeBuilder.add("name", String.class);

			if (referenceLine == null) {
				throw new Exception("Erreur de lecture de la ligne de référence vérifier les données en entrée");
			}
			LineString refLine = WPSUtils.getReferenceLineFromFeature(referenceLine);
			// create radials
			LinkedList<LineString> segements = WPSUtils.createSegments(refLine, distance);
			LinkedList<LineString> listRadiales = new LinkedList<LineString>();
			LineString radiale = null;

			// create radials
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Create radials with " + segements.size() + " elements");
			}
			int n = 0;
			for (LineString l : segements) {
				if (n == 0) {
					radiale = WPSUtils.createRadialSegment(l, length, direction, false);
					listRadiales.add(radiale);
				} else {
					radiale = WPSUtils.createRadialSegment(l, length, direction, true);
					listRadiales.add(radiale);
				}
				n++;
			}

			// init DefaultFeatureCollection
			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(
					simpleFeatureTypeBuilder.buildFeatureType());
			resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());

			// add geometrie to defaultFeatures
			for (int i = 0; i < listRadiales.size(); i++) {
				int id = i + 1;
				simpleFeatureBuilder.add(listRadiales.get(i));
				simpleFeatureBuilder.add("radiale");
				simpleFeatureBuilder.add(id);
				resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(Integer.toString(id)));
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
			@DescribeParameter(name = "coastlines", description = "the input Coaslines") final FeatureCollection<SimpleFeatureType, SimpleFeature> coastLines) {

		DefaultFeatureCollection resultFeatureCollection = null;
		try {
			SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
			simpleFeatureTypeBuilder.setName("featureType");
			simpleFeatureTypeBuilder.add("geometry", LineString.class);
			simpleFeatureTypeBuilder.add("radiale", Integer.class);
			simpleFeatureTypeBuilder.add("fromDate", String.class);
			simpleFeatureTypeBuilder.add("toDate", String.class);
			simpleFeatureTypeBuilder.add("separate_dist", Double.class);
			simpleFeatureTypeBuilder.add("cumulate_dist", Double.class);
			simpleFeatureTypeBuilder.add("taux_recul", Double.class);
			simpleFeatureTypeBuilder.add("fromStartDist", Double.class);

			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(
					simpleFeatureTypeBuilder.buildFeatureType());

			Map<String, LineString> radialsMap = WPSUtils.getLinesByType(radials, 1);
			Map<Date, LineString> coastLinesMap = WPSUtils.sortBydate(WPSUtils.getLinesByType(coastLines, 2));
			List<Date> dates = WPSUtils.getDatesFromCoastLinesMap(coastLinesMap);

			Map<String, Map<Date, Point>> intersectedPoints = WPSUtils.getIntersectedPoints(radialsMap, coastLinesMap);
			Map<String, Map<Date[], LineString>> composedSegments = WPSUtils.getComposedSegment(intersectedPoints);
			resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());
			int id = 0;
			for (Map.Entry<String, Map<Date[], LineString>> radialInfos : composedSegments.entrySet()) {
				double accumulateDistance = 0;
				Point firstPoint = null;
				double distLineRefToRefDate = 0;
				int n = 0;
				Point RadialFirstPoint = null;
				for (Map.Entry<String, LineString> radial : radialsMap.entrySet()) {
					if (radialInfos.getKey() == radial.getKey() && RadialFirstPoint == null) {
						LineString radialGeom = radial.getValue();
						RadialFirstPoint = radialGeom.getStartPoint();
					}
				}
				for (Map.Entry<Date[], LineString> line : radialInfos.getValue().entrySet()) {
					LOGGER.debug("Traitement de l'id : " + id);
					double separateDistance = 0;
					double distFromStart = 0;
					Point endPoint = line.getValue().getEndPoint();

					// distance depuis la radiale pour savoir si on est en positif ou négatif
					// par rapport à la date de référence
					double distFromLineRef = WPSUtils.getDistance(RadialFirstPoint, endPoint);

					// un point d'intersection correspond à une intersection entre
					// la radiale et un trait de côte à une date donnée

					// en partant de la première date utilisée comme date de référence la plus
					// ancienne
					// on va calculer la distance de chaque segment de date
					// line => LineString point[i] -> point[i+1]
					separateDistance = line.getValue().getLength();
					if (n == 0) {
						// premier segment date ref -> date ref + 1
						firstPoint = line.getValue().getStartPoint();
						distLineRefToRefDate = WPSUtils.getDistance(RadialFirstPoint, firstPoint);
					}

					distFromStart = WPSUtils.getDistance(firstPoint, endPoint);

					if (distFromLineRef > distLineRefToRefDate) {
						// le recule du trait de côte est négatif car on perd de la distance
						// vis à vis de la ligne de référence et la date de référence
						distFromStart = -1 * distFromStart;
					}

					// La distance cummulée est la distance totale de mouvement du trait parcourus
					// depuis la 1ere date de référence intersectée la plus ancienne jusqu'à
					// la date intersectée la plus récente.
					id++;
					accumulateDistance = accumulateDistance + separateDistance;
					int nbrJours = WPSUtils.getNbrDaysBetweenTwoDate(line.getKey()[0], line.getKey()[1]);
					// Taux annuel
					double taux = (separateDistance / nbrJours) * 365;
					// distance = Line -> point[i] -> point[i+1]
					simpleFeatureBuilder.add(line.getValue());
					simpleFeatureBuilder.add(radialInfos.getKey());
					simpleFeatureBuilder.add(dateFormat.format(line.getKey()[0]));
					simpleFeatureBuilder.add(dateFormat.format(line.getKey()[1]));
					simpleFeatureBuilder.add(separateDistance);
					simpleFeatureBuilder.add(accumulateDistance);
					simpleFeatureBuilder.add(taux);
					simpleFeatureBuilder.add(distFromStart);
					resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(Integer.toString(id)));
					n++;
					LOGGER.debug("Distance information : radial - " + radialInfos.getKey() + " Date - "
							+ line.getKey()[0] + " Date - " + line.getKey()[1]);
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

		for (Date d : dates) {
			headers = headers + dateFormat.format(d) + "|";
		}

		LOGGER.debug("distancesToCSV - headers " + headers);

		headers = headers.substring(0, headers.length() - 1) + eol;

		for (int i = 1; i < dates.size(); i++) {
			subHeaders = subHeaders + "separe;cumule;taux,fromStartDist" + sep;
		}

		subHeaders = rad + sep + subHeaders.substring(0, subHeaders.length() - 1) + eol;
		dataList.add(headers);
		dataList.add(subHeaders);

		for (Integer radiale : WPSUtils.getRadialsNameFromFeatures(distances)) {
			String data = radiale + sep;
			for (int i = 1; i < dates.size(); i++) {

				double cumulateDist = WPSUtils.getDistanceByType(distances, 1, dates.get(i), radiale);
				double separateDist = WPSUtils.getDistanceByType(distances, 2, dates.get(i), radiale);
				double taux = WPSUtils.getDistanceByType(distances, 3, dates.get(i), radiale);
				double distFromStart = WPSUtils.getDistanceByType(distances, 4, dates.get(i), radiale);

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

				if (distFromStart != -1)
					data = data + distFromStart;
				else
					data = data + "none";

				data = data + sep;
			}

			data = data.substring(0, data.length() - 1) + eol;
			dataList.add(data);

		}

		for (String line : dataList) {
			csv = csv + line;
		}

		return csv;
	}

	@DescribeProcess(title = "distancesToJson", description = "parse a feautureCollection of distances to json object.")
	@DescribeResult(name = "JsonString", description = "the result of parsing distances.")
	public static String getDistancesToJson(
			@DescribeParameter(name = "distancesFeatureCollection", description = "the result distance feature collection") final FeatureCollection<SimpleFeatureType, SimpleFeature> distances) {

		List<Date> TDCdates = WPSUtils.getDatesFromFeatures(distances);

		JSONObject result = new JSONObject();
		JSONArray resultsArray = new JSONArray();

		// get all tdc result
		for (Date TDCdate : TDCdates) {

			JSONObject tdc = new JSONObject();
			JSONArray tdcValues = new JSONArray();
			tdc.put("date", dateFormat.format(TDCdate));

			FeatureIterator<SimpleFeature> iterator = distances.features();
			try {
				while (iterator.hasNext()) {
					SimpleFeature feature = iterator.next();
					Date toDate = dateFormat.parse(feature.getProperty("toDate").getValue().toString());

					// if wanted tdc -> store value to json
					if (TDCdate.equals(toDate)) {
						JSONObject tdcdata = new JSONObject();
						tdcdata.put("radiale", (Integer) feature.getProperty("radiale").getValue());
						// separate_dist
						tdcdata.put("separateDist", (Double) feature.getProperty("separate_dist").getValue());
						// cumulate_dist
						tdcdata.put("cumulateDist", (Double) feature.getProperty("cumulate_dist").getValue());
						// taux_recul
						tdcdata.put("tauxRecul", (Double) feature.getProperty("taux_recul").getValue());
						tdcdata.put("fromStartDist", (Double) feature.getProperty("fromStartDist").getValue());
						tdcValues.put(tdcdata);
					}
				}
				tdc.put("data", tdcValues);
			} catch (ClassCastException e) {
				LOGGER.error("Error while casting distance to double", e);
			} catch (ParseException e) {
				LOGGER.error("Error while parsing date", e);
			} finally {
				iterator.close();
			}
			resultsArray.put(tdc);
		}
		result.put("result", resultsArray);
		return result.toString();
	}

	@DescribeProcess(title = "coastLinesTracking", description = "coastLinesTracking WPS")
	@DescribeResult(name = "jsonString", description = "the result of coastLinesTracking WPS")
	public static String coastLinesTracking(
			@DescribeParameter(name = "radiales", description = "the calculated radial") final FeatureCollection<SimpleFeatureType, SimpleFeature> radiales,
			@DescribeParameter(name = "coastlines", description = "the input Coastlines") final FeatureCollection<SimpleFeatureType, SimpleFeature> coastLines) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Radial information : ");
			FeatureIterator<SimpleFeature> iteratorfc1 = radiales.features();
			try {
				while (iteratorfc1.hasNext()) {
					SimpleFeature feature = iteratorfc1.next();
					LOGGER.debug("Radial feature id = " + feature.getID());
					for (Property property : feature.getProperties()) {
						LOGGER.debug(property.getName() + " = " + property.getValue());
					}
				}
			} finally {
				iteratorfc1.close();
			}
		}

		FeatureCollection<SimpleFeatureType, SimpleFeature> fc2 = getDistances(radiales, coastLines);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Distance information : ");
			FeatureIterator<SimpleFeature> iteratorfc2 = fc2.features();
			try {
				while (iteratorfc2.hasNext()) {
					SimpleFeature feature = iteratorfc2.next();
					LOGGER.debug("Distance feature id = " + feature.getID());
					for (Property property : feature.getProperties()) {
						LOGGER.debug(property.getName() + " = " + property.getValue());
					}
				}
			} finally {
				iteratorfc2.close();
			}
		}

		String json = getDistancesToJson(fc2);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Json information : ");
			LOGGER.debug("Json result : " + json);
		}

		return json;
	}

}
