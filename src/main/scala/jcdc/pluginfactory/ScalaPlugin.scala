package jcdc.pluginfactory

import java.util.logging.Logger
import org.bukkit.entity.Player
import org.bukkit.event.{Listener, Event}
import org.bukkit.event.entity.{EntityDamageEvent, EntityDamageByEntityEvent, EntityListener}
import org.bukkit.command.{Command, CommandSender}
import org.bukkit.block.Block
import org.bukkit.{ChatColor, Location}
import ChatColor._

object ScalaPluginPredef {
  val log = Logger.getLogger("Minecraft")

  implicit def pimpedBlock(b:Block) = new PimpedBlock(b)
  implicit def pimpedPlayer(player:Player) = new PimpedPlayer(player)

  case class PimpedBlock(b:Block) {
    def blocksAbove: Stream[Block] = {
      val ba = new Location(b.getWorld, b.getX.toDouble, b.getY.toDouble + 1, b.getZ.toDouble).getBlock
      ba #:: ba.blocksAbove
    }
  }

  case class PimpedPlayer(player:Player){
    def x = player.getLocation.getX
    def y = player.getLocation.getY
    def z = player.getLocation.getZ

    def messageAfter[T](message: => String)(f: => T): T = {
      val t = f
      player.sendMessage(message)
      t
    }
    def messageBefore[T](message:String)(f: => T): T = {
      player.sendMessage(message)
      f
    }
    def messageAround[T](beforeMessage:String, afterMessage: => String)(f: => T): T = {
      player.sendMessage(beforeMessage)
      val t = f
      player.sendMessage(afterMessage)
      t
    }
    def sendError(message:String) = player.sendMessage(RED + message)
    def sendUsage(cmd:Command) = sendError(cmd.getUsage)

    def findPlayer(name:String)(f: Player => Unit) = Option(player.getServer.getPlayer(name)) match {
      case Some(p) => f(p)
      case None => sendError("kill could not find player: " + name)
    }
    def ban(reason:String){ player.setBanned(true); player.kickPlayer("banned: " + reason) }
  }
}

import ScalaPluginPredef._

class ScalaPlugin extends org.bukkit.plugin.java.JavaPlugin {
  def name = this.getDescription.getName

  // setup stuff
  def onEnable(){
    logInfo("enabled!")
    setupDatabase()
  }
  def onDisable(){ logInfo("disabled!") }
  def registerListener(eventType:Event.Type, listener:Listener){
    this.getServer.getPluginManager.registerEvent(eventType, listener, Event.Priority.Normal, this)
  }

  // logging
  def logInfo(message:String) { log.info("["+name+"] - " + message) }
  def logInfoAround[T](beforeMessage:String, afterMessage:String)(f: => T): T = {
    logInfo(beforeMessage)
    val t = f
    logInfo(afterMessage)
    t
  }
  def logError(e:Throwable){
    log.log(java.util.logging.Level.SEVERE, "["+name+"] - " + e.getMessage, e)
  }

  // db setup stuff.
  def dbClasses: List[Class[_]] = Nil
  override def getDatabaseClasses = new java.util.ArrayList[Class[_]](){ dbClasses.foreach(add) }
  def setupDatabase(){
    if(dbClasses.nonEmpty)
      try getDatabase.find(dbClasses.head).findRowCount
      catch{
        case e: javax.persistence.PersistenceException =>
          logInfo("Installing database due to first time usage")
          installDDL()
      }
  }
  // db commands
  def dbInsert[A](a:A) = try {
    logInfo("about to insert: " + a)
    getDatabase.insert(a)
    logInfo("inserted: " + a)
  } catch { case e => logError(e) }
  def dbQuery[T](c:Class[T]) = getDatabase.find[T](c)
  def findAll[T](c:Class[T]) = dbQuery[T](c).findList
  def dbDelete(a:AnyRef){ getDatabase.delete(a) }
}

trait MultiListenerPlugin extends ScalaPlugin {
  val listeners:List[(Event.Type, Listener)]
  override def onEnable(){ super.onEnable(); listeners.foreach((registerListener _).tupled) }
}

trait ListenerPlugin extends ScalaPlugin {
  val eventType:Event.Type; val listener:Listener
  override def onEnable(){ super.onEnable(); registerListener(eventType, listener) }
}
case class VanillaListenerPlugin(eventType:Event.Type, listener:Listener) extends ListenerPlugin

trait SingleCommandPlugin extends ScalaPlugin {
  val command: String
  val commandHandler: CommandHandler
  override def onCommand(sender:CommandSender, cmd:Command, commandLabel:String, args:Array[String]) = {
    if(cmd.getName.equalsIgnoreCase(command)) commandHandler.handle(sender.asInstanceOf[Player], cmd, args)
    true
  }
}

trait CommandHandler {
  def handle(player: Player, cmd:Command, args:Array[String])
}

trait OpOnly extends CommandHandler{
  abstract override def handle(player: Player, cmd: Command, args: Array[String]) = {
    if(player.isOp) super.handle(player, cmd, args)
    else player.sendMessage(RED + "Nice try. You must be an op to run /" + cmd.getName)
  }
}

trait Args extends CommandHandler{
  val argsCount: Int
  abstract override def handle(player: Player, cmd: Command, args: Array[String]) = {
    if(args.length >= argsCount) super.handle(player, cmd, args) else player.sendUsage(cmd)
  }
}

trait OneArg extends Args { val argsCount = 1 }

trait PlayerToPlayerCommand extends CommandHandler {
  def handle(sender: Player, cmd: Command, args: Array[String]) {
    sender.findPlayer(args(0)) { receiver => handle(sender, receiver, cmd, args) }
  }
  def handle(sender:Player, receiver:Player, cmd: Command, args: Array[String])
}


trait ManyCommandsPlugin extends ScalaPlugin {
  val commands: Map[String, CommandHandler]
  private def lowers: Map[String, CommandHandler] = commands.map{ case (k,v) => (k.toLowerCase, v)}
  override def onEnable(){
    super.onEnable()
    lowers.keys.foreach{ k => logInfo("["+name+"] command: " + k) }
  }
  override def onCommand(sender:CommandSender, cmd:Command, commandLabel:String, args:Array[String]) = {
    lowers.get(cmd.getName.toLowerCase).foreach(_.handle(sender.asInstanceOf[Player], cmd, args))
    true
  }
}

trait EntityDamageByEntityListener extends EntityListener {
  override def onEntityDamage(event:EntityDamageEvent){
    if(event.isInstanceOf[EntityDamageByEntityEvent])
      onEntityDamageByEntity(event.asInstanceOf[EntityDamageByEntityEvent])
  }
  def onEntityDamageByEntity(e:EntityDamageByEntityEvent)
}

trait PlayerDamageByEntityListener extends EntityListener {
  override def onEntityDamage(event:EntityDamageEvent){
    if(event.isInstanceOf[EntityDamageByEntityEvent] && event.getEntity.isInstanceOf[Player])
      onPlayerDamageByEntity(event.getEntity.asInstanceOf[Player], event.asInstanceOf[EntityDamageByEntityEvent])
  }
  def onPlayerDamageByEntity(p:Player, e:EntityDamageByEntityEvent)
}

trait PlayerDamageListener extends EntityListener {
  override def onEntityDamage(e:EntityDamageEvent){
    if(e.getEntity.isInstanceOf[Player]) onPlayerDamage(e.getEntity.asInstanceOf[Player], e)
  }
  def onPlayerDamage(p:Player, e:EntityDamageEvent)
}