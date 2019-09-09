package org.apache.zookeeper.server.quorum.flexible;

import java.util.HashSet;

/**
 * @author artsing
 */
public class QuorumDynamic implements QuorumVerifier{

    private int half;
    private int followerCount;
    private int count;

    public QuorumDynamic(int n) {
        this.half = n/2;
        this.followerCount = n;
        this.count = n;
    }

    @Override
    public long getWeight(long id) {
        return (long)1;
    }

    @Override
    public boolean containsQuorum(HashSet<Long> set) {
        if (count <= 2) {
            if (followerCount > 0) {
                return set.size() >= followerCount;
            } else {
                return set.size() > 0;
            }
        } else {
            return set.size() > half;
        }
    }

    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    public void reset() {
        followerCount = count;
    }

    public int getCount() {
        return count;
    }
}
