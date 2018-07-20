/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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
* Matches Ontologies by finding literal full-name matches between their       *
* Lexicons and the Lexicon of a third mediating Ontology, by employing the    *
* LexicalMatcher. Extends Lexicons with synonyms from the mediating ontology. *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import aml.AML;
import aml.ext.LexiconExtender;
import aml.knowledge.MediatorLexicon;
import aml.knowledge.MediatorOntology;
import aml.ontology.Lexicon;
import aml.settings.EntityType;
import aml.settings.LexicalType;
import aml.util.MapSorter;
import aml.util.Table2Map;

public class MediatingMatcher implements LexiconExtender, PrimaryMatcher
{

//Attributes

	private static final String DESCRIPTION = "Matches entities that have one or more exact\n" +
			  								  "String matches between their Lexicon entries\n" +
			  								  "and a common Lexicon entry of a background\n" +
			  								  "knowledge source.";
	private static final String NAME = "Mediating Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS};
	//The MediatorLexicon used by this matcher
	protected MediatorLexicon ext;
	protected String uri;
	//The type of lexical entry generated by this Lexicon extender
	protected final LexicalType TYPE = LexicalType.EXTERNAL_MATCH;
	
//Constructors

	/**
	 * Constructs a MediatingMatcher with the given external Ontology
	 * @param x: the external Ontology
	 */
	public MediatingMatcher(MediatorOntology x)
	{
		ext = x.getMediatorLexicon();
		uri = x.getURI();
	}
	
	/**
	 * Constructs a MediatingMatcher with the given MediatorLexicon
	 * @param x: the MediatorLexicon
	 * @param u: the URI of the MediatorLexicon
	 */
	public MediatingMatcher(MediatorLexicon x, String u)
	{
		ext = x;
		uri = u;
	}

//Public Methods
	
	@Override
	public void extendLexicons()
	{
		System.out.println("Extending Lexicons with Mediating Matcher using " + uri);
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Lexicon source = aml.getSource().getLexicon();
		Table2Map<Integer,Integer,Double> maps = match(source,0.0);
		for(Integer s : maps.keySet())
		{
			int hit;
			//If there are multiple mappings, choose the best
			if(maps.entryCount(s) > 1)
			{
				Map<Integer,Double> results = MapSorter.sortDescending(maps.get(s));
				Iterator<Integer> i = results.keySet().iterator();
				hit = i.next();
				int secondHit = i.next();
				//If there is a tie, then skip to next class
				if(maps.get(s,hit) == maps.get(s,secondHit))
					continue;
			}
			else
				hit = maps.keySet(s).iterator().next();
			Set<String> names = ext.getNames(hit);
			for(String n : names)
			{
				double sim = maps.get(s,hit);
				source.add(s, n, "en", TYPE, uri, sim);
			}
		}
		Lexicon target = aml.getTarget().getLexicon();
		maps = match(target,0.0);
		for(Integer s : maps.keySet())
		{
			int hit;
			//If there are multiple mappings, choose the best
			if(maps.entryCount(s) > 1)
			{
				Map<Integer,Double> results = MapSorter.sortDescending(maps.get(s));
				Iterator<Integer> i = results.keySet().iterator();
				hit = i.next();
				int secondHit = i.next();
				//If there is a tie, then skip to next class
				if(maps.get(s,hit) == maps.get(s,secondHit))
					continue;
			}
			else
				hit = maps.keySet(s).iterator().next();
			Set<String> names = ext.getNames(hit);
			for(String n : names)
			{
				double sim = maps.get(s,hit);
				target.add(s, n, "en", TYPE, uri, sim);
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
	}
	
	@Override
	public String getDescription()
	{
		return DESCRIPTION;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public EntityType[] getSupportedEntityTypes()
	{
		return SUPPORT;
	}
	
	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Mediating Matcher using " + uri);
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Lexicon source = aml.getSource().getLexicon();
		Lexicon target = aml.getTarget().getLexicon();
		Table2Map<Integer,Integer,Double> src = match(source,thresh);
		Table2Map<Integer,Integer,Double> tgt = match(target,thresh);
		//Reverse the target alignment table
		Table2Map<Integer,Integer,Double> rev = new Table2Map<Integer,Integer,Double>();
		for(Integer s : tgt.keySet())
			for(Integer t : tgt.keySet(s))
				rev.add(t, s, tgt.get(s, t));
		Alignment maps = new Alignment();
		for(Integer s : src.keySet())
		{
			for(Integer med : src.keySet(s))
			{
				if(!rev.contains(med))
					continue;
				for(Integer t : rev.keySet(med))
				{
					double similarity = Math.min(src.get(s, med), rev.get(med, t));
					maps.add(s,t,similarity);
				}
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	protected void checkEntityType(EntityType e) throws UnsupportedEntityTypeException
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		if(!check)
			throw new UnsupportedEntityTypeException(e.toString());
	}
	
	protected Table2Map<Integer,Integer,Double> match(Lexicon source, double thresh)
	{
		Table2Map<Integer,Integer,Double> maps = new Table2Map<Integer,Integer,Double>();
		for(String s : source.getNames(EntityType.CLASS))
		{
			Set<Integer> sourceIndexes = source.getEntities(EntityType.CLASS,s);
			Set<Integer> targetIndexes = ext.getEntities(s);
			//If the name doesn't exist in either ontology, skip it
			if(sourceIndexes == null || targetIndexes == null)
				continue;
			//count += sourceIndexes.size()*targetIndexes.size();
			//Otherwise, match all indexes
			for(Integer i : sourceIndexes)
			{
				double weight = source.getCorrectedWeight(s, i);
				for(Integer j : targetIndexes)
				{
					//Get the weight of the name for the term in the larger lexicon
					double similarity = ext.getWeight(s, j);
					//Then compute the similarity, by multiplying the two weights
					similarity *= weight;
					//If the similarity is above threshold
					if(similarity >= thresh && (!maps.contains(i, j) || similarity > maps.get(i, j)))
						maps.add(i, j, similarity);
				}
			}
		}
		return maps;
	}
}