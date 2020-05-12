package me.hydos.fukkitdevelopment;

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.CARET
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.*
import com.intellij.psi.util.elementType

class GloomFieldAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        print(1)
        val project = e.project ?: return
        val file = e.getData(PSI_FILE) ?: return
        val caret = e.getData(CARET) ?: return

        val element = file.findElementAt(caret.offset) ?: return
        val member = element.findReferencedMember() ?: return

        val targetReference = when (member) {
            is PsiMethod -> member.qualifiedMemberReference
            is PsiField -> member.qualifiedMemberReference
            else -> return
        }

        WindowManager.getInstance().getStatusBar(project).info = "Adding $targetReference to Gloom fields"
        println(file.parent)
        println(file)

        val methodName = element.text;
        val methodDescriptor = toString(targetReference);

        println(methodName)
        println(methodDescriptor)
    }

    private fun toString(reference: MemberReference): String {
        val builder = StringBuilder()
        if (reference.owner != null) {
            builder.append('L').append(reference.owner.replace('.', '/')).append(';')
        }
        return builder.toString()
    }

}