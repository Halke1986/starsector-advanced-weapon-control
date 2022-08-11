package com.dp.advancedgunnerycontrol.combatgui

import com.dp.advancedgunnerycontrol.combatgui.buttons.ActionButton
import com.dp.advancedgunnerycontrol.combatgui.buttons.ButtonAction
import com.dp.advancedgunnerycontrol.combatgui.buttons.ButtonInfo
import com.dp.advancedgunnerycontrol.combatgui.buttons.HoverTooltip
import com.dp.advancedgunnerycontrol.gui.groupAsString
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.*
import com.dp.advancedgunnerycontrol.utils.*
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import org.lazywizard.lazylib.ui.LazyFont
import java.awt.Color

class GuiLayout(private val ship: ShipAPI, private val font: LazyFont) {
    companion object{
        private const val xSpacing = 105f
        private const val ySpacing = 50f
        private val xTooltip = Settings.uiMessagePositionX() * Global.getSettings().screenWidthPixels
        private val yTooltip = Settings.uiMessagePositionY() * Global.getSettings().screenHeightPixels
        private val xAnchor = Settings.uiAnchorX() * Global.getSettings().screenWidthPixels
        private val yAnchor = Settings.uiAnchorY() * Global.getSettings().screenHeightPixels
        private var storageIndex = Values.storageIndex
        private val color = Color.GREEN

    }

    private fun createButtonInfo(xIndex: Int, txt: String, tooltipTxt: String) : ButtonInfo{
        return ButtonInfo(
            xAnchor + xIndex * xSpacing, yAnchor + ySpacing,
            100f, 20f, 0.5f, txt, font, color, HoverTooltip(
                xTooltip, yTooltip, tooltipTxt))
    }

    @Suppress("UNUSED_CHANGED_VALUE")
    private fun createActionButtons() : List<ActionButton>{
        var xIndex = 0
        val resetButtonInfo = createButtonInfo(xIndex++, "Reset", "Reset all tags for current ship and loadout")
        val resetButtonAction = object : ButtonAction() {
            override fun execute() {
                val noTags = listOf<String>()
                for (i in 0 until ship.weaponGroupsCopy.size){
                    applyTagsToWeaponGroup(ship, i, noTags)
                    saveTags(ship, i, storageIndex, noTags)
                }
                saveShipModes(ship, storageIndex, noTags)
                assignShipMode(noTags, ship)
                refreshButtons()
            }
        }
        val cycleLoadoutButtonInfo = createButtonInfo(xIndex++, "Cycle LO",
            "Cycle loadout for all ships (${storageIndex + 1} / ${Settings.maxLoadouts()} " +
                "<${Settings.loadoutNames().getOrNull(storageIndex) ?: "NoName"}>)")
        val cycleLoadoutAction = object : ButtonAction() {
            override fun execute() {
                storageIndex = if (storageIndex < Settings.maxLoadouts() - 1) storageIndex + 1 else 0
                cycleLoadoutButtonInfo.tooltip.txt = "Cycle loadout for all ships (${storageIndex + 1} / ${Settings.maxLoadouts()} " +
                        "<${Settings.loadoutNames().getOrNull(storageIndex) ?: "NoName"}>)"
                refreshButtons()
                reloadAllShips(storageIndex)
            }

        }


        val reloadTooltip = createButtonInfo(xIndex++, "Reload",
        "Reload all modes and apply them to deployed ships. Normally, you shouldn't have to use this button manually.")
        val reloadAction = object : ButtonAction(){
            override fun execute() {
                reloadAllShips(storageIndex)
            }
        }
        val saveTooltip = createButtonInfo(xIndex++, "Save", "Make all temporary changes to current loadout done in combat permanent." +
                "\nNote: Only relevant if in-combat persistence has been disabled in the settings.")
        val saveAction = object  : ButtonAction(){
            override fun execute() {
                persistTemporaryShipData(storageIndex, Global.getCombatEngine().ships ?: listOf())
            }
        }
        val helpTooltip = createButtonInfo(xIndex++, "Help", Values.HELP_TEXT)

        val showTagsTooltip = createButtonInfo(xIndex++, "Show Tags", "Click me to update!" +
                "\nThis will show all tags applied to weapon groups as a tooltip for this button." +
                "\nUse this to check if there are tags enabled that don't have a corresponding button.")
        val showTagsAction = object : ButtonAction(){
            override fun execute() {
                var newTooltip = "Click me to update!"
                weaponButtonGroups.forEachIndexed { i, _ ->
                    newTooltip += "\nGroup ${i+1}: "
                    newTooltip += loadTags(ship, i, storageIndex).toString()
                }
                showTagsTooltip.tooltip.txt = newTooltip
            }
        }

        val resetButton = ActionButton(resetButtonAction, resetButtonInfo)
        val cycleLoadoutButton = ActionButton(cycleLoadoutAction, cycleLoadoutButtonInfo)
        val helpButton = ActionButton(null, helpTooltip)
        helpButton.isDisabled = true
        val reloadButton = ActionButton(reloadAction, reloadTooltip)
        val saveButton = ActionButton(saveAction, saveTooltip)
        if(Settings.enableCombatChangePersistance()){
            saveButton.isDisabled = true
        }
        val showTagsButton = ActionButton(showTagsAction, showTagsTooltip)
        return listOf(resetButton, cycleLoadoutButton, helpButton, reloadButton, saveButton, showTagsButton)
    }

    private fun createWeaponGroupAction(ship: ShipAPI, index: Int): ButtonGroupAction {
        return object : ButtonGroupAction() {
            override fun execute(data: List<Any>, triggeringButtonData: Any?) {
                val tagStrings = data.filterIsInstance<String>()
                applyTagsToWeaponGroup(ship, index, tagStrings)
                saveTags(ship, index, storageIndex, tagStrings)
            }

        }
    }

    private fun createShipAiBtnGroupAction(ship: ShipAPI) : ButtonGroupAction{
        return object  : ButtonGroupAction(){
            override fun execute(data: List<Any>, triggeringButtonData: Any?) {
                var tags = data.filterIsInstance<String>()
                tags = if((defaultShipMode == triggeringButtonData) || tags.isEmpty()){
                    listOf(defaultShipMode)
                }else{
                    tags.filter { it != defaultShipMode }
                }
                assignShipMode(tags, ship)
                saveShipModes(ship, storageIndex, tags)
            }
        }
    }


    private fun createDescriptionText(index: Int): String {
        return "Group ${index + 1}: ${groupAsString(ship.fleetMember.variant.weaponGroups[index], ship.fleetMember)}"
    }

    private fun fetchCurrentWeaponTags(index: Int) : List<String>{
        return loadTags(ship, index, storageIndex)
    }

    private val shipText = font.createText("${ship.name}, ${ship.fleetMember.variant.fullDesignationWithHullNameForShip}", color)
    private val actionButtons = createActionButtons()
    private val weaponButtonGroups = List(ship.variant.weaponGroups.size) { index ->
        DataButtonGroup(
            xAnchor, yAnchor - index * ySpacing, 100f,
            20f, 0.5f, font, color, 5f, createWeaponGroupAction(ship, index), xTooltip, yTooltip, createDescriptionText(index)
        )
    }
    private val shipButtonGroup = DataButtonGroup(xAnchor,
        yAnchor - weaponButtonGroups.size * ySpacing,
    100f, 20f, 0.5f, font, color, 5f, createShipAiBtnGroupAction(ship), xTooltip, yTooltip, "Ship AI Modes")

    init {
        Settings.hotAddTags(loadAllTags(ship.fleetMember))
        weaponButtonGroups.forEachIndexed { index, buttonGroup ->
            val currentTags = fetchCurrentWeaponTags(index)
            Settings.tagList().forEach {
                buttonGroup.addButton(it, it, getTagTooltip(it), currentTags.contains(it))
            }
        }
        ShipModes.values().mapNotNull { shipModeToString[it] }.forEach {
            shipButtonGroup.addButton(it, it, detailedShipModeDescriptions[shipModeFromString[it]] ?: "")
        }
        refreshButtons()
    }

    private fun refreshButtons(){
        weaponButtonGroups.forEachIndexed { index, buttonGroup ->
            val currentTags = fetchCurrentWeaponTags(index)
            buttonGroup.refreshAllButtons(currentTags)
            buttonGroup.enableAllButtons()
            buttonGroup.buttons.forEach {
                val str = it.data as? String ?: ""
                if(isIncompatibleWithExistingTags(str, currentTags)){
                    it.isDisabled = true
                }
                if(true != ship.weaponGroupsCopy.getOrNull(index)?.weaponsCopy?.any { w -> createTag(str, w )?.isValid() == true }){
                    it.isDisabled = true
                }
            }
        }
        if(loadShipModes(ship, storageIndex).isEmpty()) saveShipModes(ship, storageIndex, listOf(defaultShipMode))
        shipButtonGroup.refreshAllButtons(loadShipModes(ship, storageIndex))
    }

    fun advance(){
        weaponButtonGroups.forEach { it.advance() }
        shipButtonGroup.advance()
        refreshButtons()
        actionButtons.forEach { it.advance() }
    }
    fun render(){
        weaponButtonGroups.forEach { it.render() }
        actionButtons.forEach { it.render() }
        shipButtonGroup.render()
        shipText.draw(xAnchor, yAnchor + (2 * ySpacing))
    }
}