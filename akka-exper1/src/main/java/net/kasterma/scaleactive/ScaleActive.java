package net.kasterma.scaleactive;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Setup and kickoff the simulation of Persons sending Ping messages to a Site
 * and Logging the results of this.  The ChaosMonkey changes the active status
 * of random Persons, while the Scaler works at maintaining a constant fraction
 * of active Persons.
 */
@Slf4j
class ScaleActive {
    public static void main(String[] args) throws InterruptedException {
        log.info("Starting");
        ActorSystem system = ActorSystem.create("scaleactive");
        ActorRef scaler = system.actorOf(Scaler.props(), "scaler");
        ActorRef logger = system.actorOf(MessageLogger.props(), "logger");
        ActorRef site = system.actorOf(Site.props(logger), "site");

        List<ActorRef> persons = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            persons.add(system.actorOf(Person.props("person-" + i, site, scaler)));
        }
        scaler.tell(new Scaler.SetScale(0.5), ActorRef.noSender());
        system.actorOf(ChaosMonkey.props(persons), "monkey");
        Thread.sleep(60 * 1000);
        system.terminate();
        log.info("All terminated");
    }
}
