package cs224n.assignment;

import cs224n.ling.Tree;
import cs224n.util.CounterMap;

import java.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
    private Grammar grammar;
    private Lexicon lexicon;
    CounterMap<List<String>,Tree<String>> knownParses;
    CounterMap<Integer,String> spanToCategories;
    
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
        // TODO: implement this method
        return null;
    }
}
