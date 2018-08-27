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
* A filtering algorithm based on cardinality.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingStatus;

public class Selector implements Filterer, Flagger
{
	
//Attributes
	
	private double thresh;
	private SelectionType type;
	
//Constructors
	
	/**
	 * Constructs a Selector with the given similarity threshold
	 * and automatic SelectionType
	 * @param thresh: the similarity threshold
	 */
	public Selector(double thresh)
	{
		this.thresh = thresh;
		type = SelectionType.getSelectionType();
	}
	
	/**
	 * Constructs a Selector with the given similarity threshold
	 * and SelectionType
	 * @param thresh: the similarity threshold
	 * @param type: the SelectionType
	 */
	public Selector(double thresh, SelectionType type)
	{
		this(thresh);
		this.type = type;
	}

//Public Methods
	
	@Override
	@SuppressWarnings("rawtypes")
	public Alignment filter(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot filter non-simple alignment!");
			return a;
		}
		System.out.println("Performing Selection");
		long time = System.currentTimeMillis()/1000;
		SimpleAlignment in = (SimpleAlignment)a;
		//The alignment to store selected mappings
		SimpleAlignment out = new SimpleAlignment();
		//Sort the active alignment
		in.sortDescending();
		//Then select Mappings in ranking order (by similarity)
		for(Mapping<String> m : in)
		{
			//If the Mapping is CORRECT, select it, regardless of anything else
			if(m.getStatus().equals(MappingStatus.CORRECT))
				out.add(m);
			//If it is INCORRECT or below the similarity threshold, discard it
			else if(m.getSimilarity() < thresh || m.getStatus().equals(MappingStatus.INCORRECT))
				continue;
			//Otherwise, add it if it obeys the rules for the chosen SelectionType:
					//In STRICT selection no conflicts are allowed
			else if((type.equals(SelectionType.STRICT) && !out.containsConflict(m)) ||
					//In PERMISSIVE selection only conflicts of equal similarity are allowed
					(type.equals(SelectionType.PERMISSIVE) && !out.containsBetterMapping(m)) ||
					//And in HYBRID selection a cardinality of 2 is allowed above 0.75 similarity
					(type.equals(SelectionType.HYBRID) && ((m.getSimilarity() > 0.75 &&
					out.cardinality(m.getEntity1()) < 2 && out.cardinality(m.getEntity2()) < 2) ||
					//And PERMISSIVE selection is employed below this limit
					!out.containsBetterMapping(m))))
				out.add(m);
		}
		if(out.size() < a.size())
		{
			for(Mapping<String> m : out)
				if(m.getStatus().equals(MappingStatus.FLAGGED))
					m.setStatus(MappingStatus.UNKNOWN);
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public void flag(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot flag non-simple alignment!");
			return;
		}
		System.out.println("Running Cardinality Flagger");
		long time = System.currentTimeMillis()/1000;
		SimpleAlignment b = (SimpleAlignment)a;
		for(Mapping<String> m : b)
			if(b.containsConflict(m) && m.getStatus().equals(MappingStatus.UNKNOWN))
				m.setStatus(MappingStatus.FLAGGED);
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
}