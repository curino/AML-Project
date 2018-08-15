/******************************************************************************
* Copyright 2013-2018 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* Aggregation of values from a list of arguments (which can be any type of    *
* Expression).                                                                *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

import aml.AML;

public class Aggregate extends AbstractExpression implements ValueExpression
{

//Attributes
	
	private String operator;
	//It is unclear whether the order of the arguments matters or not
	//so to be on the safe side, we'll preserve it
	private Vector<ValueExpression> arguments;
	
//Constructor
	
	/**
	 * Constructs a new Apply with the given operator and arguments
	 * @param operator: the uri of the operator to apply
	 * @param arguments: the expressions that are arguments of the operation
	 */
	public Aggregate(String operator, Vector<ValueExpression> arguments)
	{
		super();
		this.operator = operator;
		this.arguments = arguments;
		for(ValueExpression e : arguments)
			elements.addAll(e.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof Aggregate &&
				((Aggregate)o).operator.equals(this.operator) &&
				((Aggregate)o).arguments.equals(this.arguments);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * The components of an Apply are the list of arguments
	 */
	public Collection<ValueExpression> getComponents()
	{
		return arguments;
	}
	
	/**
	 * @return the operator of this apply
	 */
	public String getOperator()
	{
		return operator;
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<" + RDFElement.AGGREGATE_.toRDF() + " " + RDFElement.OPERATOR.toRDF() +
				"=\"" + operator + "\">\n" +
				"<" + RDFElement.ARGUMENTS.toRDF() + " " + RDFElement.RDF_PARSETYPE.toRDF() + "=\"Collection\">\n";
		for(ValueExpression e : arguments)
			rdf += e.toRDF() + "\n";
		rdf += "</" + RDFElement.ARGUMENTS.toRDF() + ">\n";
		rdf += "</" + RDFElement.AGGREGATE_.toRDF() + ">\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = AML.getInstance().getEntityMap().getLocalName(operator) + " [";
		for(ValueExpression e : arguments)
			s += e.toString() + ", ";
		s = s.substring(0, s.lastIndexOf(',')) + "]";
		return s;
	}
}