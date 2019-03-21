package info.maaskant.wmsnotes.client.synchronization.strategy.merge

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.ManualMergeStrategy.ConflictData
import info.maaskant.wmsnotes.client.synchronization.strategy.merge.MergeStrategy.MergeResult.Solution
import info.maaskant.wmsnotes.utilities.serialization.KryoSerializer
import info.maaskant.wmsnotes.utilities.serialization.readMapWithNullableValues
import info.maaskant.wmsnotes.utilities.serialization.writeMapWithNullableValues
import javax.inject.Inject

data class ManualMergeStrategyState internal constructor(
        val conflicts: Map<String, ConflictData?>,
        val solutions: Map<String, Solution?>
) {
    fun addConflict(aggId: String, conflictData: ConflictData) =
            this.copy(conflicts = conflicts + (aggId to conflictData))

    fun addSolution(aggId: String, solution: Solution) =
            this.copy(solutions = solutions + (aggId to solution))

    fun removeConflict(aggId: String) =
            this.copy(conflicts = conflicts - aggId)

    fun removeSolution(aggId: String) =
            this.copy(solutions = solutions - aggId)

    companion object {
        fun create(
                conflicts: Map<String, ConflictData?> = emptyMap(),
                solutions: Map<String, Solution?> = emptyMap()
        ) = ManualMergeStrategyState(
                conflicts = conflicts.withDefault { null },
                solutions = solutions.withDefault { null }
        )
    }
}

class KryoManualMergeStrategyStateSerializer @Inject constructor(kryoPool: Pool<Kryo>) : KryoSerializer<ManualMergeStrategyState>(
        kryoPool,
        Registration(ManualMergeStrategyState::class.java, KryoManualMergeStrategyStateSerializer(), 61)
) {

    private class KryoManualMergeStrategyStateSerializer : Serializer<ManualMergeStrategyState>() {
        override fun write(kryo: Kryo, output: Output, it: ManualMergeStrategyState) {
            output.writeMapWithNullableValues(it.conflicts) { key, value ->
                output.writeString(key)
                kryo.writeObject(output, value)
            }
            output.writeMapWithNullableValues(it.solutions) { key, value ->
                output.writeString(key)
                kryo.writeObject(output, value)
            }
            TODO()
        }

        override fun read(kryo: Kryo, input: Input, clazz: Class<out ManualMergeStrategyState>): ManualMergeStrategyState {
            val conflicts = input.readMapWithNullableValues<String, ConflictData?> {
                input.readString() to kryo.readObject(input, ConflictData::class.java)
            }
            val solutions = input.readMapWithNullableValues<String, Solution?> {
                input.readString() to kryo.readObject(input, Solution::class.java)
            }
            TODO()
            return ManualMergeStrategyState.create(
                    conflicts = conflicts,
                    solutions = solutions
            )
        }
    }
}
