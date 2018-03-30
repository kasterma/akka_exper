package net.kasterma.scaleactive;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
final class Scaler extends AbstractActorWithTimers {
    /**
     * Desired scale to maintain.
     *
     * We aim to have Math.round(scale * persons.size()) actors active at any
     * one time.
     */
    private double scale = 1;
    /**
     * List of known person actors.
     */
    private List<ActorRef> persons = new ArrayList<>();
    /**
     * Id for the current run of collecting active information.  Needed to
     * detect stragglers in the messages received.
     */
    private String runid = null;
    /**
     * List of Persons that have announced they are active in the current
     * round.  Reset every round.
     */
    private List<ActorRef> active;

    static Props props() {
        return Props.create(Scaler.class);
    }

    Scaler() {
        getTimers().startPeriodicTimer("scaletimer",
                new CheckScale(),
                new FiniteDuration(3, TimeUnit.SECONDS));
    }

    /**
     * Message to set or update the scale this scaler is attempting to maintain.
     */
    @Data
    @AllArgsConstructor
    static class SetScale {
        final double scale;
    }

    /**
     * Store the scale that we were messaged to maintain.
     *
     * @param s SetScale message received.
     */
    private void setscale(SetScale s) {
        this.scale = s.getScale();
    }

    /**
     * Message send by a new person so the Scaler knows all persons present.
     */
    static class AnnouncePerson { }

    /**
     * Register a person that has announced themselves.
     */
    private void announcePerson() {
        persons.add(getSender());
    }

    /**
     * Message to indicate it is time to check the scale with reality.
     */
    private static class CheckScale { }

    /**
     *
     */
    private void checkScale() {
        runid = UUID.randomUUID().toString();
        active = new ArrayList<>();
        persons.forEach(p -> p.tell(new Person.RUActive(runid), getSelf()));
        getTimers().startSingleTimer("activeWait", new CheckScaleDone(), FiniteDuration.create(1, TimeUnit.SECONDS));
    }

    /**
     * Message used by a person to indicate they are active.
     */
    @Data
    @AllArgsConstructor
    static class Active {
        String runid;
    }

    /**
     * Handle a message from a person that indicates they are active.
     *
     * @param a the active message containing the runid from the checkScale run
     */
    private void active(Active a) {
        if (runid == a.getRunid()) {
            active.add(getSender());
        }
    }

    /**
     * Message to indicate time interval to hear from Persons announcing they
     * are active is over.
     */
    private static class CheckScaleDone { }

    /**
     * This function is called when all active actors should have reported to
     * get reality inline with the desired scale.
     */
    private void checkScaleDone() {
        long activeCt = active.size();
        long goalCt = Math.round(scale *  persons.size());
        if (activeCt < goalCt) {
            log.info("Activate {}", goalCt - activeCt);
            List<ActorRef> candidate = persons.stream()
                    .filter(p -> !active.contains(p))
                    .collect(Collectors.toList());
            for (int i = 0; i < goalCt - activeCt; i++) {
                int idx = new Random().nextInt(candidate.size());
                candidate.get(idx).tell(new Person.Activate(), getSelf());
                candidate.remove(idx);
            }
        } else if (activeCt > goalCt) {
            log.info("Deactivate {}", activeCt - goalCt);
            for (int i = 0; i < goalCt - activeCt; i++) {
                int idx = new Random().nextInt(active.size());
                active.get(idx).tell(new Person.Activate(), getSelf());
                active.remove(idx);
            }
        } else {
            log.info("Active count is good");
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SetScale.class, this::setscale)
                .match(AnnouncePerson.class, m -> announcePerson())
                .match(CheckScale.class, m -> this.checkScale())
                .match(CheckScaleDone.class, m -> this.checkScaleDone())
                .match(Active.class, this::active)
                .build();
    }
}
