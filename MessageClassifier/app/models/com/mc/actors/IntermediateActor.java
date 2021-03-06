/**
 * 
 */
package models.com.mc.actors;

import java.util.concurrent.TimeoutException;

import mc.messages.TextMessage;
import models.com.mc.configs.ClassifiersConfig;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;


/**
 * Intermediate Actor for taking responsibility of consuming each single message
 * @deprecated Used WorkExecutor Instead
 */
public final class IntermediateActor extends UntypedActor {

	ActorRef broadcastingActor;
	ActorSelection messageCollectingActor;

	@Override
	public void preStart() throws Exception {

	// Set Broadcasting Actor to the context of IntermediateActor
	broadcastingActor = getContext().actorOf(Props.create(BroadcastingActor.class), "broadcastingActor");
	//get the message collecting actor
	messageCollectingActor = getContext().actorSelection("/user/mcActor*");
	}

	@Override
	public void onReceive(Object arg0) throws Exception {

		if(arg0 instanceof TextMessage)
		{

			try {				
			
			Timeout timeout = new Timeout(Duration.create(ClassifiersConfig.CLASSIFIER_SERVICE_TIMEOUT, "seconds"));
			Future<Object> future = Patterns.ask(broadcastingActor, arg0, timeout);
			TextMessage result = (TextMessage) Await.result(future, timeout.duration());
			System.out.println("RETURNED: "+result.getMessage());
			// Send to message store
			messageCollectingActor.tell(result, getSelf());

			} catch (TimeoutException te) {
				// TODO: handle exception
				System.out.println("Classifier Service Unavailable");
				
				throw te;
			}

		}

	}
	
	/**
	 * Test main - SAMPLE LOGIC FOR MASTER ACTOR
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			final ActorSystem system = ActorSystem.create("helloakka");

	        // Create the 'IntermediateActor' actor, *need to specify unique names
	        final ActorRef iActor = system.actorOf(Props.create(IntermediateActor.class), "iActor");
	        
	        iActor.tell(new TextMessage("Hello"), ActorRef.noSender());
	        
	        final ActorRef iActor2 = system.actorOf(Props.create(IntermediateActor.class), "iActor2");
	        
	        iActor2.tell(new TextMessage("World"), ActorRef.noSender());
	        
	        iActor.tell(new TextMessage("Akka"), ActorRef.noSender());
	        
	        final ActorRef mcActor = system.actorOf(Props.create(MessageCollectingActor.class), "mcActor");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
