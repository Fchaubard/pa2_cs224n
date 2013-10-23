package cs224n.assignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cs224n.assignment.Grammar.BinaryRule;
import cs224n.assignment.Grammar.UnaryRule;
import cs224n.ling.Tree;
import cs224n.util.CollectionUtils;
import cs224n.util.Counter;

// Grammar ====================================================================

/**
 * Simple implementation of a PCFG grammar, offering the ability to
 * look up rules by their child symbols.  Rule probability estimates
 * are just relative frequency estimates off of training trees.
 */
public class VM1_Grammar extends Grammar{

	/* A builds PCFG using the observed counts of binary and unary
	 * productions in the training trees to estimate the probabilities
	 * for those rules.  */ 
	public VM1_Grammar(List<Tree<String>> trainTrees) {
		super();
		Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
		Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
		Counter<String> symbolCounter = new Counter<String>();
		
		for (Tree<String> trainTree : trainTrees) {

			tallyTree(trainTree, symbolCounter, unaryRuleCounter, binaryRuleCounter,"");
		}
		for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
			double unaryProbability = 
					unaryRuleCounter.getCount(unaryRule) / 
					symbolCounter.getCount(unaryRule.getParent());
			unaryRule.setScore(unaryProbability);
			addUnary(unaryRule);
		}
		for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
			double binaryProbability = 
					binaryRuleCounter.getCount(binaryRule) / 
					symbolCounter.getCount(binaryRule.getParent());
			binaryRule.setScore(binaryProbability);
			addBinary(binaryRule);
		}
	}
	private void addBinary(BinaryRule binaryRule) {
		CollectionUtils.addToValueList(binaryRulesByLeftChild, 
				binaryRule.getLeftChild(), binaryRule);
		CollectionUtils.addToValueList(binaryRulesByRightChild, 
				binaryRule.getRightChild(), binaryRule);
	}

	private void addUnary(UnaryRule unaryRule) {
		CollectionUtils.addToValueList(unaryRulesByChild, 
				unaryRule.getChild(), unaryRule);
	}


	private void tallyTree(Tree<String> tree, Counter<String> symbolCounter,
			Counter<UnaryRule> unaryRuleCounter, 
			Counter<BinaryRule> binaryRuleCounter, String parent) {
		if (tree.isLeaf()) return;
		if (tree.isPreTerminal()) return;
		// is not leaf or preterminal
		//String classLabel = tree.getLabel()+"^"+ tree.getPreOrderTraversal();
		
		if (tree.getChildren().size() == 1) {
			UnaryRule unaryRule = makeUnaryRule(tree,parent);
			symbolCounter.incrementCount(makeVerticalRuleLabel(tree,parent), 1.0);
			unaryRuleCounter.incrementCount(unaryRule, 1.0);
		}
		if (tree.getChildren().size() == 2) {
			BinaryRule binaryRule = makeBinaryRule(tree,parent);
			symbolCounter.incrementCount(makeVerticalRuleLabel(tree,parent), 1.0);
			binaryRuleCounter.incrementCount(binaryRule, 1.0);
		}
		if (tree.getChildren().size() < 1 || tree.getChildren().size() > 2) {
			throw new RuntimeException("Attempted to construct a Grammar with an illegal tree: "+tree);
		}
		for (Tree<String> child : tree.getChildren()) {
			tallyTree(child, symbolCounter, unaryRuleCounter,  binaryRuleCounter, tree.getLabel());
		}
	}

	private UnaryRule makeUnaryRule(Tree<String> tree, String parent) {
		return new UnaryRule(makeVerticalRuleLabel(tree,parent),makeVerticalRuleLabel(tree.getChildren().get(0),tree.getLabel()));
	}

	private BinaryRule makeBinaryRule(Tree<String> tree, String parent) {
		return new BinaryRule(makeVerticalRuleLabel(tree,parent), makeVerticalRuleLabel(tree.getChildren().get(0),tree.getLabel()), 
				makeVerticalRuleLabel(tree.getChildren().get(1),tree.getLabel()));
	}
	private String makeVerticalRuleLabel(Tree<String> tree, String parent){
		if (tree.getLabel().equals("ROOT") || tree.isPreTerminal() || tree.isLeaf()){
			return tree.getLabel();
		}else{
			return tree.getLabel()+"^"+parent;			
		}
		
	}
}
