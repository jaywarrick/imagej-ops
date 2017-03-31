/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, University of Konstanz and Brian Northan.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.ops.create.labelingMapping;

import net.imagej.ops.Ops;
import net.imagej.ops.special.function.AbstractNullaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.NullaryFunctionOp;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.type.numeric.IntegerType;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Create a LabelingMapping which can store at least maxNumSets different Sets
 *
 * @author Christian Dietz (University of Konstanz)
 * @param <L> label type
 */
@Plugin(type = Ops.Create.LabelingMapping.class)
public class DefaultCreateLabelingMapping<L> extends
	AbstractNullaryFunctionOp<LabelingMapping<L>> implements
	Ops.Create.LabelingMapping
{

	@Parameter(required = false)
	private int maxNumSets;

	@SuppressWarnings("rawtypes")
	private NullaryFunctionOp<IntegerType> indexTypeCreator;

	@Override
	public void initialize() {
		indexTypeCreator = Functions.nullary(ops(),
			Ops.Create.IntegerType.class, IntegerType.class, maxNumSets);
	}

	@Override
	public LabelingMapping<L> calculate() {
		return new LabelingMapping<>(indexTypeCreator.calculate());
	}

}
