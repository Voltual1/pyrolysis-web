package dev.rushii.arsc.builders

import dev.rushii.arsc.*

public fun ArscPackage.type(name: ArscTypeName, block: ArscType.() -> Unit): ArscType {
	val type = types.computeIfAbsent(name) {
		ArscType(
			id = highestTypeId() + 1U,
			name = name,
			configs = mutableListOf(),
			specs = null,
		)
	}

	block(type)

	if (type.specs == null) throw IllegalArgumentException("No specs defined")
	if (type.name.isEmpty()) throw IllegalArgumentException("Empty name")
	return type
}

public fun ArscType.spec(id: UInt? = null, block: ArscSpecs.Spec.() -> Unit): ArscSpecs.Spec {
	val specsObj = specs ?: ArscSpecs(0u, mutableMapOf()).also { specs = it }
	val targetId = (id ?: specsObj.highestSpecId()) + 1U
	val spec = specsObj.specs.computeIfAbsent(targetId) {
		ArscSpecs.Spec(id = targetId, flags = 0u)
	}
	block(spec)
	return spec
}