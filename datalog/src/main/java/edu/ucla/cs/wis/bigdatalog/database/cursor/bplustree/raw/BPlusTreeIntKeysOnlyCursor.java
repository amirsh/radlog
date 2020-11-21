package edu.ucla.cs.wis.bigdatalog.database.cursor.bplustree.raw;

import edu.ucla.cs.wis.bigdatalog.database.store.bplustree.intkeysonly.BPlusTreeIntKeysOnly;
import edu.ucla.cs.wis.bigdatalog.database.store.bplustree.intkeysonly.BPlusTreeIntKeysOnlyLeaf;

public class BPlusTreeIntKeysOnlyCursor {

	public class Result {
		public int value;
		public Result(){}		
	}
	
	protected int						keyIndex;	
	protected BPlusTreeIntKeysOnlyLeaf currentLeaf;
	protected BPlusTreeIntKeysOnly 	tree;
	
	public BPlusTreeIntKeysOnlyCursor(BPlusTreeIntKeysOnly tree) {
		this.tree = tree;
		this.initialize();
	}
	
	public void initialize() {
		this.currentLeaf = this.tree.getFirstChild();
		this.keyIndex = 0;
	}
	
	public void reset() {
		this.initialize();
	}
	
	public int get(Result result) {
		// get leaf
		// get tuple address array
		// get all values from array
		// when out of values in array, get next array
		// when out of arrays, get next leaf
		// when out of leaves, done
		while (this.currentLeaf != null) {
			if (this.keyIndex < this.currentLeaf.getHighWaterMark()) {
				result.value = this.currentLeaf.getKeys()[this.keyIndex++];
				return 0;
			}
			
			// out of keys, so move to next leaf
			this.currentLeaf = this.currentLeaf.getNext();
			this.keyIndex = 0;
		}
		
		return -1;
	}	
}
