package net.kasterma.scaleactive;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Person extends AbstractActor {
    /**
     * Current state of the person.
     */
    private boolean active = false;

    static Props props(ActorRef scaler) {
        return Props.create(Person.class, scaler);
    }

    Person(ActorRef scaler) {
        scaler.tell(new Scaler.AnnouncePerson(), getSelf());
    }

    /**
     * Message with request to see if we are active.
     */
    @Data
    @AllArgsConstructor
    static class RUActive {
        String runid;
    }

    /**
     * Reaction to message about if we are active.
     *
     * @param a RUActive message with runid for knowing which round this message
     *          is about
     */
    void ruActive(RUActive a) {
        if (active) {
            getSender().tell(new Scaler.Active(a.getRunid()), getSelf());
        }
    }

    /**
     * Message request to active.
     */
    static class Activate { }

    /**
     * Set this person to active.
     */
    void activate() {
        log.info("activate");
        active = true;
    }

    /**
     * Message request to deactivate.
     */
    static class Deactivate { }

    /**
     * Set this person to inactive.
     */
    void deActivate() {
        log.info("deactivate");
        active = false;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RUActive.class, this::ruActive)
                .match(Activate.class, m -> this.activate())
                .match(Deactivate.class, m -> this.deActivate())
                .build();
    }
}
