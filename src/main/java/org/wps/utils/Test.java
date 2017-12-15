package org.wps.utils;

public class Test {
	// return a
	public Double getSlope(double x1, double y1, double x2, double y2) {
		return (y1 - y2) / (x1 - x2);
	}

	// return b
	public Double getB(double x, double y, double slope) {
		return y - (slope * x);
	}

	public void getSecLine(double x, double y, double slope) {
		double secSlope = -1 / slope;
		double b = getB(x, y, secSlope);

		System.out.println(" sec slope = " + secSlope + " sec b = " + b);
	}

	public static void main(String[] args) {
		Test test = new Test();
		// A
		double x1 = 1;
		double y1 = 1;
		// B
		double x2 = 3;
		double y2 = 4;

		test.getSecLine(x1, y1, test.getSlope(x1, y1, x2, y2));
	}

}