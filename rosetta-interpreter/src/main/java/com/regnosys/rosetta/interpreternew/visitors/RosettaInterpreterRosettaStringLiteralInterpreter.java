package com.regnosys.rosetta.interpreternew.visitors;

import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterEnvironment;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterStringValue;
import com.regnosys.rosetta.rosetta.expression.RosettaStringLiteral;

public class RosettaInterpreterRosettaStringLiteralInterpreter 
	extends RosettaInterpreterConcreteInterpreter {

	public RosettaInterpreterStringValue interp(RosettaStringLiteral exp, 
			RosettaInterpreterEnvironment env) {
		return new RosettaInterpreterStringValue(exp.getValue());
	}

}
