package com.regnosys.rosetta.interpreternew.visitors;

import java.util.*;

import com.regnosys.rosetta.rosetta.interpreter.RosettaInterpreterBaseEnvironment;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterTypedValue;
import com.regnosys.rosetta.rosetta.expression.RosettaExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaFeatureCall;
import com.regnosys.rosetta.rosetta.expression.RosettaOnlyExistsExpression;
import com.regnosys.rosetta.rosetta.expression.RosettaSymbolReference;
import com.regnosys.rosetta.rosetta.interpreter.RosettaInterpreterValue;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterBooleanValue;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterListValue;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterTypedFeatureValue;

public class RosettaInterpreterOnlyExistsInterpreter extends RosettaInterpreterConcreteInterpreter {
	
	/**
     * Interprets a 'only exists' expression.
     *
     * @param exp the 'only exists' expression to interpret.
     * @param env the environment containing variable bindings.
     * @return a boolean value indicating if only the specified features exist.
     */
    public RosettaInterpreterValue interp(RosettaOnlyExistsExpression exp, RosettaInterpreterBaseEnvironment env) {
        Set<String> expectedFeatures = new HashSet<>();

        for (RosettaExpression expression : exp.getArgs()) {
            RosettaFeatureCall featureCall = (RosettaFeatureCall) expression;
            
            // Add the feature name to the set of expected features
            RosettaInterpreterValue finalAttribute = getAttributeUtil(featureCall, env);
            expectedFeatures.add(((RosettaInterpreterTypedFeatureValue)finalAttribute).getName());
        }

        // Get the variable (from the environment) that we need to check the attributes for
        RosettaExpression firstExpression = exp.getArgs().get(0);
        RosettaFeatureCall firstFeatureCall = (RosettaFeatureCall) firstExpression;
        
        RosettaInterpreterValue objectInstance = getReceiverUtil(firstFeatureCall.getReceiver(), env);
        
        // Check if the only non-null attributes are the expected features.
        // We expect either to find an attribute from expectedFeatures that is declared
        // or find one that is not in expectedFeatures and is not declared
        List<RosettaInterpreterTypedFeatureValue> attributes = 
        		((RosettaInterpreterTypedValue) objectInstance).getAttributes();
        boolean onlyExpectedFeaturesExist = attributes.stream()
            .allMatch(attr -> expectedFeatures.contains(attr.getName()) == isDeclared(attr));

        return new RosettaInterpreterBooleanValue(onlyExpectedFeaturesExist);
    }
    
    // Checks if an attribute is declared
    private boolean isDeclared(RosettaInterpreterTypedFeatureValue attr) {
    	// Returns true if attribute is not a RosettaInterpreterListValue 
    	// containing an empty list of expressions
    	if (attr.getValue() instanceof RosettaInterpreterListValue) {
    		return !((RosettaInterpreterListValue) attr.getValue()).getExpressions().isEmpty();
    	}
    		
    	return true; // if it's not a RosettaInterpreterListValue it cannot possibly be "empty"
    }

     /**
      * This is just used to get the final attribute's name from a feature call.
      * Example:
   	  * For "(foo -> bar -> firstNumber, foo -> bar -> secondNumber) only exists" it will 
      * get "firstNumber" from the first element, and "secondNumber" from the second one.
      * These will then be saved in the "expectedFeatures" set so we know which ones are 
      * supposed to be declared in the "bar" object.
      *
      * @param featureCall The feature call that contains the attribute at the end
      * @param env The Current environment
      * @return The wanted attribute value
      */
    private RosettaInterpreterValue getAttributeUtil(RosettaFeatureCall featureCall, 
    		RosettaInterpreterBaseEnvironment env) {
        // Recursively get the next receiver until the last one
    	// (foo -> bar -> baz -> attribute) would mean that the last receiver is "baz"
    	RosettaInterpreterValue receiver = getReceiverUtil(featureCall.getReceiver(), env);
        
        // Find the correct attribute in the receiver's attribute list
        String featureName = featureCall.getFeature().getName();
        return ((RosettaInterpreterTypedValue)receiver).getAttributes().stream()
            .filter(attr -> attr.getName().equals(featureName))
            .findFirst().get();
    }
 
    /**
     * Recursively gets the receiver value of a feature call.
     * Example:
     * For "foo -> bar -> baz -> attr only exists", it will recursively get the rightside
     * of the "->", until it gets to the last receiver, which is "baz" in this case.
     * "baz" will be used afterwards to check which attributes it contains that are/are not declared.
     *
     * @param receiver The receiver of the feature call
     * @param env The current environment
     * @return The last receiver of the nested feature call
     */
    private RosettaInterpreterValue getReceiverUtil(RosettaExpression receiver, RosettaInterpreterBaseEnvironment env) {
        if (receiver instanceof RosettaSymbolReference) {
        	// Meaning we already got to the end, we don't have another feature call
            RosettaSymbolReference ref = (RosettaSymbolReference) receiver;
            String receiverSymbolName = ref.getSymbol().getName();
            return (RosettaInterpreterTypedValue) env.findValue(receiverSymbolName);
            
        } else {
        	// We need to recursively get the next receiver
            RosettaFeatureCall featureCall = (RosettaFeatureCall) receiver;           
            RosettaInterpreterValue nextReceiver = getReceiverUtil(featureCall.getReceiver(), env);
            
            String featureName = featureCall.getFeature().getName();
            return ((RosettaInterpreterTypedValue) nextReceiver).getAttributes().stream()
                .filter(attr -> attr.getName().equals(featureName))
                .map(RosettaInterpreterTypedFeatureValue::getValue)
                .filter(value -> value instanceof RosettaInterpreterTypedValue)
                .map(value -> (RosettaInterpreterTypedValue) value)
                .findFirst().get();
        }
    }
}

