package cs224n.assignment;

import cs224n.assignment.Grammar.UnaryRule;
import cs224n.ling.Tree;
import cs224n.util.CounterMap;
import cs224n.util.Pair;

import java.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
    CounterMap<List<String>,Tree<String>> knownParses;
    CounterMap<Pair<Integer,Integer>,String> spanToCategories;
    
    public void train(List<Tree<String>> trainTrees) {
        // TODO: before you generate your grammar, the training trees
        // need to be binarized so that rules are at most binary
    	
        lexicon = new Lexicon(trainTrees);
        
        // binarize the train trees
        List<Tree<String>> binarizedTrainTrees = new ArrayList<Tree<String>>();
        for (Tree<String> trainTree : trainTrees) {
        	binarizedTrainTrees.add(TreeAnnotations.annotateTree(trainTree));
        }
        
        grammar = new Grammar(binarizedTrainTrees);
        
        /*for (Tree<String> trainTree : trainTrees) {
            List<String> tags = trainTree.getPreTerminalYield();
            knownParses.incrementCount(tags, trainTree, 1.0);
            tallySpans(trainTree, 0);
        }*/
    }

    public Tree<String> getBestParse(List<String> sentence) {
    	spanToCategories = new CounterMap<Pair<Integer,Integer>,String>();
    	
    	//Get the lexicon tags
    	Set<String> allTags = lexicon.getAllTags();
    	
    	//Get nonterminal counts
    	for (int i=0; i < sentence.size(); i++){
    		for (String tag : allTags)
    			spanToCategories.setCount(getIntPair(i,i),tag,lexicon.scoreTagging(sentence.get(i), tag));
    	}
    	
    	//Add unaries
    	boolean added = true;
    	while (added){
    		for (int i=0; i < sentence.size(); i++){
    			for (String tag : allTags){
    				List<UnaryRule> unaryParents = grammar.getUnaryRulesByChild(tag);
        			for (UnaryRule up : unaryParents){
        				double p = up.getScore() * spanToCategories.getCount(getIntPair(i,i), up.parent);
        				if (p > spanToCategories.getCount(getIntPair(i,i), tag)){
        					spanToCategories.setCount(getIntPair(i,i), tag,p);
        				}
    			}
    		}
    	}

    	
        // TODO: implement this method
        return null;
    }
    
    private Pair<Integer,Integer> getIntPair(int a, int b){
    	return new Pair<Integer,Integer>(a,b);
    }
}
