package net.kasterma.scaleactive;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ChaosMonkey extends AbstractActorWithTimers {
    private List<ActorRef> persons;

    static Props props(List<ActorRef> persons) {
        return Props.create(ChaosMonkey.class, persons);
    }

    ChaosMonkey(List<ActorRef> persons) {
        this.persons = persons;
        getTimers().startPeriodicTimer("chaos",
                new Chaos(),
                FiniteDuration.apply(100, TimeUnit.MILLISECONDS));
    }

    static class Chaos { }

    void chaos() {
        ActorRef victim = persons.get(new Random().nextInt(persons.size()));
        if (new Random().nextFloat() < 0.3) {
            victim.tell(new Person.Activate(), getSelf());
        } else {
            victim.tell(new Person.Deactivate(), getSelf());
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Chaos.class, m -> chaos())
                .build();
    }
}
