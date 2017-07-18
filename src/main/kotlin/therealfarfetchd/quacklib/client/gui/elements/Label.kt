package therealfarfetchd.quacklib.client.gui.elements

import net.minecraft.client.gui.Gui
import net.minecraft.client.resources.I18n
import therealfarfetchd.quacklib.client.gui.GuiElement
import therealfarfetchd.quacklib.client.gui.mapper
import therealfarfetchd.quacklib.client.gui.number

/**
 * Created by marco on 17.07.17.
 */
class Label : GuiElement() {

  var value: String by mapper()
  var shadow: Boolean by mapper()
  var background: Boolean by mapper()
  var r: Float by number()
  var g: Float by number()
  var b: Float by number()

  init {
    value = ""
    shadow = true
    background = true
    r = 1F
    g = 1F
    b = 1F
    height = mc.fontRenderer.FONT_HEIGHT + 4
  }

  override fun render(mouseX: Int, mouseY: Int) {
    var text = value
    if (value.startsWith("#") && I18n.hasKey(value.substring(1))) {
      text = I18n.format(value.substring(1))
    }
    width = mc.fontRenderer.getStringWidth(text) + 4
    val color = 0xFF000000.toInt() or
        ((r * 255).toInt() shl 16) or
        ((g * 255).toInt() shl 8) or
        ((g * 255).toInt())
    if (background) Gui.drawRect(0, 0, width, height, 0x70707070)
    mc.fontRenderer.drawString(text, 2F, 2F, color, shadow)
  }
}