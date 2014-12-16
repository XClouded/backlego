package com.taobao.update;

public interface UpdateLister {
	void onNeedUpdate();
	void onNoUpdate();
	//更新的情况如isLephon就无法进行更新，必须到相应的商店去
	void onOtherCondition();

}
