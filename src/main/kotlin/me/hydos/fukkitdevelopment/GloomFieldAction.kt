package me.hydos.fukkitdevelopment

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.github.fukkitmc.gloom.DefinitionSerializer
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.JButton


class GloomFieldAction : AnAction() {
    /**
     * Implement this method to provide your action handler.
     *
     * @param e Carries information on the invocation place
     */
    override fun actionPerformed(e: AnActionEvent) {

        val definitions = DefinitionSerializer.fromString(File("E:/dev/fabric/mods/LegacyFukkit/definitions/fields.json").readText());
        val testButton = JButton()
        testButton.addActionListener { actionEvent: ActionEvent? ->
            if (FieldTextBox().showAndGet()) {

            }
        }


    }

}