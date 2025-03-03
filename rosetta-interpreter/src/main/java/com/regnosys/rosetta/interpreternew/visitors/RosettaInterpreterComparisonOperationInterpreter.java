package com.regnosys.rosetta.interpreternew.visitors;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.ecore.EObject;

import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterBaseValue;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterBooleanValue;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterEnvironment;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterError;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterErrorValue;
import com.regnosys.rosetta.interpreternew.values.RosettaInterpreterListValue;
import com.regnosys.rosetta.rosetta.expression.ModifiableBinaryOperation;
import com.regnosys.rosetta.rosetta.expression.RosettaExpression;
import com.regnosys.rosetta.rosetta.interpreter.RosettaInterpreterValue;

public class RosettaInterpreterComparisonOperationInterpreter extends 
	RosettaInterpreterConcreteInterpreter {
	
	private static List<String> comparisonOperators = 
			Arrays.asList("<", "<=", ">", ">=", "=", "<>");
	
	private static List<String> comparableClasses = 
			Arrays.asList(
					"com.regnosys.rosetta.interpreternew.values."
					+ "RosettaInterpreterBooleanValue",
					"com.regnosys.rosetta.interpreternew.values."
					+ "RosettaInterpreterNumberValue",
					"com.regnosys.rosetta.interpreternew.values."
					+ "RosettaInterpreterStringValue");
	
	
	/**
	 * Interprets a comparison operation, evaluating the comparison between two operands.
	 *
	 * @param expr The ComparisonOperation expression to interpret
	 * @param env RosettaInterpreterBaseEnvironment that keeps track
	 *		   of the current state of the program
	 * @return If no errors are encountered, a RosettaInterpreterBooleanValue representing
	 * 		   the result of the comparison operation.
	 * 		   If errors are encountered, a RosettaInterpreterErrorValue representing
     *         the error.
	 */
	public RosettaInterpreterBaseValue interp(ModifiableBinaryOperation expr,
			RosettaInterpreterEnvironment env) {		
		if (!comparisonOperators.contains(expr.getOperator())) {
			return new RosettaInterpreterErrorValue(
					new RosettaInterpreterError(
							"operator not suppported", expr)); 
		}
		RosettaExpression left = expr.getLeft();
		RosettaExpression right = expr.getRight();
		
		RosettaInterpreterValue leftValue = left.accept(visitor, env);
		RosettaInterpreterValue rightValue = right.accept(visitor, env);
		
		if (RosettaInterpreterErrorValue.errorsExist(leftValue)
				&& RosettaInterpreterErrorValue.errorsExist(rightValue)) {
			return RosettaInterpreterErrorValue
					.merge((RosettaInterpreterErrorValue)leftValue,
					(RosettaInterpreterErrorValue)rightValue);
		}
		else if (RosettaInterpreterErrorValue.errorsExist(leftValue)) {
			return (RosettaInterpreterErrorValue)leftValue;
		}
		else if (RosettaInterpreterErrorValue.errorsExist(rightValue)) {
			return (RosettaInterpreterErrorValue)rightValue;
		}
		
		//check cardinality operation
		switch (expr.getCardMod()) {		
		case ANY:
			return compareAny(leftValue, rightValue, expr.getOperator(), expr);
			
		case ALL:
			return compareAll(leftValue, rightValue, expr.getOperator(), expr);
		default: //case NONE
			//normally compare left and right side.
			boolean result = checkComparableTypes(leftValue, 
					rightValue, 
					expr.getOperator());
			return new RosettaInterpreterBooleanValue(result);			
		}
	}

	private RosettaInterpreterBaseValue compareAny(RosettaInterpreterValue leftValue, 
			RosettaInterpreterValue rightValue, 
			String operator, EObject associatedObject) {
		//list vs list case:
		if (leftValue instanceof RosettaInterpreterListValue 
				&& rightValue instanceof RosettaInterpreterListValue) {
			return new RosettaInterpreterErrorValue(
					new RosettaInterpreterError(
							"cannot compare two lists", associatedObject));
		}
		
		//list vs element case:
		else if (leftValue instanceof RosettaInterpreterListValue) {
			
			RosettaInterpreterListValue lfList = 
					(RosettaInterpreterListValue) leftValue;
			
			//only way this is allowed is if left side has a length of
			//  more than 1
			if (lfList.getExpressions().size() > 1) {
			
				//for all elements in left list, check if the comparison 
				// between them and right-hand side is true
				boolean anyTrue = false;
				for (RosettaInterpreterValue e : lfList.getExpressions()) {
					anyTrue |= checkComparableTypes(e, 
							rightValue, 
							operator);
				}
				return new RosettaInterpreterBooleanValue(anyTrue); 
			}
		}
		
		return new RosettaInterpreterErrorValue(
				new RosettaInterpreterError(
						"cannot use \"ANY\" keyword "
						+ "to compare two elements", associatedObject));
	}

	private RosettaInterpreterBaseValue compareAll(RosettaInterpreterValue leftValue, 
			RosettaInterpreterValue rightValue, 
			String operator, EObject associatedObject) {
		//list vs list case:
		if (leftValue instanceof RosettaInterpreterListValue 
				&& rightValue instanceof RosettaInterpreterListValue) {
			return new RosettaInterpreterErrorValue(
					new RosettaInterpreterError(
							"cannot compare two lists", associatedObject));
		}
		
		//list vs element case:
		else if (leftValue instanceof RosettaInterpreterListValue) {
			
			RosettaInterpreterListValue lfList = 
					(RosettaInterpreterListValue) leftValue;
			
			//only way this is allowed is if left side has a length of
			//  more than 1
			if (lfList.getExpressions().size() > 1) {
			
				//for all elements in left list, check if the comparison 
				// between them and right-hand side is true
				boolean allTrue = true;
				for (RosettaInterpreterValue e : lfList.getExpressions()) {
					allTrue &= checkComparableTypes(e, 
							rightValue, 
							operator);
				}
				return new RosettaInterpreterBooleanValue(allTrue); 
			}
		}
		
		return new RosettaInterpreterErrorValue(
				new RosettaInterpreterError(
						"cannot use \"ALL\" keyword "
						+ "to compare two elements", associatedObject));
	}

	private boolean checkComparableTypes(RosettaInterpreterValue leftValue, 
			RosettaInterpreterValue rightValue,
			String operator) {
		int comparisonResult = 2;
		
		
		boolean isComparable = false;
		
		//left and right will be of type comparableClazz
		Class<?> comparableClazz = null; 
		
		try {
			for (String clazzString : comparableClasses) {
				Class<?> clazz = Class.forName(clazzString);
				
				//check that both expressions are of the same type
				boolean isInstance = (clazz.isInstance(leftValue) 
						&& clazz.isInstance(rightValue));
				if (isInstance) {
					comparableClazz = clazz;
				}
				isComparable |= isInstance;
			}  
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Not a class");
			e.printStackTrace();
		}
		
		if (isComparable) {
			try {
				//compare the two expressions
				comparisonResult = (int) comparableClazz
						.getDeclaredMethod("compareTo",comparableClazz)
						.invoke(leftValue, rightValue);
			} catch (IllegalAccessException | IllegalArgumentException 
					| InvocationTargetException | NoSuchMethodException 
					| SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
		return compareComparableValues(comparisonResult, 
				operator);
	}

	private boolean compareComparableValues(int comparisonResult, String operator) {
		if (comparisonResult == 2) {
			return false;
		}
		switch (operator) {
		case "<>":
			return comparisonResult != 0;
		case "<":
			return comparisonResult == -1;
		case "<=":
			return comparisonResult <= 0;
		case ">":
			return comparisonResult == 1;
		case ">=":
			return comparisonResult >= 0;
		default:
			return comparisonResult == 0; //case "="
		}
	}

}
