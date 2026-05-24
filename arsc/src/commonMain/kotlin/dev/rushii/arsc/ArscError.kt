package dev.rushii.arsc

public class ArscError(position: Int, value: Any?, message: String) :
	Error("Failed to parse arsc at index $position, value $value: $message")