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
	private Set<String> allTags;
	
	public void train(List<Tree<String>> trainTrees) {
		// TODO: before you generate your grammar, the training trees
		// need to be binarized so that rules are at most binary

		lexicon = new Lexicon(trainTrees);

		// binarize the train trees
		List<Tree<String>> binarizedTrainTrees = new ArrayList<Tree<String>>();
		for (Tree<String> trainTree : trainTrees) {
			binarizedTrainTrees.add(TreeAnnotations.annotateTree(trainTree));
		}

		//grammar = new Grammar(binarizedTrainTrees);
		grammar = new VM1_Grammar(binarizedTrainTrees);

		/*for (Tree<String> trainTree : trainTrees) {
            List<String> tags = trainTree.getPreTerminalYield();
            knownParses.incrementCount(tags, trainTree, 1.0);
            tallySpans(trainTree, 0);
        }*/
	}

	public Tree<String> getBestParse(List<String> sentence) {
		
		CounterMap<Pair<Integer,Integer>,String> score = new CounterMap<Pair<Integer,Integer>,String>();
		HashMap<Pair<Pair<Integer,Integer>,String>,Pair<Integer,Pair<String,String>>> back = new HashMap<Pair<Pair<Integer,Integer>,String>,Pair<Integer,Pair<String,String>>>();
		
		//Declare variables;
		Set<String> B_terms,new_B_terms,C_terms;
		double p;
		
		//Get all tags
		allTags = lexicon.getAllTags();
		
		//Get nonterminal counts
		for (int i=0; i < sentence.size(); i++){
			for (String tag : allTags){
				score.setCount(getIntPair(i,i),tag,lexicon.scoreTagging(sentence.get(i), tag));
			}
		}

		for (int i=0; i < sentence.size(); i++){
			//Handle unaries
			B_terms = new HashSet<String>(score.getCounter(getIntPair(i, i)).keySet());
			new_B_terms = new HashSet<String>();
			boolean added = true;
			while (added){
				B_terms.addAll(new_B_terms);
				new_B_terms.clear();
				added=false;
				for (String B : B_terms){
					List<UnaryRule> A_unaryParents = grammar.getUnaryRulesByChild(B);
					for (UnaryRule A : A_unaryParents){
						if (score.getCount(getIntPair(i,i),B) > 0) {
							p = A.getScore() * score.getCount(getIntPair(i,i), B);
							if (p > score.getCount(getIntPair(i,i), A.parent)){
								score.setCount(getIntPair(i,i),A.parent, p);
								back.put(getIntIntStrTriple(i, i, A.parent), getIntStrStrTriple(-1,B,null));	
								//System.out.printf("%d:%d:%s->%s\n", i,i,A.getParent(),B);
								added=true;
								new_B_terms.add(A.getParent());
							}
						}
					}
				}
			}
		}


		for (int span = 1; span < sentence.size(); span++) {
			for (int begin=0; begin < sentence.size()-span; begin++){
				int end=begin+span;	
				for (int split = begin; split < end; split++) {
					B_terms = score.getCounter(getIntPair(begin, split)).keySet();
					C_terms = score.getCounter(getIntPair(split+1, end)).keySet();
					for (String B : B_terms){
						for (BinaryRule rule : grammar.getBinaryRulesByLeftChild(B)){
							//System.out.printf("%d:%d:%s->%s:%s\n", begin,end,rule.getParent(),B,rule.getRightChild());
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
					B_terms = new HashSet<String>(score.getCounter(getIntPair(begin, end)).keySet());
					new_B_terms = new HashSet<String>();
					while (added){
						added=false;
						B_terms.addAll(new_B_terms);
						new_B_terms.clear();
						for (String B : B_terms){
							List<UnaryRule> A_unaryParents = grammar.getUnaryRulesByChild(B);
							for (UnaryRule A : A_unaryParents){
								if (score.getCount(getIntPair(begin,end),B) > 0) {
									p = A.getScore() * score.getCount(getIntPair(begin,end), B);
									if (p > score.getCount(getIntPair(begin,end), A.parent)){
										score.setCount(getIntPair(begin,end),A.parent, p);
										back.put(getIntIntStrTriple(begin, end, A.parent), getIntStrStrTriple(-1,B,null));	
										added=true;
										//Add the new unary parent to the list of terms we are checking
										new_B_terms.add(A.getParent());
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
		Tree<String> root = buildTreeRec(sentence,0,sentence.size()-1,"ROOT",score,back);		
		return TreeAnnotations.unAnnotateTree(root);
	}

	private Tree<String> buildTreeRec(List<String> sentence,int begin, int end, String label, CounterMap<Pair<Integer,Integer>,String> score, Map<Pair<Pair<Integer,Integer>,String>,Pair<Integer,Pair<String,String>>> back) {
		Tree<String> tree = new Tree<String>(label);
		//If at a leaf
		if (begin==end && allTags.contains(label)){
			Tree<String> leaf = new Tree<String>(sentence.get(begin));
			tree.setChildren(Collections.singletonList(leaf));
			return tree;
		}
		Pair<Integer, Pair<String, String>> rule = back.get(getIntIntStrTriple(begin,end,label));
		//System.out.printf("%d:%d:%s\n", begin,end,label);
		int split=rule.getFirst();
		if (split >= 0){
			List<Tree<String>> children = new ArrayList<Tree<String>>();
			//binary rule
			Tree<String> leftTree = buildTreeRec(sentence,begin,split,rule.getSecond().getFirst(),score,back);
			Tree<String> rightTree = buildTreeRec(sentence,split+1,end,rule.getSecond().getSecond(),score,back);
			children.add(leftTree);
			children.add(rightTree);
			tree.setChildren(children);
			return tree;
		} else {
			//Unary rule
			Tree<String> subTree = buildTreeRec(sentence,begin,end,rule.getSecond().getFirst(),score,back);
			tree.setChildren(Collections.singletonList(subTree));
			return tree;
		}
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
