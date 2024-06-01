package com.regnosys.rosetta.interpreternew.values;

import java.time.LocalTime;
import java.util.stream.Stream;

import com.regnosys.rosetta.rosetta.interpreter.RosettaInterpreterValue;

public class RosettaInterpreterTimeValue extends RosettaInterpreterBaseValue {

	private LocalTime time;
	
	@Override
	public Stream<Object> toElementStream() {
		return Stream.of(time);
	}
	
	@Override
	public Stream<RosettaInterpreterValue> toValueStream() {
		return Stream.of(this);
	}
}
