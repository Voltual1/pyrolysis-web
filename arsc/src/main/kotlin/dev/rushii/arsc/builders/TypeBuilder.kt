package dev.rushii.arsc.builders

import dev.rushii.arsc.*

public fun ArscPackage.type(name: ArscTypeName, block: ArscType.() -> Unit): ArscType {
	// 使用 this@type 明确指向 ArscPackage 实例
	val type = types.getOrPut(name) {
		ArscType(
			id = this.highestTypeId() + 1U,
			name = name,
			configs = mutableListOf(),
			specs = null,
		)
	}

	block(type)

	if (type.specs == null)
		throw IllegalArgumentException("No specs defined for new type")
	if (type.name.isEmpty())
		throw IllegalArgumentException("Type cannot have an empty name")

	return type
}

public fun ArscType.spec(id: UInt? = null, block: ArscSpecs.Spec.() -> Unit): ArscSpecs.Spec {
	val currentSpecs = specs ?: ArscSpecs(0u, mutableMapOf()).also { specs = it }
	val targetId = id ?: (currentSpecs.highestSpecId() + 1U)

	val spec = currentSpecs.specs.getOrPut(targetId) {
		ArscSpecs.Spec(id = targetId, flags = 0u)
	}

	block(spec)
	return spec
}