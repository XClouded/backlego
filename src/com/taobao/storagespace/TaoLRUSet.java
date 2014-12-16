package com.taobao.storagespace;

import java.util.Collection;
import java.util.LinkedHashSet;


public class TaoLRUSet<E> extends LinkedHashSet<E> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8210739201409145322L;

	@Override
    public boolean add(E object) {
        if(contains(object)){
            remove(object);
        }
        return super.add(object);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        removeAll(collection);
        return super.addAll(collection);
    }

    
}
