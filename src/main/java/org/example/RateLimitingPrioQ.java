package org.example;

import java.util.*;

import com.google.common.util.concurrent.RateLimiter;

public class RateLimitingPrioQ implements Comparator<User> {

    RateLimiter rateLimiter;
    private Map<Integer, Integer> requestCounterMap; // Map<user.UID, RequestCounter>
    private Queue<User> prioQ;
    private static int REQUEST_COUNT_LIMIT = 5;
    private final static int HIGH_PRIO = 100;
    private final static int LOW_PRIO = 20;

    public RateLimitingPrioQ(int permitsPerTimeUnit, int requestCountLimit){
        this.rateLimiter = RateLimiter.create(permitsPerTimeUnit);
        this.requestCounterMap = new HashMap<>();
        this.prioQ = new PriorityQueue<>(this::compare);
        REQUEST_COUNT_LIMIT = requestCountLimit;

    }


    @Override
    public int compare(User user1, User user2) { //TODO: revisit and complete the logic
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
            if (!this.requestCounterMap.containsKey(uid)) { // new user
                this.requestCounterMap.put(uid, 1);
                this.prioQ.add(user);
            } else {
                Integer newCount = requestCounterMap.get(uid); // existing user
                if (newCount > REQUEST_COUNT_LIMIT) {
                    user.setPriority(LOW_PRIO);
                }
                this.prioQ.add(user); // add new request of known user to prioQ
                ++newCount;
                this.requestCounterMap.replace(uid, newCount); // update request counter
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
        User user = this.prioQ.poll(); // retrieve user who's first in Q
        if (user == null) {
            return null;
        }
        Integer uid = user.getUID();
        if (this.requestCounterMap.containsKey(uid)) { // if user is known, decrement request counter
            Integer newCount = requestCounterMap.get(uid);
            this.requestCounterMap.replace(uid, --newCount);
        }
        return user;
    }

    public void applyRateLimiting(User user){
        if (user != null) {
            if (user.getPriority() == HIGH_PRIO) {
                rateLimiter.acquire();
            }
        }
    }


}
