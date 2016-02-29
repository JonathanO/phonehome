package net.woaf.jono.phonehome

import akka.actor.{ Props, Actor }
import com.firebase.client.{FirebaseError, AuthData, DataSnapshot, ValueEventListener, Firebase, Logger}
import com.firebase.client.Firebase.{ AuthResultHandler, CompletionListener }
import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable

/**
 * helper class for initializing a NestActor with appropriate args
 */
object NestActor {
  def props(nestToken: String, firebaseURL: String): Props = Props(new NestActor(nestToken, firebaseURL))
}

class NestActor(nestToken: String, firebaseURL: String) extends Actor with LazyLogging {
  Firebase.setDefaultConfig(
    {
      val config = new com.firebase.client.Config()
      config.setLogLevel(Logger.Level.DEBUG)
      config.setLogger(new FirebaseLogger)
      config
    }
  )
  private val fb = new Firebase(firebaseURL)

  // maintain maps of the current state of things so we can trigger on edge changes
  private val structureStates = mutable.HashMap[String, String]()
  private val structMap = mutable.HashMap[String, String]()

  // authenticate with our current credentials
  fb.authWithCustomToken(nestToken, new AuthResultHandler {
    override def onAuthenticated(authData: AuthData): Unit = {
      logger.info("fb auth success: " + authData)
      // when we've successfully authed, add a change listener to the whole tree
      fb.addValueEventListener(new ValueEventListener {
        def onDataChange(snapshot: DataSnapshot) {
          // when data changes we send our receive block an update
          self ! snapshot
        }

        def onCancelled(err: FirebaseError) {
          // on an err we should just bail out
          self ! err
        }
      })
    }
    override def onAuthenticationError(firebaseError: FirebaseError): Unit = {
      logger.error("fb auth error: " + firebaseError)
    }

  })

  private def handleSnapshot(s: DataSnapshot): Unit = {
    try {
      import scala.collection.JavaConversions._ // scalastyle:ignore
      logger.info("got firebase snapshot " + s)
      // this looks scary, but because processing is single threaded here we're ok
      structMap.clear()
      // process structure specific data
      val structures = s.child("structures")
      if (structures.hasChildren) {
        structures.getChildren.foreach { struct =>
          // update our map of struct ids -> struct names for lookup later
          val structName = struct.child("name").getValue.toString
          structMap += (struct.getName -> structName)
          // now compare states and send an update if they changed
          val structState = struct.child("away").getValue.toString
          val oldState = structureStates.getOrElse(structName, "n/a")
          structureStates += (structName -> structState)
          if (oldState != structState) {
            context.parent ! StructureUpdate(structName, structState)
            logger.info(s"Structure $structName (${struct.getName}) is in away state $structState")
          }
        }
      } else {
        logger.warn("no structures? children=" + s.getChildren.map(_.getName).mkString(", "))
      }
    } catch {
      case e: Exception =>
        logger.error("uhoh " + e, e)
    }
  }

  override def receive: Actor.Receive = {
    case a: AwayState =>
      structMap.keys.toList match {
        case structId :: xs =>
          val structName = structMap(structId)
          val awayRef = fb.child("structures").child(structId).child("away")
          val newState = if (a.away) "away" else "home"
          if (structureStates(structName) != newState) {
            logger.info(s"setting away state to $newState on $structName ($structId)")
            awayRef.setValue(newState, new CompletionListener {
              override def onComplete(err: FirebaseError, fb: Firebase) = {
                if (err != null) { // scalastyle:ignore
                  logger.error(s"completed with err=${err.getCode}-${err.getMessage}, fb=$fb")
                }
              }
            })
          } else {
            logger.info(s"Was told to change state to $newState but $structName ($structId) is already in it")
          }
        case _ => // noop
      }
    case s: DataSnapshot =>
      handleSnapshot(s)
    case e: FirebaseError =>
      logger.error("got firebase error " + e)
    case m: Any =>
      logger.warn(s"NestActor got unknown msg: $m (${m.getClass.getName})")
  }

}