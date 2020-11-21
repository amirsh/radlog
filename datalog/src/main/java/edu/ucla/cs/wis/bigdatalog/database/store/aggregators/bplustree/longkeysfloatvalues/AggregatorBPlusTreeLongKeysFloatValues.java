package edu.ucla.cs.wis.bigdatalog.database.store.aggregators.bplustree.longkeysfloatvalues;

import java.io.Serializable;

import edu.ucla.cs.wis.bigdatalog.database.Tuple;
import edu.ucla.cs.wis.bigdatalog.database.store.aggregators.AggregatorInsertStatus;
import edu.ucla.cs.wis.bigdatalog.database.store.aggregators.AggregatorResult;
import edu.ucla.cs.wis.bigdatalog.database.store.aggregators.bplustree.AggregatorBPlusTreeStoreStructure;
import edu.ucla.cs.wis.bigdatalog.database.store.bplustree.rangesearch.RangeSearchResult;
import edu.ucla.cs.wis.bigdatalog.database.store.bplustree.rangesearch.RangeSearchableStorageStructure;
import edu.ucla.cs.wis.bigdatalog.database.store.changetracking.ChangeTrackingStore;
import edu.ucla.cs.wis.bigdatalog.database.type.DbDouble;
import edu.ucla.cs.wis.bigdatalog.database.type.DbInteger;
import edu.ucla.cs.wis.bigdatalog.database.type.DbLong;
import edu.ucla.cs.wis.bigdatalog.database.type.DbString;
import edu.ucla.cs.wis.bigdatalog.database.type.DbTypeBase;
import edu.ucla.cs.wis.bigdatalog.database.type.TypeManager;
import edu.ucla.cs.wis.bigdatalog.interpreter.AggregateInfo;
import edu.ucla.cs.wis.bigdatalog.type.DataType;

// THIS CLASS SHOULD ONLY BE USED WHEN KEYS ARE UNIQUE
public class AggregatorBPlusTreeLongKeysFloatValues 
	extends AggregatorBPlusTreeStoreStructure<AggregatorBPlusTreeLongKeysFloatValuesPage, 
		AggregatorBPlusTreeLongKeysFloatValuesLeaf, AggregatorBPlusTreeLongKeysFloatValuesNode> 
	implements ChangeTrackingStore<Long>, RangeSearchableStorageStructure<Long>, Serializable {
	private static final long serialVersionUID = 1L;

	protected AggregatorBPlusTreeLongKeysFloatValuesInsertResult insertResult;
	protected AggregatorBPlusTreeLongKeysFloatValuesGetResult getResult;
	//protected DoubleAggregatorHelper aggregator;
	protected AggregatorBPlusTreeLongKeysFloatValuesGetResultRange getResultRange;

	public AggregatorBPlusTreeLongKeysFloatValues() { super(); }
	
	public AggregatorBPlusTreeLongKeysFloatValues(int nodeSize, int[] keyColumns, DataType[] keyColumnTypes, 
			int[] valueColumns, DataType[] valueColumnTypes, AggregateInfo[] aggregateInfos, TypeManager typeManager) {
		super(nodeSize, 8, keyColumns, keyColumnTypes, valueColumns, valueColumnTypes, aggregateInfos, typeManager);
		
		this.insertResult = new AggregatorBPlusTreeLongKeysFloatValuesInsertResult();
		this.getResult = new AggregatorBPlusTreeLongKeysFloatValuesGetResult();
		this.getResultRange = new AggregatorBPlusTreeLongKeysFloatValuesGetResultRange();
		//this.aggregator = DoubleAggregatorHelper.getAggregatorHelper(aggregateInfos[0].aggregateType, valueColumnTypes[0]);
		this.initialize();
	}

	@Override
	protected AggregatorBPlusTreeLongKeysFloatValuesPage allocatePage() {
		if (this.rootNode == null)
			return new AggregatorBPlusTreeLongKeysFloatValuesLeaf(this.nodeSize, this.aggregateInfos[0].aggregateType);
		
		return new AggregatorBPlusTreeLongKeysFloatValuesNode(this.nodeSize, this.aggregateInfos[0].aggregateType);
	}
	
	@Override
	public void insert(Tuple tuple, AggregatorResult result) {
		long key = this.getKeyL(tuple.columns); 

		// case 1 - room to insert in root node
		this.rootNode.insert(key, ((DbDouble)tuple.columns[this.valueColumns[0]]).getValue(), 
				this.insertResult);
		result.status = this.insertResult.status;
		
		if (this.insertResult.status == AggregatorInsertStatus.NEW) {
			this.numberOfEntries++;
			if (this.changeTracker != null)
				this.changeTracker.add(key);
		} else if (this.insertResult.status == AggregatorInsertStatus.UPDATE) {
			if (this.changeTracker != null)
				this.changeTracker.add(key);
		}
				
		if (this.insertResult.newPage == null)
			return;

		// case 2 we have grow the tree as the root node was split 
		AggregatorBPlusTreeLongKeysFloatValuesNode newRoot = (AggregatorBPlusTreeLongKeysFloatValuesNode)this.allocatePage();
		newRoot.children[0] = this.rootNode;
		newRoot.children[1] = this.insertResult.newPage;
		newRoot.keys[0] = newRoot.children[1].getLeftMostLeafKey();
				
		newRoot.highWaterMark += 2;
		this.rootNode = newRoot;
	}
	
	public int getTuple(DbTypeBase[] keyColumns, Tuple tuple) {
		this.rootNode.get(this.getKeyL(keyColumns), this.getResult);
		if (this.getResult.success) {		
			if (this.keyColumns.length == 1) {
				tuple.columns[this.keyColumns[0]] = keyColumns[0];
			} else {
				tuple.columns[this.keyColumns[0]] = keyColumns[0];
				tuple.columns[this.keyColumns[1]] = keyColumns[1];
			}
			tuple.columns[this.valueColumns[0]] = DbDouble.create(this.getResult.value);
			return 1;
		}
		return 0;
	}
	
	@Override
	public int getTuple(Long key, Tuple tuple) {
		this.rootNode.get(key, this.getResult);
		if (this.getResult.success)
			return this.loadTuple(key, this.getResult.value, tuple);

		return 0;
	}
	
	@Override
	public void getTuple(Long startKey, Long endKey, RangeSearchResult result) {
		this.rootNode.get(startKey, endKey, this.getResultRange);
		
		result.success = this.getResultRange.success;
		if (this.getResultRange.success) {
			result.index = this.getResultRange.index;
			result.leaf = this.getResultRange.leaf;
			result.success = true;
			return;
		}
		result.success = false;
	}
	
	public int loadTuple(long key, double value, Tuple tuple) {
		if (this.keyColumns.length == 1) {
			tuple.columns[this.keyColumns[0]] = DbLong.create(key);
		} else {
			if (this.keyColumnTypes[0] == DataType.INT)
				tuple.columns[this.keyColumns[0]] = DbInteger.create((int)(key >> 32));
			else
				tuple.columns[this.keyColumns[0]] = DbString.load((int)(key >> 32), this.typeManager);
			
			if (this.keyColumnTypes[1] == DataType.INT)
				tuple.columns[this.keyColumns[1]] = DbInteger.create((int)key);
			else
				tuple.columns[this.keyColumns[1]] = DbString.load((int)key, this.typeManager);
		}
		
		tuple.columns[this.valueColumns[0]] = DbDouble.create(value);
		return 1;
	}

	@Override
	public boolean delete(Tuple tuple) {	
		boolean status = this.rootNode.delete(this.getKeyL(tuple.columns));
		if (status) {
			this.numberOfEntries--;
			if (this.numberOfEntries == 0) {
				this.rootNode = null;
				this.rootNode = this.allocatePage();
			}
		}
		
		return status;
	}
}