package therealfarfetchd.quacklib.common.api.wires

import mcmultipart.api.container.IMultipartContainer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import therealfarfetchd.quacklib.client.api.render.wires.EnumWireRender
import therealfarfetchd.quacklib.common.api.block.capability.Capabilities
import therealfarfetchd.quacklib.common.api.block.capability.IConnectable
import therealfarfetchd.quacklib.common.api.extensions.isServer
import therealfarfetchd.quacklib.common.api.extensions.nibbles
import therealfarfetchd.quacklib.common.api.extensions.unpackNibbles
import therealfarfetchd.quacklib.common.api.qblock.IQBlockMultipart
import therealfarfetchd.quacklib.common.api.qblock.QBlock
import therealfarfetchd.quacklib.common.api.util.EnumFaceLocation
import therealfarfetchd.quacklib.common.api.util.WireCollisionHelper
import therealfarfetchd.quacklib.common.api.wires.EnumWireConnection.*

interface BaseConnectable {

  var connections: Map<EnumFaceLocation, EnumWireConnection>

  fun updateCableConnections(): Boolean {
    var changed = false
    if (b.world.isServer) {
      val oldconn = connections
      connections = validEdges.map { edge ->
        edge to (Internal.takeIf { edge.side != null && checkBlock(b.pos, edge, edge.side, edge.base, it) } ?:
                 External.takeIf { checkBlock(b.pos.offset(edge.base), edge, edge.base, edge.side, it) } ?:
                 Corner.takeIf { edge.side != null && checkBlock(b.pos.offset(edge.base).offset(edge.side), edge, edge.side, edge.base.opposite, it) } ?:
                 None)
      }.toMap()
      if (connections != oldconn) {
        b.dataChanged()
        changed = true
      }
    }
    return changed
  }

  fun getNeighbor(l: EnumFaceLocation): Any? {
    return when (connections[l]) {
      Internal -> if (l.side != null) getBlock(b.pos, l.side, l.base) else null
      External -> getBlock(b.pos.offset(l.base), l.base, l.side)
      Corner -> if (l.side != null) getBlock(b.pos.offset(l.base).offset(l.side), l.side, l.base.opposite) else null
      else -> null
    }
  }

  private fun checkBlock(pos: BlockPos, e: EnumFaceLocation, f1: EnumFacing, f2: EnumFacing?, c: EnumWireConnection): Boolean {
    if (c == Corner && WireCollisionHelper.collides(b.world, b.pos.offset(e.base), e)) return false
    if (c == Internal && b.actualWorld.getTileEntity(b.pos) !is IMultipartContainer) return false
    val cap: IConnectable = b.actualWorld.getTileEntity(pos)?.getCapability(Capabilities.Connectable, f1.opposite) ?: return false
    val localCap: IConnectable = b.getCapability(Capabilities.Connectable, e.base) ?: return false
    cap.getEdge(f2) ?: return false
    localCap.getEdge(e.side) ?: return false
    return cap.getType(f2) == localCap.getType(e.side) && (c != Corner || (cap.allowCornerConnections(f2) || localCap.allowCornerConnections(e.side)))
  }

  private fun getBlock(pos: BlockPos, f1: EnumFacing, f2: EnumFacing?): Any? {
    val cap: IConnectable = b.actualWorld.getTileEntity(pos)?.getCapability(Capabilities.Connectable, f1.opposite) ?: return false
    cap.getEdge(f2) ?: return null
    return cap.getEdge(f2)
  }

  fun serializeConnections(): List<Byte> {
    var list: List<Int> = emptyList()
    for ((a, b) in connections.filterValues { it.renderType != EnumWireRender.Invisible }) {
      list += a.base.index
      list += a.side?.index ?: 6
      list += b.identifierId
    }
    return list.nibbles()
  }

  fun deserializeConnections(list: List<Byte>) {
    var l = list.unpackNibbles()
    connections = emptyMap()
    while (l.size >= 3) {
      val a1 = EnumFacing.getFront(l[0])
      val a2 = if (l[1] == 6) null else EnumFacing.getFront(l[1])
      val b = EnumWireConnection.byIdentifier(l[2])
      connections += EnumFaceLocation.fromFaces(a1, a2) to b
      l = l.slice(3 until l.size)
    }
  }

  val validEdges: Set<EnumFaceLocation>
    get() = EnumFaceLocation.Values.filter { b.getCapability(Capabilities.Connectable, it.base)?.getEdge(it.side) != null }.toSet()

}

private val BaseConnectable.b: QBlock
  get() = this as QBlock

private val QBlock.actualWorld: World
  get() = if (this is IQBlockMultipart) actualWorld else world