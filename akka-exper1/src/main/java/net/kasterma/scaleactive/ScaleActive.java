package net.kasterma.scaleactive;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ScaleActive {
    public static void main(String[] args) throws InterruptedException {
        log.info("Starting");
        ActorSystem system = ActorSystem.create("scaleactive");
        ActorRef scaler = system.actorOf(Scaler.props(), "scaler");

        List<ActorRef> persons = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            persons.add(system.actorOf(Person.props(scaler)));
        }
        scaler.tell(new Scaler.SetScale(0.5), ActorRef.noSender());
        system.actorOf(ChaosMonkey.props(persons), "monkey");
        Thread.sleep(20 * 1000);
        system.terminate();
    }
}
