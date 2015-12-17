package net.imagej.ops.math;

import org.scijava.plugin.Parameter;

import net.imagej.ops.ComputerWrapper;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.special.Computers;
import net.imagej.ops.special.UnaryComputerOp;
import net.imglib2.type.numeric.RealType;

public class RealMathOpComputerWrapper<I extends RealType<I>, O extends RealType<O>> implements ComputerWrapper<I,O> {
	
	@Parameter
	OpService ops;
	
	UnaryComputerOp<I,O> op = null;
	
	@Override
	public void compute(Class<? extends Op> opClass, I input, O output)
	{
		if(op == null)
		{
			this.op = Computers.unary(ops, opClass, output, input);
		}
		this.op.compute1(input, output);
	}

}
