package org.wps;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.StaticMethodsProcessFactory;
import org.geotools.text.Text;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wps.utils.WPSUtils;

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
			
		double lengthOfRadiale = WPSUtils.kilometreToMetre(length);
		double distanceBetweenRadials =  WPSUtils.kilometreToMetre(distance);
		
		
		
		
		
		
		return null;
	}
}
