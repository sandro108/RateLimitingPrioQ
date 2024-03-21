package org.example;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    private int UID;
    private LocalDateTime arrivalTime;

    private int priority;

    public User(int UID) {
        this.arrivalTime = LocalDateTime.now(); // this is used as default metric for PrioQ
        this.UID = UID;  // only needed for mapping user to request counter!
        this.priority = 100; // TODO: magic number  //this is used as metric for PrioQ, if request Counter is exceeded!

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return getUID() == user.getUID() && Objects.equals(getArrivalTime(), user.getArrivalTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUID(), getArrivalTime());
    }

    @Override
    public String toString() {
        return "User{" +
                "UID=" + UID +
                ", arrivalTime=" + arrivalTime +
                ", priority=" + priority +
                '}';
    }

    public int getUID() {
        return UID;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }


}
