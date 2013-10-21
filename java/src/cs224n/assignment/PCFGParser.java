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
				back.put(getIntIntStrTriple(i, i, tag), getIntStrStrTriple(-1,sentence.get(i),null));	
				System.out.printf("Nonterminal %d:%s:%s\n",i,tag,sentence.get(i));
			}
		}

		double p;
		for (int i=0; i < sentence.size(); i++){
			//Handle unaries
			boolean added = true;
			while (added){
				//System.out.printf("Unary\nCheck %d:%d\n",i,i);
				added=false;
				Set<String> B_terms = score.getCounter(getIntPair(i, i)).keySet();
				for (String B : B_terms){
					List<UnaryRule> A_unaryParents = grammar.getUnaryRulesByChild(B);
					for (UnaryRule A : A_unaryParents){
						//System.out.printf("Check %s-->%s\n",A.getParent(),B);
						if (score.getCount(getIntPair(i,i),B) > 0) {
							p = A.getScore() * score.getCount(getIntPair(i,i), B);
							if (p > score.getCount(getIntPair(i,i), A.parent)){
								score.setCount(getIntPair(i,i),A.parent, p);
								back.put(getIntIntStrTriple(i, i, A.parent), getIntStrStrTriple(-1,B,null));	
								System.out.printf("Back %s-->%s\n",A.getParent(),B);
								added=true;
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
					//System.out.printf("Check %d:%d:%d\n",begin,split,end);
					ArrayList<String> B_terms = new ArrayList<String>(score.getCounter(getIntPair(begin, split)).keySet());
					ArrayList<String> C_terms = new ArrayList<String>(score.getCounter(getIntPair(split+1, end)).keySet());
					for (String B : B_terms){
						for (BinaryRule rule : grammar.getBinaryRulesByLeftChild(B)){
							//System.out.printf("Check %s-->%s:%s\n",rule.getParent(),B,rule.getRightChild());
							if (C_terms.contains(rule.getRightChild())){
								String C = rule.getRightChild();
								String A = rule.getParent();
								//System.out.printf("Check %d:%d:%d:%s->%s:%s\n",begin,split,end,A,B,C);
								p=score.getCount(getIntPair(begin,split), B)*
										score.getCount(getIntPair(split+1,end), C)*
										rule.getScore();
								if (p > score.getCount(getIntPair(begin,end), A)){
									score.setCount(getIntPair(begin,end), A,p);
									back.put(getIntIntStrTriple(begin, end, A), getIntStrStrTriple(split, B, C));
									System.out.printf("Back %d:%d:%d:%s->%s:%s\n",begin,split,end,A,B,C);
								}
							}
						}
					}
					//Handle unaries
					boolean added = true;
					while (added){
						//System.out.printf("Unary\nCheck %d:%d\n",begin,end);
						added=false;
						B_terms = new ArrayList<String>(score.getCounter(getIntPair(begin, end)).keySet());
						for (String B : B_terms){
							//System.out.printf("Check %s\n",B);
							List<UnaryRule> A_unaryParents = grammar.getUnaryRulesByChild(B);
							for (UnaryRule A : A_unaryParents){
								//System.out.printf("Check %s-->%s\n",A.getParent(),B);
								if (score.getCount(getIntPair(begin,end),B) > 0) {
									p = A.getScore() * score.getCount(getIntPair(begin,end), B);
									if (p > score.getCount(getIntPair(begin,end), A.parent)){
										score.setCount(getIntPair(begin,end),A.parent, p);
										back.put(getIntIntStrTriple(begin, end, A.parent), getIntStrStrTriple(-1,B,null));	
										System.out.printf("Back %d:%d:%s->%s\n",begin,end,A.getParent(),B);
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
		Pair<Integer, Pair<String, String>> rule = back.get(getIntIntStrTriple(0,sentence.size()-1,"S"));
		List<Tree<String>> children;
		int split=rule.getFirst();
		if (split >= 0){
			//binary rule
			children = buildBinaryTreeRec(sentence,0,sentence.size()-1,split,rule.getSecond().getFirst(),rule.getSecond().getSecond(),score,back);
		} else {
			//Unary rule
			children = buildUnaryTreeRec(sentence,0,sentence.size()-1,rule.getSecond().getFirst(),score,back);
		}
		topS.setChildren(children);
		return root;
	}

	//TODO FIX THIS
	private List<Tree<String>> buildBinaryTreeRec(List<String> sentence,int begin, int end,int split, String leftLabel, String rightLabel, CounterMap<Pair<Integer,Integer>,String> score, Map<Pair<Pair<Integer,Integer>,String>,Pair<Integer,Pair<String,String>>> back) {
		System.out.printf("Found rule for span %d:%d ->%s:%s\n",begin,end,leftLabel,rightLabel);
		//Build left tree
		System.out.printf("Build left tree span %d:%d\n",begin,split);
		Tree<String> leftTree = new Tree<String>(leftLabel);
		Pair<Integer, Pair<String, String>> rule = back.get(getIntIntStrTriple(begin,split,leftLabel));
		List<Tree<String>> leftchildren;
		int newsplit;
		//If at a leaf
		if (begin==split && lexicon.isKnown(leftLabel)){
			leftchildren = leftTree.toSubTreeList();
		} else if ((newsplit=rule.getFirst()) >= 0){
			//binary rule
			leftchildren = buildBinaryTreeRec(sentence,begin,split,newsplit,rule.getSecond().getFirst(),rule.getSecond().getSecond(),score,back);
		} else {
			//Unary rule
			leftchildren = buildUnaryTreeRec(sentence,begin,split,rule.getSecond().getFirst(),score,back);
		}
		leftTree.setChildren(leftchildren);

		//Build right tree
		System.out.printf("Build right tree span %d:%d\n",split+1,end);
		Tree<String> rightTree = new Tree<String>(rightLabel);
		rule = back.get(getIntIntStrTriple(split+1,end,rightLabel));
		List<Tree<String>> rightchildren;
		//If at a leaf
		if (begin==split && lexicon.isKnown(rightLabel)){
			rightchildren = rightTree.toSubTreeList();
		} else if ((newsplit=rule.getFirst()) >= 0){
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
		System.out.printf("Found rule for span %d:%d ->%s\n",begin,end,label);
		//If at a leaf
		if (begin==end && lexicon.isKnown(label)){
			return new Tree<String>(label).toSubTreeList();
		}
		//Build tree
		System.out.printf("Build unary tree span %d:%d\n",begin,end);
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
