package net.imagej.ops.geom.geom2d;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops;
import net.imagej.ops.special.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

@Plugin(type = Ops.Geometric.SmallestEnclosingCircle.class, priority = Priority.FIRST_PRIORITY)
public class DefaultSmallestEnclosingCircle extends AbstractUnaryFunctionOp<IterableInterval<?>, Circle>
implements Ops.Geometric.SmallestEnclosingCircle {

	@Parameter(required = false)
	RealLocalizable center = null;

	@Parameter(required = false)
	double paddingRatio = 1.0;

	@Parameter(required = false)
	boolean randomizePointRemoval = true;

	@Parameter(required = false)
	int rndSeed = 1234;

	@Override
	public Circle compute1(IterableInterval<?> input) throws IllegalArgumentException {
		if (input.size() > Integer.MAX_VALUE || (center != null && input.size() > Integer.MAX_VALUE / 2)) {
			throw new IllegalArgumentException();
		}

		List<RealLocalizable> points = getInitialPointList(input);
		List<RealLocalizable> boundary = new Vector<RealLocalizable>(3);
		
		SS ss = new SS();
		ss.p = points;
		ss.b = boundary;

		if(randomizePointRemoval)
		{
			Collections.shuffle(ss.p, new Random(rndSeed));
		}

		Circle D = miniDisk(ss);

		if(paddingRatio != 1.0)
		{
			D = new Circle(D.getCenter(), D.getRadius()*paddingRatio);
		}

		return D;
	}

	private List<RealLocalizable> getInitialPointList(IterableInterval<?> input) {
		List<RealLocalizable> points;
		if (center == null)
			points = new Vector<RealLocalizable>((int) input.size());
		else
			points = new Vector<RealLocalizable>(2 * (int) input.size());

		Cursor<?> c = input.cursor();
		while (c.hasNext()) {
			c.fwd();
			RealPoint p = new RealPoint(c);
			points.add(p);
			if (center != null) {
				// Add a mirroring point
				double[] pos = new double[c.numDimensions()];
				for (int d = 0; d < c.numDimensions(); d++) {
					pos[d] = 2 * center.getDoublePosition(d) - p.getDoublePosition(d);
				}
				points.add(new RealPoint(pos[0], pos[1]));
			}
		}
		return points;
	}
	
	public Circle makeNextCircle(SS ss)
	{
		Circle D;
		if (ss.b.size() == 3) {
			D = makeCircle3(ss.b);
			return D;
			//			System.out.println("A " + D + " p:" + points + " b:" + boundary);
		} else if (ss.p.size() == 1 && ss.b.size() == 0) {
			D = makeCircle1(ss.p);
			return D;
			//			System.out.println("B " + D + " p:" + points + " b:" + boundary);
		} else if (ss.p.size() == 0 && ss.b.size() == 2) {
			D = makeCircle2(ss.b);
			return D;
			//			System.out.println("C " + D + " p:" + points + " b:" + boundary);
		} else if (ss.p.size() == 1 && ss.b.size() == 1) {
			RealLocalizable p1 = ss.p.get(0);
			RealLocalizable p2 = ss.b.get(0);
			List<RealLocalizable> pl = new Vector<>();
			pl.add(p1);
			pl.add(p2);
			D = makeCircle2(pl); // pointList and boundary
			return D;
			//			System.out.println("D " + D + " p:" + points + " b:" + boundary);
		} 
		return null;
	}

	private Circle miniDisk(SS ss) {

		// Make a circle if you can and return
		Circle D =  makeNextCircle(ss);
		if(D != null)
		{
			return D;
		}
		else
		{
			// Trim and call
			D = miniDisk(ss.getTrimmed());
			if (!D.contains(ss.getTestPoint())) {
				ss.next();
				D = miniDisk(ss);
				return D;
			}
			return D;
		}
	}

	private Circle makeCircle1(List<RealLocalizable> points) {
		return new Circle(points.get(0), 0);
	}

	private Circle makeCircle2(List<RealLocalizable> points) {
		double[] pos1 = new double[points.get(0).numDimensions()];
		double[] pos2 = new double[points.get(0).numDimensions()];

		points.get(0).localize(pos1);
		points.get(1).localize(pos2);

		double x0 = (pos1[0] + pos2[0]) / 2.0;
		double y0 = (pos1[1] + pos2[1]) / 2.0;
		double r = calcDistance(pos1[0], pos1[1], pos2[0], pos2[1]) / 2.0;

		RealPoint center = new RealPoint(x0, y0);

		return new Circle(center, r);

	}

	private Circle makeCircle3(List<RealLocalizable> points) {

		Circle D;

		double[] pos1 = new double[points.get(0).numDimensions()];
		double[] pos2 = new double[points.get(0).numDimensions()];
		double[] pos3 = new double[points.get(0).numDimensions()];

		points.get(0).localize(pos1);
		points.get(1).localize(pos2);
		points.get(2).localize(pos3);

		double x1 = pos1[0];
		double x2 = pos2[0];
		double x3 = pos3[0];

		double y1 = pos1[1];
		double y2 = pos2[1];
		double y3 = pos3[1];

		double ma = (y2 - y1) / (x2 - x1);
		double mb = (y3 - y2) / (x3 - x2);

		// If the 3 points lie on a line, use only the 2 outermost points
		if (ma == mb) {
			double d12 = calcDistance(x1, y1, x2, y2);
			double d13 = calcDistance(x1, y1, x3, y3);
			double d23 = calcDistance(x2, y2, x3, y3);

			double biggest = Math.max(Math.max(d12, d13), d23);

			if (biggest == d12)
				points.remove(2);
			else if (biggest == d13)
				points.remove(1);
			else if (biggest == d23)
				points.remove(0);

			D = makeCircle2(points);
		} else {
			// Calculate the center (intersection of lines perpendicular to
			// those separating the points)
			double x0 = (ma * mb * (y1 - y3) + mb * (x1 + x2) - ma * (x2 + x3)) / (2.0 * (mb - ma));
			double y0 = (-1.0 / ma) * (x0 - (x1 + x2) / 2.0) + (y1 + y2) / 2.0;

			// Calculate the radius
			double r = calcDistance(x0, y0, x1, y1);

			// Make circle
			RealPoint center = new RealPoint(x0, y0);
			D = new Circle(center, r);
		}

		return D;
	}

	private double calcDistance(double xa, double ya, double xb, double yb) {
		return Math.sqrt(Math.pow(xb - xa, 2) + Math.pow(yb - ya, 2));
	}
	
	class SS
	{
		public List<RealLocalizable> p, b;
		int stage = 0;
		RealLocalizable test = null;
		Circle c = null;

		public SS getTrimmed()
		{
			SS ret = this.copy();
			ret.p = ret.p.subList(0, ret.p.size()-1);
			return ret;
		}
		
		public RealLocalizable getTestPoint()
		{
			return this.p.get(this.p.size()-1);
		}
		
		public SS getNext()
		{
			SS ret = this.copy();
			ret.b.add(ret.p.remove(ret.p.size()-1));
			return ret;
		}
		
		public void next()
		{
			b.add(p.remove(p.size()-1));
		}

		public void add(RealLocalizable test)
		{
			b.add(test);
		}

		public void setCircle()
		{
			this.c = makeNextCircle(this);
		}

		public SS copy()
		{
			SS ret = new SS();
			ret.p = new Vector<RealLocalizable>(this.p);
			ret.b = new Vector<RealLocalizable>(this.b);
			ret.stage = this.stage;
			if(this.test == null)
			{
				ret.test = null;
			}
			else
			{
				ret.test = new RealPoint(this.test);
			}
			return ret;
		}

		public void print()
		{
			System.out.println("---- SnapShot ----");
			System.out.println(p);
			System.out.println(b);
			System.out.println(stage);
			System.out.println(test);
			System.out.println(c);
		}
	}

}