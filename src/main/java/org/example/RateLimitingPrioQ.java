package org.example;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.RateLimiter;

public class RateLimitingPrioQ implements Comparator<User> {

    RateLimiter rateLimiter;
    private Map<Integer, Integer> counterMap; // Map<user.UID, RequestCounter>
    private Queue<User> prioQ;
    private static int REQUEST_COUNT_LIMIT = 5;
    private final static int HIGH_PRIO = 100;
    private final static int LOW_PRIO = 20;

    public RateLimitingPrioQ(int permitsPerTimeUnit, int requestCountLimit){
        this.rateLimiter = RateLimiter.create(permitsPerTimeUnit);
        this.counterMap = new HashMap<>();
        this.prioQ = new PriorityQueue<>(this::compare);
        REQUEST_COUNT_LIMIT = requestCountLimit;

    }


    @Override
    public int compare(User user1, User user2) {
        if (user1.getPriority() == user2.getPriority()){
            return user1.getArrivalTime().compareTo(user2.getArrivalTime());
        } else if (user1.getPriority() < user2.getPriority()) {
            return -1;
        } else {
            return 1;
        }

    }

    public boolean enqueUserRequest(User user){
        if (user != null) {
            Integer uid = user.getUID();
            if (!this.counterMap.containsKey(uid)) {
                this.counterMap.put(uid, 1);
                this.prioQ.add(user);
            } else {
                Integer newCount = counterMap.get(uid);
                if (newCount > REQUEST_COUNT_LIMIT) {
                    user.setPriority(LOW_PRIO);
                }
                this.prioQ.add(user);
                ++newCount;
                this.counterMap.replace(uid, newCount);
            }
            return true;
        }
        return false;
    }

    /**
     *
     * @return if not null: the dequed user (request) , null otherwise
     */
    public User dequeUserRequest(){
        User user = this.prioQ.remove();
        if (user != null) {
            Integer uid = user.getUID();
            if (this.counterMap.containsKey(uid)) {
                Integer newCount = counterMap.get(uid);
                this.counterMap.replace(uid, --newCount);
            }
            return user;
        }
        return null;
    }
}
