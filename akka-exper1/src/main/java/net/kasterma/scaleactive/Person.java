package net.kasterma.scaleactive;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Person actor.
 *
 * Sets up an entity (the Person) that can be active or not.  When active send
 * a ping to site every so many seconds (see timer set in activate).  It
 * reacts to RUActive, Activate, and Deactivate messages from the Scaler.
 */
@Slf4j
final class Person extends AbstractActorWithTimers {
    /**
     * id for communicating in human understandable method which person this is.
     */
    private final String id;

    /**
     * Current state of the person.
     */
    private boolean active = false;

    /**
     * Site this Person is action on.
     */
    private final ActorRef site;

    static Props props(String id, ActorRef site, ActorRef scaler) {
        return Props.create(Person.class, id, site, scaler);
    }

    Person(String id, ActorRef site, ActorRef scaler) {
        this.id = id;
        this.site = site;
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
    private void ruActive(RUActive a) {
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
    private void activate() {
        log.info("activate");
        getTimers().startPeriodicTimer("timer-" + id,new Act(),
                FiniteDuration.apply(3, TimeUnit.SECONDS));
        active = true;
    }

    /**
     * Message request to deactivate.
     */
    static class Deactivate { }

    /**
     * Set this person to inactive.
     */
    private void deActivate() {
        log.info("deactivate");
        active = false;
        getTimers().cancel("timer-" + id);
    }

    /**
     * Timer generated message to act.
     */
    private static class Act { }

    /**
     * The Person acts; i.e. sends a ping to the site.
     */
    private void act() {
        site.tell(new Site.Ping(id), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RUActive.class, this::ruActive)
                .match(Activate.class, m -> this.activate())
                .match(Deactivate.class, m -> this.deActivate())
                .match(Act.class, m -> this.act())
                .build();
    }
}
