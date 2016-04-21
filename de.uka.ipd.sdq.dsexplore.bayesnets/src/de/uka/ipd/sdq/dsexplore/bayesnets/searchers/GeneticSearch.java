package de.uka.ipd.sdq.dsexplore.bayesnets.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import de.uka.ipd.sdq.dsexplore.bayesnets.utility.BayesNetwork;
import de.uka.ipd.sdq.dsexplore.bayesnets.utility.BayesNetworkScore;

public class GeneticSearch {

	Map<String,Double> Population;
	
	int[][] Data;
	/**
	 * Give your own population.
	 * @param Population
	 */
	public GeneticSearch(Map<String,Double> Population, int[][] Data){
		this.Population = Population;
		this.Data = Data;
	}
	
	/**
	 * Create random population
	 * @param popsize Population size
	 * @param dimension Dimension of a single candidate in population. Should
	 * always be a square
	 */
	public GeneticSearch(int popsize, int dimension, int[][] Data){
		Map<String,Double> population = new HashMap<String,Double>();
		this.Data = Data;
		for(int i=0;i<popsize;i++){
			// Generate a random string
			String candidate = generateRandomGraphString(dimension);
			System.out.println(candidate);
			// Score the candidate
			BayesNetworkScore bnscore 
			= new BayesNetworkScore(generateAdjMatFromString(candidate),
					this.Data);
			population.put(candidate, bnscore.K2NetworkScore());
		}
		this.Population = population;
		System.out.println("Population Set up ...");
	}
	
	private int[][] generateAdjMatFromString(String grphstring){
		int nodes = (int) Math.sqrt(grphstring.toCharArray().length);
		int[][] AdjMat = new int[nodes][nodes];
		int pos = -1;
		for(int i=0;i<nodes;i++){
			for(int j=0;j<nodes;j++){
				pos++;
				AdjMat[i][j]=Character.getNumericValue(grphstring.charAt(pos));
			}
		}
		return AdjMat;
	}
	
	private String generateRandomBinaryString(int length){
		Random random = new Random();
		String Result = "";
		for(int i=0;i<length;i++){
			Result = Result+((Integer)random.nextInt(2)).toString();
		}
		return Result;
	}
	
	/**
	 * Generates a random binary string corresponding to 
	 * an acyclic graph
	 * @param length length of string must be a square
	 * @return
	 */
	private String generateRandomGraphString(int length){
		System.out.println("Generating random graph ...");
		BayesNetwork bn = new BayesNetwork((int) Math.sqrt(length));
		
		int[][] RandomGraph = bn.createRandomStructure();
		// Convert the graph to string
		String Result = "";
		for(int i=0;i<RandomGraph.length;i++){
			for(int j=0;j<RandomGraph[i].length;j++){
				Result = Result + ((Integer)RandomGraph[i][j]).toString();
			}
		}
		System.out.println("Converting graph to string ...");
		return Result;
	}
	
	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub

		int[][] DataMat = new int[100][10];
		Random r = new Random();
		for(int i=0;i<100;i++){
			for(int j=0;j<10;j++){
				DataMat[i][j] = r.nextInt(2);
			}
			
		}
		GeneticSearch gnsrch = new GeneticSearch(10,100,DataMat);
		int[][] graph = gnsrch.search();
		for(int i=0;i<graph.length;i++){
			for(int j=0;j<graph.length;j++){
				System.out.print(graph[i][j]);
			}
			System.out.println();
		}
	}

	/**
	 * 
	 * @param DataMatrix
	 * @return
	 * @throws InterruptedException 
	 */
	public int[][] search() throws InterruptedException{
		// Random initialization of population is done in
		// constructor
		System.out.println("Starting search ...");
		for(int iter = 0;iter<100;iter++){
			//Now pick some parents for crossover
			System.out.println("Picking parents ...");
			ArrayList<Double> scores 
			= new ArrayList<Double>(this.Population.values());
			Collections.sort(scores);
			Collections.reverse(scores);
			
			// Pick the best 8 candidates
			int ParentSize = 8;
			ArrayList<String> Parents = new ArrayList<String>();
			for(int i=0;i<ParentSize;i++){
				Iterator<String> iterator 
				= this.Population.keySet().iterator();
				while(iterator.hasNext()){
					String candidate = iterator.next();
					if(this.Population.get(candidate)==scores.get(i)){
						Parents.add(candidate);
						break;
					}
				}
			}
			
			// Produce children through crossover and mutation
			int ChildrenSize = 15;
			ArrayList<String> Children = new ArrayList<String>();
			double CrossProb = .75;
			double MutProb = .25;
			while(Children.size()<ChildrenSize){
				double rand = Math.random();
				if(rand<0.75){
					// do crossover
					Random chooseParent = new Random();
					int index1 = chooseParent.nextInt(ParentSize);
					int index2 = chooseParent.nextInt(ParentSize);
					String Parent1 = Parents.get(index1);
					String Parent2 = Parents.get(index2);
					ArrayList<String> crosschildren = 
							doCrossover(Parent1,Parent2);
					String child1 = crosschildren.get(0);
					String child2 = crosschildren.get(1);
					// Check whether children are acyclic
					BayesNetwork cyclechecker = new BayesNetwork(this.Data[0].length);
					boolean child1IsAcyclic = 
							cyclechecker.checkCycles(
									generateAdjMatFromString(crosschildren.get(0)));
					boolean child2IsAcyclic = 
							cyclechecker.checkCycles(
									generateAdjMatFromString(crosschildren.get(1)));
					if(!child1IsAcyclic){
						crosschildren.remove(child1);
					}
					if(!child2IsAcyclic){
						crosschildren.remove(child2);
					}
					Children.addAll(crosschildren);
				}else{
					// do mutation
					Random chooseParent = new Random();
					int index = chooseParent.nextInt(ParentSize);
					String Parent = Parents.get(index);
					String child = doMutation(Parent);
					// check cyclicity
					BayesNetwork cyclechecker = new BayesNetwork(this.Data[0].length);
					boolean childIsAcyclic = 
							cyclechecker.checkCycles(
									generateAdjMatFromString(child));
					if(childIsAcyclic){
						Children.add(child);
					}
					
				}
			}
			System.out.println("Children produced ...");
			// Score the Children and add to population
			System.out.println("Scoring children ...");
			System.out.println(Children.size());
			Map<String,Double> ChildrenMap = new HashMap<String,Double>();
			for(int i=0;i<Children.size();i++){
				String child = Children.get(i);
				int[][] childAdjMat = generateAdjMatFromString(child);
				BayesNetworkScore bnscore = 
						new BayesNetworkScore(childAdjMat,this.Data);
				ChildrenMap.put(child, bnscore.K2NetworkScore());
			}
			int OriginalPopSize = this.Population.size();
			System.out.println(OriginalPopSize);
			this.Population.putAll(ChildrenMap);
			int NewPopSize = this.Population.size();
			System.out.println(NewPopSize);
			// Discard some bad scoring candidates so that
			// population size is same
			//ArrayList<Double> popscores = 
			//		new ArrayList<Double>(this.Population.values());
			//Collections.sort(popscores);
			//System.out.println(popscores.size());
			Collection<Double> popscores = this.Population.values();
			for(int i=0;i<(NewPopSize-OriginalPopSize);i++){
				popscores.remove(Collections.min(popscores));
			}
			System.out.println(this.Population.size());
			//Thread.sleep(2000);
			System.out.println("Resizing the population ...");
		}
		// return the best candidate in the population
		String BestNetwork = "";
		ArrayList<Double> finalcandidatescores = 
				new ArrayList<Double>(this.Population.values());
		Collections.sort(finalcandidatescores);
		Collections.reverse(finalcandidatescores);
		Iterator<String> iterator 
		= this.Population.keySet().iterator();
		while(iterator.hasNext()){
			String candidate = iterator.next();
			if(this.Population.get(candidate)==
					finalcandidatescores.get(0)){
				BestNetwork = candidate;
				break;
			}
		}
		System.out.println("The score of the best network is:"
		+this.Population.get(BestNetwork));
		return generateAdjMatFromString(BestNetwork);
	}
	
	/**
	 * Does crossover on the parent strings. Currently, the parent 
	 * strings are asumed to be binary strings.
	 * @param Parent1
	 * @param Parent2
	 * @return
	 */
	private ArrayList<String> doCrossover(String Parent1, String Parent2){
		ArrayList<String> Children = new ArrayList<String>();
		if(Parent1.toCharArray().length != Parent2.toCharArray().length){
			return null;
		}else{
			Random random = new Random();
			int pos = random.nextInt(Parent1.toCharArray().length);
			//System.out.println(pos);
			String Child1 = Parent1.substring(0, pos)+Parent2.substring(pos);
			String Child2 = Parent2.substring(0, pos)+Parent1.substring(pos);
			Children.add(Child1);
			Children.add(Child2);
		}
		return Children;
	}
	
	/**
	 * Mutates a given binary Parent string and returns a
	 * child string
	 * @param Parent
	 * @return
	 */
	private String doMutation(String Parent){
		Random random = new Random();
		int pos = random.nextInt(Parent.toCharArray().length);
		//System.out.println(pos);
		if(Parent.charAt(pos)=='1'){
			StringBuilder ParentSB = new StringBuilder(Parent);
			ParentSB.setCharAt(pos, '0');
			Parent = ParentSB.toString();
		}else if(Parent.charAt(pos)=='0'){
			StringBuilder ParentSB = new StringBuilder(Parent);
			ParentSB.setCharAt(pos, '1');
			Parent = ParentSB.toString();
		}
		return Parent;
	}
}
