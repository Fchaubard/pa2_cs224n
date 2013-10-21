package cs224n.assignment;

import cs224n.assignment.Grammar.BinaryRule;
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
        CounterMap<Pair<Integer,Integer>,String> score = new CounterMap<Pair<Integer,Integer>,String>();
        Map<Pair<Pair<Integer,Integer>,String>,Pair<Integer,Pair<String,String>>> back = new HashMap<Pair<Pair<Integer,Integer>,String>,Pair<Integer,Pair<String,String>>>();
    	
    	//Get the lexicon tags
    	Set<String> allTags = lexicon.getAllTags();

    	//Get nonterminal counts
    	for (int i=0; i < sentence.size(); i++){
    		for (String tag : allTags){
    			score.setCount(getIntPair(i,i),tag,lexicon.scoreTagging(sentence.get(i), tag));
    		}
    	}
    	
    	double p;
    	for (int begin=0; begin < sentence.size(); begin++){
    		for (int end=0; end < sentence.size(); end++){
    			for (int split = begin; split < end; split++) {
    				Set<String> B_terms = score.getCounter(getIntPair(begin, split)).keySet();
    				Set<String> C_terms = score.getCounter(getIntPair(split, end)).keySet();
    				for (String B : B_terms){
    					for (BinaryRule rule : grammar.getBinaryRulesByLeftChild(B)){
    						if (C_terms.contains(rule.getRightChild())){
    							String C = rule.getRightChild();
    							String A = rule.getParent();
    							p=score.getCount(getIntPair(begin,split), B)*
    									score.getCount(getIntPair(split+1,end), C)*
    									rule.getScore();
    							if (p > score.getCount(getIntPair(begin,end), A)){
    								score.setCount(getIntPair(begin,end), A,p);
    								back.put(getIntIntStrTriple(begin, end, A), getIntStrStrTriple(split, B, C));
    							}
    						}
    					}
    				}
					//Handle unaries
    				boolean added = true;
    				while (added){
    					added=false;
    					B_terms = score.getCounter(getIntPair(begin, end)).keySet();
    					for (String B : B_terms){
    						List<UnaryRule> A_unaryParents = grammar.getUnaryRulesByChild(B);
    						for (UnaryRule A : A_unaryParents){
    							if (score.getCount(getIntPair(begin,end),B) > 0) {
    								p = A.getScore() * score.getCount(getIntPair(begin,end), B);
    								if (p > score.getCount(getIntPair(begin,end), A.parent)){
    									score.setCount(getIntPair(begin,end),A.parent, p);
    									back.put(getIntIntStrTriple(begin, end, A.parent), getIntStrStrTriple(-1,B,null));	
    									added=true;
    								}
    							}
    						}
    					}
    				}
    			}
    		}
    	}
    							
    	return buildTree(sentence,score,back);
    }

    private Tree<String> buildTree(List<String> sentence, CounterMap<Pair<Integer,Integer>,String> score, Map<Pair<Pair<Integer,Integer>,String>,Pair<Integer,Pair<String,String>>> back) {
    	Tree<String> root = new Tree<String>("ROOT");
    	Tree<String> topS = new Tree<String>("S");
    	root.setChildren(topS.toSubTreeList());
    	Pair<Integer, Pair<String, String>> rule = back.get(getIntIntStrTriple(0,sentence.size(),"S"));
    	List<Tree<String>> children;
    	int split=rule.getFirst();
    	if (split > 0){
    		//binary rule
    		children = buildBinaryTreeRec(sentence,0,sentence.size(),split,rule.getSecond().getFirst(),rule.getSecond().getSecond(),score,back);
    	} else {
    		//Unary rule
    		children = buildUnaryTreeRec(sentence,0,sentence.size(),rule.getSecond().getFirst(),score,back);
    	}
    	topS.setChildren(children);
    	return root;
    }
    
    private List<Tree<String>> buildBinaryTreeRec(List<String> sentence,int begin, int end,int split, String leftLabel, String rightLabel, CounterMap<Pair<Integer,Integer>,String> score, Map<Pair<Pair<Integer,Integer>,String>,Pair<Integer,Pair<String,String>>> back) {
    	//Build left tree
    	Tree<String> leftTree = new Tree<String>(leftLabel);
    	Pair<Integer, Pair<String, String>> rule = back.get(getIntIntStrTriple(begin,split,leftLabel));
    	List<Tree<String>> leftchildren;
    	int newsplit=rule.getFirst();
    	if (newsplit > 0){
    		//binary rule
    		leftchildren = buildBinaryTreeRec(sentence,begin,split+1,newsplit,rule.getSecond().getFirst(),rule.getSecond().getSecond(),score,back);
    	} else {
    		//Unary rule
    		leftchildren = buildUnaryTreeRec(sentence,begin,split+1,rule.getSecond().getFirst(),score,back);
    	}
    	leftTree.setChildren(leftchildren);

    	//Build right tree
    	Tree<String> rightTree = new Tree<String>(rightLabel);
    	rule = back.get(getIntIntStrTriple(begin,split,leftLabel));
    	List<Tree<String>> rightchildren;
    	newsplit=rule.getFirst();
    	if (newsplit > 0){
    		//binary rule
    		rightchildren = buildBinaryTreeRec(sentence,split+1,end,newsplit,rule.getSecond().getFirst(),rule.getSecond().getSecond(),score,back);
    	} else {
    		//Unary rule
    		rightchildren = buildUnaryTreeRec(sentence,split+1,end,rule.getSecond().getFirst(),score,back);
    	}
    	rightTree.setChildren(rightchildren);
    	
    	List<Tree<String>> children = new ArrayList<Tree<String>>();
    	children.add(leftTree);
        children.add(rightTree);
        return children;
    }
    
        	
    private List<Tree<String>> buildUnaryTreeRec(List<String> sentence,int begin, int end, String label, CounterMap<Pair<Integer,Integer>,String> score, Map<Pair<Pair<Integer,Integer>,String>,Pair<Integer,Pair<String,String>>> back) {
    	//Build tree
    	Tree<String> newTree = new Tree<String>(label);
    	Pair<Integer, Pair<String, String>> rule = back.get(getIntIntStrTriple(begin,end,label));
    	List<Tree<String>> children;
    	int newsplit=rule.getFirst();
    	if (newsplit > 0){
    		//binary rule
    		children = buildBinaryTreeRec(sentence,begin,end,newsplit,rule.getSecond().getFirst(),rule.getSecond().getSecond(),score,back);
    	} else {
    		//Unary rule
    		children = buildUnaryTreeRec(sentence,begin,end,rule.getSecond().getFirst(),score,back);
    	}
    	newTree.setChildren(children);
        return newTree.toSubTreeList();
    }
        		

    private Pair<Integer,Integer> getIntPair(int f, int s){
    	return new Pair<Integer,Integer>(f,s);
    }

    private Pair<String,String> getStringPair(String f, String s){
    	return new Pair<String,String>(f,s);
    }
    
    private Pair<Pair<Integer,Integer>,String> getIntIntStrTriple(int f, int s, String t){
    	return new Pair<Pair<Integer,Integer>,String>(getIntPair(f,s),t);
    }
    private Pair<Integer,Pair<String,String>> getIntStrStrTriple(int f, String s, String t){
    	return new Pair<Integer,Pair<String,String>>(f,getStringPair(s,t));
    }
}
