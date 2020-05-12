package me.hydos.fukkitdevelopment

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.stream
import java.io.Serializable
import java.lang.reflect.Type
import java.util.stream.Stream

/**
 * Represents a reference to a class member (a method or a field). It may
 * resolve to multiple members if [matchAll] is set or if the member is
 * not full qualified.
 */
data class MemberReference(
    val name: String,
    val descriptor: String? = null,
    val owner: String? = null,
    val matchAll: Boolean = false
) : Serializable {

    val qualified
        get() = this.owner != null

    val withoutOwner
        get() = if (this.owner == null) this else MemberReference(this.name, this.descriptor, null, this.matchAll)

    fun matchOwner(psiClass: PsiClass): Boolean {
        return this.owner == null || this.owner == psiClass.fullQualifiedName
    }

    fun match(method: PsiMethod, qualifier: PsiClass): Boolean {
        return this.name == method.internalName && matchOwner(qualifier) &&
                (this.descriptor == null || this.descriptor == method.descriptor)
    }

    fun match(field: PsiField, qualifier: PsiClass): Boolean {
        return this.name == field.name && matchOwner(qualifier) &&
                (this.descriptor == null || this.descriptor == field.descriptor)
    }

    fun resolve(
        project: Project,
        scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
    ): Pair<PsiClass, PsiMember>? {
        return resolve(project, scope, ::Pair)
    }

    fun resolveMember(project: Project, scope: GlobalSearchScope = GlobalSearchScope.allScope(project)): PsiMember? {
        return resolve(project, scope) { _, member -> member }
    }

    private inline fun <R> resolve(project: Project, scope: GlobalSearchScope, ret: (PsiClass, PsiMember) -> R): R? {
        if (this.owner == null) {
            throw IllegalStateException("Cannot resolve unqualified member reference (owner == null)")
        }

        val psiClass = findQualifiedClass(project, this.owner, scope) ?: return null

        val member: PsiMember? = if (descriptor != null && descriptor.startsWith('(')) {
            // Method, we assume there is only one (since this member descriptor is full qualified)
            psiClass.findMethods(this, checkBases = true).findAny().orElse(null)
        } else {
            // Field
            psiClass.findField(this, checkBases = true)
        }

        return member?.let { ret(psiClass, member) }
    }

    object Deserializer : JsonDeserializer<MemberReference> {
        override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): MemberReference {
            val ref = json.asString
            val className = ref.substringBefore('#')
            val methodName = ref.substring(className.length + 1, ref.indexOf("("))
            val methodDesc = ref.substring(className.length + methodName.length + 1)
            return MemberReference(methodName, methodDesc, className)
        }
    }
}

// Class

fun PsiClass.findMethods(member: MemberReference, checkBases: Boolean = false): Stream<PsiMethod> {
    if (!member.matchOwner(this)) {
        return Stream.empty()
    }

    val result = findMethodsByInternalName(member.name, checkBases)
    return if (member.descriptor != null) {
        result.stream().filter { it.descriptor == member.descriptor }
    } else {
        result.stream()
    }
}

fun PsiClass.findField(member: MemberReference, checkBases: Boolean = false): PsiField? {
    if (!member.matchOwner(this)) {
        return null
    }

    val field = findFieldByName(member.name, checkBases) ?: return null
    if (member.descriptor != null && member.descriptor != field.descriptor) {
        return null
    }

    return field
}

// Method

val PsiMethod.memberReference
    get() = MemberReference(internalName, descriptor)

val PsiMethod.qualifiedMemberReference
    get() = MemberReference(internalName, descriptor, containingClass!!.fullQualifiedName)

fun PsiMethod.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return MemberReference(internalName, descriptor, owner.fullQualifiedName)
}

fun PsiMethod?.isSameReference(reference: PsiMethod?): Boolean =
    this != null && (this === reference || qualifiedMemberReference == reference?.qualifiedMemberReference)

// Field
val PsiField.simpleMemberReference
    get() = MemberReference(name)

val PsiField.memberReference
    get() = MemberReference(name, descriptor)

val PsiField.simpleQualifiedMemberReference
    get() = MemberReference(name, null, containingClass!!.fullQualifiedName)

val PsiField.qualifiedMemberReference
    get() = MemberReference(name, descriptor, containingClass!!.fullQualifiedName)

fun PsiField.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return MemberReference(name, descriptor, owner.fullQualifiedName)
}