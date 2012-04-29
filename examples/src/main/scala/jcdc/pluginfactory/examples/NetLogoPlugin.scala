package jcdc.pluginfactory.examples

import jcdc.pluginfactory.{NPCPlugin, CommandsPlugin}
import org.nlogo.headless.HeadlessWorkspace
import org.bukkit.entity.Player
import ch.spacebase.npccreatures.npcs.entity.NPC
import org.nlogo.agent.{Patch, Turtle}
import org.bukkit.{Material, World, Location}
import Material._

class NetLogoPlugin extends CommandsPlugin with NPCPlugin {

  /**
   * This map holds the state of the NetLogo world on the last tick (or the last call to go).
   * It gets populated during nl:setup, and then gets updated after each call to go.
   * The Long key is the turtle id.
   */
  val entities = collection.mutable.Map[Long, NPC]()
  var workspace: Option[HeadlessWorkspace] = None

  val commands = List(
    Command("open", "Open a model", args(existingFile) { case p ~ model =>
      // implicitly start over.
      dispose()
      workspace = Some(HeadlessWorkspace.newInstance)
      // model must be a file on the server....
      // maybe it could somehow be a url that we pull down?
      workspace.foreach(_.open(model.getAbsolutePath))
      p ! ("loaded " + model)
    }),
    Command("setup", "Call the setup proc.",    noArgs { callProc(_, "setup") }),
    Command("go",    "Call the go proc once.",  noArgs { callProc(_, "go") }),
    Command("call",  "Call a NetLogo proc.",    args(anyString+) {
      case p ~ proc => callProc(p, proc.mkString(" "))
    }),
    Command("loop",  "Call go until it is finished.", args(num ~ opt(long)) {
      case p ~ (n ~ sleepTime) =>
        new Thread(new Runnable() { def run() {
          for (_ <- 0 to n) { callProc(p, "go"); sleepTime.foreach(Thread.sleep) }
          p ! ("looped " + n + " times.")
        }}).start()
      }),
    Command("dispose", "Start over.", noArgs{ p =>
      // todo: what if we are running the go loop in a new thread here?
      // we probably should shut it down...
      dispose()
    }),
    Command("report", "Report something...", args(anyString+){ case p ~ reporter =>
      usingWorkspace(p)(ws => p ! (ws.report(reporter.mkString(" ")).toString))
    }),
    Command("count-entities", "Show the number of entities", noArgs{ p =>
      p ! (entities.size + " entities.")
    })
  )

  def callProc(p: Player, proc:String) = usingWorkspace(p)(_.command(proc))

  def usingWorkspace(p: Player)(f: HeadlessWorkspace => Unit) =
    workspace.fold(p ! "call open first!"){ ws => f(ws); update(p, ws) }

  def dispose(){
    entities.values.foreach(despawn)
    entities.clear()
    workspace.foreach(_.dispose())
    workspace = None
  }

  // update the turtles map so we know who was alive last tick.
  // todo: this might be able to be folded into usingWorkspace. nbd though.
  def update(p:Player, ws: HeadlessWorkspace): Unit = {
    val turtles: Map[Long, Turtle] =
      ws.world.turtles.toLogoList.scalaIterator.collect{case t: Turtle => t}.map(t => (t.id, t)).toMap

    turtles.foreach{ case (id, t) =>
      val loc = p.world(t.xcor, 4d, t.ycor).loc
      // if new turtles are born, create them.
      if (! entities.contains(id)) entities += (id -> npc(id, loc, t.getBreed.printName))
      // update the positions of existing turtles
      else entities(id).teleport(loc)
    }
    // if turtles die, kill them
    val deadTurtles = entities.filter{ case (id, _) => ! turtles.contains(id) }
    deadTurtles.foreach{ case (id, npc) => despawn(npc); entities.remove(id) }

    // a hack for wolf-sheep that might be useful later in other models.
    val patches: Iterator[Patch] = ws.world.patches.toLogoList.scalaIterator.collect{case p: Patch => p}
    patches.foreach(updatePatchColorMaybe(_, p.world))
  }

  def npc(id:Long, loc: Location, breed: String) = (breed.toLowerCase match {
    case "wolves" => wolf  _
    case "sheep"  => sheep _
    case _        => human _
  })(id.toString, loc)

  val colors = Map(
    // brown
    35d -> DIRT,
    // green
    55d -> GRASS
  )

  def updatePatchColorMaybe(patch: Patch, world: World): Unit = {
    colors.get(patch.pcolorDouble).foreach{ m =>
      val block = world(patch.pxcor, 3, patch.pycor)
      if (! (block is m)) block changeTo m
    }
  }
}
