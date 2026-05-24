package org.duangsuse.bin.type

typealias Idx = Int
typealias IdxRange = IntRange
typealias Cnt = Long // 修改为 Long 以适配 kotlinx.io
typealias Producer<R> = () -> R