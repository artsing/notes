package com.demo.cluster;

import java.util.ArrayList;
import java.util.List;

/**
 * Auxiliary cache to handle changes to the lists of tasks and of workers.
 * @author artsing
 */
class ChildrenCache {

    private List<String> children;

    ChildrenCache() { }
    
    ChildrenCache(List<String> children) {
        this.children = children;        
    }
        
    List<String> getList() {
        return children;
    }
        
    List<String> addedAndSet(List<String> newChildren) {
        List<String> diff;
        
        if (children == null) {
            diff = new ArrayList<String>(newChildren);
        } else {
            diff =diff(newChildren);
        }
        this.children = newChildren;
            
        return diff;
    }
        
    List<String> removedAndSet(List<String> newChildren) {
        List<String> diff;

        if (children == null) {
            diff = null;
        } else {
            diff = diff(newChildren);
        }

        this.children = newChildren;
        
        return diff;
    }

    private List<String> diff(List<String> newChildren) {
        List<String> diff = null;
        for(String s: children) {
            if(!newChildren.contains( s )) {
                if(diff == null) {
                    diff = new ArrayList<String>();
                }

                diff.add(s);
            }
        }
        return diff;
    }
}
